package com.davidblackcn.lorianarchorbit.palette.hueblocks;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;

public final class GradientRecipeFiles {
    private GradientRecipeFiles() { }
    public static Path directory(Path config) { return config.toAbsolutePath().normalize().resolve("gradient-plans"); }
    public static Path save(Path config, String text) throws IOException {
        var recipe = GradientRecipe.decode(text);
        Path directory = directory(config); Files.createDirectories(directory);
        Path temporary = Files.createTempFile(directory, ".gradient-", ".tmp");
        Path file = directory.resolve((recipe.paletteOnly() ? "palette-" : "gradient-")
                + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
                + "-" + java.util.UUID.randomUUID() + ".json");
        try {
            Files.writeString(temporary, text, StandardCharsets.UTF_8);
            try { Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(temporary, file); }
            return file;
        } finally { Files.deleteIfExists(temporary); }
    }
    public static List<Path> list(Path config) throws IOException {
        Path directory = directory(config); Files.createDirectories(directory);
        try (var files = Files.list(directory)) {
            return files.filter(Files::isRegularFile).filter(f -> f.getFileName().toString().endsWith(".json"))
                    .sorted().toList();
        }
    }
    public static GradientRecipe.Recipe read(Path config, Path file) throws IOException {
        Path directory = directory(config).toRealPath(), real = file.toRealPath();
        if (!real.startsWith(directory) || !real.getFileName().toString().endsWith(".json")) throw new IOException("Not a gradient-plans JSON file");
        try (var stream = Files.newInputStream(real)) {
            byte[] bytes = stream.readNBytes(GradientRecipe.MAX_LENGTH + 1);
            if (bytes.length > GradientRecipe.MAX_LENGTH) throw new IOException("Gradient file too large");
            return GradientRecipe.decode(new String(bytes, StandardCharsets.UTF_8));
        }
    }
}
