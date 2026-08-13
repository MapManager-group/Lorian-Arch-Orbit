package com.davidblackcn.lorianarchorbit.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class ConfigFileTest {
    private static final System.Logger LOGGER = System.getLogger("lorian_arch_orbit.config.test");

    @TempDir
    Path temporaryDirectory;

    @Test
    public void firstLoadCreatesAllFourVersionedFiles() {
        ClientConfigManager client = new ClientConfigManager(temporaryDirectory, LOGGER);
        assertTrue(client.load().values().stream().allMatch(ConfigLoadResult::successful));
        ServerConfigManager server = new ServerConfigManager(temporaryDirectory, LOGGER);
        assertTrue(server.load().successful());

        for (String name : List.of(
                ConfigConstants.CLIENT_FILE,
                ConfigConstants.SERVER_FILE,
                ConfigConstants.PRIMARY_WHEEL_FILE,
                ConfigConstants.SECONDARY_WHEEL_FILE
        )) {
            Path path = temporaryDirectory.resolve(name);
            assertTrue(Files.isRegularFile(path), name);
            assertEquals(ConfigConstants.CURRENT_VERSION, json(path).get("config_version").getAsInt());
        }
        assertEquals(PalettePreset.ITEM_TAG_A, client.client().primaryPalettePreset());
        assertEquals(PalettePreset.ITEM_TAG_B, client.client().secondaryPalettePreset());
        assertEquals(100, client.client().smartPickHoldThresholdMs());
        assertFalse(client.client().invisibleBlocksVisible());
        assertTrue(client.client().showBarriers());
        assertTrue(client.client().showLightBlocks());
        assertEquals(68, client.primaryWheel().groups().size());
        assertEquals(56, client.secondaryWheel().groups().size());
        assertTrue(json(temporaryDirectory.resolve(ConfigConstants.PRIMARY_WHEEL_FILE))
                .getAsJsonArray("groups").isEmpty());
        assertTrue(json(temporaryDirectory.resolve(ConfigConstants.SECONDARY_WHEEL_FILE))
                .getAsJsonArray("groups").isEmpty());
    }

    @Test
    public void readsAndClampsClientAndServerBoundaries() throws IOException {
        Files.writeString(temporaryDirectory.resolve(ConfigConstants.CLIENT_FILE), """
                {
                  "config_version": 1,
                  "features": {
                    "reach_extension": {"enabled": true, "distance": 999},
                    "smart_pick": {
                      "scan_radius": -4,
                      "candidate_limit": 99,
                      "hold_threshold_ms": 1,
                      "debug_stats": true
                    }
                  }
                }
                """);
        ConfigFile<ClientConfigSnapshot> client = clientFile(ignored -> { });
        ConfigLoadResult clientResult = client.load();
        assertTrue(clientResult.successful());
        assertEquals(128, client.current().reachDistance());
        assertEquals(1, client.current().smartPickRadius());
        assertEquals(24, client.current().smartPickCandidateLimit());
        assertEquals(50, client.current().smartPickHoldThresholdMs());
        assertTrue(client.current().smartPickDebugStats());
        assertEquals(4, clientResult.warnings().size());

        Files.writeString(temporaryDirectory.resolve(ConfigConstants.SERVER_FILE), """
                {"config_version":1,"features":{"reach_extension":{
                  "maximum_distance":2,"required_permission_level":9,"requests_per_second":0
                }}}
                """);
        ServerConfigManager server = new ServerConfigManager(temporaryDirectory, LOGGER);
        assertTrue(server.load().successful());
        assertEquals(5, server.current().maximumDistance());
        assertEquals(4, server.current().requiredPermissionLevel());
        assertEquals(1, server.current().requestsPerSecond());
    }

    @Test
    public void migrationCreatesBackupAndPreservesUnknownFields() throws IOException {
        Path path = temporaryDirectory.resolve(ConfigConstants.CLIENT_FILE);
        String versionZero = """
                {
                  "features": {"palette_wheel": false},
                  "third_party": {"keep_me": 42}
                }
                """;
        Files.writeString(path, versionZero);
        ConfigFile<ClientConfigSnapshot> file = clientFile(ignored -> { });

        ConfigLoadResult result = file.load();

        assertTrue(result.successful());
        assertFalse(file.current().featureEnabled("palette_wheel"));
        assertEquals(versionZero, Files.readString(path.resolveSibling("client.json.bak")));
        JsonObject migrated = json(path);
        assertEquals(ConfigConstants.CURRENT_VERSION, migrated.get("config_version").getAsInt());
        assertEquals(42, migrated.getAsJsonObject("third_party").get("keep_me").getAsInt());

        ClientConfigDraft draft = new ClientConfigDraft(file.current());
        draft.setHudEnabled(false);
        assertTrue(file.save(draft.snapshot()).successful());
        assertEquals(42, json(path).getAsJsonObject("third_party").get("keep_me").getAsInt());
    }

    @Test
    public void versionOneWallFixMigratesToConnectedTextureOptions() throws IOException {
        Path path = temporaryDirectory.resolve(ConfigConstants.CLIENT_FILE);
        Files.writeString(path, """
                {
                  "config_version": 1,
                  "features": {
                    "wall_visual_fix": {"enabled": false, "future_option": 7}
                  }
                }
                """);
        ConfigFile<ClientConfigSnapshot> file = clientFile(ignored -> { });

        assertTrue(file.load().successful());
        assertFalse(file.current().featureEnabled("connected_texture_fix"));
        assertTrue(file.current().fixWalls());
        assertTrue(file.current().fixBeds());
        assertTrue(file.current().fixDoors());

        JsonObject features = json(path).getAsJsonObject("features");
        assertFalse(features.has("wall_visual_fix"));
        JsonObject connected = features.getAsJsonObject("connected_texture_fix");
        assertEquals(7, connected.get("future_option").getAsInt());
        assertTrue(Files.isRegularFile(path.resolveSibling("client.json.bak")));
    }

    @Test
    public void damagedJsonKeepsOriginalAndLastValidSnapshot() throws IOException {
        ConfigFile<ClientConfigSnapshot> file = clientFile(ignored -> { });
        assertTrue(file.load().successful());
        ClientConfigSnapshot previous = file.current();
        String damaged = "{\n  \"features\": [\n";
        Files.writeString(file.path(), damaged);

        ConfigLoadResult result = file.reload();

        assertFalse(result.successful());
        assertEquals(previous, file.current());
        assertEquals(damaged, Files.readString(file.path()));
        assertTrue(result.errorMessage().orElseThrow().contains(file.path().toString()));
        assertTrue(result.errorMessage().orElseThrow().contains("line"));
    }

    @Test
    public void interruptedSaveKeepsFileAndSnapshot() throws IOException {
        AtomicBoolean fail = new AtomicBoolean();
        NioAtomicFileWriter delegate = new NioAtomicFileWriter(LOGGER);
        AtomicFileWriter writer = (path, content) -> {
            if (fail.get()) {
                throw new IOException("simulated interruption");
            }
            delegate.write(path, content);
        };
        ConfigFile<ClientConfigSnapshot> file = new ConfigFile<>(
                temporaryDirectory.resolve(ConfigConstants.CLIENT_FILE),
                new ClientConfigCodec(), writer, LOGGER, ignored -> { }
        );
        assertTrue(file.load().successful());
        byte[] previousFile = Files.readAllBytes(file.path());
        ClientConfigSnapshot previous = file.current();
        ClientConfigDraft draft = new ClientConfigDraft(previous);
        draft.setFeatureEnabled("smart_pick", false);
        fail.set(true);

        ConfigLoadResult result = file.save(draft.snapshot());

        assertFalse(result.successful());
        assertEquals(previous, file.current());
        assertTrue(java.util.Arrays.equals(previousFile, Files.readAllBytes(file.path())));
    }

    @Test
    public void failedMigrationRetainsOriginalAndProvidesBackup() throws IOException {
        Path path = temporaryDirectory.resolve(ConfigConstants.CLIENT_FILE);
        String original = "{\"features\":{\"smart_pick\":false}}";
        Files.writeString(path, original);
        ConfigFile<ClientConfigSnapshot> file = new ConfigFile<>(
                path,
                new ClientConfigCodec(),
                (target, content) -> { throw new IOException("simulated migration failure"); },
                LOGGER,
                ignored -> { }
        );

        ConfigLoadResult result = file.load();

        assertFalse(result.successful());
        assertEquals(original, Files.readString(path));
        assertEquals(original, Files.readString(path.resolveSibling("client.json.bak")));
        assertNotNull(file.current());
    }

    @Test
    public void successfulSaveNotifiesOnlyChangedFeatureNamespace() {
        List<ConfigChange> changes = new ArrayList<>();
        ConfigFile<ClientConfigSnapshot> file = clientFile(changes::add);
        assertTrue(file.load().successful());
        ClientConfigDraft draft = new ClientConfigDraft(file.current());
        draft.setFeatureEnabled("palette_wheel", false);

        assertTrue(file.save(draft.snapshot()).successful());

        assertEquals(1, changes.size());
        assertEquals(java.util.Set.of("palette_wheel"), changes.getFirst().namespaces());
    }

    @Test
    public void serverWatcherReloadsReachChanges() throws Exception {
        CountDownLatch changed = new CountDownLatch(1);
        AtomicReference<ConfigChange> observed = new AtomicReference<>();
        ServerConfigManager manager = new ServerConfigManager(
                temporaryDirectory,
                LOGGER,
                new NioAtomicFileWriter(LOGGER),
                change -> {
                    observed.set(change);
                    changed.countDown();
                }
        );
        assertTrue(manager.load().successful());
        manager.startWatching(Runnable::run);
        try {
            Files.writeString(manager.path(), """
                    {"config_version":1,"features":{"reach_extension":{
                      "enabled":true,"maximum_distance":64,"creative_only":true,
                      "required_permission_level":0,"requests_per_second":10
                    }}}
                    """);

            assertTrue(changed.await(3, TimeUnit.SECONDS));
            assertTrue(manager.current().reachEnabled());
            assertEquals(64, manager.current().maximumDistance());
            assertEquals(java.util.Set.of("reach_extension"), observed.get().namespaces());
        } finally {
            manager.close();
        }
    }

    @Test
    public void changingPresetImmediatelyRebuildsEffectiveWheelWithoutOverwritingOverrides() {
        ClientConfigManager manager = new ClientConfigManager(temporaryDirectory, LOGGER);
        assertTrue(manager.load().values().stream().allMatch(ConfigLoadResult::successful));
        ClientConfigDraft draft = new ClientConfigDraft(manager.client());
        draft.setPrimaryPalettePreset(PalettePreset.COLOR_CATEGORIES);

        assertTrue(manager.save(draft).successful());

        assertEquals(8, manager.primaryWheel().typedGroups().size());
        assertTrue(manager.primaryWheel().overrideGroups().isEmpty());
        assertTrue(json(temporaryDirectory.resolve(ConfigConstants.PRIMARY_WHEEL_FILE))
                .getAsJsonArray("groups").isEmpty());
    }

    @Test
    public void legacyColorPlaceholderNameMapsToColorCategories() {
        JsonObject root = new JsonObject();
        root.addProperty("config_version", 1);
        JsonObject features = new JsonObject();
        JsonObject palette = new JsonObject();
        palette.addProperty("primary_default_preset", "color_placeholder");
        features.add("palette_wheel", palette);
        root.add("features", features);

        ClientConfigSnapshot snapshot = new ClientConfigCodec().decode(root).snapshot();

        assertEquals(PalettePreset.COLOR_CATEGORIES, snapshot.primaryPalettePreset());
    }

    @Test
    public void unknownOnlyChangeIsPreservedWithoutFeatureNotification() throws IOException {
        List<ConfigChange> changes = new ArrayList<>();
        ConfigFile<ClientConfigSnapshot> file = clientFile(changes::add);
        assertTrue(file.load().successful());
        JsonObject edited = new ClientConfigCodec().encode(file.current());
        edited.getAsJsonObject("features").getAsJsonObject("smart_pick").addProperty("future_option", 7);
        Files.writeString(file.path(), edited.toString());

        assertTrue(file.reload().successful());

        assertTrue(changes.isEmpty());
        ClientConfigDraft reset = new ClientConfigDraft(file.current());
        reset.restoreDefaults();
        assertTrue(file.save(reset.snapshot()).successful());
        assertEquals(7, json(file.path()).getAsJsonObject("features")
                .getAsJsonObject("smart_pick").get("future_option").getAsInt());
    }

    private ConfigFile<ClientConfigSnapshot> clientFile(java.util.function.Consumer<ConfigChange> listener) {
        return new ConfigFile<>(
                temporaryDirectory.resolve(ConfigConstants.CLIENT_FILE),
                new ClientConfigCodec(),
                new NioAtomicFileWriter(LOGGER),
                LOGGER,
                listener
        );
    }

    private static JsonObject json(Path path) {
        try {
            return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException exception) {
            throw new AssertionError(exception);
        }
    }
}
