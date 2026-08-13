package com.davidblackcn.lorianarchorbit.client;

import com.davidblackcn.lorianarchorbit.LorianArchOrbit;
import com.davidblackcn.lorianarchorbit.config.ClientConfigDraft;
import com.davidblackcn.lorianarchorbit.config.ClientConfigManager;
import com.davidblackcn.lorianarchorbit.config.ClientConfigSnapshot;
import com.davidblackcn.lorianarchorbit.config.ConfigChange;
import com.davidblackcn.lorianarchorbit.config.ConfigLoadResult;
import com.davidblackcn.lorianarchorbit.client.connected.ConnectedTextureRuntime;
import com.davidblackcn.lorianarchorbit.client.invisible.InvisibleBlocksRuntime;
import com.davidblackcn.lorianarchorbit.feature.FeatureManager;
import com.davidblackcn.lorianarchorbit.feature.FeatureServices;
import com.davidblackcn.lorianarchorbit.feature.RuntimeSide;
import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.event.events.client.ClientCommandRegistrationEvent;
import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.platform.Platform;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public final class ClientConfigRuntime {
    private static final System.Logger LOGGER = System.getLogger(LorianArchOrbit.MOD_ID + ".client.config");
    private static final String YACL_MOD_ID = "yet_another_config_lib_v3";
    private static ClientConfigManager configManager;
    private static FeatureManager featureManager;
    private static KeyMapping openConfig;

    private ClientConfigRuntime() {
    }

    public static synchronized void initialize() {
        if (configManager != null) {
            return;
        }
        configManager = new ClientConfigManager(
                Platform.getConfigFolder().resolve(LorianArchOrbit.MOD_ID), LOGGER
        );
        configManager.load();
        ClientInteractionRuntime.initialize();
        featureManager = new FeatureManager(
                LorianArchOrbit.features(), RuntimeSide.CLIENT, FeatureServices.noOp(LOGGER)
        );
        applySnapshot(configManager.client());
        featureManager.initialize();
        configManager.addListener(ClientConfigRuntime::onConfigChanged);
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(LorianArchOrbit.MOD_ID, "main")
        );
        ClientPaletteRuntime.initialize(category);
        ClientReachRuntime.initialize(category);
        InvisibleBlocksRuntime.initialize(category);
        openConfig = new KeyMapping(
                "key.lorian_arch_orbit.open_config", InputConstants.KEY_O, category
        );
        KeyMappingRegistry.register(openConfig);
        ClientTickEvent.CLIENT_POST.register(ClientConfigRuntime::onClientTick);
        ClientCommandRegistrationEvent.EVENT.register((dispatcher, context) -> {
            var root = ClientCommandRegistrationEvent.literal(LorianArchOrbit.MOD_ID)
                    .then(ClientCommandRegistrationEvent.literal("reload").executes(command -> {
                            Map<Path, ConfigLoadResult> results = configManager.reloadAll();
                            boolean successful = results.values().stream().allMatch(ConfigLoadResult::successful);
                            if (successful) {
                                command.getSource().arch$sendSuccess(
                                        () -> Component.translatable("command.lorian_arch_orbit.reload.success"), false
                                );
                                return 1;
                            }
                            command.getSource().arch$sendFailure(
                                    Component.translatable("command.lorian_arch_orbit.reload.failure")
                            );
                            return 0;
                        }));
            if (Platform.isDevelopmentEnvironment()) {
                root.then(ClientCommandRegistrationEvent.literal("preview_hud").executes(command -> {
                    boolean visible = ClientInteractionRuntime.toggleDevelopmentPreview();
                    command.getSource().arch$sendSuccess(
                            () -> Component.translatable(visible
                                    ? "command.lorian_arch_orbit.preview_hud.shown"
                                    : "command.lorian_arch_orbit.preview_hud.hidden"),
                            false
                    );
                    return 1;
                }));
            }
            dispatcher.register(root);
        });
        ClientLifecycleEvent.CLIENT_STARTED.register(ClientConfigRuntime::onClientStarted);
        ClientLifecycleEvent.CLIENT_STOPPING.register(client -> close());
    }

    public static ClientConfigManager configManager() {
        if (configManager == null) {
            throw new IllegalStateException("Client configuration runtime is not initialized");
        }
        return configManager;
    }

    public static boolean initialized() {
        return configManager != null;
    }

    public static ConfigLoadResult save(ClientConfigDraft draft) {
        return configManager().save(draft);
    }

    public static Screen createScreen(Screen parent) {
        if (!Platform.isModLoaded(YACL_MOD_ID)) {
            LOGGER.log(System.Logger.Level.WARNING, "YACL is not installed; the configuration screen is unavailable");
            return parent;
        }
        return ClientConfigScreen.create(parent);
    }

    private static void onClientTick(Minecraft minecraft) {
        ClientPaletteRuntime.tick(minecraft);
        ClientReachRuntime.tick(minecraft);
        InvisibleBlocksRuntime.tick(minecraft);
        while (openConfig.consumeClick()) {
            minecraft.setScreenAndShow(createScreen(null));
        }
    }

    private static void onClientStarted(Minecraft minecraft) {
        ClientSmartPickRuntime.initialize();
        try {
            configManager.startWatching(minecraft::execute);
        } catch (IOException exception) {
            LOGGER.log(System.Logger.Level.ERROR, "Could not start client configuration watcher", exception);
        }
    }

    private static void onConfigChanged(ConfigChange change) {
        ClientConfigSnapshot snapshot = configManager.client();
        for (String namespace : change.namespaces()) {
            if (namespace.equals("palette_wheel")) {
                ClientPaletteRuntime.configsChanged();
            }
            if (namespace.equals("smart_pick")) {
                ClientSmartPickRuntime.configsChanged();
            }
            if (namespace.equals("reach_extension")) {
                ClientReachRuntime.configsChanged();
            }
            if (namespace.equals(ConnectedTextureRuntime.FEATURE_ID)) {
                ConnectedTextureRuntime.configsChanged();
            }
            if (namespace.equals("invisible_blocks")) {
                InvisibleBlocksRuntime.configsChanged();
            }
            if (snapshot.enabledFeatures().containsKey(namespace)) {
                featureManager.setUserEnabled(namespace, snapshot.featureEnabled(namespace));
            } else if (namespace.equals("ui") && !snapshot.hudEnabled()) {
                ClientInteractionRuntime.clearTransientState();
            }
        }
    }

    private static void applySnapshot(ClientConfigSnapshot snapshot) {
        snapshot.enabledFeatures().forEach(featureManager::setUserEnabled);
    }

    private static synchronized void close() {
        if (configManager != null) {
            configManager.close();
        }
        if (featureManager != null) {
            featureManager.shutdown();
        }
        ClientSmartPickRuntime.closeRuntime();
        ClientReachRuntime.closeRuntime();
        ClientPaletteRuntime.closeRuntime();
        InvisibleBlocksRuntime.closeRuntime();
        ClientInteractionRuntime.close();
    }
}
