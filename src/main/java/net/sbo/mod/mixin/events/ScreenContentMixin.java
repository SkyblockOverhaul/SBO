package net.sbo.mod.mixin.events;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.sbo.mod.utils.events.SBOEvent;
import net.sbo.mod.utils.events.impl.guis.GuiContentRenderEvent;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Menus and signs: the content is drawn here, the item tooltip only later as a deferred element. */
@Mixin({AbstractContainerScreen.class, AbstractSignEditScreen.class})
final class ScreenContentMixin {
    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private final void sbo$onContentRendered(@NonNull final GuiGraphicsExtractor context, final int mouseX, final int mouseY, final float delta, @NonNull final CallbackInfo ci) {
        SBOEvent.INSTANCE.emit(new GuiContentRenderEvent((Screen) (Object) this, context, mouseX, mouseY));
    }
}
