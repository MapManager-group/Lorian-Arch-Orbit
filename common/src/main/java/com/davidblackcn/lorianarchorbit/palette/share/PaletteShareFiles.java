package com.davidblackcn.lorianarchorbit.palette.share;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public final class PaletteShareFiles {
    public static final String DIRECTORY = "palette-shares";

    private PaletteShareFiles() {
    }

    public static Path export(Path configDirectory, PaletteShareBundle bundle, PaletteShareCodec codec)
            throws IOException, PaletteShareException {
        Path directory = shareDirectory(configDirectory);
        Files.createDirectories(directory);
        String base = safeFileName(bundle.name());
        Path target = uniqueTarget(directory, base);
        Path temporary = Files.createTempFile(directory, ".palette-share-", ".tmp");
        try {
            Files.writeString(temporary, codec.encodeJson(bundle) + System.lineSeparator(), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, target);
            }
            return target;
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public static PaletteShareBundle read(Path configDirectory, Path file, PaletteShareCodec codec)
            throws IOException, PaletteShareException {
        Path directory = shareDirectory(configDirectory);
        Files.createDirectories(directory);
        Path normalized = file.toAbsolutePath().normalize();
        if (!normalized.startsWith(directory) || !normalized.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json")) {
            throw new PaletteShareException("share file must be a JSON file inside the palette-shares directory");
        }
        Path realDirectory = directory.toRealPath();
        Path realFile = normalized.toRealPath();
        if (!realFile.startsWith(realDirectory)) {
            throw new PaletteShareException("share file resolves outside the palette-shares directory");
        }
        if (Files.size(realFile) > PaletteShareCodec.MAX_JSON_BYTES) {
            throw new PaletteShareException("share file exceeds the size limit");
        }
        return codec.decode(Files.readString(realFile, StandardCharsets.UTF_8));
    }

    public static List<Path> list(Path configDirectory) throws IOException {
        Path directory = shareDirectory(configDirectory);
        Files.createDirectories(directory);
        try (Stream<Path> files = Files.list(directory)) {
            return files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                    .sorted(Comparator.comparingLong(PaletteShareFiles::lastModified).reversed())
                    .toList();
        }
    }

    public static Path shareDirectory(Path configDirectory) {
        return configDirectory.toAbsolutePath().normalize().resolve(DIRECTORY);
    }

    private static Path uniqueTarget(Path directory, String base) {
        Path target = directory.resolve(base + ".json");
        int suffix = 2;
        while (Files.exists(target)) {
            target = directory.resolve(base + '-' + suffix++ + ".json");
        }
        return target;
    }

    private static String safeFileName(String name) {
        String safe = name.strip().replaceAll("[^\\p{L}\\p{N}._-]+", "-")
                .replaceAll("^-+|-+$", "");
        if (safe.isBlank()) {
            safe = "palette-share";
        }
        return safe.length() > 64 ? safe.substring(0, 64) : safe;
    }

    private static long lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException ignored) {
            return Long.MIN_VALUE;
        }
    }
}
