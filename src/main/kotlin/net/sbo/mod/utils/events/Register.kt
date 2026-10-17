package net.sbo.mod.utils.events

import com.mojang.brigadier.arguments.StringArgumentType
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.minecraft.network.chat.Component
import net.minecraft.client.Minecraft
import net.minecraft.commands.SharedSuggestionProvider
import net.sbo.mod.utils.chat.ChatHandler
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Utility object for registering events
 */
object Register {
    /**
     * Registers a command with the specified name and aliases.
     * Player names are suggested by default when no other suggestions
     * are available from the command.
     *
     * @param name The name of the command.
     * @param aliases Optional aliases for the command.
     * @param action The action to execute. It receives the command arguments.
     */
    fun command(
        name: String,
        vararg aliases: String,
        action: (Array<String>) -> Unit
    ) {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->

            fun createLiteral(commandName: String) =
                ClientCommands.literal(commandName)
                    .executes {
                        action(emptyArray())
                        1
                    }
                    .then(
                        ClientCommands.argument("args", StringArgumentType.greedyString())
                            .suggests { _, builder ->
                                val world = Minecraft.getInstance().level

                                if (world != null) {
                                    SharedSuggestionProvider.suggest(
                                        world.players().map { it.gameProfile.name },
                                        builder
                                    )
                                } else {
                                    builder.buildFuture()
                                }
                            }
                            .executes {
                                val argsString =
                                    StringArgumentType.getString(it, "args")

                                val args = argsString.split(' ')
                                    .filter { s -> s.isNotEmpty() }
                                    .toTypedArray()

                                action(args)
                                1
                            }
                    )

            dispatcher.register(createLiteral(name))

            aliases.forEach { alias ->
                dispatcher.register(createLiteral(alias))
            }
        }
    }

    /**
     * Registers a tick event that executes an action every specified number of ticks.
     *
     * @param tick The number of ticks after which the action should be executed.
     * @param action The action to execute. It receives a lambda to unregister itself.
     */
    fun onTick(
        tick: Int,
        action: (unregister: () -> Unit) -> Unit
    ) {
        val task: TickScheduler.ScheduledTask = TickScheduler.ScheduledTask(tick) {
            var remove = false

            action {
                remove = true
            }

            remove
        }

        TickScheduler.schedule(task)
    }

    /**
     * Registers an event that listens for chat messages that match a regex.
     *
     * @param regex The regular expression to filter messages with.
     * @param action The action to execute. It receives the message and the `MatchResult`.
     */
    fun onChatMessage(
        regex: Regex,
        noFormatting: Boolean = false,
        action: (message: Component, matchResult: MatchResult) -> Unit
    ) {
        ChatHandler.registerListener(regex, noFormatting, action)
    }

    /**
     * Registers an event that listens for chat messages that match a regex.
     *
     * If the action returns false, the message will be canceled.
     *
     * This is the original API and remains unchanged for existing callers.
     */
    fun onChatMessageCancelable(
        regex: Pattern,
        noFormatting: Boolean = false,
        action: (message: Component, matchResult: Matcher) -> Boolean
    ) {
        ChatHandler.registerHandler(regex, noFormatting, action)
    }

    /**
     * Registers an event that listens for chat messages that match a regex.
     *
     * If the action returns false, the message will be canceled.
     *
     * The action also receives an unregister callback. Calling unregister()
     * removes this handler after the current message has finished processing.
     *
     * This is useful for one-shot chat hooks that should stop listening after
     * successfully canceling a message.
     */
    fun onChatMessageCancelable(
        regex: Pattern,
        noFormatting: Boolean = false,
        action: (
            message: Component,
            matchResult: Matcher,
            unregister: () -> Unit
        ) -> Boolean
    ) {
        ChatHandler.registerHandler(regex, noFormatting, action)
    }
}
