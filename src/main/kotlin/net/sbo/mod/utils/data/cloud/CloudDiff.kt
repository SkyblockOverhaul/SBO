package net.sbo.mod.utils.data.cloud

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive

enum class CloudSide { PC, CLOUD }

// pc and cloud are the texts shown, null if that side does not have it. newer is the side that changed it, null if unknown
data class CloudField(
    val id: String,
    val file: String,
    val label: String,
    val pc: String?,
    val cloud: String?,
    val newer: CloudSide? = null,
    val bothChanged: Boolean = false,
    val group: String? = null,
)

// id is the file, or the name of files that are picked together. With perField each field gets its own pick, otherwise the whole area
data class CloudArea(
    val id: String,
    val label: String,
    val files: List<String>,
    val perField: Boolean,
    val fields: List<CloudField>,
    val newer: CloudSide? = null,
    val bothChanged: Boolean = false,
) {
    // Long areas only show their first differences, a pick still covers all of them
    val shown: List<CloudField> get() = if (perField) fields else fields.take(MAX_SHOWN)
    val hidden: Int get() = fields.size - shown.size

    private companion object {
        const val MAX_SHOWN = 40
    }
}

// A value SBO decided on its own. null removes it, an empty path replaces the whole file
class CloudAuto(val file: String, val path: List<String>, val value: JsonElement?)

// When each field (id) or whole file was last changed, on this PC and in the cloud save
data class CloudTimes(val pc: Map<String, Long> = emptyMap(), val cloud: Map<String, Long> = emptyMap())

// areas still need a choice, auto holds everything SBO decided on its own. coaChanged: the two sides track different CoAs
data class CloudComparison(val areas: List<CloudArea>, val auto: List<CloudAuto>, val coaChanged: Boolean = false)

// Compares and merges the files of a cloud save, no game code so it can be tested
object CloudDiff {
    const val CONFIG = "config"
    const val TRACKERS = "dianaTrackers"
    private const val SBO_DATA = "SboData.json"
    private const val ACHIEVEMENTS = "sbo_achievements.json"
    private const val PAST_EVENTS = "pastDianaEvents.json"
    private const val TRACKER_TOTAL = "dianaTrackerTotal.json"
    private const val TRACKER_MAYOR = "dianaTrackerMayor.json"
    private const val TRACKER_SESSION = "dianaTrackerSession.json"
    private val TRACKER_FILES = setOf(TRACKER_TOTAL, TRACKER_MAYOR, TRACKER_SESSION)
    private const val COA_UUID = "currentCoaUuid"
    private const val COA_START = "COA Start Coins"

    private val PER_FIELD = setOf(CONFIG, SBO_DATA, "soundSettingsData.json")
    private val IGNORED = setOf("rconfig:version")
    // The running mod sets these itself, so this PC's value is always kept
    private val IGNORED_PATHS = mapOf(
        SBO_DATA to setOf(
            listOf("settingsVersion"), listOf("settingsPreviousVersion"), listOf("whatsNewChatVersion"),
            listOf("whatsNewOpenedVersion"), listOf(TIMES_KEY),
        ),
    )
    // Inside the uploaded SBO Data: when each value last changed, field or file id -> time
    const val TIMES_KEY = "syncChangedAt"
    // Remembered texts where empty means never set, so the other side's text is taken
    private val EMPTY_IS_UNSET = mapOf(SBO_DATA to setOf("lastStatsProfile", COA_UUID))
    private const val MAX_TEXT = 40

    private class AreaDef(val label: String, val files: List<String>)

    private val AREAS = linkedMapOf(
        CONFIG to AreaDef("Settings", listOf(CONFIG)),
        SBO_DATA to AreaDef("SBO Data", listOf(SBO_DATA)),
        "soundSettingsData.json" to AreaDef("Sounds", listOf("soundSettingsData.json")),
        TRACKERS to AreaDef("Diana Tracker", listOf(TRACKER_TOTAL, TRACKER_MAYOR, TRACKER_SESSION)),
        ACHIEVEMENTS to AreaDef("Achievements", listOf(ACHIEVEMENTS)),
        PAST_EVENTS to AreaDef("Past Diana Events", listOf(PAST_EVENTS)),
        "partyFinderConfigState.json" to AreaDef("Party Finder", listOf("partyFinderConfigState.json")),
        "overlayData.json" to AreaDef("Overlay Positions", listOf("overlayData.json")),
    )

