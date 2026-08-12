package com.davidblackcn.lorianarchorbit.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class ConfigFile<T> {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final Path path;
    private final ConfigCodec<T> codec;
    private final AtomicFileWriter writer;
    private final System.Logger logger;
    private final Consumer<ConfigChange> listener;
    private volatile T current;

    public ConfigFile(
            Path path,
            ConfigCodec<T> codec,
            AtomicFileWriter writer,
            System.Logger logger,
            Consumer<ConfigChange> listener
    ) {
        this.path = path.toAbsolutePath().normalize();
        this.codec = Objects.requireNonNull(codec, "codec");
        this.writer = Objects.requireNonNull(writer, "writer");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.listener = Objects.requireNonNull(listener, "listener");
    }

    public synchronized ConfigLoadResult load() {
        if (!Files.exists(path)) {
            T defaults = codec.defaults();
            try {
                writeSnapshot(defaults);
                replace(defaults);
                return ConfigLoadResult.success(true, List.of());
            } catch (IOException exception) {
                current = defaults;
                return fail("Could not create configuration", exception);
            }
        }
        ConfigLoadResult result = reload();
        if (!result.successful() && current == null) {
            current = codec.defaults();
            logger.log(
                    System.Logger.Level.WARNING,
                    "Using in-memory defaults while retaining unreadable configuration at " + path
            );
        }
        return result;
    }

    public synchronized ConfigLoadResult reload() {
        byte[] original;
        try {
            original = Files.readAllBytes(path);
        } catch (IOException exception) {
            return fail("Could not read configuration", exception);
        }

        DecodedConfig<T> decoded;
        try {
            JsonElement parsed = JsonParser.parseString(new String(original, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) {
                throw new ConfigException("Root value must be a JSON object");
            }
            decoded = codec.decode(parsed.getAsJsonObject());
        } catch (JsonParseException | ConfigException | IllegalStateException exception) {
            return fail("Could not parse configuration", exception);
        }

        if (decoded.migrated()) {
            Path backup = backupPath();
            try {
                Files.copy(path, backup, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                writeSnapshot(decoded.snapshot());
            } catch (IOException exception) {
                return fail("Could not migrate configuration; original file was retained", exception);
            }
        }
        boolean changed = replace(decoded.snapshot());
        logWarnings(decoded.warnings());
        return ConfigLoadResult.success(changed, decoded.warnings());
    }

    public synchronized ConfigLoadResult save(T candidate) {
        DecodedConfig<T> normalized;
        try {
            normalized = codec.decode(codec.encode(candidate));
            writeSnapshot(normalized.snapshot());
        } catch (ConfigException | IOException exception) {
            return fail("Could not save configuration; previous file and snapshot were retained", exception);
        }
        boolean changed = replace(normalized.snapshot());
        logWarnings(normalized.warnings());
        return ConfigLoadResult.success(changed, normalized.warnings());
    }

    public T current() {
        T snapshot = current;
        if (snapshot == null) {
            throw new IllegalStateException("Configuration has not been loaded: " + path);
        }
        return snapshot;
    }

    public Path path() {
        return path;
    }

    private boolean replace(T next) {
        T previous = current;
        current = next;
        if (previous == null) {
            return true;
        }
        var namespaces = codec.changedNamespaces(previous, next);
        if (namespaces.isEmpty()) {
            return false;
        }
        listener.accept(new ConfigChange(path, namespaces));
        return true;
    }

    private void writeSnapshot(T snapshot) throws IOException {
        JsonObject encoded = codec.encode(snapshot);
        byte[] content = (GSON.toJson(encoded) + System.lineSeparator()).getBytes(StandardCharsets.UTF_8);
        writer.write(path, content);
    }

    private Path backupPath() {
        return path.resolveSibling(path.getFileName() + ".bak");
    }

    private void logWarnings(List<String> warnings) {
        for (String warning : warnings) {
            logger.log(System.Logger.Level.WARNING, path + ": " + warning);
        }
    }

    private ConfigLoadResult fail(String operation, Exception exception) {
        String detail = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
        String message = operation + " at " + path + ": " + detail;
        logger.log(System.Logger.Level.ERROR, message, exception);
        return ConfigLoadResult.failure(message);
    }
}
