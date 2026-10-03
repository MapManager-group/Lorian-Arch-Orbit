package com.davidblackcn.lorianarchorbit.client;

import com.davidblackcn.lorianarchorbit.interaction.HudLayout;
import com.davidblackcn.lorianarchorbit.interaction.HudPoint;
import com.davidblackcn.lorianarchorbit.interaction.RadialAnimationMode;
import com.davidblackcn.lorianarchorbit.interaction.RadialAnimationState;
import com.davidblackcn.lorianarchorbit.interaction.RadialGeometry;
import com.davidblackcn.lorianarchorbit.interaction.RadialMenuSnapshot;
import com.davidblackcn.lorianarchorbit.interaction.RadialMenuWindow;
import com.davidblackcn.lorianarchorbit.interaction.PaletteRadialLayout;
import com.davidblackcn.lorianarchorbit.interaction.RadialRotationState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public final class ClientHudOverlayManager {
    private static final long ANIMATION_MILLIS = 180;
    private static final long ROTATION_MILLIS = 140;
    private NumericOverlay numeric;
    private RadialOverlay radial;

    public synchronized void showNumeric(String ownerId, Component text) {
        numeric = new NumericOverlay(requireOwner(ownerId), Objects.requireNonNull(text, "text"), false, true);
    }

    public synchronized void showNumericAbove(String ownerId, Component text) {
        numeric = new NumericOverlay(requireOwner(ownerId), Objects.requireNonNull(text, "text"), true, true);
    }

    public synchronized void showTransparentNumericAbove(String ownerId, Component text) {
        numeric = new NumericOverlay(requireOwner(ownerId), Objects.requireNonNull(text, "text"), true, false);
    }

    public synchronized void hideNumeric(String ownerId) {
        if (numeric != null && numeric.ownerId.equals(ownerId)) {
            numeric = null;
        }
    }

    public synchronized void showRadial(
            String ownerId,
            RadialMenuSnapshot<RadialHudEntry> snapshot,
            RadialAnimationMode animation,
            long nowMillis
    ) {
        showRadial(ownerId, snapshot, animation, nowMillis, false);
    }

    public synchronized void showPaletteRadial(String ownerId, RadialMenuSnapshot<RadialHudEntry> snapshot,
                                               RadialAnimationMode animation, long nowMillis) {
        showRadial(ownerId, snapshot, animation, nowMillis, true);
    }

    private void showRadial(String ownerId, RadialMenuSnapshot<RadialHudEntry> snapshot,
                            RadialAnimationMode animation, long nowMillis, boolean adaptive) {
        requireOwner(ownerId);
        Objects.requireNonNull(snapshot, "snapshot");
        if (snapshot.entries().isEmpty()) {
            hideRadial(ownerId);
            return;
        }
        RadialAnimationState state = new RadialAnimationState(animation, nowMillis, ANIMATION_MILLIS);
        radial = new RadialOverlay(
                ownerId, snapshot, state, RadialRotationState.idle(nowMillis, ROTATION_MILLIS), -1L,
                adaptive, adaptive ? paletteLayout(snapshot).visibleCount() : snapshot.entries().size()
        );
    }

    public synchronized void updateRadial(String ownerId, RadialMenuSnapshot<RadialHudEntry> snapshot) {
        if (radial != null && radial.ownerId.equals(ownerId)) {
            radial = new RadialOverlay(
                    ownerId, Objects.requireNonNull(snapshot, "snapshot"), radial.animation,
                    radial.rotation, radial.closingStartedAtMillis, radial.adaptive, radial.visibleCount
            );
        }
    }

    public synchronized void rotateRadial(
            String ownerId,
            RadialMenuSnapshot<RadialHudEntry> snapshot,
            int selectionSteps,
            long nowMillis
    ) {
        if (radial != null && radial.ownerId.equals(ownerId)) {
            Objects.requireNonNull(snapshot, "snapshot");
            int entryCount = radial.adaptive ? paletteLayout(snapshot).visibleCount() : snapshot.entries().size();
            if (entryCount <= 0 || selectionSteps == 0) {
                updateRadial(ownerId, snapshot);
                return;
            }
            RadialRotationState previous = !radial.adaptive || entryCount == radial.visibleCount ? radial.rotation
                    : RadialRotationState.idle(nowMillis, ROTATION_MILLIS);
            RadialRotationState rotation = previous.retarget(
                    selectionSteps, entryCount, nowMillis, ROTATION_MILLIS
            );
            radial = new RadialOverlay(
                    ownerId, snapshot, radial.animation, rotation, radial.closingStartedAtMillis, radial.adaptive, entryCount
            );
        }
    }

    public synchronized void hideRadial(String ownerId) {
        if (radial != null && radial.ownerId.equals(ownerId)) {
            radial = null;
        }
    }

    public synchronized void closeRadial(String ownerId, long nowMillis) {
        if (radial != null && radial.ownerId.equals(ownerId) && radial.closingStartedAtMillis < 0) {
            radial = new RadialOverlay(
                    radial.ownerId, radial.snapshot, radial.animation, radial.rotation, nowMillis,
                    radial.adaptive, radial.visibleCount
            );
        }
    }

    public synchronized void clear() {
        numeric = null;
        radial = null;
    }

    public synchronized boolean hasRadial(String ownerId) {
        return radial != null && radial.ownerId.equals(ownerId);
    }

    public synchronized void render(GuiGraphicsExtractor graphics, Minecraft minecraft, long nowMillis) {
        if (minecraft.gui.screen() != null || minecraft.level == null || minecraft.player == null) {
            return;
        }
        if (radial != null) {
            if (radial.adaptive) {
                int visible = paletteLayout(radial.snapshot).visibleCount();
                if (visible != radial.visibleCount) {
                    radial = new RadialOverlay(radial.ownerId, radial.snapshot, radial.animation,
                            RadialRotationState.idle(nowMillis, ROTATION_MILLIS), radial.closingStartedAtMillis, true, visible);
                }
                try (var ignored = PaletteRenderContext.open(graphics, paletteViewport())) {
                    renderRadial(graphics, minecraft, radial, nowMillis);
                    if (numeric != null && numeric.ownerId.equals(radial.ownerId)) {
                        renderPaletteName(graphics, minecraft, numeric, radial);
                    }
                }
            } else {
                renderRadial(graphics, minecraft, radial, nowMillis);
            }
            if (radial.closingStartedAtMillis >= 0
                    && nowMillis - radial.closingStartedAtMillis >= ANIMATION_MILLIS) {
                radial = null;
            }
        }
        if (numeric != null && (radial == null || !radial.adaptive || !numeric.ownerId.equals(radial.ownerId))) {
            renderNumeric(graphics, minecraft, numeric);
        }
    }

    private static PaletteViewport paletteViewport() {
        var window = Minecraft.getInstance().getWindow();
        return PaletteViewport.calculate(Math.max(1, window.getWidth()), Math.max(1, window.getHeight()), window.getGuiScale());
    }

    private static PaletteRadialLayout paletteLayout(RadialMenuSnapshot<?> snapshot) {
        PaletteViewport viewport = paletteViewport();
        return PaletteRadialLayout.hud(snapshot.entries().size(), viewport.width(), viewport.height(), 1 / viewport.scale());
    }

    private static void renderPaletteName(GuiGraphicsExtractor graphics, Minecraft minecraft,
                                          NumericOverlay numeric, RadialOverlay radial) {
        int maximum = graphics.guiWidth() - 24;
        String label = numeric.text.getString();
        if (minecraft.font.width(label) > maximum) {
            label = minecraft.font.plainSubstrByWidth(label, maximum - minecraft.font.width("…")) + "…";
        }
        renderNumeric(graphics, minecraft, new NumericOverlay(numeric.ownerId, Component.literal(label), false, true));
        if (radial.snapshot.entries().size() > radial.visibleCount) {
            Component hint = Component.translatable("palette_editor.lorian_arch_orbit.wheel_overflow",
                    radial.snapshot.selectedIndex() + 1, radial.snapshot.entries().size());
            graphics.centeredText(minecraft.font, hint, graphics.guiWidth() / 2,
                    graphics.guiHeight() / 2 + 26, 0xFFBBBBBB);
        }
    }

    private static void renderNumeric(
            GuiGraphicsExtractor graphics,
            Minecraft minecraft,
            NumericOverlay overlay
    ) {
        int width = minecraft.font.width(overlay.text);
        HudPoint point = overlay.above
                ? HudLayout.crosshairTextAbove(graphics.guiWidth(), graphics.guiHeight(), width,
                        minecraft.font.lineHeight, HudLayout.DEFAULT_MARGIN)
                : HudLayout.crosshairText(graphics.guiWidth(), graphics.guiHeight(), width,
                        minecraft.font.lineHeight, HudLayout.DEFAULT_MARGIN);
        if (overlay.background) {
            graphics.fill(point.x() - 3, point.y() - 2, point.x() + width + 3,
                    point.y() + minecraft.font.lineHeight + 2, 0x90000000);
        }
        graphics.text(minecraft.font, overlay.text, point.x(), point.y(), 0xFFFFFFFF, true);
    }

    private static void renderRadial(
            GuiGraphicsExtractor graphics,
            Minecraft minecraft,
            RadialOverlay overlay,
            long nowMillis
    ) {
        int radius = HudLayout.adaptiveRadialRadius(
                graphics.guiWidth(), graphics.guiHeight(),
                overlay.snapshot.entries().size(),
                RadialWheelVisuals.MINIMUM_RADIUS, RadialWheelVisuals.ITEM_HALF_SIZE, HudLayout.DEFAULT_MARGIN
        );
        RadialMenuSnapshot<RadialHudEntry> snapshot = overlay.snapshot;
        if (overlay.adaptive) {
            PaletteRadialLayout layout = paletteLayout(snapshot);
            radius = layout.radius();
            snapshot = RadialMenuWindow.from(snapshot.entries(), snapshot.selectedIndex(), Math.max(1, layout.visibleCount()));
        }
        HudPoint center = HudLayout.crosshairRadialCenter(
                graphics.guiWidth(), graphics.guiHeight(), HudLayout.DEFAULT_MARGIN
        );
        double closeProgress = overlay.closingStartedAtMillis < 0 ? 1.0
                : Math.max(0.0, 1.0 - (double) (nowMillis - overlay.closingStartedAtMillis) / ANIMATION_MILLIS);
        var slots = RadialGeometry.slots(
                snapshot, center, radius, overlay.animation, nowMillis,
                overlay.rotation.offsetRadians(nowMillis) + (overlay.adaptive
                        ? ClientConfigRuntime.configManager().client().paletteTargetPosition().angleOffset() : 0)
        );
        for (var slot : slots) {
            if (slot.progress() * closeProgress <= 0.01) {
                continue;
            }
            int slotX = center.x() + (int) Math.round((slot.x() - center.x()) * closeProgress);
            int slotY = center.y() + (int) Math.round((slot.y() - center.y()) * closeProgress);
            ItemStack icon = slot.value().icon();
            if (!icon.isEmpty()) {
                RadialWheelVisuals.renderItem(graphics, icon, slotX, slotY);
            } else {
                Component label = slot.value().label();
                graphics.centeredText(
                        minecraft.font, label, slotX, slotY - minecraft.font.lineHeight / 2, 0xFFFFFFFF
                );
            }
        }
        if (overlay.adaptive && !snapshot.entries().isEmpty()) {
            double openProgress = overlay.animation.entryProgress(0, snapshot.entries().size(), nowMillis);
            RadialWheelVisuals.renderTargetArrow(graphics, center.x(), center.y(), radius * openProgress * closeProgress,
                    ClientConfigRuntime.configManager().client().paletteTargetPosition(),
                    ClientConfigRuntime.configManager().client().paletteArrowStyle(), nowMillis);
        }
    }

    private static String requireOwner(String ownerId) {
        if (ownerId == null || ownerId.isBlank()) {
            throw new IllegalArgumentException("ownerId must not be blank");
        }
        return ownerId;
    }

    private record NumericOverlay(String ownerId, Component text, boolean above, boolean background) {
    }

    private record RadialOverlay(
            String ownerId,
            RadialMenuSnapshot<RadialHudEntry> snapshot,
            RadialAnimationState animation,
            RadialRotationState rotation,
            long closingStartedAtMillis,
            boolean adaptive,
            int visibleCount
    ) {
    }
}
