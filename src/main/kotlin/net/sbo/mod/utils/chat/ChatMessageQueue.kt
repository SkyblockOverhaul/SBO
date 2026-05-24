package net.sbo.mod.utils.chat

import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue
import net.sbo.mod.utils.events.Register
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.game.SentMessageEvent
import net.sbo.mod.utils.events.impl.game.SentCommandEvent

object ChatMessageQueue {
    private const val delayNanos = 200_000_000L
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

    fun queue(message: String) {
        queue.enqueue(message)
    }

    fun flush() {
        val player = mc.player ?: return
        val connection = player.connection ?: return

        if (queue.isEmpty) {
            return
        }

        if (0L != lastSentAt && System.nanoTime() - lastSentAt <= delayNanos) {
            return
        }

        val message = queue.dequeue()

        if (!message.isNullOrEmpty()) {
            connection.sendChat(message)
        }
    }
}
