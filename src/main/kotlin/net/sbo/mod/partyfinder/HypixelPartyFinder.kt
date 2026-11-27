package net.sbo.mod.partyfinder

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.partyfinder.gui.CreatePrefill
import net.sbo.mod.partyfinder.gui.PartyFinderGui
import net.sbo.mod.utils.TextUtil.removeFormatting
import net.sbo.mod.utils.events.TickScheduler.sleep
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.data.DataManager
import net.sbo.mod.utils.data.configs.partyfinder.PartyDraft
import net.sbo.mod.utils.events.ClickActionManager
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.game.SentCommandEvent
import net.sbo.mod.utils.events.impl.guis.GuiMouseClickBefore
import net.sbo.mod.utils.events.impl.guis.GuiOpenEvent
import java.time.LocalDate

/** The settings of a Kuudra group in Hypixel's party finder. */
private data class HypixelListing(val note: String? = null, val tier: String? = null, val combatLevel: Int? = null)

/** A player who joined through Hypixel's party finder, dungeon is true when they came through the dungeon finder. */
internal data class HypixelJoin(val player: String, val dungeon: Boolean) {
    // Auto kick is only for party types Hypixel's party finder has too, and SBO has no dungeon parties yet
    fun canAutoKick(partyType: String): Boolean = !dungeon && partyType == "kuudra"

    companion object {
        val MESSAGE = Regex("^Party Finder > (.+?) joined the (dungeon )?group!")

        fun of(match: MatchResult): HypixelJoin = HypixelJoin(match.groupValues[1], match.groupValues[2].isNotEmpty())
    }
}

/** Remembers the settings of the group a player builds in Hypixel's party finder. */
private class HypixelQueueSetup {
    private var listing = HypixelListing()

    /** Hypixel prints a setting when it changes, which can be right before the group is queued. */
    fun readChat(setting: String, value: String) {
        when (setting.lowercase()) {
            "note" -> listing = listing.copy(note = value.trim())
            "kuudra tier" -> listing = listing.copy(tier = value.trim().removeSuffix(" Tier").lowercase())
            "combat level required" -> listing = listing.copy(combatLevel = value.trim().toIntOrNull() ?: return)
        }
    }

    /** The lines are the item names and lore lines of the Group Builder. Dungeon groups have no tier there and are skipped. */
    fun readGroupBuilder(lines: List<String>) {
        val tier = lines.firstNotNullOfOrNull { TIER_LINE.find(it)?.groupValues?.get(1)?.lowercase() } ?: return
        listing = HypixelListing(
            note = valueAfter(lines, "Current Note:") ?: listing.note,
            tier = tier,
            combatLevel = valueAfter(lines, "Current Level Required:")?.let { if (it.isEmpty()) 0 else it.toIntOrNull() } ?: listing.combatLevel
        )
    }

    // The value follows on its own line with a dot at the end, long notes run over several lines. An empty setting shows "None!" without the dot.
    private fun valueAfter(lines: List<String>, label: String): String? {
        val start = lines.indexOfFirst { it.trim() == label }
        if (start < 0) return null
        val value = lines.drop(start + 1).takeWhile { it.isNotBlank() }.joinToString(" ").trim()
        return if (value == NOT_SET) "" else value.removeSuffix(".").ifEmpty { null }
    }

    fun take(): HypixelListing = listing.also { listing = HypixelListing() }

    companion object {
        private const val NOT_SET = "None!"
        private val TIER_LINE = Regex("^Currently Selected: (\\w+) Tier$")

        /** Hypixel calls it the party finder for Kuudra and the dungeon finder for dungeons. */
        val QUEUED = Regex("^Party Finder > Your (?:party|group) has been queued in the (party|dungeon) finder!$")

        private val TIERS = setOf("basic", "hot", "burning", "fiery", "infernal")
        private val TIER_ITEM = Regex("^Kuudra's Hollow - (\\w+) Tier$")
        private val TIER_COMMAND = Regex("^/?join(?:instance|kuudra|dungeon)\\s+kuudra_(\\w+)$", RegexOption.IGNORE_CASE)

        /** The tier of an item in Hypixel's Select Tier menu, e.g. "Kuudra's Hollow - Infernal Tier". */
        fun tierOfItem(name: String): String? = TIER_ITEM.find(name.trim())?.groupValues?.get(1)?.lowercase()?.takeIf { it in TIERS }

        /** The tier of a command like /joininstance KUUDRA_INFERNAL. */
        fun tierOfCommand(command: String): String? {
            val tier = TIER_COMMAND.find(command.trim())?.groupValues?.get(1)?.lowercase() ?: return null
            // The commands may call the first tier normal, the menus call it basic
            return (if (tier == "normal") "basic" else tier).takeIf { it in TIERS }
        }
    }
}

