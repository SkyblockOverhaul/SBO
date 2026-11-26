package buildlogic;

import org.gradle.api.Action;
import org.gradle.api.Task;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public final class RecordFingerprintSuccess implements Action<Task> {
    private final Provider<RegularFile> successMarker;
    private final Provider<RegularFile> fingerprint;

    public RecordFingerprintSuccess(final Provider<RegularFile> successMarker, final Provider<RegularFile> fingerprint) {
        this.successMarker = successMarker;
        this.fingerprint = fingerprint;
    }

    @Override
    public final void execute(final Task task) {
        final var marker = successMarker.get().getAsFile().toPath();
        final var current = fingerprint.get().getAsFile().toPath();
        try {
            Files.createDirectories(marker.getParent());
            Files.copy(current, marker, StandardCopyOption.REPLACE_EXISTING);
        } catch (final IOException exception) {
            throw new RuntimeException("Could not record successful task input fingerprint", exception);
        }
    }
}
