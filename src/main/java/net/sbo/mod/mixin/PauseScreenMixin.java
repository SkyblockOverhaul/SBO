package net.sbo.mod.mixin;

import gg.essential.universal.UScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.GridLayout.RowHelper;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.sbo.mod.SBOKotlin;
import net.sbo.mod.guis.AchievementsGUI;
import net.sbo.mod.guis.Guis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin {
    @Unique
    int addedButtons;
    @Unique
    boolean injected = false;

    @Inject(method = "createPauseMenu", at = @At(value = "HEAD"))
    void sbo$createPauseMenuHead(CallbackInfo ci) {
        addedButtons = 0;
        injected = false;
    }

    @Redirect(method = "createPauseMenu", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/layouts/GridLayout$RowHelper;addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;"))
    LayoutElement sbo$onAddChild(RowHelper helper, LayoutElement original) {
        if (addedButtons == 0 && !injected) {
            injected = true;

            Button custom = Button.builder(
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

        addedButtons++;
        return helper.addChild(original);
    }
}
