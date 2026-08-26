package net.sbo.mod.utils.waypoint

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.diana.DianaTracker
import net.sbo.mod.diana.burrows.BurrowDetector
import net.sbo.mod.diana.guesses.ArrowGuessBurrow
import net.sbo.mod.settings.categories.Customization
import net.sbo.mod.settings.categories.Diana
import net.sbo.mod.settings.categories.General.HideOwnWaypoints
import net.sbo.mod.settings.categories.General.hideOwnWaypoints
import net.sbo.mod.settings.categories.General.patcherWaypoints
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.Helper.sleep
import net.sbo.mod.utils.Player
import net.sbo.mod.utils.SoundHandler.playCustomSound
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.data.SboDataObject
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.game.World
import net.sbo.mod.utils.math.SboVec
import net.sbo.mod.utils.render.RenderUtils3D
import net.sbo.mod.utils.render.WaypointRenderer
import java.awt.Color
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

private const val RARE_MOB_STALE_TICKS = 20
private const val RARE_MOB_VALIDATION_DISTANCE = 30.0

object WaypointManager {
    private val waypoints = ConcurrentHashMap<String, CopyOnWriteArrayList<Waypoint>>()
    private val rareMobs: Set<String> = setOf(
        "minos inquisitor",
        "inquisitor",
        "inq",
        "manticore",
        "king minos",
        "king",
        "sphinx"
    )

