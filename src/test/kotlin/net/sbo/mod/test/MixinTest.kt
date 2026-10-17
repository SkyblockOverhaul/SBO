package net.sbo.mod.test

import net.sbo.mod.mixinbase.SBOMixinPlugin
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.spongepowered.asm.mixin.MixinEnvironment
import org.spongepowered.asm.mixin.transformer.IMixinTransformer

// Taken from https://github.com/hannibal002/SkyHanni/blob/5bbf2d1cc23a58553d7602a29d1a90f4eb94e3d7/src/test/java/at/hannibal2/skyhanni/test/MixinTest.kt#L9

/**
 * Audits mixins to ensure their validity without launching a full Minecraft client.
 * Implementation inspired by [Skyblocker](https://github.com/SkyblockerMod/Skyblocker).
 */
class MixinTest {

    @Test
    fun `mixins load successfully`() {
        val environment = MixinEnvironment.getCurrentEnvironment()
        Assertions.assertInstanceOf(IMixinTransformer::class.java, environment.activeTransformer)
        environment.audit()
    }

    @Test
    fun `mixin discovery is successful`() {
        val classLoader = javaClass.classLoader
        val discovered = SBOMixinPlugin().mixins
        Assertions.assertTrue(discovered.isNotEmpty()) {
            "Mixin discovery returned nothing, so this test would pass without inspecting a single mixin. " +
                "SBOMixinPlugin resolves them relative to its own code source, " +
                "which the mixinTest classpath has to expose."
        }
        discovered.forEach { mixin ->
            val path = "$MIXIN_PACKAGE_PATH/${mixin.replace('.', '/')}.class"
            checkNotNull(classLoader.getResource(path)) {
                "Mixin $mixin was discovered but $path is not on the classpath"
            }
        }
    }

    companion object {
        private const val MIXIN_PACKAGE_PATH = "net/sbo/mod/mixin"
    }
}

