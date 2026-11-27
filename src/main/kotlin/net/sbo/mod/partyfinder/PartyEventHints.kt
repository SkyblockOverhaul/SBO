package net.sbo.mod.partyfinder

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ItemStack
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.partyfinder.api.CategoryDef
import net.sbo.mod.partyfinder.api.PartyFinderApi
import net.sbo.mod.partyfinder.gui.PartyFinderGui
import net.sbo.mod.utils.TextUtil.removeFormatting
import net.sbo.mod.utils.HypixelModApi
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.data.DataManager
import net.sbo.mod.utils.data.configs.partyfinder.PartyFinderConfigState
import net.sbo.mod.utils.events.ClickActionManager
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.guis.GuiCloseEvent
import net.sbo.mod.utils.events.impl.guis.GuiMouseClickBefore
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.sbo.mod.utils.game.World
import java.util.concurrent.TimeUnit
import kotlin.reflect.KMutableProperty1

/** An event party type that is running, or starts soon when startsInMs is set. */
private data class PartyEventHint(val event: String, val partyType: String, val subType: String, val label: String, val startsInMs: Long?) {
    val key: String get() = "$partyType/$subType"
}

/** What the event reminders check about a player: whether they fish and the area they are in. */
private data class PartyHintPlayer(val fishing: Boolean, val area: String)

/** The kinds of reminders, each with its own setting. The label is the name used in chat messages. */
private enum class PartyHintKind(val label: String, private val setting: KMutableProperty1<PartyFinderConfigState, Boolean>) {
    EVENTS("fishing event", PartyFinderConfigState::eventHints),
    DRAGONS("dragon", PartyFinderConfigState::dragonHints),
    SAFARI("Safari", PartyFinderConfigState::safariHints),
    SCATHA("Scatha", PartyFinderConfigState::scathaHints);

    fun isOn(config: PartyFinderConfigState): Boolean = setting.get(config)

    fun turnOff(config: PartyFinderConfigState) = setting.set(config, false)
}

/** Something a player does alone that a party of partyKey does together, like placing a Summoning Eye. */
private data class PartyActivity(val kind: PartyHintKind, val partyKey: String, val question: String) {
    val partyType: String get() = partyKey.substringBefore('/')
    val subType: String get() = partyKey.substringAfter('/', "")
}

private object PartyEventRules {
    private const val FISHING = "fishing"
    // Areas that only players who want the event visit
    private val EVENT_AREAS = mapOf("winter" to "Jerry's Workshop")
    // Event names that can't be built from the category and subcategory names
    private val EVENT_NAMES = mapOf("fishingFestival" to "Fishing Festival")

    private val SOON_MS = TimeUnit.HOURS.toMillis(1)
    private val WORTH_JOINING_MS = TimeUnit.MINUTES.toMillis(15)
    private val REST_MS = TimeUnit.DAYS.toMillis(2)

    /** Each kind of reminder comes at most every 2 real days. A time in the future means the clock was wrong. */
    fun rested(kind: PartyHintKind, shownAt: Map<String, Long>, now: Long): Boolean {
        val last = shownAt[kind.name] ?: return true
        return now - last !in 0..<REST_MS
    }

    /**
     * Events that surely run for at least 15 more minutes or start within the hour. Parties can be listed for longer around holidays,
     * but Hypixel's bonus events there have no known times, so they get no reminder.
     */
    fun due(categories: List<CategoryDef>, now: Long): List<PartyEventHint> =
        categories.flatMap { category ->
            category.subcategories.mapNotNull { sub ->
                val event = sub.event ?: return@mapNotNull null
                val opensAt = sub.calendarOpensAt
                val closesAt = sub.calendarClosesAt
                val startsIn = when {
                    sub.calendarOpen -> if (closesAt != null && closesAt - now >= WORTH_JOINING_MS) null else return@mapNotNull null
                    // The definitions can be a few minutes old, so the start may already have passed
                    sub.createOpen == true && opensAt != null && opensAt - now <= SOON_MS -> (opensAt - now).takeIf { it > 0 }
                    else -> return@mapNotNull null
                }
                PartyEventHint(event, category.id, sub.id, EVENT_NAMES[event] ?: "${sub.label} ${category.label}", startsIn)
            }
        }

