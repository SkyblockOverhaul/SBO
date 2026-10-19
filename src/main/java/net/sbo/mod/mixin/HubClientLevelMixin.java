package net.sbo.mod.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.sbo.mod.utils.hubcache.FakeChunkManager;
import net.sbo.mod.utils.hubcache.HubChunkCache;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Higher priority than Sodium, so this runs after Sodium's unload hook.
@Mixin(value = ClientLevel.class, priority = 1100)
final class HubClientLevelMixin {
    // Vanilla calls unload() late for chunks that left its view area, by then a fake chunk may already
    // show there. Sodium un-renders it here, so tell Sodium about the fake chunk again.
    @Inject(method = "unload", at = @At("HEAD"))
    private final void sbo$keepFakeChunkInSodium(@NonNull final LevelChunk chunk, @NonNull final CallbackInfo ci) {
        if (!HubChunkCache.sodiumLoaded) return;
        final var manager = HubChunkCache.managerFor(((ClientLevel) (Object) this).getChunkSource());
        if (manager == null) return;
        final var pos = chunk.getPos();
        manager.refreshSodium(pos.x(), pos.z());
    }
}