/** Offers to list a party on SBO too when it was just listed in Hypixel's party finder. */
object HypixelPartyFinder {
    private const val DEFAULT_TIER = "infernal"
    private const val MENU_ITEMS_DELAY_MS = 500L
    // Longer than MENU_ITEMS_DELAY_MS, so the Group Builder is read first and the offer shows below Hypixel's own message
    private const val OFFER_DELAY_MS = 600L
    private val FINDER_MENUS = setOf("Party Finder", "Need a party!", "Undersized party!")
    private const val TIER_MENU = "Select Tier"
    private val setup = HypixelQueueSetup()

    // The tier picked last in Select Tier or with /joininstance, the SBO button opens the same one
    private var pickedTier: String? = null

    private const val IGNORED_OFFERS_BEFORE_PAUSE = 2
    // True while nobody clicked the last offer
    private var offerOpen = false
    private var ignoredOffers = 0

    fun init() {
        Register.onChatMessage(Regex("^Set (.+?) to: (.*)$"), noFormatting = true) { _, match ->
            setup.readChat(match.groupValues[1], match.groupValues[2])
        }
        Register.onChatMessage(HypixelQueueSetup.QUEUED, noFormatting = true) { _, match ->
            if (match.groupValues[1] == "party") sleep(OFFER_DELAY_MS) { onQueued() } else setup.take()
        }
        Register.onChatMessage(Regex("^Party Finder > Your group has been de-listed!$"), noFormatting = true) { _, _ ->
            onDelisted()
        }
        // The menus for finding a group in Hypixel's party finder also get an SBO Party Finder button.
        // /joininstance alone or with a small party opens one of the other two menus first.
        ScreenEvents.AFTER_INIT.register { _, screen, _, _ ->
            if (screen !is AbstractContainerScreen<*>) return@register
            when (screen.title.string) {
                in FINDER_MENUS -> addSboButton(screen, pickedTier) { menu ->
                    HypixelMenus.lines(menu).any { it.startsWith("Tier: ") || "the Kuudra" in it }
                }
                // No tier is picked yet here, so it opens all Kuudra parties
                TIER_MENU -> addSboButton(screen, null) { menu ->
                    HypixelMenus.lines(menu).any { HypixelQueueSetup.tierOfItem(it) != null }
                }
            }
        }
    }

    // Dungeons use menus with the same titles, so the button waits for the items that tell Kuudra apart
    private fun addSboButton(screen: AbstractContainerScreen<*>, tier: String?, isKuudra: (AbstractContainerScreen<*>) -> Boolean) {
        val parties = if (tier == null) "the Kuudra parties" else "the ${ProblemText.title(tier)} Tier Kuudra parties"
        HypixelMenus.addPartyButton(
            screen, if (tier == null) "kuudra" else "kuudra/$tier",
            "Opens $parties of the SBO Party Finder. You see their requirements before you join.",
            show = isKuudra
        )
    }

    @SboEvent
    fun onTierClick(event: GuiMouseClickBefore) {
        val screen = event.screen as? AbstractContainerScreen<*> ?: return
        if (screen.title.string != TIER_MENU) return
        val slot = HypixelMenus.slotAt(screen, event.mouseX, event.mouseY) ?: return
        HypixelQueueSetup.tierOfItem(slot.item.hoverName.removeFormatting())?.let { pickedTier = it }
    }

    @SboEvent
    fun onCommand(event: SentCommandEvent) {
        HypixelQueueSetup.tierOfCommand(event.content)?.let { pickedTier = it }
    }

    @SboEvent
    fun onMenuOpen(event: GuiOpenEvent) {
        val screen = event.screen as? AbstractContainerScreen<*> ?: return
        // Opening Select Tier again clears the last picked tier
        if (screen.title.string == TIER_MENU) pickedTier = null
        if (screen.title.string != "Group Builder") return
        // The items arrive a moment after the menu
        sleep(MENU_ITEMS_DELAY_MS) { setup.readGroupBuilder(HypixelMenus.lines(screen)) }
    }

