package net.sbo.mod.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.world.level.ChunkPos;
import net.sbo.mod.utils.hubcache.FakeChunkManager;
import net.sbo.mod.utils.hubcache.FakeLightQueueExt;
import net.sbo.mod.utils.hubcache.HubChunkCache;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Higher priority than Sodium, so the forget packet hook runs after Sodium's.
@Mixin(value = ClientPacketListener.class, priority = 1100)
final class HubClientPacketListenerMixin implements FakeLightQueueExt {
    @Shadow private ClientLevel level;

    @Unique
    private @Nullable Runnable sbo$pendingFakeLightRemoval;

    @Override
    public final void sbo_queueFakeLightRemoval(@NonNull final Runnable task) {
        // Not picked up for some reason, run it now so the light does not leak.
        if (this.sbo$pendingFakeLightRemoval != null) this.sbo$pendingFakeLightRemoval.run();
        this.sbo$pendingFakeLightRemoval = task;
    }

    @Override
    public final void sbo_flushFakeLightRemoval() {
        final var removal = this.sbo$pendingFakeLightRemoval;
        this.sbo$pendingFakeLightRemoval = null;
        if (removal != null) removal.run();
    }

    // The chunk builder reads light live from other threads, so the fake light must stay until the real
    // light is in. Vanilla's queue runs in batches, so both have to be one task.
    @ModifyArg(method = "handleLevelChunkWithLight", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;queueLightUpdate(Ljava/lang/Runnable;)V"))
    private final @NonNull Runnable sbo$removeFakeLightWithRealLight(@NonNull final Runnable applyRealLight) {
        final var removal = this.sbo$pendingFakeLightRemoval;
        if (removal == null) return applyRealLight;
        this.sbo$pendingFakeLightRemoval = null;
        return () -> {
            removal.run();
            applyRealLight.run();
        };
    }

    // Sodium un-renders the forgotten chunk at the end of this method, after the fake chunk replaced it.
    @Inject(method = "handleForgetLevelChunk", at = @At("RETURN"))
    private final void sbo$keepFakeChunkInSodium(@NonNull final ClientboundForgetLevelChunkPacket packet, @NonNull final CallbackInfo ci) {
        if (!HubChunkCache.sodiumLoaded) return;
        final var manager = HubChunkCache.managerFor(this.level.getChunkSource());
        if (manager == null) return;
        final var pos = packet.pos();
        manager.refreshSodium(pos.x(), pos.z());
    }
}
