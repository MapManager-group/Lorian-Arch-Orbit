package com.davidblackcn.lorianarchorbit.config;

import com.davidblackcn.lorianarchorbit.LorianArchOrbit;
import com.davidblackcn.lorianarchorbit.feature.FeatureManager;
import com.davidblackcn.lorianarchorbit.feature.FeatureServices;
import com.davidblackcn.lorianarchorbit.feature.RuntimeSide;
import com.davidblackcn.lorianarchorbit.reach.ServerReachRuntime;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.platform.Platform;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;

public final class CommonConfigRuntime {
    private static final System.Logger LOGGER = System.getLogger(LorianArchOrbit.MOD_ID + ".config");
    private static ServerConfigManager configManager;
    private static FeatureManager featureManager;
    private static MinecraftServer activeServer;

    private CommonConfigRuntime() {
    }

    public static synchronized void initialize() {
        if (configManager != null) {
            return;
        }
        configManager = new ServerConfigManager(
                Platform.getConfigFolder().resolve(LorianArchOrbit.MOD_ID),
                LOGGER,
                new NioAtomicFileWriter(LOGGER),
                CommonConfigRuntime::onConfigChanged
        );
        configManager.load();
        featureManager = new FeatureManager(
                LorianArchOrbit.features(), RuntimeSide.SERVER, FeatureServices.noOp(LOGGER)
        );
        featureManager.setUserEnabled("reach_extension", configManager.current().reachEnabled());
        featureManager.initialize();
        LifecycleEvent.SERVER_STARTED.register(CommonConfigRuntime::serverStarted);
        LifecycleEvent.SERVER_STOPPING.register(CommonConfigRuntime::serverStopping);
    }

    public static ServerConfigManager configManager() {
        if (configManager == null) {
            throw new IllegalStateException("Common configuration runtime is not initialized");
        }
        return configManager;
    }

    public static FeatureManager featureManager() {
        if (featureManager == null) {
            throw new IllegalStateException("Common feature runtime is not initialized");
        }
        return featureManager;
    }

    private static synchronized void serverStarted(MinecraftServer server) {
        activeServer = server;
        configManager.reload();
        try {
            configManager.startWatching(server::execute);
        } catch (IOException exception) {
            LOGGER.log(System.Logger.Level.ERROR, "Could not start server configuration watcher", exception);
        }
    }

    private static synchronized void serverStopping(MinecraftServer server) {
        configManager.close();
        if (activeServer == server) activeServer = null;
    }

    private static synchronized void onConfigChanged(ConfigChange change) {
        if (!change.namespaces().contains("reach_extension") || featureManager == null) return;
        featureManager.setUserEnabled("reach_extension", configManager.current().reachEnabled());
        if (activeServer != null) ServerReachRuntime.configsChanged(activeServer);
    }
}
