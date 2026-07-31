package net.sbo.mod.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import gg.essential.universal.UScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.GridLayout.RowHelper;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.sbo.mod.SBOKotlin;
import net.sbo.mod.guis.AchievementsGUI;
import net.sbo.mod.guis.Guis;
import net.sbo.mod.settings.categories.General;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
final class PauseScreenMixin {
    @Unique
    private int sbo$addedButtons;
    @Unique
    private boolean sbo$injected;

    @Inject(method = "createPauseMenu", at = @At("HEAD"))
    private final void sbo$createPauseMenuHead(@NonNull final CallbackInfo ci) {
        sbo$addedButtons = 0;
        sbo$injected = false;
    }

    @Inject(method = "createPauseMenu", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/layouts/GridLayout$RowHelper;addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;"))
    private final void sbo$onAddChild(@NonNull final CallbackInfo ci, @Local @NonNull final RowHelper helper) {
        if (sbo$addedButtons == 0 && !sbo$injected && General.INSTANCE.getAchievementsButton()) {
            sbo$injected = true;

            final Button custom = Button.builder(
                    Component.literal("SBO Achievements"),
                    b -> SBOKotlin.mc.schedule(() -> {
                        if (Guis.INSTANCE.getAchievementsGui() == null) {
                            Guis.INSTANCE.setAchievementsGui(new AchievementsGUI());
                        }
                        UScreen.displayScreen(Guis.INSTANCE.getAchievementsGui());
                    })
            ).width(204).build();

            helper.addChild(custom, 2);
        }

        sbo$addedButtons++;
    }
}