    fun init() {
        Register.command("sbosendping") { args ->
            val playerPos = Player.getLastPosition()
            if (args.isNotEmpty()) {
                Chat.pc("x: ${playerPos.x.roundToInt()}, y: ${playerPos.y.roundToInt() - 1}, z: ${playerPos.z.roundToInt()} | ${args.joinToString(" ")}")
            } else
                Chat.pc("x: ${playerPos.x.roundToInt()}, y: ${playerPos.y.roundToInt() - 1}, z: ${playerPos.z.roundToInt()}")
        }

        Register.onChatMessage(
            Regex("^(?<channel>.*> )?(?<playerName>.+?)[§&]f: (?:[§&]r)?x: (?<x>[^ ,]+),? y: (?<y>[^ ,]+),? z: (?<z>[^ ,]+)(?<trailing>.*)$")
        ) { _, match ->
            val channel = match.groups["channel"]?.value ?: "Unknown"
            val player = match.groups["playerName"]?.value ?: "Unknown"
            val world = mc.level ?: return@onChatMessage

            val x = match.groups["x"]?.value?.toIntOrNull() ?: 0
            var y = match.groups["y"]?.value?.toIntOrNull() ?: 0
            val z = match.groups["z"]?.value?.toIntOrNull() ?: 0
            y = findBlock(world, x, y, z)

            val trailing = match.groups["trailing"]?.value ?: ""
            val mob = trailing.replace("|", "").trim().lowercase()
            val selfName = Player.getName() ?: ""
            if (!channel.contains("Guild")) {
                if (rareMobs.contains(mob) && Diana.receiveRareMob) {
                    val mobType: Diana.ReceiveList = when (mob) {
                        "minos inquisitor", "inquisitor", "inq" -> Diana.ReceiveList.INQ
                        "king minos", "king" -> Diana.ReceiveList.KING
                        "manticore" -> Diana.ReceiveList.MANTICORE
                        "sphinx" -> Diana.ReceiveList.SPHINX
                        else -> Diana.ReceiveList.OTHER
                    }
                    if (mobType !in Diana.ReceiveMobs) return@onChatMessage

                    val pos = SboVec(x.toDouble(), y.toDouble(), z.toDouble())

                    val existing = getWaypointsOfType("rareMob")
                    if (existing.any { it.pos.distanceTo(pos) <= 10 }) {
                        return@onChatMessage
                    }

                    val isOwnSpawn = player.contains(selfName)
                    val mobDisplayName = notifyRareMob(player, mobType)

                    addRareMobWaypoint(
                        player,
                        pos,
                        mobType,
                        mobDisplayName,
                        isOwnSpawn
                    )
                } else if (patcherWaypoints) {
                    if (hideOwnWaypoints.contains(HideOwnWaypoints.NORMAL) && player.contains(selfName)) return@onChatMessage

                    addWaypoint(Waypoint(player, x.toDouble(), y.toDouble(), z.toDouble(), ttl = 30, type = "world"))
                }
            }
        }

        Register.onTick(1) { _ ->
            if (Diana.receiveRareMob && World.getWorld() == "Hub") {
                updateRareMobWaypoints()
            }

            val playerPos = Player.getLastPosition()

            val knownBurrows = getWaypointsOfType("burrow")
            val shovelGuesses = getWaypointsOfType("guess")
            val arrowGuesses = getWaypointsOfType("arrow")
            val subGuesses = getWaypointsOfType("subGuess")

            // These do not change location when using the shovel
            val allStaticBurrowWaypoints = knownBurrows + arrowGuesses

            // Remove all TTL expired waypoints
            val currentTime = System.nanoTime()

            this.forEachWaypoint { waypoint ->
                if (waypoint.ttl > 0L && currentTime - waypoint.creationNs > TimeUnit.SECONDS.toNanos(waypoint.ttl)) {
                    removeWaypoint(waypoint)

                    val waypointPos = waypoint.pos

                    if (waypoint.type == "arrow") {
                        ArrowGuessBurrow.removeFromInternalState(waypointPos)
                    } else if (waypoint.type == "burrow") {
                        BurrowDetector.removeFromInternalState(waypointPos)
                    }
                }
            }

            // Remove all waypoints that are not in radius of typical burrow locations x y z
            this.forEachWaypoint { waypoint ->
                if (World.getWorld() != "Hub" || waypoint.preventInvalidRemoval || waypoint.type == "world") return@forEachWaypoint

                val underWorld = waypoint.pos.y < 60
                val aboveWorld = waypoint.pos.y > 105
                val outsideZ = waypoint.pos.z > 205
                val outsideX = waypoint.pos.x > 175
                val outsideNegativeZ = waypoint.pos.z < 0 && -waypoint.pos.z > 208
                val outsideNegativeX = waypoint.pos.x < 0 && -waypoint.pos.x > 283

                if (underWorld || aboveWorld || outsideZ || outsideX || outsideNegativeZ || outsideNegativeX) {
                    removeWaypoint(waypoint)

                    val waypointPos = waypoint.pos

                    if (waypoint.type == "arrow") {
                        ArrowGuessBurrow.removeOrMoveFromInternalState(waypointPos)
                    } else if (waypoint.type == "burrow") {
                        BurrowDetector.removeFromInternalState(waypointPos)
                    }
                }
            }

            // Remove shovel guesses pointing to invalid burrow locations
            shovelGuesses.forEach { shovelGuess ->
                if (World.getWorld() != "Hub") return@forEach

                if (!ArrowGuessBurrow.isBlockValid(shovelGuess.pos)) {
                    removeWaypoint(shovelGuess)
                }
            }

            // Remove arrow guesses pointing to invalid burrow locations after being existing for over 15 seconds (During 15 seconds period, we hide them instead to give time for moveToNext to do its job)
            arrowGuesses.forEach { arrowGuess ->
                if (World.getWorld() != "Hub") return@forEach

                if (!ArrowGuessBurrow.isBlockValid(arrowGuess.pos)) {
                    if (arrowGuess.isOlderThan(Duration.ofSeconds(15))) {
                        removeWaypoint(arrowGuess)
                        ArrowGuessBurrow.removeOrMoveFromInternalState(arrowGuess.pos)
                    } else {
                        arrowGuess.hidden = true
                    }
                } else {
                    arrowGuess.hidden = false
                }
            }

            // Remove all invalid sub guesses as they can be under many grass blocks and unreachable often
            subGuesses.forEach { subGuess ->
                if (World.getWorld() != "Hub") return@forEach

                if (!ArrowGuessBurrow.isBlockValid(subGuess.pos)) {
                    removeWaypoint(subGuess)

                    ArrowGuessBurrow.removeSubGuessFromInternalState(subGuess.pos)
                }
            }

            // Remove the shovel guess if a known burrow, or an arrow guess exists at the same block, or 30 blocks near it (contrary to the name, precise guess is less precise than arrow guess)
            shovelGuesses.forEach { shovelGuess ->
                val shovelGuessBlock = shovelGuess.pos.roundLocationToBlock()

                allStaticBurrowWaypoints.firstOrNull { staticBurrow ->
                    val waypointBlock = staticBurrow.pos.roundLocationToBlock()

                    waypointBlock == shovelGuessBlock || waypointBlock.distanceTo(shovelGuessBlock) <= 30
                }?.let { staticBurrow ->
                    staticBurrow.carryOverState(shovelGuess)
                    removeWaypoint(shovelGuess)
                }
            }

            // Remove duplicate shovel guesses that are within 30 blocks of each other
            shovelGuesses.forEachIndexed { index, shovelGuess ->
                val shovelGuessBlock = shovelGuess.pos.roundLocationToBlock()

                shovelGuesses.drop(index + 1).firstOrNull { otherGuess ->
                    shovelGuessBlock.distanceTo(otherGuess.pos.roundLocationToBlock()) <= 30
                }?.let { otherGuess ->
                    val keep = if (shovelGuess.hasStrongerStateThan(otherGuess)) shovelGuess else otherGuess
                    val remove = if (keep === shovelGuess) otherGuess else shovelGuess

                    keep.carryOverState(remove)
                    removeWaypoint(remove)
                }
            }

            // Remove the arrow guesses representing the same blocks as an already-known treasure/mob/start burrow, and transfer its state to the known burrow instead if the arrow guess was dug more times than the known burrow
            arrowGuesses.forEach { arrowGuess ->
                val arrowGuessBlock = arrowGuess.pos.roundLocationToBlock()

                knownBurrows.firstOrNull { knownBurrow ->
                    knownBurrow.pos.roundLocationToBlock() == arrowGuessBlock
                }?.let { knownBurrow ->
                    knownBurrow.carryOverState(arrowGuess)
                    removeWaypoint(arrowGuess)

                    ArrowGuessBurrow.removeFromInternalState(arrowGuess.pos)
                }
            }

            val bestGuessWp = getBestGuess()

            val rareWp = getWaypointsOfType("rareMob")

            this.forEachWaypoint { waypoint ->
                waypoint.isClosest = waypoint == bestGuessWp
                waypoint.format(rareWp)
            }
        }

        LevelRenderEvents.COLLECT_SUBMITS.register(WaypointRenderer)
    }

