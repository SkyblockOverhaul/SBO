package net.sbo.mod.mixinbase;

import org.jspecify.annotations.NonNull;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Finds our hooks that did not apply. They use require = 0 so another mod rebuilding the same game code costs the
 * feature instead of crashing the game; this notices it right when the mixin is applied, so the player can be told
 * and it can be reported (see ModConflicts).
 */
public final class MixinHealth {
    /** A hook whose handler is never called in the target class. */
    public record FailedHook(@NonNull String feature, @NonNull String targetClass, @NonNull String handler) {}

    // Mixin class (simple name) to its handlers that have to be called in the target class, with what they do
    private static final @NonNull Map<String, Map<String, String>> WATCHED = Map.of(
            "ChatComponentMixin", Map.of("sbo$decorateChat", "Badges in chat"),
            "EntityRendererMixin", Map.of("sbo$submitBadge", "Badges above heads"),
            "AvatarRendererMixin", Map.of(
                    "sbo$extractBadge", "Badges above heads",
                    "sbo$showOwnBadge", "Own badge in third person"),
            "PlayerTabOverlayMixin", Map.of("sbo$addBadge", "Badges in the tab list"));

    private static final @NonNull List<FailedHook> FAILED = new CopyOnWriteArrayList<>();

    private MixinHealth() {
    }

    /** Called right after one of our mixins was applied to [target]. */
    static void check(@NonNull final String targetClassName, @NonNull final ClassNode target, @NonNull final String mixinClassName) {
        final var handlers = WATCHED.get(mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1));
        if (handlers == null) return;
        handlers.forEach((handler, feature) -> {
            if (!isCalled(target, handler)) FAILED.add(new FailedHook(feature, targetClassName, handler));
        });
    }

    // Mixin renames merged handlers ("handler$zza000$sbo$decorateChat"), the original name stays at the end
    private static boolean isCalled(@NonNull final ClassNode target, @NonNull final String handler) {
        final String suffix = "$" + handler;
        for (final MethodNode method : target.methods) {
            for (final AbstractInsnNode insn : method.instructions) {
                if (insn instanceof final MethodInsnNode call && (call.name.equals(handler) || call.name.endsWith(suffix))) return true;
            }
        }
        return false;
    }

    public static @NonNull List<FailedHook> failedHooks() {
        return List.copyOf(FAILED);
    }

    /** The classes our watched hooks go into, where other mods' mixins are worth listing. */
    public static @NonNull List<String> watchedTargets() {
        return List.of(
                "net.minecraft.client.gui.components.ChatComponent",
                "net.minecraft.client.renderer.entity.EntityRenderer",
                "net.minecraft.client.renderer.entity.player.AvatarRenderer",
                "net.minecraft.client.gui.components.PlayerTabOverlay");
    }
}
