package net.sbo.mod.utils.waypoint

import net.sbo.mod.diana.guesses.PreciseGuessBurrow
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.sbo.mod.SBOKotlin
import net.sbo.mod.settings.categories.Customization
import net.sbo.mod.utils.render.WaypointRenderer
import net.sbo.mod.settings.categories.Diana
import net.sbo.mod.settings.categories.General.HideOwnWaypoints
import net.sbo.mod.settings.categories.General.hideOwnWaypoints
import net.sbo.mod.settings.categories.General.patcherWaypoints
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.Helper.checkDiana
import net.sbo.mod.utils.Helper.sleep
import net.sbo.mod.utils.Player
import net.sbo.mod.utils.SoundHandler.playCustomSound
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.game.WorldChangeEvent
import net.sbo.mod.utils.math.SboVec
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.roundToInt
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents
import net.sbo.mod.utils.game.World

object WaypointManager {
    var guessWp: Waypoint? = null
    private val waypoints = ConcurrentHashMap<String, CopyOnWriteArrayList<Waypoint>>()
    var closestWaypoint: Pair<Waypoint?, Double> = null to 1000.0
    val rareMobs: List<String> = listOf(
        "minos inquisitor",
        "inquisitor",
        "inq",
        "manticore",
        "king minos",
        "king",
        "sphinx"
    )

    fun init() {
        if (guessWp == null) {
            guessWp = Waypoint("Guess", 100.0, 100.0, 100.0, 0.0f, 0.964f, 1.0f, 0,"guess")
        }

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
            val world = SBOKotlin.mc.level ?: return@onChatMessage

            val x = match.groups["x"]?.value?.toIntOrNull() ?: 0
            var y = match.groups["y"]?.value?.toIntOrNull() ?: 0
            val z = match.groups["z"]?.value?.toIntOrNull() ?: 0
            y = findBlock(world, x, y, z)

            val trailing = match.groups["trailing"]?.value ?: ""
            val mob = trailing.replace("|", "").trim().lowercase()
            val playername = Player.getName() ?: ""
            if (!channel.contains("Guild")) {
                if ((!trailing.startsWith(" ") || rareMobs.contains(mob) || Diana.allWaypointsAreInqs) && Diana.receiveRareMob && checkDiana()) {
                    val mobType: Diana.ReceiveList = when (mob) {
                        "minos inquisitor", "inquisitor", "inq" -> Diana.ReceiveList.INQ
                        "king minos", "king" -> Diana.ReceiveList.KING
                        "manticore" -> Diana.ReceiveList.MANTICORE
                        "sphinx" -> Diana.ReceiveList.SPHINX
                        else -> Diana.ReceiveList.OTHER
                    }
                    if (mobType !in Diana.ReceiveMobs) return@onChatMessage

                    when (mob) { // todo: add custom sounds per mob
                        "minos inquisitor", "inquisitor", "inq" -> {
                            Helper.showTitle("§r§6§l<§b§l§kO§6§l> §b§lINQUISITOR! §6§l<§b§l§kO§6§l>", player, Diana.rareTitleFadeIn, Diana.rareTitleTime, Diana.rareTitleFadeOut)
                            playCustomSound(Customization.inqSound[0], Customization.inqVolume)
                        }
                        "king minos", "king" -> {
                            Helper.showTitle("§r§6§l<§b§l§kO§6§l> §b§lKING MINOS! §6§l<§b§l§kO§6§l>", player, Diana.rareTitleFadeIn, Diana.rareTitleTime, Diana.rareTitleFadeOut)
                            playCustomSound(Customization.kingSound[0], Customization.kingVolume)
                        }
                        "manticore" -> {
                            Helper.showTitle("§r§6§l<§b§l§kO§6§l> §b§lMANTICORE! §6§l<§b§l§kO§6§l>", player, Diana.rareTitleFadeIn, Diana.rareTitleTime, Diana.rareTitleFadeOut)
                            playCustomSound(Customization.mantiSound[0], Customization.mantiVolume)
                        }
                        "sphinx" -> {
                            Helper.showTitle("§r§6§l<§b§l§kO§6§l> §b§lSPHINX! §6§l<§b§l§kO§6§l>", player, Diana.rareTitleFadeIn, Diana.rareTitleTime, Diana.rareTitleFadeOut)
                            playCustomSound(Customization.sphinxSound[0], Customization.sphinxVolume)
                        }
                        else -> {
                            Helper.showTitle("§r§6§l<§b§l§kO§6§l> §b§lRARE MOB! §6§l<§b§l§kO§6§l>", player, Diana.rareTitleFadeIn, Diana.rareTitleTime, Diana.rareTitleFadeOut)
                            playCustomSound(Customization.rareMobSound[0], Customization.rareMobVolume)
                        }
                    }
                    addRareMobWaypoint(
                        player,
                        SboVec(x.toDouble(), y.toDouble(), z.toDouble()),
                        mob,
                        playername
                    )
                } else if (patcherWaypoints) {
                    if (hideOwnWaypoints.contains(HideOwnWaypoints.NORMAL) && player.contains(playername)) return@onChatMessage
                    addWaypoint(Waypoint(player, x.toDouble(), y.toDouble(), z.toDouble(), 0.0f, 0.2f, 1.0f, 30, type = "world"))
                }
            }
        }