    fun interested(player: PartyHintPlayer, hint: PartyEventHint): Boolean =
        (hint.partyType == FISHING && player.fishing) ||
            EVENT_AREAS[hint.event]?.equals(player.area, ignoreCase = true) == true

    /** Skips the server request when nothing can make the player interested. */
    fun anyInterest(player: PartyHintPlayer): Boolean =
        player.fishing || EVENT_AREAS.values.any { it.equals(player.area, ignoreCase = true) }

    fun isRod(loreLines: List<String>): Boolean = loreLines.any { "FISHING ROD" in it }

    /** Armor with fishing stats. */
    fun isFishingGear(loreLines: List<String>): Boolean =
        loreLines.any { it.startsWith("Fishing Speed:") || it.startsWith("Sea Creature Chance:") }

    fun announcement(hint: PartyEventHint, parties: Int?): String {
        val state = hint.startsInMs?.let { ms ->
            val minutes = TimeUnit.MILLISECONDS.toMinutes(ms).coerceAtLeast(1)
            if (minutes == 1L) "starts in 1 minute" else "starts in $minutes minutes"
        } ?: "is running right now"
        return "${hint.label} $state.${listed(parties)}"
    }

    fun announcement(activity: PartyActivity, parties: Int?): String = "${activity.question}${listed(parties)}"

    private fun listed(parties: Int?): String = when (parties) {
        null -> ""
        1 -> " 1 party is queued."
        else -> " $parties parties are queued."
    }
}

/**
 * Tells players in chat when a party event starts, if they look like they want it and have time for it.
 * If a player doesn't look interested at first, the reminder comes later once they do.
 * Players who start something alone that others do in parties, like placing a Summoning Eye, also get a reminder for those parties.
 * Each kind of reminder is shown at most once every 2 days.
 */
object PartyEventHints {
    // Short, because a check only requests the party types from the server when the cached ones are outdated
    private const val CHECK_TICKS = 20 * 5
    private val FISHED_RECENTLY_MS = TimeUnit.MINUTES.toMillis(10)

    private val DRAGONS = PartyActivity(PartyHintKind.DRAGONS, "bestiary/dragons", "Looking for Dragon parties?")
    private val SAFARI = PartyActivity(PartyHintKind.SAFARI, "safari", "Looking for Safari parties?")
    private val SCATHA = PartyActivity(PartyHintKind.SCATHA, "mining/scatha", "Looking for Scatha parties?")
    // Hypixel says the same for a Worm and a Scatha, both only come to players digging tunnels for them
    private val WORM_SPAWN = Regex("^You hear the sound of something approaching\\.\\.\\.$")
    private const val SCATHA_AREA = "Crystal Hollows"
    private const val SAFARI_MENU = "Critter Safari Entry"
    private const val ENTER = "Click to enter!"
    private const val SAFARI_AREA = "Safari"

    // Set once the player started a run from the menu or opened the SBO Party Finder from it
    private var safariHandled = false

    // Hypixel can open the menu again right after it closed, so the reminder waits a second after it closes
    private val MENU_CLOSE_DELAY_MS = TimeUnit.SECONDS.toMillis(1)
    private val SAFARI_HINT_MAX_WAIT_MS = TimeUnit.MINUTES.toMillis(5)
    private var safariHintAt = 0L

    private var lastFishedAt = 0L
    // Diana players carry their spade all the time, only one in hand shows they dig right now
    private val DUG_RECENTLY_MS = TimeUnit.MINUTES.toMillis(5)
    private var lastSpadeAt = 0L

    // Joining SkyBlock fills the chat with Hypixel's own messages, an event hint in between would get lost
    private val JOIN_QUIET_MS = TimeUnit.SECONDS.toMillis(30)
    private var inSkyblockSince = 0L

