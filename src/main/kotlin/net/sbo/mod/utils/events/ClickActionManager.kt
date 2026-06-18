package net.sbo.mod.utils.events

import java.util.*

/**
 * Manages click actions by mapping a unique ID to a lambda function.
 * This allows us to execute arbitrary Kotlin code from a chat message click.
 */
object ClickActionManager {
    private val actions = mutableMapOf<UUID, () -> Unit>()

    fun init() {
        Register.command("__sbo_run_clickable_action") { args ->
            if (args.isNotEmpty()) {
                try {
                    val actionId = UUID.fromString(args[0])
                    executeAction(actionId)
                } catch (_: IllegalArgumentException) {
                }
            }
        }
    }
    /**
     * Registers a lambda to be executed on click.
     * @param onClick The () -> Unit lambda function to run.
     * @return A unique UUID that identifies this action.
     */
    fun registerAction(onClick: () -> Unit): UUID {
        val id = UUID.randomUUID()
        actions[id] = onClick
        return id
    }

    /**
     * Executes the action corresponding to the given ID.
     * The action is removed after execution to prevent memory leaks.
     * @param id The UUID of the action to execute.
     */
    fun executeAction(id: UUID) {
        actions.remove(id)?.invoke()
    }
}