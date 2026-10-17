package net.sbo.mod.utils.hubcache;

/**
 * Added to {@link net.minecraft.world.level.lighting.LevelLightEngine} by HubLevelLightEngineMixin.
 * Marks the light of fake chunk columns as ready, so they render lit.
 */
public interface FakeLightColumnsExt {
    void sbo_enableFakeColumn(long columnPos);

    void sbo_disableFakeColumn(long columnPos);
}