    private fun onQueued() {
        val listing = setup.take()
        val config = DataManager.partyFinderConfigState
        if (listing.tier != null) {
            config.lastKuudraTier = listing.tier
            config.save()
        }
        if (!config.hypixelOffer || PartyFinderManager.inQueue || PartyFinderManager.isPaused) return
        // Only the leader can list the party
        if (PartyFinderManager.isInParty && !PartyFinderManager.isLeader) return
        // Kuudra players queue many times an hour, so an offer they ignored twice pauses until the next day
        val today = LocalDate.now().toString()
        if (config.hypixelOfferPausedOn == today) return
        if (offerOpen) ignoredOffers++
        if (ignoredOffers >= IGNORED_OFFERS_BEFORE_PAUSE) {
            answeredOffer()
            config.hypixelOfferPausedOn = today
            config.save()
            return
        }
        // The hover of Queue now needs the names of the requirements
        PartyCategories.get { mc.execute { if (!PartyFinderManager.inQueue) showOffer(listing) } }
    }

    internal fun onListedOnSbo() = answeredOffer()

    private fun answeredOffer() {
        offerOpen = false
        ignoredOffers = 0
    }

    // Kuudra is the only party type that both Hypixel's party finder and SBO have, so a listed party of another type stays as it is
    private fun onDelisted() {
        if (!PartyFinderManager.inQueue || PartyFinderManager.draft?.partyType != "kuudra") return
        val config = DataManager.partyFinderConfigState
        when {
            config.hypixelAutoDelist -> removeFromSbo()
            config.hypixelDelistQuestion -> askToDelist()
        }
    }

    private fun askToDelist() {
        // The question stays clickable in chat, so it only removes the listing it was asked about
        val listedAt = PartyFinderManager.queuedParty?.createdAt
        fun removeAskedListing() {
            val current = PartyFinderManager.queuedParty
            if (PartyFinderManager.inQueue && current?.partyType == "kuudra" && current.createdAt == listedAt) removeFromSbo()
            else Chat.chat("§6[SBO] §eThat party is no longer in the SBO queue.")
        }
        val remove = ClickActionManager.registerAction { removeAskedListing() }
        val always = ClickActionManager.registerAction {
            val config = DataManager.partyFinderConfigState
            config.hypixelAutoDelist = true
            config.save()
            removeAskedListing()
            Chat.chat("§6[SBO] §eFrom now on your party will be removed automatically. You can change this in the settings of /sbopf.")
        }
        val stop = ClickActionManager.registerAction {
            val config = DataManager.partyFinderConfigState
            config.hypixelDelistQuestion = false
            config.save()
            Chat.chat("§6[SBO] §eSBO won't ask this again. You can turn it back on in the settings of /sbopf.")
        }
        Chat.chat(
            Chat.textComponent("§6[SBO] §eYour party was removed from Hypixel's party finder. Do you also want to remove it from the SBO queue?\n"),
            Chat.textComponent("§7[§cRemove from SBO§7]", "Removes your party from the SBO queue.", "/__sbo_run_clickable_action $remove"),
            Chat.textComponent(
                " §7[§eAlways remove§7]",
                "Removes it now, and from now on every time it gets removed from Hypixel's party finder. You can change this in the settings of /sbopf.",
                "/__sbo_run_clickable_action $always"
            ),
            Chat.textComponent(
                " §7[§cDon't ask again§7]",
                "Your party stays queued on SBO. You can turn this back on in the settings of /sbopf.",
                "/__sbo_run_clickable_action $stop"
            ),
        )
    }

    private fun removeFromSbo() {
        if (!PartyFinderManager.inQueue) return
        PartyFinderManager.usedPf = false
        PartyFinderManager.removePartyFromQueue()
    }

