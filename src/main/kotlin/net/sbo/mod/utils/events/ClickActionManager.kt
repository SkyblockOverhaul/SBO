package net.sbo.mod.utils.events

import java.util.*

/**
 * Manages click actions by mapping a unique ID to a lambda function.
 * This allows us to execute arbitrary Kotlin code from a chat message click.
 */
object ClickActionManager {
    private val actions = mutableMapOf<UUID, () -> Unit>()
    private val repeatableActions = mutableSetOf<UUID>()
    private val sharedActions = mutableMapOf<String, UUID>()

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

    /** Like registerAction, but every click runs it, for buttons that may have to be clicked again. */
    fun registerRepeatableAction(onClick: () -> Unit): UUID = registerAction(onClick).also { repeatableActions.add(it) }

    /**
     * A repeatable action that every message with the same key shares, so sending the button again registers no new action.
     * Only the first onClick for a key is kept, so it must not depend on the message it is sent with.
     */
    fun registerRepeatableAction(key: String, onClick: () -> Unit): UUID =
        sharedActions.getOrPut(key) { registerRepeatableAction(onClick) }

    /**
     * Executes the action corresponding to the given ID.
     * The action is removed after execution to prevent memory leaks, unless it is repeatable.
     * @param id The UUID of the action to execute.
     */
    private fun executeAction(id: UUID) {
        val action = if (id in repeatableActions) actions[id] else actions.remove(id)
        action?.invoke()
    }
}