    fun init() {
        Register.onTick(20) {
            val now = System.currentTimeMillis()
            if (!World.isInSkyblock()) inSkyblockSince = 0L else if (inSkyblockSince == 0L) inSkyblockSince = now
            if (World.isInSkyblock() && (holdsRod() || mc.player?.fishing != null)) lastFishedAt = now
            if (mc.player?.mainHandItem?.hoverName?.removeFormatting()?.endsWith(" Spade") == true) lastSpadeAt = now
            showSafariHint(now)
        }
        Register.onTick(CHECK_TICKS) { check() }
        Register.onChatMessage(Regex("^☬ You placed a Summoning Eye!"), noFormatting = true) { _, _ ->
            if (!PartyFinderManager.isInParty) offer(DRAGONS)
        }
        Register.onChatMessage(WORM_SPAWN, noFormatting = true) { _, _ ->
            if (!PartyFinderManager.isInParty && World.getWorld() == SCATHA_AREA) offer(SCATHA)
        }
        ScreenEvents.AFTER_INIT.register { _, screen, _, _ ->
            if (screen is AbstractContainerScreen<*> && screen.title.string == SAFARI_MENU) onSafariMenu(screen)
        }
    }

    private fun check() {
        val config = DataManager.partyFinderConfigState
        if (!PartyHintKind.EVENTS.isOn(config) || !World.isInSkyblock() || mc.screen != null || busy()) return
        val now = System.currentTimeMillis()
        if (inSkyblockSince == 0L || now - inSkyblockSince < JOIN_QUIET_MS) return
        if (!PartyEventRules.rested(PartyHintKind.EVENTS, config.hintsShownAt, now)) return
        val player = PartyHintPlayer(fishing(), World.getWorld())
        if (!PartyEventRules.anyInterest(player) || PartyCategories.failedRecently) return
        PartyCategories.get { data ->
            val categories = data?.categories ?: return@get
            // After a failed load these are the old ones, with events that may be over
            if (PartyCategories.failedRecently) return@get
            mc.execute {
                // Several checks can wait for the same categories, only the first may show a reminder
                val loadedAt = System.currentTimeMillis()
                if (!PartyEventRules.rested(PartyHintKind.EVENTS, config.hintsShownAt, loadedAt)) return@execute
                PartyEventRules.due(categories, loadedAt)
                    .firstOrNull { PartyEventRules.interested(player, it) && hasRoomFor(it.partyType, it.subType) }
                    ?.let { hint -> announce(PartyHintKind.EVENTS, hint.key) { parties -> PartyEventRules.announcement(hint, parties) } }
            }
        }
    }

    // A click on a ticket starts the run at once, so the menu gets a button and the chat message only comes when no run started
    private fun onSafariMenu(screen: AbstractContainerScreen<*>) {
        safariHandled = false
        // Also shown to party members, they may want to look at the parties too
        HypixelMenus.addPartyButton(
            screen, SAFARI.partyKey,
            "Opens the Safari parties of the SBO Party Finder, so you can find players before you use your ticket.",
            onUse = ::handledSafari
        ) { menu -> HypixelMenus.lines(menu).any { it.endsWith("Safari Experience") } }
    }

    @SboEvent
    fun onSafariClick(event: GuiMouseClickBefore) {
        val screen = event.screen as? AbstractContainerScreen<*> ?: return
        if (screen.title.string != SAFARI_MENU) return
        val slot = HypixelMenus.slotAt(screen, event.mouseX, event.mouseY) ?: return
        if (ENTER in HypixelMenus.loreOf(slot)) handledSafari()
    }

    // After the menu, so the message doesn't scroll away behind it. Without a ticket Hypixel shows no "Click to enter!" and no run can start.
    @SboEvent
    fun onSafariMenuClose(event: GuiCloseEvent) {
        val screen = event.screen as? AbstractContainerScreen<*> ?: return
        if (screen.title.string != SAFARI_MENU || safariHandled || PartyFinderManager.isInParty) return
        if (ENTER in HypixelMenus.lines(screen)) safariHintAt = System.currentTimeMillis() + MENU_CLOSE_DELAY_MS
    }

    private fun handledSafari() {
        safariHandled = true
        safariHintAt = 0L
    }

