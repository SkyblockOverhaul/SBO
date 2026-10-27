package net.sbo.mod.profileviewer

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.sbo.guilib.core.dom.component
import net.sbo.guilib.core.dsl.NodeBuilder
import net.sbo.guilib.core.dsl.aside
import net.sbo.guilib.core.dsl.b
import net.sbo.guilib.core.dsl.button
import net.sbo.guilib.core.dsl.classNames
import net.sbo.guilib.core.dsl.div
import net.sbo.guilib.core.dsl.h2
import net.sbo.guilib.core.dsl.header
import net.sbo.guilib.core.dsl.img
import net.sbo.guilib.core.dsl.input
import net.sbo.guilib.core.dsl.item
import net.sbo.guilib.core.dsl.main
import net.sbo.guilib.core.dsl.p
import net.sbo.guilib.core.dsl.playerHead
import net.sbo.guilib.core.dsl.scroll
import net.sbo.guilib.core.dsl.segmented
import net.sbo.guilib.core.dsl.sortableList
import net.sbo.guilib.core.dsl.span
import net.sbo.guilib.core.dsl.tabs
import net.sbo.guilib.core.dsl.tooltip
import net.sbo.guilib.fabric.GuiLib
import net.sbo.mod.SBOKotlin
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.guis.look.SboLook
import net.sbo.mod.guis.look.UiScale
import net.sbo.mod.guis.look.useSboScale
import net.sbo.mod.guis.look.useSboTheme
import net.sbo.mod.partyfinder.PartyCategories
import net.sbo.mod.partyfinder.PartyFinderManager
import net.sbo.mod.partyfinder.ProblemText
import net.sbo.mod.partyfinder.api.CategoriesData
import net.sbo.mod.partyfinder.api.CategoryDef
import net.sbo.mod.partyfinder.api.GeneralStats
import net.sbo.mod.partyfinder.api.Networth
import net.sbo.mod.partyfinder.api.TrackingData
import net.sbo.mod.partyfinder.gui.PartyFinderGui
import net.sbo.mod.partyfinder.gui.Piece
import net.sbo.mod.partyfinder.gui.StatItems
import net.sbo.mod.partyfinder.gui.StatView
import net.sbo.mod.partyfinder.gui.ago
import net.sbo.mod.partyfinder.gui.pieces
import net.sbo.mod.partyfinder.gui.statItem
import net.sbo.mod.partyfinder.gui.statLabel
import net.sbo.mod.partyfinder.gui.supporterLogo
import net.sbo.mod.partyfinder.gui.teamTag
import net.sbo.mod.partyfinder.gui.uuidOf
import net.sbo.mod.settings.categories.Themes
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.HypixelModApi
import net.sbo.mod.utils.Player
import net.sbo.mod.utils.data.DataManager
import net.sbo.mod.utils.data.configs.partyfinder.FavoritePlayer
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.game.World
import java.util.Locale
import kotlin.random.Random

object ProfileViewerGui {
    private val STYLES = listOf("sbo:ui/partyfinder/partyfinder.css", "sbo:ui/profileviewer/profileviewer.css", SboLook.STYLE)
    private const val KOFI_URL = "https://ko-fi.com/skyblock_overhaul"
    private val SKILLS = listOf(
        "Farming", "Mining", "Combat", "Foraging", "Fishing", "Enchanting", "Alchemy", "Taming", "Carpentry", "Hunting",
        "Runecrafting", "Social"
    )
    private val GENERAL_STATS = setOf("sbLevel", "ironman", "magicalPower", "dianaRank") + SKILLS.map { "${it.lowercase()}Level" }
    // kuudraCompletionsAll already shows the completions per tier
    private val SHOWN_BY = mapOf("kuudraCompletions" to "kuudraCompletionsAll")
    private const val SETS_PAGE = 9
    // As many rows as the worn items next to them
    private const val LOADOUTS_PAGE = 5
    private const val CONTAINER_PAGE = 45
    private const val ENDER_CHEST_PAGES = 9
    private const val BACKPACK_SLOTS = 18
    // As many cells as fit next to the item tabs, arrows and the close button included
    private const val PAGER_CELLS = 8
    private const val PREVIEW_DELAY_MS = 100L
    private val STORED_TABS = setOf("storage", "pets", "accessories")
    private const val LOADING_CELLS = 16 * 4
    // The last block grows for 350 ms, then the check shows 100 ms before the items
    private const val LOADING_DONE_MS = 450L
    // When each block fills, with a fixed seed so the scattered order is the same every time
    private val LOADING_ORDER = (0 until LOADING_CELLS).shuffled(Random(7))
    private val ITEM_TABS = listOf(
        "inventory" to "Inventory", "wardrobe" to "Wardrobe", "equipment" to "Equipment", "loadouts" to "Loadouts",
        "storage" to "Storage", "pets" to "Pets", "accessories" to "Accessory Bag"
    )
    private const val PROFILE_INFO = "The SkyBlock profile the player has selected right now, all stats come from it."
    private const val NETWORTH_INFO = "What everything on this profile is worth together: items, pets and the coins in purse and bank. " +
        "It is calculated with SkyHelper Networth, which other mods use too, so the numbers are comparable."
    private const val NETWORTH_NOTE = "The museum is not counted."
    private val NETWORTH_PARTS = mapOf(
        "purse" to "Purse", "bank" to "Bank", "inventory" to "Inventory", "armor" to "Armor", "equipment" to "Equipment",
        "wardrobe" to "Wardrobe", "enderchest" to "Ender Chest", "storage" to "Backpacks", "accessories" to "Accessory Bag",
        "pets" to "Pets", "personal_vault" to "Personal Vault", "sacks" to "Sacks", "sacks_bag" to "Sack of Sacks",
        "fishing_bag" to "Fishing Bag", "potion_bag" to "Potion Bag", "quiver" to "Quiver", "candy_inventory" to "Candy Bag",
        "carnival_mask_inventory" to "Carnival Masks", "essence" to "Essence", "museum" to "Museum",
        "farming_toolkit" to "Farming Toolkit", "hunting_toolkit" to "Hunting Toolkit"
    )
    private const val SKILL_AVERAGE_INFO = "The average level of Farming, Mining, Combat, Foraging, Fishing, Enchanting, Alchemy, " +
        "Taming, Carpentry and Hunting. Progress towards the next level counts too, so halfway to level 51 counts as 50.5. " +
        "Runecrafting and Social are left out."

