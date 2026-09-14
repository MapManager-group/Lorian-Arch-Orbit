package com.davidblackcn.lorianarchorbit.client.connected;

import com.davidblackcn.lorianarchorbit.LorianArchOrbit;
import com.davidblackcn.lorianarchorbit.client.ClientConfigRuntime;
import com.davidblackcn.lorianarchorbit.config.ClientConfigSnapshot;
import net.minecraft.world.level.block.state.properties.ChestType;

import java.util.EnumSet;
import java.util.Set;

public final class ChestRenderDiagnostics {
    public static final String BUILD_MARKER = "chest-production-diagnosis-1";

    private static final System.Logger LOGGER =
            System.getLogger(LorianArchOrbit.MOD_ID + ".chest_diagnostics");
    private static final Set<ChestType> LOGGED_TYPES = EnumSet.noneOf(ChestType.class);

    private static boolean initialized;
    private static volatile boolean forceSealed;
    private static long redirectHits;
    private static long singleHits;
    private static long leftHits;
    private static long rightHits;
    private static long vanillaReturns;
    private static long singleVanillaReturns;
    private static long leftVanillaReturns;
    private static long rightVanillaReturns;
    private static long sealedLeftReturns;
    private static long sealedRightReturns;
    private static boolean lastRuntimeEnabled;
    private static ChestType lastType;

    private ChestRenderDiagnostics() {
    }

    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        LOGGER.log(System.Logger.Level.INFO, "[LAO ChestDiag] build=" + BUILD_MARKER);
    }

    public static synchronized void onRedirect(ChestType type) {
        redirectHits++;
        lastType = type;
        switch (type) {
            case SINGLE -> singleHits++;
            case LEFT -> leftHits++;
            case RIGHT -> rightHits++;
        }
    }

    public static synchronized void recordRuntimeEnabled(ChestType type, boolean enabled) {
        lastType = type;
        lastRuntimeEnabled = enabled;
    }

    public static synchronized void recordVanillaReturn(ChestType type, boolean runtimeEnabled) {
        vanillaReturns++;
        switch (type) {
            case SINGLE -> singleVanillaReturns++;
            case LEFT -> leftVanillaReturns++;
            case RIGHT -> rightVanillaReturns++;
        }
        logFirstReturn(type, runtimeEnabled, "VANILLA");
    }

    public static synchronized void recordSealedReturn(ChestType type, boolean runtimeEnabled) {
        switch (type) {
            case LEFT -> sealedLeftReturns++;
            case RIGHT -> sealedRightReturns++;
            case SINGLE -> throw new IllegalArgumentException("A single chest cannot use a sealed double model");
        }
        logFirstReturn(type, runtimeEnabled, "SEALED_" + type);
    }

    public static boolean forceSealed() {
        return forceSealed;
    }

    public static void setForceSealed(boolean forced) {
        forceSealed = forced;
    }

    public static synchronized void resetCounters() {
        redirectHits = 0L;
        singleHits = 0L;
        leftHits = 0L;
        rightHits = 0L;
        vanillaReturns = 0L;
        singleVanillaReturns = 0L;
        leftVanillaReturns = 0L;
        rightVanillaReturns = 0L;
        sealedLeftReturns = 0L;
        sealedRightReturns = 0L;
    }

    public static synchronized String statusReport() {
        boolean configRuntimeInitialized = ClientConfigRuntime.initialized();
        boolean connectedTextureFeatureEnabled = false;
        boolean fixChests = false;
        if (configRuntimeInitialized) {
            ClientConfigSnapshot config = ClientConfigRuntime.configManager().client();
            connectedTextureFeatureEnabled = config.featureEnabled(ConnectedTextureRuntime.FEATURE_ID);
            fixChests = config.fixChests();
        }
        boolean runtimeEnabled = ConnectedTextureRuntime.enabled(ConnectionFixKind.CHEST);

        return "LAO Chest Diagnosis\n"
                + "build: " + BUILD_MARKER + "\n\n"
                + "configRuntimeInitialized: " + configRuntimeInitialized + "\n"
                + "connectedTextureFeatureEnabled: " + connectedTextureFeatureEnabled + "\n"
                + "fixChests: " + fixChests + "\n"
                + "runtimeEnabled(CHEST): " + runtimeEnabled + "\n\n"
                + "redirectHits: " + redirectHits + "\n"
                + "vanillaReturns: " + vanillaReturns + "\n\n"
                + "SINGLE:\n"
                + "seen: " + singleHits + "\n"
                + "vanilla: " + singleVanillaReturns + "\n\n"
                + "LEFT:\n"
                + "seen: " + leftHits + "\n"
                + "vanilla: " + leftVanillaReturns + "\n"
                + "sealed: " + sealedLeftReturns + "\n\n"
                + "RIGHT:\n"
                + "seen: " + rightHits + "\n"
                + "vanilla: " + rightVanillaReturns + "\n"
                + "sealed: " + sealedRightReturns + "\n\n"
                + "forceSealed: " + forceSealed + "\n\n"
                + "lastType: " + (lastType == null ? "NONE" : lastType) + "\n"
                + "lastRuntimeEnabled: " + lastRuntimeEnabled;
    }

    private static void logFirstReturn(
            ChestType type,
            boolean runtimeEnabled,
            String returnedModel
    ) {
        if (!LOGGED_TYPES.add(type)) return;
        LOGGER.log(
                System.Logger.Level.INFO,
                "[LAO ChestDiag] redirect entered: type=" + type
                        + " runtimeEnabled=" + runtimeEnabled
                        + " return=" + returnedModel
        );
    }
}
