package net.sbo.mod.mixin;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.sbo.mod.utils.hubcache.FakeLightColumnsExt;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Reports the light of fake chunk columns as ready, otherwise the renderer skips them.
 */
@Mixin(LevelLightEngine.class)
final class HubLevelLightEngineMixin implements FakeLightColumnsExt {
    @Unique
    private final LongSet sbo$fakeColumns = LongSets.synchronize(new LongOpenHashSet());

    @Unique
    private volatile boolean sbo$hasFakeColumns;

    @Override
    public final void sbo_enableFakeColumn(final long columnPos) {
        this.sbo$fakeColumns.add(columnPos);
        this.sbo$hasFakeColumns = true;
    }

    @Override
    public final void sbo_disableFakeColumn(final long columnPos) {
        this.sbo$fakeColumns.remove(columnPos);
        this.sbo$hasFakeColumns = !this.sbo$fakeColumns.isEmpty();
    }

    @Inject(method = "lightOnInColumn", at = @At("HEAD"), cancellable = true)
    private final void sbo$fakeColumnLightOn(final long columnPos, @NonNull final CallbackInfoReturnable<Boolean> cir) {
        if (this.sbo$hasFakeColumns && this.sbo$fakeColumns.contains(columnPos)) cir.setReturnValue(true);
    }
}
