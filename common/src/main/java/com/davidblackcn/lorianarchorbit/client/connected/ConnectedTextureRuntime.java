package com.davidblackcn.lorianarchorbit.client.connected;

import com.davidblackcn.lorianarchorbit.client.ClientConfigRuntime;
import com.davidblackcn.lorianarchorbit.config.ClientConfigSnapshot;
import net.minecraft.client.Minecraft;

public final class ConnectedTextureRuntime {
    public static final String FEATURE_ID = "connected_texture_fix";
    private ConnectedTextureRuntime() {
    }

    public static boolean enabled(ConnectionFixKind kind) {
        ClientConfigSnapshot config = ClientConfigRuntime.configManager().client();
        if (!config.featureEnabled(FEATURE_ID)) return false;
        return switch (kind) {
            case WALL -> config.fixWalls();
            case BED -> config.fixBeds();
            case DOOR -> config.fixDoors();
            case PISTON -> config.fixPistons();
            case NETHER_PORTAL -> config.fixNetherPortals();
            case END_PORTAL -> config.fixEndPortals();
        };
    }

    public static void configsChanged() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) minecraft.reloadResourcePacks();
    }

}
