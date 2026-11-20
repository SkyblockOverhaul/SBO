package buildlogic;

import org.gradle.api.Task;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.specs.Spec;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public final class TestInfrastructureNeeded implements Spec<Task> {
    private final boolean forced;
    private final Provider<RegularFile> testFingerprint;
    private final List<Provider<RegularFile>> testSuccessMarkers;
    private final Provider<RegularFile> mixinFingerprint;
    private final Provider<RegularFile> mixinSuccessMarker;

    public TestInfrastructureNeeded(final boolean forced,
                                    final Provider<RegularFile> testFingerprint,
                                    final List<Provider<RegularFile>> testSuccessMarkers,
                                    final Provider<RegularFile> mixinFingerprint,
                                    final Provider<RegularFile> mixinSuccessMarker) {
        this.forced = forced;
        this.testFingerprint = testFingerprint;
        this.testSuccessMarkers = testSuccessMarkers;
        this.mixinFingerprint = mixinFingerprint;
        this.mixinSuccessMarker = mixinSuccessMarker;
    }

    @Override
    public final boolean isSatisfiedBy(final Task task) {
        if (forced) {
            return true;
        }

        try {
            for (final var marker : testSuccessMarkers) {
                if (hasChanged(testFingerprint, marker)) {
                    return true;
                }
            }
            return mixinFingerprint != null && hasChanged(mixinFingerprint, mixinSuccessMarker);
        } catch (final IOException exception) {
            return true;
        }
    }

    private static final boolean hasChanged(final Provider<RegularFile> fingerprint,
                                      final Provider<RegularFile> successMarker) throws IOException {
        final var current = fingerprint.get().getAsFile().toPath();
        final var previous = successMarker.get().getAsFile().toPath();
        return !Files.isRegularFile(previous)
                || !Files.readString(previous).equals(Files.readString(current));
    }
}