    // Headings inside an area with more than one file
    private val GROUP_LABELS = mapOf(TRACKER_TOTAL to "Total", TRACKER_MAYOR to "Mayor", TRACKER_SESSION to "Session")

    fun label(file: String): String = AREAS[file]?.label ?: file.removeSuffix(".json")

    // A value that differs. base is its value at the last sync, hasBase tells if that sync is known
    private class Change(
        val file: String,
        val path: List<String>,
        val pc: JsonElement?,
        val cloud: JsonElement?,
        val base: JsonElement?,
        val hasBase: Boolean,
    ) {
        val pcChanged get() = hasBase && pc != base
        val cloudChanged get() = hasBase && cloud != base
    }

    // The value SBO picked on its own, value null removes it
    private class Decided(val value: JsonElement?)

    // A rule that knows SBO cannot decide it
    private val ASK = Decided(null)

    /**
     * base: the files of the last sync on this PC, if known. times: when values last changed on each side.
     * Everything SBO can decide safely ends up in auto, the rest in areas.
     */
    fun compare(
        pc: Map<String, String>,
        cloud: Map<String, String>,
        base: Map<String, String>? = null,
        times: CloudTimes = CloudTimes(),
    ): CloudComparison {
        val sides = prepare(pc, cloud, base)
        return compareSides(sides.pc, sides.cloud, sides.base, times)
    }

    private fun compareSides(
        pc: Map<String, String>,
        cloud: Map<String, String>,
        base: Map<String, String>?,
        times: CloudTimes,
    ): CloudComparison {
        val auto = mutableListOf<CloudAuto>()
        val areas = groups(pc.keys + cloud.keys).mapNotNull { [id, def] ->
            // A file only the cloud has is taken, one only this PC has stays
            def.files.filter { it in cloud && it !in pc }.forEach { auto += CloudAuto(it, emptyList(), read(it, cloud.getValue(it))) }
            val both = def.files.filter { it in pc && it in cloud }
            val perField = id in PER_FIELD
            val depth = if (!perField) Int.MAX_VALUE else if (id == CONFIG) 2 else 1

            val conflicts = both.flatMap { file ->
                val pcJson = read(file, pc.getValue(file))
                val cloudJson = read(file, cloud.getValue(file))
                if (pcJson == cloudJson) return@flatMap emptyList()
                wholeFile(file, pcJson, cloudJson)?.let {
                    auto += CloudAuto(file, emptyList(), it)
                    return@flatMap emptyList()
                }
                val baseJson = base?.get(file)?.let { read(file, it) }
                if (file in TRACKER_FILES) when (val tracker = trackerRule(file, pcJson, cloudJson, baseJson, times)) {
                    null -> {}
                    ASK -> return@flatMap diff(file, pcJson, cloudJson, baseJson, baseJson != null, depth)
                    else -> {
                        auto += CloudAuto(file, emptyList(), tracker.value)
                        return@flatMap emptyList()
                    }
                }
                diff(file, pcJson, cloudJson, baseJson, baseJson != null, depth).filter { change ->
                    val decided = resolve(change, pcJson, cloudJson, times) ?: return@filter true
                    auto += CloudAuto(file, change.path, decided.value)
                    false
                }
            }
            if (conflicts.isEmpty()) return@mapNotNull null

            val fields = conflicts.map { change ->
                val [newer, bothChanged] = if (perField) newer(change.pcChanged, change.cloudChanged, change.hasBase) {
                    if (change.file == SBO_DATA) guessValue(change.pc, change.cloud) else null
                } else null to false
                CloudField(
                    id = idOf(change.file, change.path),
                    file = change.file,
                    label = change.path.joinToString(" › ") { humanize(it) },
                    pc = change.pc?.let(::display),
                    cloud = change.cloud?.let(::display),
                    newer = newer,
                    bothChanged = bothChanged,
                    group = if (def.files.size > 1) GROUP_LABELS[change.file] ?: label(change.file) else null,
                )
            }
            if (perField) return@mapNotNull CloudArea(id, def.label, def.files, true, fields)

            val hasBase = both.all { base?.get(it) != null }
            val [newer, bothChanged] = newer(conflicts.any { it.pcChanged }, conflicts.any { it.cloudChanged }, hasBase) {
                guessArea(id, { pc[it]?.let(::parse) }, { cloud[it]?.let(::parse) })
            }
            CloudArea(id, def.label, def.files, false, fields, newer, bothChanged)
        }
        val coaChanged = pc[SBO_DATA] != null && cloud[SBO_DATA] != null &&
            read(SBO_DATA, pc.getValue(SBO_DATA)).get(COA_UUID) != read(SBO_DATA, cloud.getValue(SBO_DATA)).get(COA_UUID)
        return CloudComparison(areas, auto, coaChanged)
    }