    private fun notifyRareMob(player: String, mobType: Diana.ReceiveList): String {
        return when (mobType) {
            Diana.ReceiveList.INQ -> {
                Helper.showTitle(
                    "§r§6§l<§b§l§kO§6§l> §d§lINQUISITOR! §6§l<§b§l§kO§6§l>",
                    player.ifEmpty { null },
                    Diana.rareMobTitleFadeInTime,
                    Diana.rareMobTitleStayTime,
                    Diana.rareMobTitleFadeOutTime
                )
                playCustomSound(
                    SboDataObject.soundSettingsData.inqSound,
                    volume = SboDataObject.soundSettingsData.inqVolume
                )
                "§dInquisitor"
            }

            Diana.ReceiveList.KING -> {
                Helper.showTitle(
                    "§r§6§l<§b§l§kO§6§l> §6§lKING MINOS! §6§l<§b§l§kO§6§l>",
                    player.ifEmpty { null },
                    Diana.rareMobTitleFadeInTime,
                    Diana.rareMobTitleStayTime,
                    Diana.rareMobTitleFadeOutTime
                )
                playCustomSound(
                    SboDataObject.soundSettingsData.kingSound,
                    volume = SboDataObject.soundSettingsData.kingVolume
                )
                "§6King Minos"
            }

            Diana.ReceiveList.MANTICORE -> {
                Helper.showTitle(
                    "§r§6§l<§b§l§kO§6§l> §2§lMANTICORE! §6§l<§b§l§kO§6§l>",
                    player.ifEmpty { null },
                    Diana.rareMobTitleFadeInTime,
                    Diana.rareMobTitleStayTime,
                    Diana.rareMobTitleFadeOutTime
                )
                playCustomSound(
                    SboDataObject.soundSettingsData.mantiSound,
                    volume = SboDataObject.soundSettingsData.mantiVolume
                )
                "§2Manticore"
            }

            Diana.ReceiveList.SPHINX -> {
                Helper.showTitle(
                    "§r§6§l<§b§l§kO§6§l> §9§lSPHINX! §6§l<§b§l§kO§6§l>",
                    player.ifEmpty { null },
                    Diana.rareMobTitleFadeInTime,
                    Diana.rareMobTitleStayTime,
                    Diana.rareMobTitleFadeOutTime
                )
                playCustomSound(
                    SboDataObject.soundSettingsData.sphinxSound,
                    volume = SboDataObject.soundSettingsData.sphinxVolume
                )
                "§9Sphinx"
            }

            else -> {
                Helper.showTitle(
                    "§r§6§l<§b§l§kO§6§l> §3§lRARE MOB! §6§l<§b§l§kO§6§l>",
                    player.ifEmpty { null },
                    Diana.rareMobTitleFadeInTime,
                    Diana.rareMobTitleStayTime,
                    Diana.rareMobTitleFadeOutTime
                )
                playCustomSound(
                    SboDataObject.soundSettingsData.rareMobSound,
                    volume = SboDataObject.soundSettingsData.rareMobVolume
                )
                "§3Rare Mob"
            }
        }
    }

