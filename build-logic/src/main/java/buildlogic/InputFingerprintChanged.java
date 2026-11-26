package buildlogic;

import org.gradle.api.Task;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.specs.Spec;

import java.io.IOException;
import java.nio.file.Files;

public final class InputFingerprintChanged implements Spec<Task> {
    private final Provider<RegularFile> successMarker;
    private final Provider<RegularFile> fingerprint;

    public InputFingerprintChanged(final Provider<RegularFile> successMarker, final Provider<RegularFile> fingerprint) {
        this.successMarker = successMarker;
        this.fingerprint = fingerprint;
    }

    @Override
    public final boolean isSatisfiedBy(final Task task) {
        final var marker = successMarker.get().getAsFile().toPath();
        final var current = fingerprint.get().getAsFile().toPath();
        try {
            return !Files.isRegularFile(marker) || !Files.readString(marker).equals(Files.readString(current));
        } catch (final IOException exception) {
            throw new RuntimeException("Could not compare task input fingerprints", exception);
        }
    }
}
