package com.davidblackcn.lorianarchorbit.config;

import java.util.List;

public final class ConfigConstants {
    public static final int CURRENT_VERSION = 2;
    public static final String CLIENT_FILE = "client.json";
    public static final String SERVER_FILE = "server.json";
    public static final String PRIMARY_WHEEL_FILE = "lorian_arch_orbit-wheel-primary.json";
    public static final String SECONDARY_WHEEL_FILE = "lorian_arch_orbit-wheel-secondary.json";
    public static final List<String> CLIENT_FEATURE_IDS = List.of(
            "reach_extension",
            "palette_wheel",
            "smart_pick",
            "connected_texture_fix",
            "invisible_blocks"
    );

    private ConfigConstants() {
    }
}
