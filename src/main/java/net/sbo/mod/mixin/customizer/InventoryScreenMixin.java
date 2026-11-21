package net.sbo.mod.mixin.customizer;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.sbo.mod.customizer.SboCustomizer;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Player models in menus (inventory, other mods' screens using it) are the real player: while their render state is
 * built, {@link AvatarRendererMixin} leaves out name tag, customizer and size, which only belong in the world.
 * require = 0: without it the menu model just shows them.
 */
@Mixin(InventoryScreen.class)
final class InventoryScreenMixin {
    @WrapMethod(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;", require = 0)
    private static final @NonNull EntityRenderState sbo$markMenuModel(@NonNull final LivingEntity entity, @NonNull final Operation<EntityRenderState> original) {
        final var outer = SboCustomizer.INSTANCE.getMenuModel();
        SboCustomizer.INSTANCE.setMenuModel(true);
        try {
            return original.call(entity);
        } finally {
            SboCustomizer.INSTANCE.setMenuModel(outer);
        }
    }
}
