package net.sbo.mod.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.sbo.mod.utils.events.SBOEvent;
import net.sbo.mod.utils.events.impl.game.PlayerInteractEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class PlayerInteractMixin {

    @Unique
    private final Minecraft client = Minecraft.getInstance();

    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void onInteractItem(CallbackInfoReturnable<InteractionResult> cir) {
        if (client.player != null) {
            PlayerInteractEvent event = new PlayerInteractEvent(
                    "useItem", null, client.player, client.player.level(), false
            );
            SBOEvent.INSTANCE.emit(event);

            if (event.isCanceled()) {
                cir.setReturnValue(InteractionResult.FAIL);
                cir.cancel();
            }
        }
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void onInteractBlock(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (hand == InteractionHand.MAIN_HAND) {
            PlayerInteractEvent event = new PlayerInteractEvent(
                    "useBlock", hitResult.getBlockPos(), player, player.level(), false
            );
            SBOEvent.INSTANCE.emit(event);

            if (event.isCanceled()) {
                cir.setReturnValue(InteractionResult.FAIL);
                cir.cancel();
            }
        }
    }
}

