package com.davidblackcn.lorianarchorbit.client;

import com.davidblackcn.lorianarchorbit.interaction.ClientInputCoordinator;
import com.davidblackcn.lorianarchorbit.interaction.RadialAnimationMode;
import com.davidblackcn.lorianarchorbit.interaction.RadialMenuSnapshot;
import com.davidblackcn.lorianarchorbit.interaction.ScrollAccumulator;
import com.davidblackcn.lorianarchorbit.interaction.WheelInputArbiter;
import com.davidblackcn.lorianarchorbit.interaction.WheelLease;
import com.davidblackcn.lorianarchorbit.interaction.WheelPriority;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientRawInputEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class ClientInteractionRuntime {
    private static final String PREVIEW_OWNER = "development_preview";
    private static final long TIME_ORIGIN_NANOS = System.nanoTime();
    private static final ClientInputCoordinator INPUTS = new ClientInputCoordinator();
    private static final WheelInputArbiter WHEEL = new WheelInputArbiter();
    private static final ClientHudOverlayManager HUD = new ClientHudOverlayManager();
    private static boolean initialized;
    private static Object lastWorld;
    private static WheelLease previewLease;
    private static RadialMenuSnapshot<RadialHudEntry> previewRadial;
    private static final ScrollAccumulator PREVIEW_SCROLL = new ScrollAccumulator();

    private ClientInteractionRuntime() {
    }

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        ClientTickEvent.CLIENT_POST.register(ClientInteractionRuntime::tick);
        ClientRawInputEvent.MOUSE_SCROLLED.register(ClientInteractionRuntime::onMouseScrolled);
        ClientGuiEvent.RENDER_HUD.register(ClientInteractionRuntime::renderHud);
    }

    public static ClientInputCoordinator inputs() {
        return INPUTS;
    }

    public static WheelInputArbiter wheel() {
        return WHEEL;
    }

    public static ClientHudOverlayManager hud() {
        return HUD;
    }

    public static synchronized boolean toggleDevelopmentPreview() {
        if (previewLease != null && previewLease.active()) {
            clearDevelopmentPreview();
            return false;
        }
        previewRadial = new RadialMenuSnapshot<>(List.of(
                RadialHudEntry.text(Component.literal("A")),
                RadialHudEntry.text(Component.literal("B")),
                RadialHudEntry.text(Component.literal("C")),
                RadialHudEntry.text(Component.literal("D")),
                RadialHudEntry.text(Component.literal("E")),
                RadialHudEntry.text(Component.literal("F")),
                RadialHudEntry.text(Component.literal("G")),
                RadialHudEntry.text(Component.literal("H")),
                RadialHudEntry.text(Component.literal("I")),
                RadialHudEntry.text(Component.literal("J")),
                RadialHudEntry.text(Component.literal("K")),
                RadialHudEntry.text(Component.literal("L"))
        ), 0);
        previewLease = WHEEL.claim(
                PREVIEW_OWNER,
                WheelPriority.PALETTE_WHEEL,
                ClientInteractionRuntime::scrollPreview,
                ClientInteractionRuntime::previewRevoked
        ).orElse(null);
        if (previewLease == null) {
            previewRadial = null;
            return false;
        }
        long now = nowMillis();
        HUD.showRadial(PREVIEW_OWNER, previewRadial, RadialAnimationMode.CLOCKWISE, now);
        HUD.showNumeric(PREVIEW_OWNER, Component.literal("Stage 4 preview"));
        return true;
    }

    public static synchronized void clearDevelopmentPreview() {
        WheelLease lease = previewLease;
        previewLease = null;
        if (lease != null) {
            lease.close();
        }
        previewRadial = null;
        PREVIEW_SCROLL.reset();
        HUD.hideRadial(PREVIEW_OWNER);
        HUD.hideNumeric(PREVIEW_OWNER);
    }

    public static synchronized void clearTransientState() {
        clearDevelopmentPreview();
        WHEEL.clear();
        HUD.clear();
    }

    public static synchronized void close() {
        clearTransientState();
        INPUTS.reset(nowMillis());
        lastWorld = null;
    }

    private static void tick(Minecraft minecraft) {
        long now = nowMillis();
        boolean focused = minecraft.isWindowActive()
                && minecraft.getWindow().isFocused()
                && minecraft.gui.screen() == null;
        Object world = minecraft.level;
        if (!focused || world != lastWorld) {
            WHEEL.clear();
            HUD.clear();
            PREVIEW_SCROLL.reset();
            previewLease = null;
            previewRadial = null;
        }
        lastWorld = world;
        INPUTS.tick(now, focused, world);
    }

    private static EventResult onMouseScrolled(Minecraft minecraft, double amountX, double amountY) {
        if (!minecraft.isWindowActive() || !minecraft.getWindow().isFocused() || minecraft.gui.screen() != null) {
            WHEEL.clear();
            return EventResult.pass();
        }
        return WHEEL.dispatch(amountX, amountY) ? EventResult.interruptFalse() : EventResult.pass();
    }

    private static void renderHud(net.minecraft.client.gui.GuiGraphicsExtractor graphics,
                                  net.minecraft.client.DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (ClientConfigRuntime.configManager().client().hudEnabled()) {
            HUD.render(graphics, minecraft, nowMillis());
        }
    }

    private static synchronized boolean scrollPreview(double amountX, double amountY) {
        if (previewRadial == null || !HUD.hasRadial(PREVIEW_OWNER)) {
            return false;
        }
        double primaryAmount = amountY != 0.0 ? amountY : amountX;
        int steps = PREVIEW_SCROLL.add(primaryAmount);
        if (steps != 0) {
            previewRadial = previewRadial.rotate(-steps);
            HUD.updateRadial(PREVIEW_OWNER, previewRadial);
        }
        return true;
    }

    private static synchronized void previewRevoked() {
        previewLease = null;
        previewRadial = null;
        PREVIEW_SCROLL.reset();
        HUD.hideRadial(PREVIEW_OWNER);
        HUD.hideNumeric(PREVIEW_OWNER);
    }

    public static long nowMillis() {
        return (System.nanoTime() - TIME_ORIGIN_NANOS) / 1_000_000L;
    }
}