    // A whole file decided at once: the mayor tracker of the newer year wins, the older one is already in the past events.
    // A reset tracker has year 0, that is not an older year.
    private fun wholeFile(file: String, pc: JsonObject, cloud: JsonObject): JsonObject? {
        if (file != TRACKER_MAYOR || number(pc, "year") <= 0 || number(cloud, "year") <= 0) return null
        return when (bigger(number(pc, "year"), number(cloud, "year"))) {
            CloudSide.PC -> pc
            CloudSide.CLOUD -> cloud
            null -> null
        }
    }

    /**
     * A tracker as a whole, before its values are compared one by one. null: compare the values, ASK: SBO cannot decide.
     * A reset (button, command, deleted file) lowers counts; what was counted before it must not come back.
     */
    private fun trackerRule(file: String, pc: JsonObject, cloud: JsonObject, base: JsonObject?, times: CloudTimes): Decided? {
        if (base == null) {
            // A tracker that never counted anything, like on a new PC: the other side's, unless it was emptied last
            if (isEmptyTracker(pc) == isEmptyTracker(cloud)) return null
            val last = laterSide(times.pc[file], times.cloud[file])
                ?: if (isEmptyTracker(pc)) CloudSide.CLOUD else CloudSide.PC
            return Decided(if (last == CloudSide.PC) pc else cloud)
        }
        // Changed on one side only: its values win one by one anyway
        if (pc == base || cloud == base) return null
        val pcReset = wasReset(pc, base)
        val cloudReset = wasReset(cloud, base)
        if (!pcReset && !cloudReset) return null
        // Everything the other side counted happened before the reset, so the reset tracker is right
        fun before(reset: Map<String, Long>, other: Map<String, Long>): Boolean {
            val resetAt = reset[resetKey(file)] ?: return false
            val otherAt = other[file] ?: return false
            return otherAt < resetAt
        }
        return when {
            pcReset && before(times.pc, times.cloud) -> Decided(pc)
            cloudReset && before(times.cloud, times.pc) -> Decided(cloud)
            // Counted after the reset somewhere else: adding would bring back old counts, dropping would lose new ones
            else -> ASK
        }
    }

    // Some count went down since the last sync, which only a reset does
    private fun wasReset(tracker: JsonObject, base: JsonObject): Boolean =
        counts(base).any { (path, before) -> (counts(tracker)[path] ?: 0.0) < before }

    private fun isEmptyTracker(tracker: JsonObject): Boolean = counts(tracker).values.all { it <= 0.0 }

    // The drop and mob counts of a tracker, -1 (not counted yet) as 0
    private fun counts(tracker: JsonObject): Map<String, Double> = buildMap {
        for (group in listOf("items", "mobs")) {
            tracker.getAsJsonObjectOrNull(group)?.entrySet()?.forEach { (key, value) ->
                if (key != COA_START && isNumber(value)) put("$group/$key", value.asDouble.coerceAtLeast(0.0))
            }
        }
    }

