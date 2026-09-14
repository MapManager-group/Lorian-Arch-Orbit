package com.davidblackcn.lorianarchorbit.fabric.client.compat.radial;

import com.davidblackcn.lorianarchorbit.LorianArchOrbit;
import com.davidblackcn.lorianarchorbit.client.ClientPaletteRuntime;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class RadialInputCompat {
    private static final System.Logger LOGGER = System.getLogger(LorianArchOrbit.MOD_ID + ".compat.radial");
    private static boolean initialized;
    private static boolean disabled;
    private static KeyMapping radialKey;
    private static Method lockKey;

    private RadialInputCompat() {
    }

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        ClientTickEvents.START_CLIENT_TICK.register(RadialInputCompat::onStartClientTick);
    }

    private static void onStartClientTick(Minecraft minecraft) {
        if (disabled || !resolveRadialAccess()) {
            return;
        }
        KeyMapping lorianKey = ClientPaletteRuntime.wheelKey();
        if (lorianKey == null
                || !lorianKey.isDown()
                || !lorianKey.saveString().equals(radialKey.saveString())
                || !ClientPaletteRuntime.canHandleCurrentInput(minecraft)) {
            return;
        }
        try {
            lockKey.invoke(null);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            disable("Could not lock Radial's open key; key conflict arbitration is disabled", exception);
        }
    }

    private static synchronized boolean resolveRadialAccess() {
        if (disabled) {
            return false;
        }
        if (radialKey != null && lockKey != null) {
            return true;
        }
        try {
            Class<?> radialClient = Class.forName("dev.velolib.radial.RadialClient");
            Field openRadial = radialClient.getField("OPEN_RADIAL");
            Object key = openRadial.get(null);
            if (!(key instanceof KeyMapping mapping)) {
                throw new IllegalStateException("Radial OPEN_RADIAL is not a KeyMapping");
            }
            Method lock = radialClient.getMethod("lockKey");
            radialKey = mapping;
            lockKey = lock;
            return true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            disable("Radial internals are incompatible; key conflict arbitration is disabled", exception);
            return false;
        }
    }

    private static synchronized void disable(String message, Throwable throwable) {
        if (disabled) {
            return;
        }
        disabled = true;
        radialKey = null;
        lockKey = null;
        LOGGER.log(System.Logger.Level.WARNING, message, throwable);
    }
}