    private fun floorToGround(level: ClientLevel, pos: SboVec): SboVec {
        val x = pos.x.roundToInt()
        val y = pos.y.roundToInt()
        val z = pos.z.roundToInt()

        val isGrass: (BlockState) -> Boolean = {
            it.`is`(Blocks.GRASS_BLOCK)
        }

        val isSolid: (BlockState) -> Boolean = {
            !it.isAir()
        }

        // 1. Prefer grass directly below.
        findGroundY(level, x, y, z, isGrass)?.let {
            return SboVec(x.toDouble(), it.toDouble(), z.toDouble())
        }

        // 2. Prefer nearby grass over nearby solid blocks.
        findNearby(level, x, y, z, isGrass)?.let {
            return it
        }

        // 3. Fallback to any solid block directly below.
        findGroundY(level, x, y, z, isSolid)?.let {
            return SboVec(x.toDouble(), it.toDouble(), z.toDouble())
        }

        // 4. Last solid fallback.
        findNearby(level, x, y, z, isSolid)?.let {
            return it
        }

        return pos
    }

    private fun findGroundY(
        level: ClientLevel,
        x: Int,
        startY: Int,
        z: Int,
        predicate: (BlockState) -> Boolean,
        maxDepth: Int = 10
    ): Int? {
        var y = startY
        var depth = 0

        while (y > level.minY && depth++ < maxDepth) {
            val state = level.getBlockState(BlockPos(x, y, z))
            if (predicate(state)) {
                return y
            }
            y--
        }

        return null
    }

    private fun findNearby(
        level: ClientLevel,
        x: Int,
        startY: Int,
        z: Int,
        predicate: (BlockState) -> Boolean
    ): SboVec? {
        var best: BlockPos? = null
        var bestDistance = Double.MAX_VALUE

        for (dx in -3..3) {
            for (dz in -3..3) {
                val groundY = findGroundY(
                    level,
                    x + dx,
                    startY,
                    z + dz,
                    predicate
                ) ?: continue

                val candidate = BlockPos(x + dx, groundY, z + dz)
                val distance = candidate.distSqr(BlockPos(x, startY, z))

                if (distance < bestDistance) {
                    bestDistance = distance
                    best = candidate
                }
            }
        }

        return best?.let {
            SboVec(
                it.x.toDouble(),
                it.y.toDouble(),
                it.z.toDouble()
            )
        }
    }

    private fun updateRareMobWaypoints() {
        val level = mc.level ?: return
        val player = mc.player ?: return

        val shouldAddWaypoints = Diana.scanWorldForRareMob

        val existing = getWaypointsOfType("rareMob").toMutableList()
        val rareMobPositions = mutableListOf<SboVec>()

        level.entitiesForRendering().forEach { entity ->
            val stand = entity as? ArmorStand ?: return@forEach

            val name = stand.customName?.string ?: return@forEach
            if (name.contains("0/")) return@forEach

            val (mobType, mobName) = when {
                name.contains("King Minos") -> Diana.ReceiveList.KING to "King Minos"
                name.contains("Minos Inquisitor") -> Diana.ReceiveList.INQ to "Minos Inquisitor"
                name.contains("Manticore") -> Diana.ReceiveList.MANTICORE to "Manticore"
                name.contains("Sphinx") -> Diana.ReceiveList.SPHINX to "Sphinx"
                else -> return@forEach
            }

            if (mobType !in Diana.ReceiveMobs) return@forEach
            if (stand.isDeadOrDying()) return@forEach

            val standPos = SboVec(stand.x, stand.y, stand.z)
            rareMobPositions += standPos

            if (!shouldAddWaypoints) return@forEach

            if (existing.none { it.pos.distanceTo(standPos) <= 60 }) {
                // Best-effort to not be considered a cheat
                if (!player.hasLineOfSight(entity)) return@forEach

                val pos = floorToGround(level, standPos)

                val isOwnSpawn =
                    DianaTracker.lastSpawnedMob == mobName &&
                    System.nanoTime() - DianaTracker.lastSpawnedMobTime <= TimeUnit.SECONDS.toNanos(5)

                addRareMobWaypoint(
                    player = "",
                    pos = pos,
                    mobType = mobType,
                    mobDisplayName = notifyRareMob("", mobType),
                    isOwnSpawn
                )?.let(existing::add)
            }
        }

        removeStaleRareMobWaypoints(level, rareMobPositions)
    }