    private fun JsonObject.getAsJsonObjectOrNull(key: String): JsonObject? = get(key) as? JsonObject

    /** Id in the change times for the last reset of a tracker file. */
    fun resetKey(file: String): String = "$file#reset"

    /** All counts of each tracker added up, a lower sum than before means it was reset. */
    fun trackerSums(files: Map<String, String>): Map<String, Double> =
        files.filterKeys { it in TRACKER_FILES }.mapValues { (_, text) -> counts(parse(text)).values.sum() }

    private fun laterSide(pc: Long?, cloud: Long?): CloudSide? = when {
        pc == null || cloud == null -> null
        pc > cloud -> CloudSide.PC
        cloud > pc -> CloudSide.CLOUD
        else -> null
    }

    private class Sides(val pc: Map<String, String>, val cloud: Map<String, String>, val base: Map<String, String>?)

    /**
     * Ends the event of an older mayor tracker the way SBO does at an election: it moves to the past events.
     * Then each event is compared with itself, and counts of both PCs in the old event are added up instead of dropped.
     */
    private fun prepare(pc: Map<String, String>, cloud: Map<String, String>, base: Map<String, String>?): Sides {
        val pcYear = pc[TRACKER_MAYOR]?.let { number(parse(it), "year").toInt() } ?: 0
        val cloudYear = cloud[TRACKER_MAYOR]?.let { number(parse(it), "year").toInt() } ?: 0
        val newest = maxOf(pcYear, cloudYear)
        val older = pcYear > 0 && cloudYear > 0 && pcYear != cloudYear
        val pcOut = if (older && pcYear < cloudYear) endEvent(pc) else pc
        val cloudOut = if (older && cloudYear < pcYear) endEvent(cloud) else cloud

        val baseMayor = base?.get(TRACKER_MAYOR)?.let(::parse)
        val baseYear = baseMayor?.let { number(it, "year").toInt() } ?: 0
        if (base == null || baseMayor == null || baseYear <= 0 || baseYear >= newest) return Sides(pcOut, cloudOut, base)
        // The event of the last sync ended since then
        var baseOut = endEvent(base)
        // Both sides count the new event from zero
        if (pcYear == cloudYear) baseOut = baseOut + (TRACKER_MAYOR to emptied(baseMayor, pcYear).toString())
        return Sides(pcOut, cloudOut, baseOut)
    }

    // The files after the mayor tracker went to the past events, like at an election. An empty tracker is not kept.
    private fun endEvent(files: Map<String, String>): Map<String, String> {
        val mayor = parse(files[TRACKER_MAYOR] ?: return files)
        if (isEmptyTracker(mayor)) return files
        val past = parse(files[PAST_EVENTS] ?: "{}")
        val events = past.get("events") as? JsonArray ?: JsonArray()
        // Past events keep no reset count
        events.add(mayor.deepCopy().apply { addProperty("resetCount", 0) })
        past.add("events", events)
        return files + (PAST_EVENTS to past.toString())
    }

    // A mayor tracker of the given year that has not counted anything yet
    private fun emptied(mayor: JsonObject, year: Int): JsonObject = mayor.deepCopy().apply {
        for (group in listOf("items", "mobs")) {
            getAsJsonObjectOrNull(group)?.let { counts ->
                counts.keySet().toList().forEach { key -> if (key != COA_START && isNumber(counts.get(key))) counts.addProperty(key, 0) }
            }
        }
        addProperty("year", year)
        addProperty("lastTrackedAt", 0)
    }

    // null if SBO cannot decide it safely
    private fun resolve(change: Change, pcFile: JsonObject, cloudFile: JsonObject, times: CloudTimes): Decided? {
        val pc = change.pc
        val cloud = change.cloud
        // Changed on one side only since the last sync: that side is right, also after a reset
        if (change.hasBase && pc == change.base) return Decided(cloud)
        if (change.hasBase && cloud == change.base) return Decided(pc)
        sureRule(change, pcFile, cloudFile)?.let { return it.takeIf { rule -> rule !== ASK } }
        // Only one side has it, without the last sync nothing says it was deleted on the other one
        if ((pc == null || cloud == null) && !change.hasBase) return Decided(pc ?: cloud)
        if (isCount(change)) return addCounts(change)
        return later(change, times)
    }

