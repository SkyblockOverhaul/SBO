package net.sbo.mod.utils.http

import java.util.concurrent.TimeUnit
import net.sbo.mod.SBOKotlin
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.game.World

/** Tells the backend every few minutes that this player is online in Skyblock. */
object ActiveUsers {
    private val ACTIVE_USERS_INTERVAL = TimeUnit.MINUTES.toNanos(4L)
    private var lastActiveUsersCount = 0L

    fun init() {
        Register.onTick(20) {
            val now = System.nanoTime()
            if (now - lastActiveUsersCount > ACTIVE_USERS_INTERVAL && World.isInSkyblock()) {
                lastActiveUsersCount = now
                countActiveUser()
            }
        }
    }

    /** Tells the backend this player is online, it counts everyone from the last 5 minutes. */
    private fun countActiveUser() {
        SboApi.countActiveUsers()
            .result { response ->
                if (!response.isSuccessful) {
                    SBOKotlin.logger.error("Failed to count active players: ${response.code} ${response.message}")
                }
            }
            .error { exception ->
                SBOKotlin.logger.error("Error while counting active players", exception)
            }
    }
}
