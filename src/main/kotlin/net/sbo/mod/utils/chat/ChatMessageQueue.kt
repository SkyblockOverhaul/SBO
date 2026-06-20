package net.sbo.mod.utils.chat

import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue
import net.minecraft.client.player.LocalPlayer
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.game.SentCommandEvent
import net.sbo.mod.utils.events.impl.game.SentMessageEvent

object ChatMessageQueue {
    private const val DELAY_NANOS = 250_000_000L
    private val queue = ObjectArrayFIFOQueue<String>(1)

    private var lastSentAt = 0L

    init {
         Register.onTick(1) { flush() }
    }

    @SboEvent
    fun onMessageSent(event: SentMessageEvent) {
        onCommandOrMessageSent()
    }

    @SboEvent
    fun onCommandSent(event: SentCommandEvent) {
        onCommandOrMessageSent()
    }

    fun onCommandOrMessageSent() {
        lastSentAt = System.nanoTime()
    }

    fun canSend(): Boolean {
        return 0L == lastSentAt || System.nanoTime() - lastSentAt > DELAY_NANOS
    }

    fun send(player: LocalPlayer, message: String) {
        onCommandOrMessageSent() // should be called by mixin, but just in case it doesn't for some reason
        player.connection.sendChat(message)
    }

    fun queue(message: String) {
        val player = mc.player

        if (null != player && canSend()) { // if player is null we queue so that no messages are lost
            send(player, message)
            return
        }

        queue.enqueue(message)
    }

    fun flush() {
        val player = mc.player ?: return

        if (queue.isEmpty) {
            return
        }

        if (!canSend()) {
            return
        }

        val message = queue.dequeue()
        send(player, message)
    }
}