    // Rules that hold no matter which side changed what
    private fun sureRule(change: Change, pcFile: JsonObject, cloudFile: JsonObject): Decided? {
        val path = change.path
        val tracker = trackerPath(change.file, path)
        if (tracker != null) return when (tracker) {
            // These only grow, a change of CoA is handled in merge
            listOf("items", COA_START), listOf("lastTrackedAt"), listOf("resetCount") -> max(change.pc, change.cloud)
            listOf("edited") -> Decided(JsonPrimitive(isTrue(change.pc) || isTrue(change.cloud)))
            else -> null
        }
        if (change.file != ACHIEVEMENTS) return null
        // Reset on one side (year -1): without the last sync SBO cannot tell what is gone on purpose
        if (number(pcFile, "lastEventYear") < 0 || number(cloudFile, "lastEventYear") < 0) return ASK
        return when (path.firstOrNull()) {
            "lastEventYear" -> max(change.pc, change.cloud)
            // The list of this event: from the newer event, both sides' unlocks in the same event
            "currentEventAchievements" -> when (bigger(number(pcFile, "lastEventYear"), number(cloudFile, "lastEventYear"))) {
                CloudSide.PC -> Decided(change.pc)
                CloudSide.CLOUD -> Decided(change.cloud)
                null -> Decided(union(change.pc, change.cloud))
            }
            "achievements" -> Decided(union(change.pc, change.cloud))
            else -> null
        }
    }

    // The path inside a tracker, also for the trackers of past events
    private fun trackerPath(file: String, path: List<String>): List<String>? = when {
        file in TRACKER_FILES -> path
        file == PAST_EVENTS && path.firstOrNull() == "events" && path.size > 2 -> path.drop(2)
        else -> null
    }

    // Drop, mob and achievement counters: what both sides counted since the last sync is added up
    private fun isCount(change: Change): Boolean {
        val tracker = trackerPath(change.file, change.path)
        val counter = if (tracker != null) tracker.size == 2 && tracker[0] in setOf("items", "mobs") && tracker[1] != COA_START
            else change.file == ACHIEVEMENTS && change.path.firstOrNull() == "totalAchievements"
        return counter && isNumber(change.pc) && isNumber(change.cloud)
    }

    // The count of the last sync plus what each side counted since. null without the last sync or if a side went below it.
    // A counter the last sync did not have yet, like an event that ended on both PCs since, may hold counts from before it.
    private fun addCounts(change: Change): Decided? {
        val base = change.base
        if (!change.hasBase || !isNumber(base)) return null
        val start = base!!.asLong.coerceAtLeast(0L)
        val pc = change.pc!!.asLong
        val cloud = change.cloud!!.asLong
        if (pc < start || cloud < start) return null
        return Decided(JsonPrimitive(start + (pc - start) + (cloud - start)))
    }

    // Both sides changed it: the side that changed it last wins, if SBO knows both times
    private fun later(change: Change, times: CloudTimes): Decided? {
        val key = if (change.file in PER_FIELD) idOf(change.file, change.path) else change.file
        val pcTime = times.pc[key] ?: return null
        val cloudTime = times.cloud[key] ?: return null
        return when {
            pcTime > cloudTime -> Decided(change.pc)
            cloudTime > pcTime -> Decided(change.cloud)
            else -> null
        }
    }

    // The side that changed since the last sync; a guess if both did or the last sync is unknown
    private fun newer(pcChanged: Boolean, cloudChanged: Boolean, hasBase: Boolean, guess: () -> CloudSide?): Pair<CloudSide?, Boolean> {
        if (!hasBase || pcChanged == cloudChanged) return guess() to (hasBase && pcChanged)
        return (if (pcChanged) CloudSide.PC else CloudSide.CLOUD) to false
    }