    private fun addRareMobWaypoint(
        player: String,
        pos: SboVec,
        mobType: Diana.ReceiveList,
        mobDisplayName: String,
        isOwnSpawn: Boolean
    ): Waypoint? {
        if (isOwnSpawn) {
            when (mobType) {
                Diana.ReceiveList.INQ -> if (hideOwnWaypoints.contains(HideOwnWaypoints.INQ)) return null
                Diana.ReceiveList.KING -> if (hideOwnWaypoints.contains(HideOwnWaypoints.KING)) return null
                Diana.ReceiveList.MANTICORE -> if (hideOwnWaypoints.contains(HideOwnWaypoints.MANTICORE)) return null
                Diana.ReceiveList.SPHINX -> if (hideOwnWaypoints.contains(HideOwnWaypoints.SPHINX)) return null
                else -> {}
            }
        }

        val owner = if (player.isNotEmpty()) " §7($player§7)" else ""
        val waypoint = Waypoint("$mobDisplayName$owner", pos.x, pos.y, pos.z, ttl = 45, type = "rareMob")

        addWaypoint(waypoint)
        return waypoint
    }

    private fun removeStaleRareMobWaypoints(
        level: ClientLevel,
        rareMobPositions: List<SboVec>
    ) {
        val player = mc.player ?: return
        val playerPos = SboVec(player.x, player.y, player.z)

        getWaypointsOfType("rareMob").forEach { waypoint ->
            if (playerPos.distanceTo(waypoint.pos) > RARE_MOB_VALIDATION_DISTANCE) {
                waypoint.rareMobMissingTicks = 0
                return@forEach
            }

            val mobPresent = rareMobPositions.any {
                it.distanceTo(waypoint.pos) <= RARE_MOB_VALIDATION_DISTANCE
            }

            if (mobPresent) {
                waypoint.rareMobMissingTicks = 0
                return@forEach
            }

            waypoint.rareMobMissingTicks++

            if (waypoint.rareMobMissingTicks >= RARE_MOB_STALE_TICKS) {
                removeWaypoint(waypoint)
            }
        }
    }

    /**
     * Renders all waypoints in the management system.
     * @param context The world render context.
     */
    fun renderAllWaypoints(context: LevelRenderContext) {
        if (World.getWorld() != "Hub" || !Helper.hasSpade) {
            getWaypointsOfType("world").forEach { waypoint ->
                waypoint.render(context)
            }
            return
        }

        this.forEachWaypoint { waypoint ->
            waypoint.render(context)
        }

        renderSubGuessLines(context)
        renderGuessChainLines(context)
    }

    private fun renderSubGuessLines(context: LevelRenderContext) {
        if (!Diana.showArrowSubGuesses) return

        val color = Color(Customization.SubGuessColor)

        val rgb = floatArrayOf(
            color.red / 255f,
            color.green / 255f,
            color.blue / 255f
        )

        ArrowGuessBurrow.allGuesses.forEach { guess ->
            guess.getVisibleChain()
                .zipWithNext { a, b ->
                    val opacity =
                        (
                            if (Customization.dynamicWaypointOpacity) {
                                val distance = Player.getLastPosition().distanceTo(
                                    b.center()
                                )

                                calculateDynamicOpacity(distance)
                            } else {
                                Customization.waypointOpacity / 100f
                            }
                        )
                            .coerceIn(0.4f, 1f) // In-world path lines need a higher minimum opacity for readability.

                    RenderUtils3D.drawLine(
                        context,
                        a.center().toVec3d().add(0.0, 0.5, 0.0),
                        b.center().toVec3d().add(0.0, 0.5, 0.0),
                        rgb,
                        Diana.dianaLineWidth.toFloat(),
                        opacity
                    )
                }
        }
    }

    private fun buildGreedyGuessChain(startPos: SboVec): List<Waypoint> {
        val remaining = getAllGuessesAndBurrows()
            .asSequence()
            .filter { !it.hidden }
            .toMutableList()

        if (remaining.isEmpty()) return emptyList()

        val chain = mutableListOf<Waypoint>()
        var currentPos = startPos

        while (remaining.isNotEmpty()) {
            val next = remaining.minByOrNull { wp ->
                if (Diana.ignoreYLevel) {
                    wp.pos.distanceToIgnoringY(currentPos)
                } else {
                    wp.pos.distanceTo(currentPos)
                }
            } ?: break

            chain += next
            remaining.remove(next)
            currentPos = next.pos
        }

        return chain
    }