    private var startName: String? = null
    private var startParty = false
    private var lastCategory: String? = null
    private var lastSearch = ""

    // From the last Hypixel party packet, leader first
    @Volatile
    private var partyUuids: List<String> = emptyList()
    @Volatile
    private var inParty = false

    /** Only opens in Skyblock. Must run on the client thread. */
    fun open(name: String? = null, party: Boolean = false) {
        if (!World.isInSkyblock()) {
            SBOKotlin.toast(
                Component.literal("SBO").withStyle(ChatFormatting.GOLD),
                Component.literal("Join Skyblock before opening Profile Viewer!").withStyle(ChatFormatting.RED)
            )
            return
        }
        startName = name
        startParty = party
        if (name != null) lastSearch = name
        GuiLib.open(App, STYLES, title = "SBO Profile Viewer")
    }

    fun register() {
        Register.command("sbopv", "spv", "sboprofileviewer", "sbocheck", "sboc") { args ->
            val party = args.firstOrNull()?.equals("party", ignoreCase = true) == true
            mc.schedule { open(if (party) null else args.firstOrNull(), party) }
        }
        Register.command("sbocheckparty", "sbocheckp", "sbocp") { mc.schedule { open(party = true) } }
        HypixelModApi.onPartyInfo { isInParty, _, members ->
            inParty = isInParty
            partyUuids = members.map { it.replace("-", "") }
        }
    }

    private fun ownName(): String = Player.getName() ?: Player.accountName()

    private fun ownUuid(): String = Player.getUUIDString().replace("-", "")

    private fun queuedType(): String? = PartyFinderManager.queuedParty?.takeIf { PartyFinderManager.inQueue }?.partyType

    private fun startCategory(data: CategoriesData?): String =
        (queuedType() ?: lastCategory)?.takeIf { it !in ViewerLayout.HIDDEN_CATEGORIES } ?: data?.categories?.firstOrNull()?.id ?: "diana"