    private fun showSafariHint(now: Long) {
        if (safariHintAt == 0L) return
        // In the run it is too late for a party
        if (now - safariHintAt > SAFARI_HINT_MAX_WAIT_MS || PartyFinderManager.isInParty || World.getWorld() == SAFARI_AREA) {
            safariHintAt = 0L
            return
        }
        if (now < safariHintAt || !World.isInSkyblock() || mc.player == null || mc.screen != null) return
        safariHintAt = 0L
        offer(SAFARI)
    }

    private fun offer(activity: PartyActivity) {
        val config = DataManager.partyFinderConfigState
        if (!activity.kind.isOn(config) || PartyFinderManager.inQueue) return
        if (!PartyEventRules.rested(activity.kind, config.hintsShownAt, System.currentTimeMillis())) return
        PartyCategories.get { data ->
            val target = data?.let { PartyCategories.target(activity.partyType, activity.subType) } ?: return@get
            mc.execute {
                // Only players outside a party get here, so there is always room
                if (!target.open) return@execute
                if (!PartyEventRules.rested(activity.kind, config.hintsShownAt, System.currentTimeMillis())) return@execute
                announce(activity.kind, activity.partyKey) { parties -> PartyEventRules.announcement(activity, parties) }
            }
        }
    }

    // Instances, Safari runs and digging for Diana leave no time for it, a member can't list the party anyway
    private fun busy(): Boolean =
        HypixelModApi.inInstance ||
            World.getWorld() == SAFARI_AREA ||
            PartyFinderManager.inQueue ||
            (PartyFinderManager.isInParty && !PartyFinderManager.isLeader) ||
            (World.isInHub() && System.currentTimeMillis() - lastSpadeAt < DUG_RECENTLY_MS && !holdsRod())

    private fun hasRoomFor(partyType: String, subType: String): Boolean {
        val maxSize = PartyCategories.target(partyType, subType)?.maxSize ?: return true
        return PartyFinderManager.memberCount < maxSize
    }

    // The 2 day pause counts for the whole kind, while the message and its buttons are about the parties of key
    private fun announce(kind: PartyHintKind, key: String, text: (parties: Int?) -> String) {
        val config = DataManager.partyFinderConfigState
        config.hintsShownAt[kind.name] = System.currentTimeMillis()
        config.save()
        PartyFinderApi.counts(onError = { show(kind, key, text(null)) }) { counts -> show(kind, key, text(counts[key] ?: 0)) }
    }

    private fun show(kind: PartyHintKind, key: String, text: String) {
        val open = ClickActionManager.registerRepeatableAction("openParties:$key") { mc.schedule { PartyFinderGui.openParties(key) } }
        val stop = ClickActionManager.registerAction {
            val config = DataManager.partyFinderConfigState
            kind.turnOff(config)
            config.save()
            Chat.chat("§6[SBO] §eNo more ${kind.label} reminders. You can turn them back on in the settings of /sbopf.")
        }
        Chat.chat(Chat.getChatBreak())
        Chat.chat(
            Chat.textComponent("§6[SBO] §e$text\n"),
            Chat.textComponent("§7[§aOpen Party Finder§7]", "Join one of these parties or queue your own.", "/__sbo_run_clickable_action $open"),
            Chat.textComponent(" §7[§cDon't show again§7]", "Turns off ${kind.label} party reminders. You can turn them back on in the settings of /sbopf.", "/__sbo_run_clickable_action $stop"),
        )
        Chat.chat(Chat.getChatBreak())
    }

    // Many players keep a rod only to swap pets with it, so the armor has to fish too
    private fun fishing(): Boolean {
        val rod = System.currentTimeMillis() - lastFishedAt < FISHED_RECENTLY_MS || (0 until 9).any { slot -> mc.player?.inventory?.getItem(slot)?.let(::isRod) == true }
        return rod && ARMOR.any { slot -> mc.player?.getItemBySlot(slot)?.let { PartyEventRules.isFishingGear(loreOf(it)) } == true }
    }

    private val ARMOR = listOf(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)

    private fun holdsRod(): Boolean = mc.player?.mainHandItem?.let(::isRod) == true

    private fun isRod(stack: ItemStack): Boolean = PartyEventRules.isRod(loreOf(stack))

    private fun loreOf(stack: ItemStack): List<String> = stack.get(DataComponents.LORE)?.lines.orEmpty().map { it.removeFormatting() }
}