        Register.onTick(1) { _ ->
            val playerPos = Player.getLastPosition()
            closestWaypoint = getClosestWaypointFrom(playerPos) ?: (null to 1000.0)

            val preciseGuesses = getWaypointsOfType("guess")
            val arrowGuesses = getWaypointsOfType("arrow")

            val bestGuessWp = getBestGuess()

            val posP = SboVec(playerPos.x, playerPos.y, playerPos.z).roundLocationToBlock()
            val allGuesses = preciseGuesses + arrowGuesses

            val guessesToRemove = allGuesses
                .filter { waypoint ->
                    waypoint.distanceToPlayer() < 3.0 ||
                            waypoint.pos.y < 60 ||
                            (waypoint.pos.x == posP.x && waypoint.pos.z == posP.z)
                }

            val rareWp = getWaypointsOfType("rareMob")
            if (Diana.removeRareMobwaypoint) {
                val rareMobsToRemove = rareWp
                    .filter { waypoint -> waypoint.distanceToPlayer() < 8.0 }
                    .toList()
                rareMobsToRemove.forEach { waypoint -> removeWaypoint(waypoint) }
            }

            guessesToRemove.forEach { waypoint ->
                removeWaypoint(waypoint)
            }

            this.forEachWaypoint { waypoint ->
                if (waypoint.ttl > 0 && waypoint.creation + waypoint.ttl * 1000 < System.currentTimeMillis()) {
                    removeWaypoint(waypoint)
                    return@forEachWaypoint
                }

                waypoint.isClosest = waypoint == bestGuessWp
                waypoint.format(rareWp)
            }

            guessWp?.let { wp ->
                if (wp.distanceToPlayer() < Diana.removeGuessDistance) {
                    removeWaypoint(wp)
                    guessWp = null
                }
            }

            val shouldLegacyHaveLine = bestGuessWp != null && bestGuessWp.type == "guess" && !bestGuessWp.hidden

            guessWp?.isClosest = shouldLegacyHaveLine
            guessWp?.format(rareWp)
        }

