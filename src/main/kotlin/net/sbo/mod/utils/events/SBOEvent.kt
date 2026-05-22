package net.sbo.mod.utils.events

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.event.player.AttackEntityCallback
import net.minecraft.world.InteractionResult
import net.sbo.mod.utils.events.impl.entity.EntitiyHitEvent
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.entity.EntityLoadEvent
import net.sbo.mod.utils.events.impl.entity.EntityUnloadEvent
import net.sbo.mod.utils.events.impl.game.ChatMessageAllowEvent
import net.sbo.mod.utils.events.impl.game.ChatMessageEvent
import net.sbo.mod.utils.events.impl.game.DisconnectEvent
import net.sbo.mod.utils.events.impl.game.GameCloseEvent
import net.sbo.mod.utils.events.impl.game.TickEvent
import net.sbo.mod.utils.events.impl.game.WorldChangeEvent
import net.sbo.mod.utils.events.impl.guis.GuiCloseEvent
import net.sbo.mod.utils.events.impl.guis.GuiMouseClickAfter
import net.sbo.mod.utils.events.impl.guis.GuiMouseClickBefore
import net.sbo.mod.utils.events.impl.guis.GuiPostRenderEvent
import kotlin.reflect.KClass

object SBOEvent {
    private val listeners = java.util.concurrent.ConcurrentHashMap<KClass<*>, MutableList<Listener>>()

    private data class Listener(
        val callback: (Any) -> Unit,
        val priority: Int = SboEvent.Priority.NORMAL
    )

    /**
     * Initialize the event system by registering Fabric events to emit custom events.
     * This should be called once during mod initialization.
     */
    fun init () {
        /**
         * World Change Event
         * Fired after the client world changes (e.g., when joining a new world or server).
         */
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register { mc, world ->
            emit(WorldChangeEvent(mc, world))
        }
        /**
         * Disconnect Event
         * Fired when the client disconnects from a server.
         */
        ClientPlayConnectionEvents.DISCONNECT.register { handler, client ->
            emit(DisconnectEvent(handler, client))
        }
        /**
         * Game Close Event
         * Fired when the Minecraft client is stopping.
         */
        ClientLifecycleEvents.CLIENT_STOPPING.register { client ->
            emit(GameCloseEvent(client))
        }
        /**
         * Entity Load/Unload Events
         * Fired when an entity is loaded into or unloaded from the client world.
         */
        ClientEntityEvents.ENTITY_LOAD.register { entity, world ->
            emit(EntityLoadEvent(entity, world))
        }
        ClientEntityEvents.ENTITY_UNLOAD.register { entity, world ->
            emit(EntityUnloadEvent(entity, world))
        }
        /**
         * Chat Message Event
         * Fired when a chat message is received.
         */
        ClientReceiveMessageEvents.ALLOW_GAME.register { message, signed ->
            emit(ChatMessageEvent(message, signed))
            true
        }
        /**
         * Chat Message Allow Event
         * Fired to determine if a chat message should be displayed.
         * Allows for filtering of spammy messages.
         */
        ClientReceiveMessageEvents.ALLOW_GAME.register { message, signed ->
            val event = ChatMessageAllowEvent(message, signed, true)
            emit(event)
            event.isAllowed
        }

        /**
         * Client Tick Event
         * Fired every game tick (20 times per second) on the client.
         */
        ClientTickEvents.START_CLIENT_TICK.register { client ->
            emit(TickEvent(client))
        }


        ScreenEvents.AFTER_INIT.register { client, screen, scaledWidth, scaledHeight ->
            /**
             * GUI Close Event
             * Fired when a GUI screen is closed.
             */
            ScreenEvents.remove(screen).register {
                emit(GuiCloseEvent(client, screen, scaledWidth, scaledHeight))
            }

            /** GUI Post Render Event
             * Fired after a GUI screen is rendered.
             */
            ScreenEvents.afterRender(screen).register { renderScreen, drawContext, mouseX, mouseY, tickDelta ->
                emit(GuiPostRenderEvent(renderScreen, drawContext, mouseX, mouseY, tickDelta))
            }

            /** GUI Mouse Click Event
             * Fired before a mouse click is processed in a GUI screen.
             */
            ScreenMouseEvents.beforeMouseClick(screen).register { s, click ->
                val mouseX = click.x
                val mouseY = click.y
                val button = click.buttonInfo().button
                emit(GuiMouseClickBefore(s, mouseX, mouseY, button))
            }

            /** GUI Mouse Click Event
             * Fired after a mouse click is processed in a GUI screen.
             */
            ScreenMouseEvents.afterMouseClick(screen).register { s, click, consumed ->
                val mouseX = click.x
                val mouseY = click.y
                val button = click.buttonInfo().button
                emit(GuiMouseClickAfter(s, mouseX, mouseY, button))
                consumed
            }
        }

        /**
         * Entity Hit Event
         * Fired when a player hits an entity.
         */
        AttackEntityCallback.EVENT.register { player, world, hand, entity, hitResult ->
            emit(EntitiyHitEvent(player, world, hand, entity, hitResult))
            InteractionResult.PASS
        }
    }

    /** Register a listener for a specific event type. Returns an unregister function. */
    fun <T : Any> on(eventType: KClass<T>, priority: Int = SboEvent.Priority.NORMAL, callback: (T) -> Unit): () -> Unit {
        val callbacks = listeners.getOrPut(eventType) { mutableListOf() }
        @Suppress("UNCHECKED_CAST")
        val listener = Listener(callback as (Any) -> Unit, priority)
        callbacks.add(listener)
        callbacks.sortByDescending { it.priority }
        return {
            callbacks.remove(listener)
        }
    }

    /** Emit an event to all registered listeners. */
    fun emit(event: Any) {
        listeners[event::class]?.forEach { listener ->
            listener.callback(event)
        }
    }

    /** Convenience inline version for type inference. Returns an unregister function. */
    inline fun <reified T : Any> on(priority: Int = SboEvent.Priority.NORMAL, noinline callback: (T) -> Unit): () -> Unit {
        return on(T::class, priority, callback)
    }
}
