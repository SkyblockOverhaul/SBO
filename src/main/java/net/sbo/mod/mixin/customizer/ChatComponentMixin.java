package net.sbo.mod.mixin.customizer;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.network.chat.Component;
import net.sbo.mod.customizer.SboCustomizer;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Customizers on chat lines, decorated when the line goes into the chat.
 * <p>
 * Chat mods change lines on the way there (Fabric's chat events, or their own mixins at the start of addMessage),
 * some rebuild them from § codes and would drop the customizer's hex colors and logo font. This runs right where the
 * finished line becomes a GuiMessage, after all of that, and order = 2000 puts it after other mods' changes there too.
 * require = 0: if another mod rebuilds addMessage so this can't apply, chat lines just have no customizer instead of a crash.
 */
@Mixin(ChatComponent.class)
final class ChatComponentMixin {
    /** Only lines from the server, not messages mods print themselves. */
    @ModifyArg(
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/chat/GuiMessage;<init>(ILnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V"),
            index = 1,
            order = 2000,
            require = 0
    )
    private final @NonNull Component sbo$decorateChat(@NonNull final Component line, @Local(argsOnly = true) @NonNull final GuiMessageSource source) {
        if (source == GuiMessageSource.SYSTEM_CLIENT) return line;
        return SboCustomizer.INSTANCE.decorateChat(line);
    }
}
