package net.sbo.mod.mixin;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.lighting.LightEngine;
import net.sbo.mod.utils.hubcache.FakeLightExt;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Answers light queries for fake chunks, the vanilla engine has no data for them.
 */
@Mixin(LightEngine.class)
final class HubLightEngineMixin implements FakeLightExt {
    @Unique
    private final Long2ObjectMap<DataLayer> sbo$fakeLight = Long2ObjectMaps.synchronize(new Long2ObjectOpenHashMap<>());

    // Light queries are hot, skip the locked map lookup outside the Hub.
    @Unique
    private volatile boolean sbo$hasFakeLight;

    @Override
    public final void sbo_addFakeLight(final long sectionPos, @NonNull final DataLayer data) {
        this.sbo$fakeLight.put(sectionPos, data);
        this.sbo$hasFakeLight = true;
    }

    @Override
    public final void sbo_removeFakeLight(final long sectionPos) {
        this.sbo$fakeLight.remove(sectionPos);
        this.sbo$hasFakeLight = !this.sbo$fakeLight.isEmpty();
    }

    @Inject(method = "getDataLayerData(Lnet/minecraft/core/SectionPos;)Lnet/minecraft/world/level/chunk/DataLayer;", at = @At("HEAD"), cancellable = true)
    private final void sbo$getFakeLayer(@NonNull final SectionPos pos, @NonNull final CallbackInfoReturnable<DataLayer> cir) {
        if (!this.sbo$hasFakeLight) return;
        final var data = this.sbo$fakeLight.get(pos.asLong());
        if (data != null) cir.setReturnValue(data);
    }

    @Inject(method = "getLightValue(Lnet/minecraft/core/BlockPos;)I", at = @At("HEAD"), cancellable = true)
    private final void sbo$getFakeLight(@NonNull final BlockPos pos, @NonNull final CallbackInfoReturnable<Integer> cir) {
        if (!this.sbo$hasFakeLight) return;
        final var data = this.sbo$fakeLight.get(SectionPos.asLong(
            SectionPos.blockToSectionCoord(pos.getX()),
            SectionPos.blockToSectionCoord(pos.getY()),
            SectionPos.blockToSectionCoord(pos.getZ())
        ));
        if (data != null) {
            cir.setReturnValue(data.get(
                SectionPos.sectionRelative(pos.getX()),
                SectionPos.sectionRelative(pos.getY()),
                SectionPos.sectionRelative(pos.getZ())
            ));
        }
    }
}
