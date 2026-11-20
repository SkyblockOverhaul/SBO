package buildlogic;

import org.gradle.api.Task;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.specs.Spec;

import java.io.IOException;
import java.nio.file.Files;

public final class TestsRequestedOrChanged implements Spec<Task> {
    private final boolean enabled;
    private final boolean explicitlyRequested;
    private final Provider<RegularFile> fingerprint;
    private final Provider<RegularFile> successMarker;

    public TestsRequestedOrChanged(final boolean enabled, final boolean explicitlyRequested,
                                   final Provider<RegularFile> fingerprint,
                                   final Provider<RegularFile> successMarker) {
        this.enabled = enabled;
        this.explicitlyRequested = explicitlyRequested;
        this.fingerprint = fingerprint;
        this.successMarker = successMarker;
    }

    @Override
    public final boolean isSatisfiedBy(final Task task) {
        if (enabled || explicitlyRequested) {
            return true;
        }

        try {
            final var currentFingerprint = fingerprint.get().getAsFile().toPath();
            final var previousFingerprint = successMarker.get().getAsFile().toPath();
            return !Files.isRegularFile(previousFingerprint)
                    || !Files.readString(previousFingerprint).equals(Files.readString(currentFingerprint));
        } catch (final IOException exception) {
            return true;
        }
    }
}
