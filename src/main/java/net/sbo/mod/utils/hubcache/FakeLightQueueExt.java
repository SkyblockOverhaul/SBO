package net.sbo.mod.utils.hubcache;

/**
 * Added to {@link net.minecraft.client.multiplayer.ClientPacketListener} by HubClientPacketListenerMixin.
 * Vanilla applies the light of a new real chunk a few frames late. The fake light of the chunk it replaces is
 * removed in the same task, so the chunk never renders dark in between.
 */
public interface FakeLightQueueExt {
    void sbo_queueFakeLightRemoval(Runnable task);

    /** Runs a queued removal now, so it does not keep the old level alive after a level change. */
    void sbo_flushFakeLightRemoval();
}