    // What each area or field starts with: the newer side, otherwise this PC
    fun suggested(areas: List<CloudArea>): Map<String, CloudSide> = areas.flatMap { area ->
        if (area.perField) area.fields.map { it.id to (it.newer ?: CloudSide.PC) }
        else listOf(area.id to (area.newer ?: CloudSide.PC))
    }.toMap()

    // choices maps an area or field id to a side, anything missing stays as on this PC. Everything decided on its own is applied as well.
    fun merge(
        pc: Map<String, String>,
        cloud: Map<String, String>,
        comparison: CloudComparison,
        choices: Map<String, CloudSide>,
    ): Map<String, String> {
        // The same shape compare saw, an ended event already in the past events
        val sides = prepare(pc, cloud, null)
        val working = mutableMapOf<String, JsonObject>()
        sides.pc.forEach { (file, text) -> if (text != pc[file]) working[file] = read(file, text) }
        fun work(file: String): JsonObject = working.getOrPut(file) { read(file, sides.pc[file] ?: sides.cloud[file] ?: "{}") }
        val cloudJson = mutableMapOf<String, JsonObject>()
        fun cloudOf(file: String): JsonObject = cloudJson.getOrPut(file) { read(file, sides.cloud[file] ?: "{}") }

        // Whole files first, values inside them may follow
        comparison.auto.sortedBy { it.path.size }.forEach { auto ->
            if (auto.path.isEmpty()) working[auto.file] = (auto.value as? JsonObject ?: JsonObject()).deepCopy()
            else put(work(auto.file), auto.path, auto.value)
        }
        for (area in comparison.areas) {
            for (field in area.fields) {
                val side = choices[if (area.perField) field.id else area.id] ?: CloudSide.PC
                if (side == CloudSide.CLOUD) put(work(field.file), pathOf(field.file, field.id), get(cloudOf(field.file), pathOf(field.file, field.id)))
            }
        }
        // Different CoAs: the start coins may belong to the other one, the tracker sets them again without counting
        if (comparison.coaChanged) {
            TRACKER_FILES.filter { it in pc || it in cloud }.forEach { file ->
                put(work(file), listOf("items", COA_START), JsonPrimitive(-1))
            }
        }

        val merged = pc.toMutableMap()
        working.forEach { (file, json) ->
            // Unchanged files keep their text, the config keeps its comments
            if (pc[file] == null || json != read(file, pc.getValue(file))) merged[file] = restore(file, json).toString()
        }
        return merged
    }

    /** One hash per field (settings, SBO Data, sounds) or per whole file, to notice what changed and when. */
    fun fingerprints(files: Map<String, String>): Map<String, Int> {
        val out = mutableMapOf<String, Int>()
        for ([file, text] in files) {
            val json = read(file, text)
            if (file !in PER_FIELD) {
                out[file] = json.toString().hashCode()
                continue
            }
            fun walk(obj: JsonObject, depth: Int, prefix: List<String>) {
                val ignored = IGNORED_PATHS[file].orEmpty()
                for ((key, value) in obj.entrySet()) {
                    val path = prefix + key
                    if (key in IGNORED || path in ignored) continue
                    if (depth > 1 && value is JsonObject) walk(value, depth - 1, path)
                    else out[idOf(file, path)] = value.toString().hashCode()
                }
            }
            walk(json, if (file == CONFIG) 2 else 1, emptyList())
        }
        return out
    }

    private fun put(root: JsonObject, path: List<String>, value: JsonElement?) {
        if (value == null) remove(root, path) else set(root, path, value.deepCopy())
    }

    private fun groups(files: Set<String>): List<Pair<String, AreaDef>> {
        val known = AREAS.entries.filter { (_, def) -> def.files.any { it in files } }.map { it.key to it.value }
        val grouped = AREAS.values.flatMap { it.files }.toSet()
        val other = files.filter { it !in grouped }.sorted().map { it to AreaDef(label(it), listOf(it)) }
        return known + other
    }