    private fun renderGuessChainLines(context: LevelRenderContext) {
        if (!Diana.drawOptimalOrderLines) return

        val chain = buildGreedyGuessChain(Player.getLastPosition())
        if (chain.size < 2) return

        val color = Color(Customization.OptimalOrderLineColor)
        val rgb = floatArrayOf(
            color.red / 255f,
            color.green / 255f,
            color.blue / 255f
        )

        chain.zipWithNext().forEachIndexed { index, (a, b) ->
            val bDistance = Player.getLastPosition().distanceTo(b.pos)

            // Always show the first line.
            // Only show the 2nd and 3rd lines if the destination is nearby.
            if (index > 0 && (index > 2 || bDistance > 50)) {
                return@forEachIndexed
            }

            val opacity =
                (
                    if (Customization.dynamicWaypointOpacity) {
                        calculateDynamicOpacity(bDistance)
                    } else {
                        Customization.waypointOpacity / 100f
                    }
                ).coerceIn(0.3f, 0.5f)

            RenderUtils3D.drawLine(
                context,
                a.pos.toVec3d().add(0.0, 0.5, 0.0),
                b.pos.toVec3d().add(0.0, 0.5, 0.0),
                rgb,
                (Diana.dianaLineWidth.toFloat() / 1.6f).coerceIn(1.0f, 20.0f),
                opacity
            )
        }
    }

    /**
     * Adds a waypoint to the management system.
     * @param waypoint The waypoint to add.
     */
    fun addWaypoint(waypoint: Waypoint, playSound: Boolean = true) {
        val type = waypoint.type

        waypoints.computeIfAbsent(type) { CopyOnWriteArrayList() }.add(waypoint)

        if (type == "burrow" && playSound) {
            playCustomSound(SboDataObject.soundSettingsData.burrowFoundSound, volume = SboDataObject.soundSettingsData.burrowVolume)
        }
    }

    /**
     * Removes a specific waypoint from the management system.
     * @param waypoint The waypoint to remove.
     */
    fun removeWaypoint(waypoint: Waypoint) {
        waypoints[waypoint.type]?.remove(waypoint)
    }

    /**
     * Gets a waypoint at a specific position and type, or null if it does not exist.
     * @param pos The position of the waypoint to get.
     * @param type The type of the waypoint to get.
     */
    fun getWaypointAt(pos: SboVec, type: String): Waypoint? {
        val list = waypoints[type]
        val waypoint = list?.find { it.pos.roundLocationToBlock() == pos.roundLocationToBlock() }
        if (waypoint != null) {
            return waypoint
        }
        return null
    }

    /**
     * Removes a waypoint at a specific position and type.
     * @param pos The position of the waypoint to remove.
     * @param type The type of the waypoint to remove.
     */
    fun removeWaypointAt(pos: SboVec, type: String) {
        val list = waypoints[type]
        val waypoint = list?.find { it.pos.roundLocationToBlock() == pos.roundLocationToBlock() }
        if (waypoint != null) {
            list.remove(waypoint)
        }
    }

    /**
     * Removes all waypoints of a specific type.
     * @param type The type of waypoints to remove.
     */
    fun removeAllOfType(type: String) {
        waypoints[type]?.clear()
    }

    /**
     * Removes limit number of waypoints of a specific type that are within a certain distance from the given position.
     * @param type The type of waypoints to remove.
     */
    private fun removeWithinDistanceFrom(pos: SboVec, type: String, distance: Int, limit: Int) {
        if (limit <= 0) return

        val list = waypoints[type] ?: return

        var removed = 0
        val iterator = list.iterator()

        while (iterator.hasNext() && removed < limit) {
            if (iterator.next().pos.distanceTo(pos) < distance) {
                iterator.remove()
                removed++
            }
        }
    }

    /**
     * Adds a spade guess waypoint.
     * @param pos The position for the spade guess waypoint.
     */
    fun addSpadeGuess(pos: SboVec?) {
        if (pos == null) return

        if (!waypointExists("burrow", pos).first) {
            val waypoint = Waypoint("Spade Guess", pos.x, pos.y, pos.z, type = "guess")
            addWaypoint(waypoint)
        }
    }

