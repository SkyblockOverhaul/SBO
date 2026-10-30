package net.sbo.mod.settings

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import net.sbo.mod.config.Category
import net.sbo.mod.config.Config

// Moves the values of an older config.jsonc to where the settings are now, before the categories load it
internal object SettingsMigration {
    const val VERSION = 1

    fun migrate(json: JsonObject) {
        val version = (json.get(Config.VERSION_KEY) as? JsonPrimitive)?.takeIf { it.isNumber }?.asInt ?: 0
        if (version < 1) toV1(json)
    }

    // The Diana category was split into Burrows, Waypoints, Warps, Rare Mobs, Trackers and Drops.
    // Customization, Medal and QOL went into those and General. Every setting kept its id, so it is
    // found by id; dropdown values and the settings merged into one (message hiders, medal clips, price overrides) are converted.
    private fun toV1(json: JsonObject) {
        val values = HashMap<String, JsonElement>()
        for (name in OLD_SUB_SWITCHES.keys) {
            val old = json.remove(name) as? JsonObject ?: continue
            for ((id, value) in old.entrySet()) {
                if (id != Category.DISABLED_SUBS_KEY) values[id] = value
            }
            // The subs don't exist anymore, so their switched off settings are turned off themselves
            (old.get(Category.DISABLED_SUBS_KEY) as? JsonArray)?.forEach { title ->
                val ids = (title as? JsonPrimitive)?.takeIf { it.isString }?.let { OLD_SUB_SWITCHES[name]?.get(it.asString) }
                ids?.forEach { values[it] = JsonPrimitive(false) }
            }
        }
        if (values.isEmpty()) return

        fun on(id: String, default: Boolean) = (values[id] as? JsonPrimitive)?.takeIf { it.isBoolean }?.asBoolean ?: default

        // Dropdowns and multi selects saved enum names, their option ids are those names in lower case
        for (id in ENUM_SETTINGS) {
            values[id] = values[id]?.let(::lowercase) ?: continue
        }

        // Your own coordinate waypoints were the "normal" option of Hide Own Waypoints, now they have their own switch
        (values["hideOwnWaypoints"] as? JsonArray)?.let { hidden ->
            val normal = JsonPrimitive("normal")
            if (normal in hidden) values["hideOwnCoordinateWaypoints"] = JsonPrimitive(true)
            values["hideOwnWaypoints"] = JsonArray().apply { hidden.filter { it != normal }.forEach(::add) }
        }

        if (HIDDEN_MESSAGES.keys.any { it in values }) {
            values["hideMessages"] = ids(HIDDEN_MESSAGES.filterKeys { on(it, false) }.values)
        }
        if (MEDAL_CLIPS.keys.any { it in values }) {
            values["medalClips"] = ids(MEDAL_CLIPS.filterKeys { on(it, true) }.values)
        }
        val price = when {
            on("ironmanOverrides", false) -> "npc_only"
            on("npcPriceOverrides", false) -> "npc_items"
            else -> null
        }
        price?.let { values["priceSource"] = JsonPrimitive(it) }

        for (category in Settings.children) {
            val target = json.get(category.id) as? JsonObject ?: JsonObject().also { json.add(category.id, it) }
            for (entry in category.entries) {
                if (!target.has(entry.id)) values[entry.id]?.let { target.add(entry.id, it) }
            }
        }
    }

    private fun ids(ids: Collection<String>) = JsonArray().apply { ids.forEach(::add) }

    private fun lowercase(value: JsonElement): JsonElement = when {
        value is JsonPrimitive && value.isString -> JsonPrimitive(value.asString.lowercase())
        value is JsonArray -> JsonArray().apply { value.forEach { add(lowercase(it)) } }
        else -> value
    }

    private val ENUM_SETTINGS = listOf(
        "lootTracker", "mobTracker", "coaTracker", "lootSort", "mobSort", "statsSort", "trackerFont", "bazaarSettingDiana",
        "legacyTrackers", "trackerBackground", "allowedWarps", "ShareMobs", "ReceiveMobs", "NoShurikenMobs", "hideOwnWaypoints",
    )

    // Old switch to the option of the setting that replaced it
    private val HIDDEN_MESSAGES = mapOf(
        "dianaMessageHider" to "diana",
        "hideAutoPetMSG" to "autopet",
        "hideImplosionMSG" to "implosion",
        "hideSacksMSG" to "sacks",
    )