    private fun parse(text: String): JsonObject =
        runCatching { JsonParser.parseString(text).asJsonObject }.getOrElse { JsonObject() }

    // A file in the shape it is compared in: empty texts that mean "not set" left out, past events keyed by year
    private fun read(file: String, text: String): JsonObject {
        val json = parse(text)
        EMPTY_IS_UNSET[file]?.forEach { key ->
            val value = json.get(key) as? JsonPrimitive
            if (value != null && value.isString && value.asString.isEmpty()) json.remove(key)
        }
        if (file == PAST_EVENTS) (json.get("events") as? JsonArray)?.let { events ->
            val byYear = JsonObject()
            events.forEachIndexed { i, event ->
                val name = if (event is JsonObject && event.has("year")) "Year ${number(event, "year").toLong()}" else "Entry ${i + 1}"
                var key = name
                var n = 2
                while (byYear.has(key)) key = "$name (${n++})"
                byYear.add(key, event)
            }
            json.add("events", byYear)
        }
        return json
    }

    // Back to the shape of the file, past events as a list from old to new
    private fun restore(file: String, json: JsonObject): JsonObject {
        if (file == PAST_EVENTS) (json.get("events") as? JsonObject)?.let { byYear ->
            val events = JsonArray()
            byYear.entrySet().map { it.value }
                .sortedBy { (it as? JsonObject)?.let { event -> number(event, "year") } ?: 0.0 }
                .forEach(events::add)
            json.add("events", events)
        }
        return json
    }

    private fun diff(
        file: String,
        pc: JsonObject,
        cloud: JsonObject,
        base: JsonObject?,
        hasBase: Boolean,
        depth: Int,
        prefix: List<String> = emptyList(),
    ): List<Change> {
        val ignored = IGNORED_PATHS[file].orEmpty()
        val keys = (pc.keySet() + cloud.keySet()).filter { it !in IGNORED && prefix + it !in ignored }
        return keys.flatMap { key ->
            val a = pc.get(key)
            val b = cloud.get(key)
            if (a == b) return@flatMap emptyList()
            val path = prefix + key
            val before = base?.get(key)
            if (depth > 1 && a is JsonObject && b is JsonObject) {
                return@flatMap diff(file, a, b, before as? JsonObject, hasBase, depth - 1, path)
            }
            listOf(Change(file, path, a, b, before, hasBase))
        }
    }

    private fun isNumber(value: JsonElement?): Boolean = value is JsonPrimitive && value.isNumber

    private fun isTrue(value: JsonElement?): Boolean = value is JsonPrimitive && value.isBoolean && value.asBoolean

    private fun max(pc: JsonElement?, cloud: JsonElement?): Decided? = when {
        isNumber(pc) && isNumber(cloud) -> Decided(if (pc!!.asDouble >= cloud!!.asDouble) pc else cloud)
        else -> null
    }

    // Unlocked on either side stays unlocked
    private fun union(pc: JsonElement?, cloud: JsonElement?): JsonElement? = when {
        pc is JsonObject && cloud is JsonObject -> pc.deepCopy().also { out ->
            cloud.entrySet().forEach { (key, value) -> out.add(key, union(out.get(key), value)) }
        }
        isTrue(pc) || isTrue(cloud) -> JsonPrimitive(true)
        else -> pc ?: cloud
    }

    // Counters and lists mostly grow, so the bigger one is likely newer
    private fun guessValue(pc: JsonElement?, cloud: JsonElement?): CloudSide? = when {
        pc is JsonPrimitive && cloud is JsonPrimitive && pc.isNumber && cloud.isNumber -> bigger(pc.asDouble, cloud.asDouble)
        pc is JsonArray && cloud is JsonArray -> bigger(pc.size().toDouble(), cloud.size().toDouble())
        else -> null
    }