    fun addArrowGuess(pos: SboVec?) {
        if (pos == null) return
        val exists = getWaypointsOfType("arrow").any { it.pos.roundLocationToBlock() == pos.roundLocationToBlock() }
        if (exists) return
        addWaypoint(
            Waypoint(
                text = "Guess",
                x = pos.x,
                y = pos.y,
                z = pos.z,
                type = "arrow"
            )
        )
    }

    fun addArrowSubGuess(pos: SboVec?) {
        if (pos == null) return

        val exists = getWaypointsOfType("subGuess")
            .any { it.pos.roundLocationToBlock() == pos.roundLocationToBlock() }

        if (exists) return

        addWaypoint(
            Waypoint(
                text = if (Diana.showTextOnSubGuess) "Possible" else "",
                x = pos.x,
                y = pos.y,
                z = pos.z,
                type = "subGuess"
            )
        )
    }

    fun removeArrowSubGuess(pos: SboVec) {
        removeWaypointAt(pos, "subGuess")
    }

    /**
     * Checks if a waypoint of a specific type exists at a given position.
     * @param type The type of the waypoint to check.
     * @param pos The position to check for the waypoint.
     * @return A pair containing a boolean indicating existence and the waypoint if found.
     */
    fun waypointExists(type: String, pos: SboVec): Pair<Boolean, Waypoint?> {
        val waypoint = getWaypointsOfType(type).find { it.pos.roundLocationToBlock() == pos.roundLocationToBlock() }
        return (waypoint != null) to waypoint
    }

    /**
     * Iterates over all waypoints and applies the given action to each.
     * Safe for concurrent use.
     * @param action The action to apply to each waypoint.
     */
    private fun forEachWaypoint(action: (Waypoint) -> Unit) {
        waypoints.values.flatten().forEach(action)
    }

    /**
     * Retrieves all waypoints of a specific type.
     * @param type The type of waypoints to retrieve.
     * @return A list of waypoints of the specified type.
     */
    fun getWaypointsOfType(type: String): List<Waypoint> {
        return waypoints[type] ?: emptyList()
    }

    private fun getWarpPoint(name: String): WarpPoint? {
        return hubWarps[name] ?: additionalHubWarps[name]
    }

    fun getAllGuessesAndBurrows(): List<Waypoint> {
        return getWaypointsOfType("burrow") + getWaypointsOfType("arrow") + getWaypointsOfType("guess")
    }

    private fun getBestGuess(): Waypoint? {
        return getBestGuessAt(Player.getLastPosition())
    }

    private fun getBestGuessAt(pos: SboVec): Waypoint? {
        return getAllGuessesAndBurrows()
            .filter { !it.hidden }
            .minByOrNull { if (Diana.ignoreYLevel) it.pos.distanceToIgnoringY(pos) else it.pos.distanceTo(pos) }
    }

    private fun getClosestWarp(pos: SboVec): String? = getClosestWarp(pos, Player.getLastPosition())

    /**
     * Gets the closest warp point to a given position.
     * @param pos The position to find the closest warp to.
     * @param playerPos The player's position to warp from.
     * @return The name of the closest warp, or null if no warps are available.
     */
    private fun getClosestWarp(pos: SboVec, playerPos: SboVec): String? {
        if (BurrowDetector.pendingUseSpadeTitle != null) {
            // Prevent warping before using spade.
            return null
        }

        var warps = hubWarps.filter { it.value.unlocked }.mapValues { it.value }
        for (warp in Diana.allowedWarps) {
            val warpName = warp.name.lowercase()
            if (additionalHubWarps.containsKey(warpName)) {
                val additionalWarp = additionalHubWarps[warpName]
                if (additionalWarp != null && additionalWarp.unlocked) {
                    warps = warps + (warpName to additionalWarp)
                }
            }
        }

        var playerDistance = if (Diana.ignoreYLevel) pos.distanceToIgnoringY(playerPos) else pos.distanceTo(playerPos)

        var closestWarp: String? = null
        var closestWarpPoint: WarpPoint? = null
        var closestDistance = Double.MAX_VALUE

        var secondClosestWarp: String? = null
        var secondClosestWarpPoint: WarpPoint? = null
        var secondClosestDistance = Double.MAX_VALUE

        for ((name, warp) in warps) {
            val distance = if (Diana.ignoreYLevel) pos.distanceToIgnoringY(warp.pos) else pos.distanceTo(warp.pos)

            if (distance < closestDistance) {
                secondClosestWarp = closestWarp
                secondClosestWarpPoint = closestWarpPoint
                secondClosestDistance = closestDistance

                closestWarp = name
                closestWarpPoint = warp
                closestDistance = distance
            } else if (distance < secondClosestDistance) {
                secondClosestWarp = name
                secondClosestWarpPoint = warp
                secondClosestDistance = distance
            }
        }

        val preferredAgainst = secondClosestWarpPoint?.preferWarpAgainstCompetitive

        if (Diana.badWarpDistance > 0 && preferredAgainst != null && preferredAgainst == closestWarpPoint?.warpType && secondClosestDistance - closestDistance < Diana.badWarpDistance) {
            closestWarp = secondClosestWarp
            closestWarpPoint = secondClosestWarpPoint
            closestDistance = secondClosestDistance
        }

        val extra = closestWarpPoint?.extraBlocks ?: 0

        val warpIsWorthIt =
            playerDistance > closestDistance + Diana.warpDiff + extra

        val targetIsFarEnough =
            !Diana.dontWarpIfBurrowClose ||
            playerDistance > 60

        val playerToWarpDistance = closestWarpPoint?.let {
            if (Diana.ignoreYLevel) {
                playerPos.distanceToIgnoringY(it.pos)
            } else {
                playerPos.distanceTo(it.pos)
            }
        } ?: Double.MAX_VALUE

        val warpIsFarEnough =
            !Diana.dontWarpIfBurrowClose ||
            playerToWarpDistance > 60

        return if (warpIsWorthIt && targetIsFarEnough && warpIsFarEnough)
            closestWarp
        else
            null
    }