    private val App = component("ProfileViewer") {
        val config = DataManager.partyFinderConfigState
        var data by useState(PartyCategories.data)
        var mode by useState(if (startParty) "party" else "player")
        var name by useState(startName ?: ownName())
        var query by useState(lastSearch)
        var category by useState(startCategory(data))
        var tab by useState("stats")
        var upgradesShown by useState("missing")
        var itemsShown by useState("inventory")
        var setPage by useState(0)
        // The ender chest page or backpack opened in the storage, like "ec:0" or "bp:3"
        var storageOpen by useState<String?>(null)
        var profile by useState(ProfileViewerApi.cached(name))
        var loading by useState(false)
        var error by useState<String?>(null)
        var reload by useState(0)
        var refresh by useState(false)
        var members by useState(partyUuids)
        var hasParty by useState(inParty)
        var favorites by useState(config.viewerFavorites.toList())
        var party by useState<List<ViewedProfile>?>(null)
        var partyError by useState<String?>(null)
        // Bumped when item stacks are built, so the slots draw them
        var stacksBuilt by useState(0)
        val now = useState(System.currentTimeMillis())
        PartyFinderGui.FONTS.keys.forEach { id -> useBodyClass("pf-font-$id", config.font == id) }
        useSboScale(UiScale.own(config.uiScale))
        val theme = useSboTheme(config.theme)
        useBodyClass("pf-hypixel", theme.hypixelColors)
        useBodyClass("pf-recomb", config.recombobulated)
        useBodyClass("pf-marks", theme.marks)

        useEffect {
            PartyCategories.get { loaded -> if (loaded != null) data = loaded else error = "Could not load the categories from the SBO server." }
        }
        useInterval(1000) {
            now.set(System.currentTimeMillis())
            members = partyUuids
            hasParty = inParty
        }
        useEffect(category) { lastCategory = category }

        useEffect(data != null, name, reload) {
            if (data == null) return@useEffect
            val wanted = name
            val fromHypixel = refresh
            refresh = false
            loading = true
            error = null
            if (!fromHypixel) ProfileViewerApi.cached(wanted)?.let { profile = it }
            ProfileViewerApi.player(wanted, fromHypixel, onError = { e ->
                if (name == wanted) {
                    error = ProblemText.error(e)
                    loading = false
                }
            }) { loaded ->
                if (name == wanted) {
                    profile = loaded
                    loading = false
                }
            }
        }

        val shown = profile?.takeIf { it.name.equals(name, ignoreCase = true) }
        useEffect(shown?.items) {
            val items = shown?.items ?: return@useEffect
            DisplayItemStacks.prepare(items.all(), items.stored()) { stacksBuilt++ }
        }
        // After loading a check shows for a moment so the items do not pop in, items built already show right away
        val loadKey = shown?.items?.let { it to (itemsShown in STORED_TABS) }
        val needed = loadKey?.let { (items, stored) -> if (stored) items.all() + items.stored() else items.all() }.orEmpty()
        val built = needed.count { it.built != null || it.failed }
        var loadingFor by useState<Any?>(null)
        var loadedFor by useState<Any?>(null)
        useEffect(loadKey, built == needed.size) {
            if (loadKey == null) return@useEffect
            if (built < needed.size) loadingFor = loadKey
            else if (loadingFor == loadKey) setTimeout(LOADING_DONE_MS) { loadedFor = loadKey }
        }
        val itemsLoad = when {
            loadKey == null || Themes.itemsLoading == Themes.ITEMS_LOADING_OFF -> null
            built < needed.size || (loadingFor == loadKey && loadedFor != loadKey) -> ItemsLoad(built, needed.size)
            else -> null
        }
        useEffect(name) {
            setPage = 0
            storageOpen = null
        }

        // The party packet only comes on request, so the list is fresh when the tab opens
        useEffect(mode) { if (mode == "party" && HypixelModApi.isOnHypixel) HypixelModApi.sendPartyInfoPacket() }

        // Own stats are loaded separately, so this only requests the other members
        useEffect(mode, members, data != null) {
            if (mode != "party" || data == null || members.size < 2) return@useEffect
            party = null
            partyError = null
            val own = ownUuid()
            val others = members.filter { it != own }
            fun loadOthers(ownProfile: ViewedProfile?) {
                ProfileViewerApi.party(others, onError = { partyError = ProblemText.error(it) }) { loaded ->
                    party = members.mapNotNull { id -> if (id == own) ownProfile else loaded.firstOrNull { it.uuid == id } }
                }
            }
            ProfileViewerApi.player(ownName(), refresh = false, fromCache = true, onError = { loadOthers(null) }) { loadOthers(it) }
        }

        val categoryDef = data?.categories?.firstOrNull { it.id == category }

        fun saveFavorites(next: List<FavoritePlayer>) {
            favorites = next
            config.viewerFavorites = next.toMutableList()
            config.save()
        }

        fun toggleFavorite(player: ViewedProfile) = saveFavorites(
            if (favorites.any { it.uuid == player.uuid }) favorites.filter { it.uuid != player.uuid }
            else favorites + FavoritePlayer(player.uuid, player.name)
        )

        // Players can change their name, so a favorite takes the one from the last visit
        useEffect(shown?.uuid, shown?.name) {
            val player = shown ?: return@useEffect
            if (favorites.any { it.uuid == player.uuid && it.name != player.name }) {
                saveFavorites(favorites.map { if (it.uuid == player.uuid) it.copy(name = player.name) else it })
            }
        }

        fun show(next: String) {
            val wanted = next.trim()
            if (wanted.isEmpty()) return
            if (!wanted.equals(name, ignoreCase = true)) profile = ProfileViewerApi.cached(wanted)
            name = wanted
            mode = "player"
            query = wanted
            lastSearch = wanted
            reload++
        }

        div(className = "pf-window pv-window") {
            header(className = "pf-header") {
                span(className = "pf-title") { +"Profile Viewer" }
                tabs(value = mode, onChange = { mode = it }, className = "pf-nav") {
                    tab("favorites", "Favorites")
                    tab("player", "Player")
                    tab("party", "Party")
                }
                div(className = "pf-spacer")
                div(className = "pv-searchbox") {
                    input(
                        type = "text",
                        value = query,
                        onInput = { query = it.value },
                        onKeyDown = { if (it.key == "Enter") show(query) },
                        placeholder = "Player",
                        title = "Type a name and press Enter to see their stats",
                        className = "pv-search",
                        maxLength = 16
                    )
                    button(className = "pv-search-button", onClick = { show(query) }) { +"Search" }
                }
                button(className = "pf-close", title = "Close", onClick = { GuiLib.close() }) {
                    img(src = "${StatView.ICONS}/close.svg", className = "pf-close-icon")
                }
            }

            if (mode == "favorites") {
                main(className = "pf-main pv-party-main") {
                    favoriteList(favorites, onOpen = { show(it) }, onReorder = { saveFavorites(it) }) { removed ->
                        saveFavorites(favorites.filter { it.uuid != removed.uuid })
                    }
                }
                return@div
            }

            if (mode == "party") {
                main(className = "pf-main pv-party-main") {
                    when {
                        !hasParty || members.size < 2 -> message("You are not in a party. Join one and the members show up here.")
                        partyError != null -> message(partyError!!)
                        party == null -> message("Loading party members...")
                        else -> partyGrid(party!!) { show(it) }
                    }
                }
                return@div
            }

            div(className = "pv-page") {
                val favorite = shown != null && favorites.any { it.uuid == shown.uuid }
                profileBar(shown, name, loading, now.value, favorite, onStar = { shown?.let(::toggleFavorite) }) {
                    refresh = true
                    reload++
                }

                if (shown != null) {
                    div(className = "pv-top") {
                        generalBox(shown, data)
                        itemsBox(shown.items, itemsShown, setPage, storageOpen, itemsLoad, onShow = {
                            itemsShown = it
                            setPage = 0
                            storageOpen = null
                        }, onOpen = { storageOpen = it }) { setPage = it }
                    }
                }

                div(className = "pf-body") {
                    aside(className = "pf-sidebar") {
                        scroll(className = "pf-side-scroll guilib-autohide") {
                            div(className = "pf-side-title") { +"Categories" }
                            val queued = queuedType()
                            data?.categories?.filter { it.id !in ViewerLayout.HIDDEN_CATEGORIES }?.forEach { cat ->
                                div(
                                    className = classNames("pf-side-item", "pf-type-${cat.id}", "active" to (cat.id == category)),
                                    key = cat.id,
                                    onClick = {
                                        category = cat.id
                                        if (cat.id != "diana") tab = "stats"
                                    }
                                ) {
                                    span(className = "pf-side-label") { +cat.label }
                                    if (cat.id == queued) {
                                        img(src = "${StatView.ICONS}/star-filled.svg", className = "pf-icon pf-star on", title = "Your party is listed here")
                                    }
                                }
                            }
                        }
                        div(
                            className = "pf-side-item pf-support",
                            title = "Opens Ko-fi in your browser. Donations help keep SBO running.",
                            onClick = { SBOKotlin.openInBrowser(KOFI_URL) }
                        ) {
                            img(src = "${StatView.ICONS}/heart.svg", className = "pf-icon pf-heart")
                            span(className = "pf-side-label") { +"Support SBO" }
                        }
                    }

                    main(className = "pf-main") {
                        when {
                            categoryDef == null && error != null -> message(error!!)
                            categoryDef == null -> message("Loading categories...")
                            shown == null && error != null -> message(error!!)
                            shown == null -> message("Loading stats...")
                            else -> {
                                div(className = "pf-toolbar pv-toolbar") {
                                    h2(className = "pf-heading pf-type-${categoryDef.id}") { +categoryDef.label }
                                    if (categoryDef.id == "diana" && shown.tracking != null) {
                                        tabs(value = tab, onChange = { tab = it }, variant = "pills", className = "pv-tabs") {
                                            tab("stats", "Stats")
                                            tab("upgrades", "Upgrades")
                                        }
                                    }
                                }
                                scroll(className = "pv-scroll guilib-autohide") {
                                    val tracking = shown.tracking
                                    if (tab == "upgrades" && categoryDef.id == "diana" && tracking != null) {
                                        upgrades(tracking, upgradesShown == "all") { upgradesShown = it }
                                    } else {
                                        statsList(shown, categoryDef)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun NodeBuilder.profileBar(
        profile: ViewedProfile?,
        name: String,
        loading: Boolean,
        now: Long,
        favorite: Boolean,
        onStar: () -> Unit,
        onRefresh: () -> Unit
    ) {
        div(className = "pv-profile") {
            if (profile == null) {
                b(className = "pv-name") { +name }
                return@div
            }
            playerHead(uuidOf(profile.uuid), className = "pv-head")
            b(className = "pv-name") { +profile.name }
            with(PartyFinderGui) { starIcon(favorite, onStar) }
            teamTag(profile.uuid)
            supporterLogo(profile.uuid)
            profile.profile?.let { span(className = "pf-tag", title = PROFILE_INFO) { +it } }
            if (profile.ironman) span(className = "pf-tag", title = "This profile is an Ironman profile") { +"Ironman" }
            div(className = "pf-spacer")
            val wait = (profile.refreshAt - now).coerceAtLeast(0)
            div(className = "pv-age") {
                profile.dataAt?.let { span(title = "When SBO last loaded this player's data from Hypixel") { +"Updated ${ago(it, now)}" } }
                button(
                    className = classNames("pf-icon-button", "loading" to loading),
                    disabled = wait > 0 || loading,
                    title = if (wait > 0) "You can load this player again in ${minutesSeconds(wait)}" else "Load the newest data from Hypixel",
                    onClick = { onRefresh() }
                ) {
                    img(src = "${StatView.ICONS}/refresh.svg", className = "pf-icon")
                }
                if (wait > 0) span(className = "pv-wait") { +minutesSeconds(wait) }
            }
        }
    }

    private fun NodeBuilder.generalBox(profile: ViewedProfile, data: CategoriesData?) {
        div(className = "pv-general-box") { profileOverview(profile, data) }
    }

    private fun NodeBuilder.profileOverview(profile: ViewedProfile, data: CategoriesData?) {
        val general = profile.general
        div(className = "pv-tiles") {
            tile("sbLevel", "SkyBlock Level", numberPieces("sbLevel", general?.sbLevel))
            tile("magicalPower", "Magical Power", numberPieces("magicalPower", general?.magicalPower))
            tile("skillAverage", "Skill Average", skillAverage(profile), SKILL_AVERAGE_INFO)
            tile("dianaRank", "Diana Rank", dianaRank(profile), PartyCategories.stat("dianaRank")?.info?.ifBlank { null })
            tile("catacombsLevel", "Catacombs", catacombs(profile))
            tile("bestiaryMilestone", "Bestiary Milestone", numberPieces("bestiaryMilestone", general?.bestiaryMilestone))
            tooltip(content = { networthInfo(profile.networth) }, placement = "bottom", className = "pf-tip") {
                tile("networth", "Networth", listOf(coinsPiece(profile.networth?.total)), infoIcon = true)
            }
            tile("purse", "Purse", listOf(coinsPiece(general?.purse)))
            tile("bank", "Bank", listOf(coinsPiece(bank(general))))
        }
        div(className = "pv-section") { +"Skills" }
        div(className = "pv-levels") {
            SKILLS.forEach { skill ->
                div(className = "pv-level", key = skill) {
                    span(className = "pv-level-label") {
                        statItem("skill:$skill")
                        +skill
                    }
                    val level = general?.skills?.get(skill.lowercase())?.level
                    span(className = "pv-level-value") { pieces(numberPieces("${skill.lowercase()}Level", level)) }
                }
            }
        }
        val slayer = data?.categories?.firstOrNull { it.id == "slayer" } ?: return
        val slayers = general?.slayers.orEmpty()
        if (slayers.isEmpty()) return
        div(className = "pv-section") { +"Slayer" }
        div(className = "pv-levels") {
            slayer.subcategories.filter { slayers.containsKey(it.id) }.forEach { sub ->
                div(className = "pv-level", key = sub.id) {
                    span(className = "pv-level-label") {
                        statItem("slayer:${sub.id}")
                        +sub.label
                    }
                    span(className = "pv-level-value") { pieces(numberPieces("slayerLevel", slayers[sub.id]?.level)) }
                }
            }
        }
    }

    /** The parts of the networth, sorted by value. */
    private fun NodeBuilder.networthInfo(networth: Networth?) {
        div(className = "pf-tip-title") { b { +"Calculated by SkyHelper Networth" } }
        if (networth == null) return div(className = "pf-tip-text") { +NETWORTH_INFO }
        networth.parts.entries.sortedByDescending { it.value }.forEach { (part, value) ->
            val label = NETWORTH_PARTS[part] ?: part.split('_').joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
            div(className = "pv-networth-part") {
                span(className = "pf-tip-text") { +label }
                pieces(listOf(coinsPiece(value)))
            }
        }
        div(className = "pv-networth-part pv-networth-sum") {
            span(className = "pf-tip-text") { +"Without soulbound items" }
            pieces(listOf(coinsPiece(networth.unsoulbound)))
        }
        div(className = "pf-tip-accuracy") { +NETWORTH_NOTE }
    }

    private fun bank(general: GeneralStats?): Double? {
        val parts = listOfNotNull(general?.bank, general?.personalBank)
        return if (parts.isEmpty()) null else parts.sum()
    }

    // infoIcon shows the info icon without an info text, for tiles that have their tooltip around the whole tile
    private fun NodeBuilder.tile(icon: String, label: String, value: List<Piece>, info: String? = null, infoIcon: Boolean = false) {
        div(className = "pv-tile") {
            val head: NodeBuilder.() -> Unit = {
                span(className = "pv-tile-label") {
                    statItem(icon)
                    +label
                    if (info != null || infoIcon) img(src = "${StatView.ICONS}/info.svg", className = "pf-icon pf-info")
                }
            }
            if (info != null) tooltip(text = info, className = "pf-tip") { head() } else head()
            span(className = "pv-tile-value") { pieces(value) }
        }
    }

    private class ItemsLoad(val built: Int, val total: Int) {
        val done: Boolean get() = built >= total
        val percent: Int get() = if (total == 0) 100 else built * 100 / total
    }

    private fun NodeBuilder.itemsBox(
        items: PlayerItems?,
        shown: String,
        setPage: Int,
        open: String?,
        load: ItemsLoad?,
        onShow: (String) -> Unit,
        onOpen: (String?) -> Unit,
        onPage: (Int) -> Unit
    ) {
        div(className = "pv-items-box") {
            if (items == null) return@div message("No items.")
            val (entries, perPage) = when (shown) {
                "wardrobe" -> maxOf(items.wardrobe.size, items.wardrobeSlot ?: 0) to SETS_PAGE
                "equipment" -> maxOf(items.equipmentSets.size, items.equipmentSlot ?: 0) to SETS_PAGE
                "loadouts" -> items.loadouts.size to LOADOUTS_PAGE
                "pets" -> items.pets.size to CONTAINER_PAGE
                "accessories" -> items.accessories.size to CONTAINER_PAGE
                else -> 0 to 1
            }
            val pages = (entries + perPage - 1) / perPage
            val page = setPage.coerceIn(0, (pages - 1).coerceAtLeast(0))
            div(className = "pv-item-tabs") {
                div(className = "pv-item-switch") {
                    ITEM_TABS.forEach { (id, label) ->
                        div(className = classNames("pv-item-tab", "active" to (id == shown)), key = id, title = label, onClick = { onShow(id) }) {
                            StatItems.stack("items:$id")?.let { item(it, className = "pv-item-tab-icon", decorations = false) }
                        }
                    }
                }
                if (shown == "storage" && open != null) {
                    val group = storageGroup(items, open)
                    val labels = group.map { (it.substringAfter(":").toInt() + 1).toString() }
                    pager(labels, group.indexOf(open), onPage = { onOpen(group[it]) }, onClose = { onOpen(null) })
                } else if (pages > 1) {
                    pager((1..pages).map { it.toString() }, page, onPage)
                }
            }
            if (mc.level == null) return@div p(className = "pf-hint") { +"Items show up once you are in a world." }
            if (load != null) return@div if (Themes.itemsLoading == Themes.ITEMS_LOADING_SIMPLE) simpleItemsLoading(load) else itemsLoading(load)
            div(className = "pv-item-row") {
                div(className = "pv-worn") {
                    listOf(0, 1, 2, 3).forEach { row ->
                        slot(items.equipment.getOrNull(row), key = "equipment$row")
                        slot(items.armor.getOrNull(row), key = "armor$row")
                    }
                    slot(items.pet, key = "pet")
                }
                div(className = "pv-item-side") {
                    when (shown) {
                        "wardrobe" -> setPage(page, items.wardrobeSlot, items::wardrobeSet)
                        "equipment" -> setPage(page, items.equipmentSlot, items::equipmentSet)
                        "loadouts" -> loadouts(items, page)
                        "storage" -> storage(items, open, onOpen)
                        "pets" -> if (items.pets.isEmpty()) p(className = "pf-hint") { +"No pets." } else grid(items.pets, page, if (items.pet != null) 0 else null)
                        "accessories" -> if (items.accessories.isEmpty()) p(className = "pf-hint") { +"No accessory bag." } else grid(items.accessories, page)
                        else -> inventory(items)
                    }
                }
            }
        }
    }

    /** In place of the slots while the items are built. Blocks fill up one by one in a scattered order like memory. */
    private fun NodeBuilder.itemsLoading(load: ItemsLoad) {
        val filled = load.built.toFloat() * LOADING_CELLS / load.total
        div(className = "pv-items-loading") {
            div(className = "pv-loading-line") {
                if (load.done) {
                    img(src = "${StatView.ICONS}/check.svg", className = "pf-icon pv-loading-check")
                    +" items loaded"
                } else {
                    span(className = "pv-loading-prompt") { +">" }
                    +" building items"
                    span(className = "pv-loading-cursor") { +"_" }
                }
            }
            div(className = "pv-cells") {
                LOADING_ORDER.forEachIndexed { cell, rank ->
                    val level = (filled - rank).coerceIn(0f, 1f)
                    div(className = "pv-cell", key = cell) {
                        div(className = "pv-cell-fill", style = "transform: scale(${String.format(Locale.ROOT, "%.3f", level)})")
                    }
                }
            }
            div(className = "pv-loading-line pf-muted") { +"${load.built} / ${load.total}  ${load.percent}%" }
        }
    }

    private fun NodeBuilder.simpleItemsLoading(load: ItemsLoad) {
        div(className = "pv-items-loading") {
            if (load.done) img(src = "${StatView.ICONS}/check.svg", className = "pf-icon pv-loading-check", key = "check")
            else img(src = "${StatView.ICONS}/refresh.svg", className = "pf-icon pv-spinner", key = "spinner")
            span(className = "pf-muted") { +(if (load.done) "Items loaded" else "Loading items ${load.percent}%") }
            bar(load.built.toDouble(), load.total.toDouble(), "pv-bar big pv-items-bar")
        }
    }

    private fun NodeBuilder.inventory(items: PlayerItems) {
        div(className = "pv-slots") {
            (9 until 36).forEach { slot(items.inventory.getOrNull(it), key = it) }
        }
        div(className = "pv-slots pv-hotbar") {
            (0 until 9).forEach { slot(items.inventory.getOrNull(it), key = it) }
        }
    }

    /** Armor or equipment sets, one per column like in game. Sets and worn count from 1. */
    private fun NodeBuilder.setPage(page: Int, worn: Int?, setOf: (Int) -> List<DisplayItem?>?) {
        val first = page * SETS_PAGE
        div(className = "pv-slots") {
            (0 until 4).forEach { piece ->
                (first + 1..first + SETS_PAGE).forEach { set ->
                    slot(setOf(set)?.getOrNull(piece), key = "$set:$piece", extra = if (worn == set) "equipped" else null)
                }
            }
        }
    }

    /** The names are too long for the row, so it shows the number and the name on hover. */
    private fun NodeBuilder.loadouts(items: PlayerItems, page: Int) {
        if (items.loadouts.isEmpty()) return p(className = "pf-hint") { +"No loadouts set up." }
        val first = page * LOADOUTS_PAGE
        div(className = "pv-loadouts") {
            items.loadouts.drop(first).take(LOADOUTS_PAGE).forEachIndexed { i, loadout ->
                div(className = "pv-loadout", key = first + i) {
                    span(className = "pv-loadout-number", title = loadout.name) { +"${first + i + 1}" }
                    val armor = loadout.armorSet?.let(items::wardrobeSet)
                    val equipment = loadout.equipmentSet?.let(items::equipmentSet)
                    (0 until 4).forEach { slot(armor?.getOrNull(it), key = "a$it") }
                    div(className = "pv-loadout-gap")
                    (0 until 4).forEach { slot(equipment?.getOrNull(it), key = "e$it") }
                }
            }
        }
    }

    private fun NodeBuilder.pager(labels: List<String>, page: Int, onPage: (Int) -> Unit, onClose: (() -> Unit)? = null) {
        val slots = PAGER_CELLS - 2 - (if (onClose != null) 1 else 0)
        div(className = "pv-pager") {
            pagerCell("<", "prev", enabled = page > 0) { onPage(page - 1) }
            shownPages(page, labels.size, slots).forEachIndexed { i, shown ->
                if (shown == null) span(className = "pv-pager-cell pv-pager-gap", key = "gap$i") { +"..." }
                else pagerCell(labels[shown], shown, active = shown == page) { onPage(shown) }
            }
            pagerCell(">", "next", enabled = page in 0 until labels.lastIndex) { onPage(page + 1) }
            if (onClose != null) {
                div(className = "pv-pager-cell pv-pager-close", key = "close", title = "Back to the storage", onClick = { onClose() }) {
                    img(src = "${StatView.ICONS}/close.svg", className = "pf-icon")
                }
            }
        }
    }

    private fun NodeBuilder.pagerCell(text: String, key: Any, enabled: Boolean = true, active: Boolean = false, onClick: () -> Unit) {
        val className = classNames("pv-pager-cell", "active" to active, "disabled" to !enabled)
        div(className = className, key = key, onClick = if (enabled && !active) { _ -> onClick() } else null) { +text }
    }

    /**
     * The pages to show in that many slots, null where "..." stands for the pages left out. Like on websites the
     * first and the last page always show, and the pages around the current one.
     */
    private fun shownPages(page: Int, pages: Int, slots: Int): List<Int?> {
        if (pages <= slots) return (0 until pages).toList()
        val edge = slots - 2
        if (page < edge) return (0 until edge).toList() + null + (pages - 1)
        if (page >= pages - edge) return listOf(0, null) + (pages - edge until pages)
        val around = slots - 4
        val first = page - (around - 1) / 2
        return listOf(0, null) + (first until first + around) + null + (pages - 1)
    }

    /** Five rows of nine like an ender chest page. */
    private fun NodeBuilder.grid(list: List<DisplayItem?>, page: Int, worn: Int? = null) {
        val first = page * CONTAINER_PAGE
        div(className = "pv-slots") {
            (first until first + CONTAINER_PAGE).forEach { slot(list.getOrNull(it), key = it, extra = if (it == worn) "equipped" else null) }
        }
    }

    /** Like Hypixel's storage menu, the ender chest pages and the backpacks open with a click. */
    private fun NodeBuilder.storage(items: PlayerItems, open: String?, onOpen: (String) -> Unit) {
        val (kind, index) = open?.split(":")?.let { it[0] to it[1].toInt() } ?: ("" to 0)
        if (open != null) div(className = "pv-storage-label") { +openedName(open) }
        if (kind == "ec") return grid(items.enderChest, index)
        if (kind == "bp") return grid(items.backpacks.firstOrNull { it.slot == index }?.items.orEmpty(), 0)
        val pages = enderChestPages(items)
        div(className = "pv-storage-label") { +"Ender Chest" }
        div(className = "pv-slots") {
            (0 until ENDER_CHEST_PAGES).forEach { page ->
                if (page >= pages) return@forEach slot(null, key = "ec$page", extra = "locked")
                storagePreview("ec:$page", items.enderChest, page) {
                    div(className = "pv-slot pv-openable", onClick = { onOpen("ec:$page") }) {
                        StatItems.stack("items:enderChestPage")?.let { item(it, className = "pv-item", decorations = false) }
                    }
                }
            }
        }
        div(className = "pv-storage-label") { +"Backpacks" }
        div(className = "pv-slots") {
            (0 until BACKPACK_SLOTS).forEach { slot ->
                val backpack = items.backpacks.firstOrNull { it.slot == slot }
                if (backpack == null) return@forEach slot(null, key = "bp$slot", extra = "locked")
                storagePreview("bp:$slot", backpack.items, 0) {
                    div(className = "pv-slot pv-openable", onClick = { onOpen("bp:$slot") }) {
                        backpack.icon?.built?.let { item(it.stack(), className = "pv-item", decorations = false) }
                    }
                }
            }
        }
    }

    private fun NodeBuilder.storagePreview(open: String, list: List<DisplayItem?>, page: Int, anchor: NodeBuilder.() -> Unit) {
        val content: NodeBuilder.() -> Unit = {
            div(className = "pv-storage-label") { +openedName(open) }
            grid(list, page)
        }
        tooltip(content = content, delayMs = PREVIEW_DELAY_MS, className = "pf-tip pv-preview", key = open, children = anchor)
    }

    private fun enderChestPages(items: PlayerItems): Int = (items.enderChest.size + CONTAINER_PAGE - 1) / CONTAINER_PAGE

    /** The ender chest pages or the backpacks, whichever the open one belongs to, for the arrows. */
    private fun storageGroup(items: PlayerItems, open: String): List<String> =
        if (open.startsWith("ec:")) (0 until enderChestPages(items)).map { "ec:$it" }
        else items.backpacks.map { "bp:${it.slot}" }

    private fun openedName(open: String): String {
        val index = open.substringAfter(":").toInt() + 1
        return if (open.startsWith("ec:")) "Ender Chest $index" else "Backpack $index"
    }

    private fun NodeBuilder.slot(shown: DisplayItem?, key: Any, extra: String? = null, onClick: (() -> Unit)? = null) {
        div(className = classNames("pv-slot", extra), key = key, onClick = if (onClick == null) null else { _ -> onClick() }) {
            shown?.built?.let { item(it.stack(), className = "pv-item", tooltip = true) }
        }
    }

    private fun NodeBuilder.statsList(profile: ViewedProfile, category: CategoryDef) {
        val bySub = profile.stats[category.id].orEmpty()
        if (bySub.isEmpty()) return message("No stats for ${category.label}.")
        // The viewer stats come first, like the XP of a skill
        val all = (category.viewer + category.reqs.map { it.stat } + category.display +
            category.subcategories.flatMap { sub -> sub.viewer + sub.reqs.map { it.stat } + sub.display }).distinct()
        val shown = { ids: List<String> -> ids.distinct().filter { it in all && isShown(it, all) } }
        val moved = ViewerLayout.TOP[category.id].orEmpty()
        // Category stats that differ per subcategory go under each subcategory
        val categoryIds = shown(category.viewer + category.reqs.map { it.stat } + category.display)
        val varying = categoryIds.filter { id -> bySub.values.mapNotNull { it[id] }.distinct().size > 1 }
        fun anyValue(id: String) = bySub.values.firstNotNullOfOrNull { it[id] }

        div(className = "pv-list") {
            (categoryIds.filter { it !in varying && it !in moved } + moved).forEach { id -> statRow(id, anyValue(id)) }
            ViewerLayout.PER_VALUE[category.id].orEmpty().forEach { perValue(profile, category, it) }
        }
        if (category.id in ViewerLayout.NO_SUBCATEGORIES) return
        category.subcategories.filter { bySub.containsKey(it.id) }.forEach { sub ->
            val ids = shown(sub.viewer + varying + sub.reqs.map { it.stat } + sub.display)
                .filter { (it !in categoryIds || it in varying) && it !in moved }
            if (ids.isEmpty()) return@forEach
            div(className = "pv-divider", key = "divider:${sub.id}") {
                span(className = "pv-divider-label") { +sub.label }
                div(className = "pv-divider-line")
            }
            div(className = "pv-list", key = "list:${sub.id}") {
                ids.forEach { id -> statRow(id, bySub[sub.id]?.get(id)) }
            }
        }
    }

    private fun NodeBuilder.perValue(profile: ViewedProfile, category: CategoryDef, per: PerValue) {
        val option = PartyCategories.target(category.id, per.subType)?.options?.firstOrNull { it.id == per.optionId } ?: return
        val base = profile.stats[category.id]?.get(per.subType)?.get(per.statId)
        // Only values that differ from the default are in byOption
        val changes = profile.byOption[category.id]?.get(per.subType)?.get(per.optionId).orEmpty()
        div(className = "pv-row", key = per.statId) { statLabel(per.statId, icons = true) }
        option.values.forEach { value ->
            div(className = "pv-row pv-subrow", key = "${per.statId}:${value.id}") {
                span(className = "pf-muted") { +value.label }
                span(className = "pf-panel-value") { pieces(StatView.valuePieces(per.statId, changes[value.id]?.get(per.statId) ?: base)) }
            }
        }
    }

    private fun NodeBuilder.statRow(id: String, value: JsonElement?) {
        // Like the completions per Kuudra tier, one row per entry
        if (value is JsonObject && PartyCategories.stat(id)?.kind == "breakdown") {
            div(className = "pv-row", key = id) { statLabel(id, icons = true) }
            value.forEach { (key, entry) ->
                div(className = "pv-row pv-subrow", key = "$id:$key") {
                    span(className = "pf-muted") { +ProblemText.title(key) }
                    span(className = "pf-panel-value") { pieces(StatView.valuePieces(id, entry)) }
                }
            }
            return
        }
        div(className = "pv-row", key = id) {
            statLabel(id, icons = true)
            span(className = classNames(
                "pf-panel-value",
                "estimated" to StatView.estimated(id),
                "missing" to (value == null || value is JsonNull)
            )) { pieces(StatView.valuePieces(id, value)) }
        }
    }

    // Items and general values show on the general part, so the categories leave them out
    private fun isShown(id: String, all: List<String>): Boolean =
        SHOWN_BY[id] !in all && id !in GENERAL_STATS && id !in ViewerLayout.HIDDEN_STATS && PartyCategories.stat(id)?.kind != "items"

    private fun NodeBuilder.upgrades(tracking: TrackingData, showAll: Boolean, onShow: (String) -> Unit) {
        div(className = "pv-upgrade-head") {
            div(className = "pv-upgrade-title") {
                b { +"Tracking ${number(tracking.tracking)} of ${number(tracking.max)}" }
                segmented(value = if (showAll) "all" else "missing", onChange = onShow, className = "pv-upgrade-filter") {
                    option("missing", "Missing")
                    option("all", "All")
                }
            }
            bar(tracking.tracking, tracking.max, "pv-bar big")
        }
        p(className = "pf-hint") {
            +(if (showAll) "Everything that gives Diana Tracking, the biggest missing gains first." else "What is still missing for more Diana Tracking, the biggest gains first.")
        }
        val sources = tracking.sources.sortedByDescending { it.max - it.value }.filter { showAll || it.value < it.max }
        if (sources.isEmpty()) return div(className = "pf-message") { p { +"Nothing missing, every source of Tracking is maxed." } }
        sources.forEach { source ->
            val done = source.value >= source.max
            div(className = classNames("pv-upgrade", "done" to done), key = source.id) {
                div(className = "pv-upgrade-label") {
                    tooltip(text = source.info, className = "pf-tip") {
                        span(className = "pf-stat-label") {
                            +source.label
                            img(src = "${StatView.ICONS}/info.svg", className = "pf-icon pf-info")
                        }
                    }
                }
                span(className = "pv-upgrade-level") {
                    if (source.level != null && source.maxLevel != null) +"Lvl ${source.level}/${source.maxLevel}"
                }
                bar(source.value, source.max, "pv-bar")
                span(className = "pv-upgrade-gain") {
                    if (done) img(src = "${StatView.ICONS}/check.svg", className = "pf-icon pv-ok")
                    else +"+${number(source.max - source.value)} missing"
                }
            }
        }
    }

    private fun NodeBuilder.bar(value: Double, max: Double, className: String) {
        val percent = if (max <= 0) 0.0 else (value / max * 100).coerceIn(0.0, 100.0)
        div(className = className) { div(className = "pv-bar-fill", style = "width: ${String.format(Locale.ROOT, "%.2f", percent)}%") }
    }

    private fun NodeBuilder.favoriteList(
        favorites: List<FavoritePlayer>,
        onOpen: (String) -> Unit,
        onReorder: (List<FavoritePlayer>) -> Unit,
        onRemove: (FavoritePlayer) -> Unit
    ) {
        div(className = "pf-toolbar pv-toolbar") { h2(className = "pf-heading") { +"Favorites" } }
        if (favorites.isEmpty()) return message("Open a player and click the star next to their name to keep them here.")
        scroll(className = "pv-scroll guilib-autohide") {
            sortableList(favorites, key = { it.uuid }, onReorder = onReorder, className = "pv-fav-list") { player, _ ->
                div(className = "pv-fav-row", title = "Open ${player.name}, drag to sort", onClick = { onOpen(player.name) }) {
                    playerHead(uuidOf(player.uuid), className = "pv-party-head")
                    span(className = "pv-fav-name") { +player.name }
                    with(PartyFinderGui) { starIcon(true) { onRemove(player) } }
                }
            }
        }
    }

    private fun NodeBuilder.partyGrid(players: List<ViewedProfile>, onOpen: (String) -> Unit) {
        div(className = "pf-toolbar pv-toolbar") {
            h2(className = "pf-heading") { +"Party" }
            if (partyUuids.size > ProfileViewerApi.MAX_OTHERS + 1) span(className = "pf-muted pv-party-note") { +"Only the first ${ProfileViewerApi.MAX_OTHERS + 1} members" }
        }
        scroll(className = "pv-scroll guilib-autohide") {
            div(className = "pv-party-grid") {
                players.forEach { player ->
                    div(className = "pv-party-card", key = player.uuid, title = "Open ${player.name}", onClick = { onOpen(player.name) }) {
                        div(className = "pv-party-who") {
                            playerHead(uuidOf(player.uuid), className = "pv-party-head")
                            div(className = "pv-party-names") {
                                b(className = "pv-party-name") { +player.name }
                                div(className = "pv-party-tags") {
                                    player.profile?.let { span(className = "pf-tag", title = PROFILE_INFO) { +it } }
                                    if (player.ironman) span(className = "pf-tag") { +"Ironman" }
                                }
                            }
                        }
                        partyLine("sbLevel", "SkyBlock Level", numberPieces("sbLevel", player.general?.sbLevel))
                        partyLine("magicalPower", "Magical Power", numberPieces("magicalPower", player.general?.magicalPower))
                        partyLine("catacombsLevel", "Catacombs", catacombs(player))
                        partyLine("skillAverage", "Skill Average", skillAverage(player))
                        tooltip(content = { networthInfo(player.networth) }, placement = "bottom", className = "pf-tip") {
                            partyLine("networth", "Networth", listOf(coinsPiece(player.networth?.total)))
                        }
                        partyLine("dianaRank", "Diana Rank", dianaRank(player))
                    }
                }
            }
        }
    }

    private fun NodeBuilder.partyLine(icon: String, label: String, value: List<Piece>) {
        div(className = "pv-party-line") {
            span(className = "pv-level-label") {
                statItem(icon)
                +label
            }
            span(className = "pv-party-value") { pieces(value) }
        }
    }

    private fun catacombs(profile: ViewedProfile): List<Piece> = numberPieces("catacombsLevel", profile.general?.catacombsLevel)

    private fun dianaRank(profile: ViewedProfile): List<Piece> {
        val rank = (profile.anyStat("dianaRank") as? JsonPrimitive)?.content?.toDoubleOrNull()?.toLong()
        return listOf(if (rank == null || rank <= 0) Piece("no data") else Piece("#${String.format(Locale.US, "%,d", rank)}"))
    }

    private fun numberPieces(statId: String, value: Number?): List<Piece> =
        StatView.valuePieces(statId, value?.let { JsonPrimitive(it) })

    private fun skillAverage(profile: ViewedProfile): List<Piece> =
        listOf(Piece(profile.skillAverage?.let { String.format(Locale.US, "%.2f", it) } ?: "no data"))

    private fun coinsPiece(value: Double?): Piece {
        if (value == null) return Piece("no data")
        // Small amounts keep their decimal like in game
        return Piece(if (value < 1000) number(value) else Helper.formatNumber(value), "pf-c-gold")
    }

    private fun number(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else String.format(Locale.US, "%.1f", value)

    private fun minutesSeconds(ms: Long): String {
        val seconds = ms / 1000
        return if (seconds >= 60) "${seconds / 60}m ${seconds % 60}s" else "${seconds}s"
    }

    private fun NodeBuilder.message(text: String) {
        div(className = "pf-message") { p { +text } }
    }
}