        WorldRenderEvents.BEFORE_TRANSLUCENT.register(WaypointRenderer)
    }

    @SboEvent
    fun onWorldChange(event: WorldChangeEvent) {
        guessWp?.hide()
        removeAllOfType("world")
        removeAllOfType("guess")
        if (!Diana.dontClearArrowGuess) removeAllOfType("arrow")
    }

    fun addRareMobWaypoint(player: String, pos: SboVec, mobName: String, playername: String) {
        when (mobName) {
            "minos inquisitor", "inquisitor" -> if (hideOwnWaypoints.contains(HideOwnWaypoints.INQ) && player.contains(playername)) return
            "king minos", "king" -> if (hideOwnWaypoints.contains(HideOwnWaypoints.KING) && player.contains(playername)) return
            "manticore" -> if (hideOwnWaypoints.contains(HideOwnWaypoints.MANTICORE) && player.contains(playername)) return
            "sphinx" -> if (hideOwnWaypoints.contains(HideOwnWaypoints.SPHINX) && player.contains(playername)) return
        }
        addWaypoint(Waypoint(player, pos.x, pos.y, pos.z, 1.0f, 0.84f, 0.0f, 45, type = "rareMob") )
    }

    fun onLootshare() {
        removeWithinDistance("rareMob", 30)
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
        this.guessWp?.render(context)
    }

    /**
     * Adds a waypoint to the management system.
     * @param waypoint The waypoint to add.
     */
    fun addWaypoint(waypoint: Waypoint) {
        waypoints.computeIfAbsent(waypoint.type.lowercase()) { CopyOnWriteArrayList() }.add(waypoint)
        if (waypoint.type.lowercase() == "burrow") playCustomSound(Customization.burrowSound[0], Customization.burrowVolume)
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
     * Removes a waypoint at a specific position and type.
     * @param pos The position of the waypoint to remove.
     * @param type The type of the waypoint to remove.
     */
    fun removeWaypointAt(pos: SboVec, type: String) {
        val list = waypoints[type.lowercase()]
        val waypoint = list?.find { it.pos == pos }
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
    fun removeWithinDistance(type: String, distance: Int) {
        val playerPos = Player.getLastPosition()
        val list = waypoints[type.lowercase()] ?: return
        list.removeIf { it.pos.distanceTo(playerPos) < distance }
    }

    /**
     * Updates the guess waypoint position.
     * @param pos The new position for the guess waypoint.
     */
    fun updateGuess(pos: SboVec?) {
        if (pos == null) return

        removeAllOfType("guess")
        addWaypoint(Waypoint("Guess", pos.x, pos.y, pos.z, 0.0f, 0.964f, 1.0f, 0, "guess"))

        if (guessWp == null) {
            guessWp = Waypoint("Guess", 100.0, 100.0, 100.0, 0.0f, 0.964f, 1.0f, 0,"guess")
        }
        guessWp?.apply {
            val position = pos
            this.pos = position

            val (exists, wp) = waypointExists("burrow", position)
            this.hidden = exists && wp != null && wp.distanceToPlayer() < 60
        }
    }

    fun addArrowGuess(pos: SboVec?) {
        if (pos == null) return
        val exists = getWaypointsOfType("arrow").any { it.pos == pos }
        if (exists) return
        addWaypoint(
            Waypoint(
                text = "Guess",
                x = pos.x,
                y = pos.y,
                z = pos.z,
                r = 0.0f,
                g = 0.964f,
                b = 1.0f,
                ttl = 0,
                type = "arrow"
            )
        )
    }

    fun removeArrowGuess(pos: SboVec) {
        removeWaypointAt(pos, "arrow")
    }

    /**
     * Checks if a waypoint of a specific type exists at a given position.
     * @param type The type of the waypoint to check.
     * @param pos The position to check for the waypoint.
     * @return A pair containing a boolean indicating existence and the waypoint if found.
     */
    fun waypointExists(type: String, pos: SboVec): Pair<Boolean, Waypoint?> {
        val waypoint = getWaypointsOfType(type).find { it.pos == pos }
        return (waypoint != null) to waypoint
    }

    /**
     * Iterates over all waypoints and applies the given action to each.
     * Safe for concurrent use.
     * @param action The action to apply to each waypoint.
     */
    fun forEachWaypoint(action: (Waypoint) -> Unit) {
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

    fun getBestGuess(): Waypoint? {
        return getAllGuessesAndBurrows()
            .filter { !it.hidden }
            .minByOrNull { it.distanceToPlayer() }
    }

    /**
     * Gets the closest waypoint of a specific type to a given position.
     * @param pos The position to find the closest waypoint to.
     * @param type The type of waypoint to search for.
     * @return The closest waypoint of the specified type, or null if none are found.
     */
    fun getClosestWaypoint(pos: SboVec, type: String): Pair<Waypoint, Double>? {
        return getClosestWaypointFrom(pos, getWaypointsOfType(type))
    }

    private fun getClosestWaypointFrom(pos: SboVec): Pair<Waypoint, Double>? {
        return getClosestWaypointFrom(pos, getAllGuessesAndBurrows())
    }

    private fun getClosestWaypointFrom(pos: SboVec, waypoints: List<Waypoint>): Pair<Waypoint, Double>? {
        if (waypoints.isEmpty()) return null

        var closestWaypoint: Waypoint? = null
        var closestDistance = Double.MAX_VALUE

        for (waypoint in waypoints) {
            if (waypoint.hidden) continue

            val distance = pos.distanceTo(waypoint.pos)
            if (distance < closestDistance) {
                closestDistance = distance
                closestWaypoint = waypoint
            }
        }

        return closestWaypoint?.let { it to closestDistance }
    }

    /**
     * Gets the closest warp point to a given position.
     * @param pos The position to find the closest warp to.
     * @return The name of the closest warp, or null if no warps are available.
     */
    fun getClosestWarp(pos: SboVec): String? {
        var warps = hubWarps.filter { it.value.unlocked }.mapValues { it.value }
        for (warp in Diana.allowedWarps) {
            if (additionalHubWarps.containsKey(warp.name.lowercase())) {
                val additionalWarp = additionalHubWarps[warp.name.lowercase()]
                if (additionalWarp != null && additionalWarp.unlocked) {
                    warps = warps + (warp.name.lowercase() to additionalWarp)
                }
            }
        }

        var closestWarp: String? = null
        var closestDistance = Double.MAX_VALUE
        val playerDistance = pos.distanceTo(Player.getLastPosition())
        for ((name, warp) in warps) {
            val distance = pos.distanceTo(warp.pos)
            if (distance < closestDistance) {
                closestDistance = distance
                closestWarp = name
            }
        }

        val condition1 = playerDistance > (closestDistance + Diana.warpDiff)
        val condition2 = condition1 && (closestWaypoint.second > 60 || getWaypointsOfType("rareMob").isNotEmpty())

        val condition = if (Diana.dontWarpIfBurrowClose) condition2 else condition1

        return if (condition) closestWarp else null
    }

    fun warpToGuess() {
        val bestGuess = getBestGuess() ?: return
        if (bestGuess.hidden) return
        getClosestWarp(bestGuess.pos)?.let {
            executeWarpCommand(it)
        } ?:
        return
    }

    fun warpToInq() {
        val newestInq = getWaypointsOfType("rareMob").maxByOrNull { it.creation }
        if (newestInq == null) return

        val warp = getClosestWarp(newestInq.pos) ?: return

        executeWarpCommand(warp)
    }

    fun warpBoth() {
        if (getWaypointsOfType("rareMob").isEmpty()) {
            warpToGuess()
        } else {
            warpToInq()
        }
    }

    var tryWarp: Boolean = false
    fun executeWarpCommand(warp: String) {
        if (World.getWorld() != "Hub" || !Helper.hasSpade) return
        if (Diana.warpDelay > 0 && System.currentTimeMillis() - PreciseGuessBurrow.lastGuessTime < Diana.warpDelay) return
        if (warp.isNotEmpty() && !tryWarp) {
            tryWarp = true
            Chat.command("warp $warp")
            sleep(500) {
                tryWarp = false
            }
        }
    }

    fun findBlock(world: ClientLevel, x: Int, y: Int, z: Int): Int {
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