    /**
     * Gets the final closest warp point to a given target position.
     * This method handles the case where a new warp would be suggested after warping to the current suggested warp by simulating up to four
     * warps and picking the final warp.
     * @param targetPos The target position to find the closest warp to.
     * @return The name of the closest warp, or null if no warps are available.
     */
    fun getFinalClosestWarp(targetPos: SboVec): String? {
        var simulatedPlayerPos = Player.getLastPosition()
        var simulatedTargetPos = targetPos
        var lastWarp: String? = null

        repeat(4) {
            val warp = getClosestWarp(simulatedTargetPos, simulatedPlayerPos) ?: return lastWarp
            if (warp == lastWarp) return warp

            lastWarp = warp
            val warpPoint = getWarpPoint(warp) ?: return warp
            val nextGuess = getBestGuessAt(warpPoint.pos) ?: return warp

            simulatedPlayerPos = warpPoint.pos
            simulatedTargetPos = nextGuess.pos
        }

        return lastWarp
    }

    fun getFinalClosestWarpToFixedTarget(targetPos: SboVec): String? {
        var simulatedPlayerPos = Player.getLastPosition()
        var lastWarp: String? = null

        repeat(4) {
            val warp = getClosestWarp(targetPos, simulatedPlayerPos) ?: return lastWarp
            if (warp == lastWarp) return warp

            lastWarp = warp
            val warpPoint = getWarpPoint(warp) ?: return warp
            simulatedPlayerPos = warpPoint.pos
        }

        return lastWarp
    }

    fun warpToGuess() {
        val bestGuess = getBestGuess() ?: return
        getFinalClosestWarp(bestGuess.pos)?.let { executeWarpCommand(it) } ?: return
    }

    fun warpToRareMob() {
        val newestRareMob = getWaypointsOfType("rareMob").maxByOrNull { it.creationNs }
        val pos = newestRareMob?.pos ?: return
        val warp = getFinalClosestWarpToFixedTarget(pos) ?: return

        executeWarpCommand(warp)
    }

    fun warpBoth() {
        if (getWaypointsOfType("rareMob").isEmpty()) {
            warpToGuess()
        } else {
            warpToRareMob()
        }
    }

    private var tryWarp: Boolean = false

    private fun executeWarpCommand(warp: String) {
        if (World.getWorld() != "Hub" || !Helper.hasSpade) return
        if (warp.isNotEmpty() && !tryWarp) {
            tryWarp = true
            Chat.command("warp $warp")
            sleep(500) {
                mc.execute {
                    tryWarp = false
                }
            }
        }
    }

    private fun findBlock(world: ClientLevel, x: Int, y: Int, z: Int): Int {
        var currentY = y
        while (currentY > world.minY) {
            val pos = BlockPos(x, currentY, z)
            val blockState = world.getBlockState(pos)
            if (!blockState.isAir) {
                return currentY
            }
            currentY--
        }
        // fallback if no block found
        return y
    }
}
