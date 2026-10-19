package net.sbo.mod.mixin;

import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.sbo.mod.utils.hubcache.ChunkSquare;
import net.sbo.mod.utils.hubcache.ChunkStorageExt;
import net.sbo.mod.utils.hubcache.FakeChunkManager;
import net.sbo.mod.utils.hubcache.HubChunkCache;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Serves the fake chunks of {@link HubChunkCache} and copies real chunks right before vanilla unloads them.
 */
@Mixin(ClientChunkCache.class)
final class HubClientChunkCacheMixin implements ChunkStorageExt {
    @Shadow @Final private LevelChunk emptyChunk;

    @Shadow
    private static final int calculateStorageRange(final int loadDistance) {
        throw new AssertionError();
    }

    // The chunks vanilla keeps. Moving its center or shrinking it drops chunks without calling drop(),
    // so we track it to copy those chunks first.
    @Unique
    private final ChunkSquare sbo$storage = new ChunkSquare();

    @Override
    public final @NonNull ChunkSquare sbo_getStorageSquare() {
        return this.sbo$storage;
    }

    @Unique
    private final @Nullable FakeChunkManager sbo$manager() {
        return HubChunkCache.managerFor((ClientChunkCache) (Object) this);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private final void sbo$initStorage(@NonNull final ClientLevel level, final int loadDistance, @NonNull final CallbackInfo ci) {
        this.sbo$storage.update(0, 0, calculateStorageRange(loadDistance), null);
    }

    @Inject(method = "getChunk(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/LevelChunk;", at = @At("RETURN"), cancellable = true)
    private final void sbo$getFakeChunk(final int x, final int z, @NonNull final ChunkStatus status, final boolean orEmpty, @NonNull final CallbackInfoReturnable<LevelChunk> cir) {
        if (cir.getReturnValue() != (orEmpty ? this.emptyChunk : null)) return;
        final var manager = this.sbo$manager();
        if (manager == null) return;
        final var fake = manager.getChunk(x, z);
        if (fake != null) cir.setReturnValue(fake);
    }

    @Inject(method = "replaceWithPacketData", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientChunkCache$Storage;getIndex(II)I"))
    //#if MC > 26.2
    //$$ private final void sbo$beforeRealChunk(final int x, final int z, final net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData data, @NonNull final CallbackInfoReturnable<LevelChunk> cir) {
    //#else
    private final void sbo$beforeRealChunk(final int x, final int z, final net.minecraft.network.FriendlyByteBuf buf, final java.util.Map<net.minecraft.world.level.levelgen.Heightmap.Types, long[]> heightmaps, final java.util.function.Consumer<net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData.BlockEntityTagOutput> blockEntities, @NonNull final CallbackInfoReturnable<LevelChunk> cir) {
    //#endif
        final var manager = this.sbo$manager();
        // Also when nothing is loaded here: it must not come back over the real chunk later.
        if (manager != null) manager.unload(x, z, true);
    }

    @Inject(method = "drop", at = @At("HEAD"))
    private final void sbo$saveDroppedChunk(@NonNull final ChunkPos pos, @NonNull final CallbackInfo ci) {
        final var manager = this.sbo$manager();
        if (manager != null) manager.saveRealChunk(pos.x(), pos.z());
    }

    @Inject(method = "updateViewCenter", at = @At("HEAD"))
    private final void sbo$saveChunksBeforeMove(final int x, final int z, @NonNull final CallbackInfo ci) {
        final var manager = this.sbo$manager();
        this.sbo$storage.updateCenter(x, z, manager == null ? null : manager::saveRealChunk);
    }

    @Inject(method = "updateViewRadius", at = @At("HEAD"))
    private final void sbo$saveChunksBeforeResize(final int loadDistance, @NonNull final CallbackInfo ci) {
        final var manager = this.sbo$manager();
        this.sbo$storage.updateRadius(calculateStorageRange(loadDistance), manager == null ? null : manager::saveRealChunk);
    }

    @Inject(method = {"drop", "updateViewCenter", "updateViewRadius"}, at = @At("RETURN"))
    private final void sbo$fakeUnloadedChunks(@NonNull final CallbackInfo ci) {
        final var manager = this.sbo$manager();
        if (manager != null) manager.substitutePending();
    }

    @Inject(method = "updateViewRadius", at = @At(value = "FIELD", target = "Lnet/minecraft/client/multiplayer/ClientChunkCache;storage:Lnet/minecraft/client/multiplayer/ClientChunkCache$Storage;", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    private final void sbo$remarkFakeChunks(final int loadDistance, @NonNull final CallbackInfo ci) {
        final var manager = this.sbo$manager();
        if (manager != null) manager.remarkAllLoaded();
    }

    @Inject(method = "gatherStats", at = @At("RETURN"), cancellable = true)
    private final void sbo$debugStats(@NonNull final CallbackInfoReturnable<String> cir) {
        if (this.sbo$manager() != null) cir.setReturnValue(cir.getReturnValue() + " " + HubChunkCache.debugString());
    }
}
