package buildlogic;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

@CacheableTask
public abstract class InputFilesFingerprint extends DefaultTask {
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getRelevantFiles();

    @Input
    public abstract ListProperty<String> getRelevantProperties();

    @OutputFile
    public abstract RegularFileProperty getFingerprintFile();

    @TaskAction
    public final void fingerprint() throws IOException, NoSuchAlgorithmException {
        final var digest = MessageDigest.getInstance("SHA-256");
        for (final var property : getRelevantProperties().get()) {
            digest.update(property.getBytes(StandardCharsets.UTF_8));
        }

        final var fileFingerprints = new ArrayList<FileFingerprint>();
        getRelevantFiles().getAsFileTree().visit(details -> {
            if (!details.isDirectory()) {
                addFileFingerprint(fileFingerprints, details);
            }
        });
        fileFingerprints.sort(Comparator.comparing(FileFingerprint::name).thenComparing(FileFingerprint::contentHash));
        for (final var file : fileFingerprints) {
            digest.update(file.name().getBytes(StandardCharsets.UTF_8));
            digest.update(file.contentHash().getBytes(StandardCharsets.UTF_8));
        }

        final var output = getFingerprintFile().get().getAsFile();
        final var parent = output.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        Files.writeString(output.toPath(), HexFormat.of().formatHex(digest.digest()));
    }

    private static final String sha256(final File file) throws IOException, NoSuchAlgorithmException {
        final var digest = MessageDigest.getInstance("SHA-256");
        try (final var input = Files.newInputStream(file.toPath())) {
            final var buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) >= 0) {
                digest.update(buffer, 0, count);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static void addFileFingerprint(final List<FileFingerprint> fingerprints,
                                           final FileVisitDetails details) {
        try {
            fingerprints.add(new FileFingerprint(
                    details.getRelativePath().getPathString(), sha256(details.getFile())));
        } catch (final IOException | NoSuchAlgorithmException exception) {
            throw new RuntimeException("Could not fingerprint " + details.getFile(), exception);
        }
    }

    private record FileFingerprint(String name, String contentHash) {
    }
}