    private fun guessArea(id: String, pc: (String) -> JsonObject?, cloud: (String) -> JsonObject?): CloudSide? {
        // Bigger score is newer, the next score only counts if this one is equal
        fun by(file: String, score: (JsonObject) -> Double): CloudSide? =
            bigger(pc(file)?.let(score) ?: 0.0, cloud(file)?.let(score) ?: 0.0)

        return when (id) {
            TRACKERS -> by(TRACKER_TOTAL, ::trackerCount)
                ?: by(TRACKER_MAYOR) { number(it, "year") }
                ?: by(TRACKER_MAYOR) { number(it, "lastTrackedAt") }
                ?: by(TRACKER_MAYOR, ::trackerCount)
                ?: by(TRACKER_SESSION, ::trackerCount)
            ACHIEVEMENTS -> by(ACHIEVEMENTS) { number(it, "lastEventYear") }
                ?: by(ACHIEVEMENTS) { (it.getAsJsonObject("achievements")?.size() ?: 0) + sum(it.get("totalAchievements")) }
            PAST_EVENTS -> by(PAST_EVENTS) { (it.getAsJsonArray("events")?.size() ?: 0).toDouble() }
                ?: by(PAST_EVENTS) { past -> past.getAsJsonArray("events")?.maxOfOrNull { number(it as? JsonObject ?: JsonObject(), "year") } ?: 0.0 }
            else -> null
        }
    }

    private fun bigger(pc: Double, cloud: Double): CloudSide? = when {
        pc > cloud -> CloudSide.PC
        cloud > pc -> CloudSide.CLOUD
        else -> null
    }

    private fun trackerCount(tracker: JsonObject): Double = sum(tracker.get("items")) + sum(tracker.get("mobs"))

    private fun number(json: JsonObject, key: String): Double =
        (json.get(key) as? JsonPrimitive)?.takeIf { it.isNumber }?.asDouble ?: 0.0

    // Sum of all numbers inside
    private fun sum(value: JsonElement?): Double = when (value) {
        is JsonPrimitive if value.isNumber -> value.asDouble
        is JsonObject -> value.entrySet().sumOf { sum(it.value) }
        is JsonArray -> value.sumOf { sum(it) }
        else -> 0.0
    }

    private fun idOf(file: String, path: List<String>): String = file + "\u0000" + path.joinToString("\u0000")

    private fun pathOf(file: String, id: String): List<String> = id.removePrefix(file + "\u0000").split("\u0000")

    private fun get(root: JsonObject, path: List<String>): JsonElement? {
        var current: JsonElement = root
        for (key in path) current = (current as? JsonObject)?.get(key) ?: return null
        return current
    }

    private fun set(root: JsonObject, path: List<String>, value: JsonElement) {
        var current = root
        for (key in path.dropLast(1)) {
            current = current.get(key) as? JsonObject ?: JsonObject().also { current.add(key, it) }
        }
        current.add(path.last(), value)
    }

    private fun remove(root: JsonObject, path: List<String>) {
        (get(root, path.dropLast(1)) as? JsonObject)?.remove(path.last())
    }

    fun display(value: JsonElement): String {
        val text = when (value) {
            is JsonPrimitive if value.isBoolean -> if (value.asBoolean) "On" else "Off"
            is JsonPrimitive if value.isString -> value.asString.ifEmpty { "(empty)" }
            is JsonPrimitive -> value.asString
            is JsonArray if value.isEmpty -> "(empty)"
            is JsonArray if value.size() <= 3 && value.all { it is JsonPrimitive } -> value.joinToString(", ") { display(it) }
            is JsonArray -> if (value.size() == 1) "1 entry" else "${value.size()} entries"
            is JsonObject -> if (value.size() == 1) "1 value" else "${value.size()} values"
            else -> "(empty)"
        }
        return if (text.length > MAX_TEXT) text.take(MAX_TEXT - 1) + "…" else text
    }

    // "mobsSinceInq" -> "Mobs Since Inq"
    fun humanize(key: String): String {
        if (key.contains(' ')) return key
        return key.replace('_', ' ')
            .replace(Regex("(?<=[a-z0-9])(?=[A-Z])"), " ")
            .split(' ')
            .asSequence()
            .filter { it.isNotEmpty() }
            .joinToString(" ") { it.replaceFirstChar(Char::uppercaseChar) }
    }
}
