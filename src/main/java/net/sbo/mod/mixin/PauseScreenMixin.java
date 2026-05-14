package net.sbo.mod.mixin;

import gg.essential.universal.UScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.sbo.mod.SBOKotlin;
import net.sbo.mod.guis.AchievementsGUI;
import net.sbo.mod.guis.Guis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin extends Screen {

    protected PauseScreenMixin() {
        super(null);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        PauseScreen self = (PauseScreen)(Object)this;

        Button button = Button.builder(Component.literal("SBO"), b -> SBOKotlin.mc.schedule(() -> {
            if (Guis.INSTANCE.getAchievementsGui$SBO() == null) {
                Guis.INSTANCE.setAchievementsGui$SBO(new AchievementsGUI());
            }
            UScreen.displayScreen(Guis.INSTANCE.getAchievementsGui$SBO());
        })).bounds(self.width / 2 + 104, self.height / 4 + 32, 30, 20).build();

        this.addRenderableWidget(button);
    }
}
