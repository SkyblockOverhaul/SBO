package net.sbo.mod.mixin.customizer;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.List;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Component;
import net.sbo.mod.customizer.SboCustomizer;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Remembers the chat lines of messages with moving customizers and the width they were split at, so {@link FontMixin}
 * can draw them in their current colors. The split itself is left alone; require = 0: without it chat customizers just stand still.
 */
@Mixin(GuiMessage.class)
final class GuiMessageMixin {
    @WrapOperation(
            method = "splitLines(Lnet/minecraft/client/gui/Font;I)Ljava/util/List;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ComponentRenderUtils;wrapComponents(Lnet/minecraft/network/chat/FormattedText;ILnet/minecraft/client/gui/Font;)Ljava/util/List;"),
            require = 0
    )
    private final @NonNull List<FormattedCharSequence> sbo$rememberAnimatedLines(
            @NonNull final FormattedText text, final int width, @NonNull final Font font,
            @NonNull final Operation<List<FormattedCharSequence>> original
    ) {
        final var lines = original.call(text, width, font);
        if (text instanceof final Component component) {
            SboCustomizer.INSTANCE.splitChatLines(component, width, lines);
        }
        return lines;
    }
}
