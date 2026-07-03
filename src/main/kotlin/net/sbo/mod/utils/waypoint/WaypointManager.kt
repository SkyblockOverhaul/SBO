package net.sbo.mod.utils.waypoint

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.diana.guesses.ArrowGuessBurrow
import net.sbo.mod.diana.burrows.BurrowDetector
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
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.game.World
import net.sbo.mod.utils.math.SboVec
import net.sbo.mod.utils.render.WaypointRenderer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

object WaypointManager {
    private val waypoints = ConcurrentHashMap<String, CopyOnWriteArrayList<Waypoint>>()
    private var closestWaypoint: Pair<Waypoint?, Double> = null to 1000.0
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
            val playerName = Player.getName() ?: ""
            if (!channel.contains("Guild")) {
                if ((!trailing.startsWith(" ") || rareMobs.contains(mob) || Diana.allWaypointsAreInqs) && Diana.receiveRareMob) {
                    val mobType: Diana.ReceiveList = when (mob) {
                        "minos inquisitor", "inquisitor", "inq" -> Diana.ReceiveList.INQ
                        "king minos", "king" -> Diana.ReceiveList.KING
                        "manticore" -> Diana.ReceiveList.MANTICORE
                        "sphinx" -> Diana.ReceiveList.SPHINX
                        else -> Diana.ReceiveList.OTHER
                    }
                    if (mobType !in Diana.ReceiveMobs) return@onChatMessage

                    val mobDisplayName = when (mobType) {
                        Diana.ReceiveList.INQ -> {
                            Helper.showTitle("§r§6§l<§b§l§kO§6§l> §d§lINQUISITOR! §6§l<§b§l§kO§6§l>", player, Diana.rareTitleFadeIn, Diana.rareTitleTime, Diana.rareTitleFadeOut)
                            playCustomSound(Customization.inqSound[0], Customization.inqVolume)
                            "§dInquisitor"
                        }
                        Diana.ReceiveList.KING -> {
                            Helper.showTitle("§r§6§l<§b§l§kO§6§l> §6§lKING MINOS! §6§l<§b§l§kO§6§l>", player, Diana.rareTitleFadeIn, Diana.rareTitleTime, Diana.rareTitleFadeOut)
                            playCustomSound(Customization.kingSound[0], Customization.kingVolume)
                            "§6King Minos"
                        }
                        Diana.ReceiveList.MANTICORE -> {
                            Helper.showTitle("§r§6§l<§b§l§kO§6§l> §2§lMANTICORE! §6§l<§b§l§kO§6§l>", player, Diana.rareTitleFadeIn, Diana.rareTitleTime, Diana.rareTitleFadeOut)
                            playCustomSound(Customization.mantiSound[0], Customization.mantiVolume)
                            "§2Manticore"
                        }
                        Diana.ReceiveList.SPHINX -> {
                            Helper.showTitle("§r§6§l<§b§l§kO§6§l> §9§lSPHINX! §6§l<§b§l§kO§6§l>", player, Diana.rareTitleFadeIn, Diana.rareTitleTime, Diana.rareTitleFadeOut)
                            playCustomSound(Customization.sphinxSound[0], Customization.sphinxVolume)
                            "§9Sphinx"
                        }
                        else -> {
                            Helper.showTitle("§r§6§l<§b§l§kO§6§l> §3§lRARE MOB! §6§l<§b§l§kO§6§l>", player, Diana.rareTitleFadeIn, Diana.rareTitleTime, Diana.rareTitleFadeOut)
                            playCustomSound(Customization.rareMobSound[0], Customization.rareMobVolume)
                            "§3Rare Mob"
                        }
                    }

                    addRareMobWaypoint(
                        player,
                        SboVec(x.toDouble(), y.toDouble(), z.toDouble()),
                        mobType,
                        playerName,
                        mobDisplayName
                    )
                } else if (patcherWaypoints) {
                    if (hideOwnWaypoints.contains(HideOwnWaypoints.NORMAL) && player.contains(playerName)) return@onChatMessage
                    addWaypoint(Waypoint(player, x.toDouble(), y.toDouble(), z.toDouble(), ttl = 30, type = "world"))
                }
            }
        }

        Register.onTick(1) { _ ->
            val playerPos = Player.getLastPosition()

            val knownBurrows = getWaypointsOfType("burrow")
            val shovelGuesses = getWaypointsOfType("guess")
            val arrowGuesses = getWaypointsOfType("arrow")

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
                if (World.getWorld() != "Hub") return@forEachWaypoint

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
                        arrowGuess.inaccurateArrow = true
                    }
                } else {
                    arrowGuess.inaccurateArrow = false
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

            // Remove duplicate shovel guesses that are within 30 blocks, or 75 if in an unloaded chunk, of each other
            shovelGuesses.forEachIndexed { index, shovelGuess ->
                val shovelGuessBlock = shovelGuess.pos.roundLocationToBlock()

                shovelGuesses.drop(index + 1).firstOrNull { otherGuess ->
                    shovelGuessBlock.distanceTo(otherGuess.pos.roundLocationToBlock()) <= if (ArrowGuessBurrow.isBlockValid(shovelGuess.pos) || ArrowGuessBurrow.isBlockValid(otherGuess.pos)) 30 else 75
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

            closestWaypoint = getClosestWaypointFrom(playerPos) ?: null to 1000.0
            val bestGuessWp = getBestGuess()

            val rareWp = getWaypointsOfType("rareMob")

            this.forEachWaypoint { waypoint ->
                waypoint.isClosest = waypoint == bestGuessWp
                waypoint.format(rareWp)
            }
        }

        WorldRenderEvents.BEFORE_TRANSLUCENT.register(WaypointRenderer)
    }

    private fun addRareMobWaypoint(player: String, pos: SboVec, mobType: Diana.ReceiveList, playerName: String, mobDisplayName: String) {
        when (mobType) {
            Diana.ReceiveList.INQ -> if (hideOwnWaypoints.contains(HideOwnWaypoints.INQ) && player.contains(playerName)) return
            Diana.ReceiveList.KING -> if (hideOwnWaypoints.contains(HideOwnWaypoints.KING) && player.contains(playerName)) return
            Diana.ReceiveList.MANTICORE -> if (hideOwnWaypoints.contains(HideOwnWaypoints.MANTICORE) && player.contains(playerName)) return
            Diana.ReceiveList.SPHINX -> if (hideOwnWaypoints.contains(HideOwnWaypoints.SPHINX) && player.contains(playerName)) return
            else -> {}
        }

        addWaypoint(Waypoint("$mobDisplayName §7($player§7)", pos.x, pos.y, pos.z, ttl = 45, type = "rareMob"))
    }

    fun removeNearbyRareMobWaypoints() {
        removeWithinDistance("rareMob", 30)
        removeWithinDistance("world", 30)
    }

    fun removeNearbyRareMobWaypointsAt(pos: SboVec) {
        removeWithinDistanceFrom(pos, "rareMob", 30)
        removeWithinDistanceFrom(pos, "world", 30)
    }

    /**
     * Renders all waypoints in the management system.
     * @param context The world render context.
     */
    fun renderAllWaypoints(context: WorldRenderContext) {
        if (World.getWorld() != "Hub") {
            return
        }

        this.forEachWaypoint { waypoint ->
            waypoint.render(context)
        }
    }

    /**
     * Adds a waypoint to the management system.
     * @param waypoint The waypoint to add.
     */
    fun addWaypoint(waypoint: Waypoint, playSound: Boolean = true) {
        val type = waypoint.type.lowercase()

        waypoints.computeIfAbsent(type) { CopyOnWriteArrayList() }.add(waypoint)

        if (type == "burrow") {
            playCustomSound(Customization.burrowFoundSound[0], Customization.burrowVolume)
        }
    }

    /**
     * Removes a specific waypoint from the management system.
     * @param waypoint The waypoint to remove.
     */
    fun removeWaypoint(waypoint: Waypoint) {
        if (closestWaypoint.first == waypoint) {
            closestWaypoint = null to 1000.0
        }
        waypoints[waypoint.type.lowercase()]?.remove(waypoint)
    }

    /**
     * Gets a waypoint at a specific position and type, or null if it does not exist.
     * @param pos The position of the waypoint to get.
     * @param type The type of the waypoint to get.
     */
    fun getWaypointAt(pos: SboVec, type: String): Waypoint? {
        val list = waypoints[type.lowercase()]
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
        val list = waypoints[type.lowercase()]
        val waypoint = list?.find { it.pos.roundLocationToBlock() == pos.roundLocationToBlock() }
        if (waypoint != null) {
            list.remove(waypoint)
            if (closestWaypoint.first == waypoint) {
                closestWaypoint = null to 1000.0
            }
        }
    }

    /**
     * Removes all waypoints of a specific type.
     * @param type The type of waypoints to remove.
     */
    fun removeAllOfType(type: String) {
        waypoints[type.lowercase()]?.clear()
    }

    /**
     * Removes all waypoints of a specific type that are within a certain distance from the player's last position.
     * @param type The type of waypoints to remove.
     */
    private fun removeWithinDistance(type: String, distance: Int) {
        removeWithinDistanceFrom(Player.getLastPosition(), type, distance)
    }

    /**
     * Removes all waypoints of a specific type that are within a certain distance from the given position.
     * @param type The type of waypoints to remove.
     */
    private fun removeWithinDistanceFrom(pos: SboVec, type: String, distance: Int) {
        val list = waypoints[type.lowercase()] ?: return
        list.removeIf { it.pos.distanceTo(pos) < distance }
    }

    /**
     * Adds a shovel guess waypoint.
     * @param pos The position for the shovel guess waypoint.
     */
    fun addShovelGuess(pos: SboVec?) {
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
        return waypoints[type.lowercase()] ?: emptyList()
    }

    fun getAllGuessesAndBurrows(): List<Waypoint> {
        return getWaypointsOfType("burrow") + getWaypointsOfType("arrow") + getWaypointsOfType("guess")
    }

    private fun getBestGuess(): Waypoint? {
        return getAllGuessesAndBurrows()
            .filter { !it.hidden }
            .minByOrNull { if (Diana.ignoreYLevel) it.distanceToPlayerIgnoringY() else it.distanceToPlayer() }
    }

    private fun getClosestWaypointFrom(pos: SboVec): Pair<Waypoint, Double>? {
        return getClosestWaypointFrom(pos, getAllGuessesAndBurrows())
    }

    private fun getClosestWaypointFrom(pos: SboVec, waypoints: List<Waypoint>): Pair<Waypoint, Double>? {
        return waypoints
            .filter { !it.hidden }
            .map { waypoint ->
                waypoint to pos.distanceTo(waypoint.pos)
            }
            .minByOrNull { it.second }
    }

    /**
     * Gets the closest warp point to a given position.
     * @param pos The position to find the closest warp to.
     * @return The name of the closest warp, or null if no warps are available.
     */
    fun getClosestWarp(pos: SboVec): String? {
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

        var playerDistance = if (Diana.ignoreYLevel) pos.distanceToIgnoringY(Player.getLastPosition()) else pos.distanceTo(Player.getLastPosition())

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

        if (Diana.ignoreYLevel) playerDistance = pos.distanceToIgnoringY(Player.getLastPosition())

        val condition1 = playerDistance > closestDistance + Diana.warpDiff + (closestWarpPoint?.extraBlocks ?: 0)
        val condition2 = condition1 && (closestWaypoint.second > 60 || getWaypointsOfType("rareMob").isNotEmpty() || getWaypointsOfType("world").isNotEmpty())

        val condition = if (Diana.dontWarpIfBurrowClose) condition2 else condition1

        return if (condition) closestWarp else null
    }

    fun warpToGuess() {
        val bestGuess = getBestGuess() ?: return
        getClosestWarp(bestGuess.pos)?.let { executeWarpCommand(it) } ?: return
    }

    fun warpToInq() {
        val newestInq = getWaypointsOfType("rareMob").maxByOrNull { it.creationNs }
        val newestWorldInq = getWaypointsOfType("world").maxByOrNull { it.creationNs }
        val pos = newestInq?.pos ?: newestWorldInq?.pos ?: return
        val warp = getClosestWarp(pos) ?: return

        executeWarpCommand(warp)
    }

    fun warpBoth() {
        if (getWaypointsOfType("rareMob").isEmpty() && getWaypointsOfType("world").isEmpty()) {
            warpToGuess()
        } else {
            warpToInq()
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
