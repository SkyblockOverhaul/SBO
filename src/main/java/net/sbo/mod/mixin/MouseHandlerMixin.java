package net.sbo.mod.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.sbo.guilib.fabric.GuiLib;
import net.sbo.guilib.fabric.GuiLibScreen;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Grabbing and releasing the mouse both put it in the middle of the window. When a screen closes and an SBO window
 * opens right after (chat to /sbo, Esc back to the hub), the mouse goes back to where it was instead. Other screens
 * keep the vanilla behaviour.
 */
@Mixin(MouseHandler.class)
final class MouseHandlerMixin {
    @Unique
    private static final long sbo$RESTORE_WITHIN_MS = 500L;

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private double xpos;

    @Shadow
    private double ypos;

    @Shadow
    private boolean mouseGrabbed;

    @Unique
    private double sbo$lastX;

    @Unique
    private double sbo$lastY;

    @Unique
    private long sbo$grabbedAt;

    @Inject(method = "grabMouse", at = @At("HEAD"))
    private final void sbo$rememberPosition(@NonNull final CallbackInfo ci) {
        if (this.mouseGrabbed || !this.minecraft.isWindowActive()) return;
        this.sbo$lastX = this.xpos;
        this.sbo$lastY = this.ypos;
        this.sbo$grabbedAt = System.currentTimeMillis();
    }

    @Inject(method = "releaseMouse", at = @At("TAIL"))
    private final void sbo$restorePosition(@NonNull final CallbackInfo ci) {
        if (System.currentTimeMillis() - this.sbo$grabbedAt > sbo$RESTORE_WITHIN_MS) return;
        if (!(GuiLib.INSTANCE.currentScreen() instanceof GuiLibScreen)) return;
        this.sbo$grabbedAt = 0L;
        this.xpos = this.sbo$lastX;
        this.ypos = this.sbo$lastY;
        InputConstants.grabOrReleaseMouse(this.minecraft.getWindow(), GLFW.GLFW_CURSOR_NORMAL, this.xpos, this.ypos);
    }
}
