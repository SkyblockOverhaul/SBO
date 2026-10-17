package net.sbo.mod.utils.hubcache;

import net.minecraft.world.level.chunk.DataLayer;
import org.jspecify.annotations.Nullable;

/**
 * Added to {@link net.minecraft.world.level.lighting.LightEngine} by HubLightEngineMixin.
 * Holds the light of fake chunks, which the vanilla engine does not know about.
 */
public interface FakeLightExt {
    void sbo_addFakeLight(long sectionPos, DataLayer data);

    void sbo_removeFakeLight(long sectionPos);

    static @Nullable FakeLightExt get(@Nullable Object lightEngine) {
        return lightEngine instanceof FakeLightExt ext ? ext : null;
    }
}