    private fun showOffer(listing: HypixelListing) {
        offerOpen = true
        val tier = tierOf(listing)
        // Copied now, so a click lists exactly what the hover showed
        val shown = (DataManager.partyFinderConfigState.drafts["kuudra/$tier"]?.deepCopy() ?: PartyDraft("kuudra", tier))
            .apply { takeOver(listing) }
        // Listing can fail and the create page can be closed, so both buttons stay clickable
        val queue = ClickActionManager.registerRepeatableAction {
            answeredOffer()
            queueNow(shown)
        }
        val create = ClickActionManager.registerRepeatableAction {
            answeredOffer()
            mc.schedule { openCreatePage(listing) }
        }
        val stop = ClickActionManager.registerAction {
            answeredOffer()
            val config = DataManager.partyFinderConfigState
            config.hypixelOffer = false
            config.save()
            Chat.chat("§6[SBO] §eSBO won't ask again. You can turn this back on in the settings of /sbopf.")
        }
        Chat.chat(Chat.getChatBreak())
        Chat.chat(
            Chat.textComponent(offerText(listing, tier) + "\n"),
            Chat.textComponent("§7[§aQueue now§7]", queueHover(shown, tierKnown = listing.tier != null), "/__sbo_run_clickable_action $queue"),
            Chat.textComponent(
                " §7[§eAdd requirements§7]",
                "Opens the SBO Party Finder with these settings, so you can add more requirements before you queue your party.",
                "/__sbo_run_clickable_action $create"
            ),
            Chat.textComponent(" §7[§cDon't ask again§7]", "You can turn this back on in the settings of /sbopf.", "/__sbo_run_clickable_action $stop"),
        )
        Chat.chat(Chat.getChatBreak())
    }

    private fun tierOf(listing: HypixelListing): String =
        listing.tier ?: DataManager.partyFinderConfigState.lastKuudraTier ?: DEFAULT_TIER

    private fun offerText(listing: HypixelListing, tier: String): String {
        val title = "${ProblemText.title(tier)} Tier"
        // Without a tier from Hypixel it can only suggest the last one, so it doesn't say that one is queued
        return if (listing.tier == null) {
            "§6[SBO] §eYour party is queued on Hypixel. Do you also want to queue it on SBO as §b$title§e?"
        } else {
            "§6[SBO] §eYour §b$title §eparty is queued on Hypixel. Do you also want to queue it in the SBO Party Finder?"
        }
    }

    /** Everything Queue now lists, also what is left from the last party of this tier. */
    private fun queueHover(shown: PartyDraft, tierKnown: Boolean): String {
        val target = PartyCategories.target(shown.partyType, shown.subType)
        val lines = buildList {
            add("Queues your party right away with all of this:")
            add("Tier: ${ProblemText.title(shown.subType)}")
            if (!tierKnown) add("This is the tier you used last, check it before you queue.")
            add("Party size: ${target?.clampSize(shown.partySize) ?: shown.partySize}")
            if (shown.note.isNotEmpty()) add("Note: ${shown.note.replace('\n', ' ')}")
            PartyFinderManager.reqsOf(shown).forEach { (stat, need) -> add(ProblemText.requirement(stat, need, target)) }
            target?.options?.forEach { option ->
                val value = shown.options[option.id]
                if (value.isNullOrEmpty() || value == option.default) return@forEach
                val picked = value.split(',').joinToString(", ") { id -> option.values.firstOrNull { it.id == id }?.label ?: id }
                add("${option.label}: $picked")
            }
            shown.ownRole?.let { add("Your role: ${target?.roleLabel(it) ?: it}") }
            if (shown.wantedRoles.isNotEmpty()) add("Wanted roles: ${shown.wantedRoles.joinToString(", ") { target?.roleLabel(it) ?: it }}")
            add("By queueing a party you agree to the rules in /sbopf.")
        }
        return lines.joinToString("\n")
    }

    private fun PartyDraft.takeOver(listing: HypixelListing) {
        listing.note?.let { note = PartyFinderManager.checkPartyNote(it) }
        listing.combatLevel?.let { level -> if (level > 0) reqs["combatLevel"] = "$level" else reqs.remove("combatLevel") }
    }

    private fun queueNow(shown: PartyDraft) {
        // The offer stays clickable in chat, an old one must not replace the party that is listed by now
        if (PartyFinderManager.inQueue) {
            Chat.chat("§6[SBO] §eYour party is already in the queue.")
            return
        }
        val config = DataManager.partyFinderConfigState
        config.drafts[shown.key] = shown.deepCopy()
        config.save()
        PartyFinderManager.createParty(shown)
    }

    private fun openCreatePage(listing: HypixelListing) {
        PartyFinderGui.openCreate("kuudra/${tierOf(listing)}", CreatePrefill("kuudra") { takeOver(listing) })
    }
}