    private val MEDAL_CLIPS = mapOf(
        "medalChimeraEnabled" to "chimera",
        "medalWoolEnabled" to "shimmering_wool",
        "medalBrainFoodEnabled" to "brain_food",
        "medalMinosRelicEnabled" to "minos_relic",
        "medalMantiCoreEnabled" to "manti_core",
        "medalFatefulStingerEnabled" to "fateful_stinger",
        "medalDaedalusStickEnabled" to "daedalus_stick",
        "medalBraidedGriffinFeatherEnabled" to "braided_griffin_feather",
        "medalMythologicalDyeEnabled" to "mythological_dye",
        "medalMythTheFishEnabled" to "myth_the_fish",
        "medalCrownOfGreedEnabled" to "crown_of_greed",
        "medalHiltOfRevelationsEnabled" to "hilt_of_revelations",
        "medalWashedUpSouvenirEnabled" to "washed_up_souvenir",
        "medalDwarfTurtleShelmetEnabled" to "dwarf_turtle_shelmet",
        "medalCrochetTigerPlushieEnabled" to "crochet_tiger_plushie",
        "medalAntiqueRemediesEnabled" to "antique_remedies",
        "medalCretanUrnEnabled" to "cretan_urn",
    )

    // The on/off settings in every old sub that could be switched off, by category and sub title
    private val OLD_SUB_SWITCHES: Map<String, Map<String, List<String>>> = mapOf(
        "Diana" to mapOf(
            "Diana Burrows" to listOf("spadeGuess", "arrowGuess", "closeBurrowDetection"),
            "Diana Burrows Preferences" to listOf(
                "showArrowSubGuesses", "showTextOnSubGuess", "drawOptimalOrderLines", "showBeaconBeam", "showTitleWhenFailure",
                "showTitleWhenChainEnds", "muteBuggedSpadeSounds", "ongoingChainsDisplay", "simpleChainsOverlayMode",
            ),
            "Diana Warp" to listOf("showTitleWhenWarpAvailable", "dontWarpIfBurrowClose", "ignoreYLevel"),
            "Diana Tracker" to listOf(
                "assumeAllLS", "npcPriceOverrides", "ironmanOverrides", "excludeCoinsFromProfit", "hideUnobtainedItems", "combineLootLines",
                "statsTracker", "hideLsStats", "resetSessionOnGameRestart", "magicFindTracker", "fourEyedFish", "sendSinceMessage", "sendKillTimeMessage",
            ),
            "Diana Announcer" to listOf("lootAnnouncerChat", "hiltDropMessage", "lootAnnouncerScreen", "announceCrownOfGreed", "lootAnnouncerParty"),
            "Diana Waypoints" to listOf("guessAndBurrowLine", "inqLine"),
            "Rare Mobs" to listOf(
                "shareRareMob", "shareRareMobInPublicChat", "receiveRareMob", "scanWorldForRareMob", "HighlightRareMobs", "lootshareDisplay",
                "lootshareMessage", "lootsharePartyOverlay", "announceCocoon", "cocoonTitle", "noShurikenOverlay",
            ),
            "Other" to listOf("mythosMobHp", "sphinxSolver"),
        ),
        "General" to mapOf(
            "Overlays" to listOf("bobberOverlay", "legionOverlay"),
            "Waypoints" to listOf("patcherWaypoints"),
            "Accessibility" to listOf("assumeMuted"),
            "Achievements" to listOf("disableAchievements", "achievementsButton"),
            "Chunk Cache" to listOf("hubChunkCache", "chunkCacheWorkEverywhere", "chunkCacheHubIslandOnly", "chunkCacheRenderHubIslandOnly", "chunkCacheAboveServerOnly"),
        ),
        "Customization" to mapOf(
            "Waypoint Customization" to listOf("lookAlike", "dynamicWaypointOpacity", "waypointTextShadow", "showTimesDug"),
            "Warp Title Customization" to listOf("warpTitleAsSubtitle"),
        ),
        "Medal" to mapOf(
            // The clip switches all needed Enable Medal Clips, so turning that off is enough and keeps the selected drops
            "Medal Clips" to listOf("medalEnabled"),
        ),
        "QOL" to emptyMap(),
    )
}
