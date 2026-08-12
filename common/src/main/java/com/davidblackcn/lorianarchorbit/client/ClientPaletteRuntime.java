package com.davidblackcn.lorianarchorbit.client;

import com.davidblackcn.lorianarchorbit.config.WheelConfigSnapshot;
import com.davidblackcn.lorianarchorbit.interaction.GestureRegistration;
import com.davidblackcn.lorianarchorbit.interaction.InputGesture;
import com.davidblackcn.lorianarchorbit.interaction.InputGestureEvent;
import com.davidblackcn.lorianarchorbit.interaction.PressTiming;
import com.davidblackcn.lorianarchorbit.interaction.RadialAnimationMode;
import com.davidblackcn.lorianarchorbit.interaction.RadialMenuSnapshot;
import com.davidblackcn.lorianarchorbit.interaction.ScrollAccumulator;
import com.davidblackcn.lorianarchorbit.interaction.WheelLease;
import com.davidblackcn.lorianarchorbit.interaction.WheelPriority;
import com.davidblackcn.lorianarchorbit.palette.PaletteGroup;
import com.davidblackcn.lorianarchorbit.palette.PaletteLookup;
import com.davidblackcn.lorianarchorbit.palette.PaletteLayerGestureState;
import com.davidblackcn.lorianarchorbit.palette.PaletteMember;
import com.davidblackcn.lorianarchorbit.palette.PaletteWheelRules;
import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class ClientPaletteRuntime {
    private static final String OWNER = "palette_wheel";
    private static KeyMapping openWheel;
    private static KeyMapping openEditor;
    private static GestureRegistration gestureRegistration;
    private static WheelLease lease;
    private static RadialMenuSnapshot<PaletteEntry> radial;
    private static final PaletteLayerGestureState LAYER_GESTURE = new PaletteLayerGestureState();
    private static final ScrollAccumulator SCROLL = new ScrollAccumulator();

    private ClientPaletteRuntime() {
    }

    public static synchronized void initialize(KeyMapping.Category category) {
        if (gestureRegistration != null) {
            return;
        }
        openWheel = new KeyMapping("key.lorian_arch_orbit.palette_wheel", InputConstants.KEY_R, category);
        openEditor = new KeyMapping("key.lorian_arch_orbit.palette_editor", InputConstants.KEY_P, category);
        KeyMappingRegistry.register(openWheel);
        KeyMappingRegistry.register(openEditor);
        gestureRegistration = ClientInteractionRuntime.inputs().register(
                OWNER,
                new PressTiming(180, 250),
                openWheel::saveString,
                openWheel::isDown,
                ClientPaletteRuntime::enabled,
                ClientPaletteRuntime::onGesture
        );
    }

    public static void tick(Minecraft minecraft) {
        while (openEditor != null && openEditor.consumeClick()) {
            if (minecraft.gui.screen() == null) {
                minecraft.setScreenAndShow(new PaletteEditorScreen(null));
            }
        }
    }

    public static synchronized void configsChanged() {
        close(false);
    }

    public static synchronized void closeRuntime() {
        close(false);
        if (gestureRegistration != null) {
            gestureRegistration.close();
            gestureRegistration = null;
        }
    }

    private static boolean enabled() {
        return ClientConfigRuntime.configManager().client().featureEnabled(OWNER);
    }

    private static synchronized void onGesture(InputGestureEvent event) {
        LAYER_GESTURE.accept(event.gesture()).ifPresent(layer -> {
            close(false);
            open(
                    layer == PaletteLayerGestureState.Layer.PRIMARY ? Layer.PRIMARY : Layer.SECONDARY,
                    event.timestampMillis()
            );
        });
        if (event.gesture() == InputGesture.RELEASED || event.gesture() == InputGesture.CANCELLED) {
            close(true);
        }
    }

    private static void open(Layer layer, long nowMillis) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.gameMode == null || !minecraft.player.isCreative()) {
            return;
        }
        WheelConfigSnapshot config = layer == Layer.PRIMARY
                ? ClientConfigRuntime.configManager().primaryWheel()
                : ClientConfigRuntime.configManager().secondaryWheel();
        ItemStack held = minecraft.player.getInventory().getSelectedItem();
        if (held.isEmpty()) {
            return;
        }
        var match = PaletteLookup.find(
                config.typedGroups(), held,
                ClientPaletteItemCodec::itemMatches,
                (member, stack) -> ClientPaletteItemCodec.exactMatches(minecraft, member, stack)
        );
        if (match.isEmpty()) {
            return;
        }
        PaletteGroup group = match.get().group();
        List<PaletteEntry> entries = new ArrayList<>();
        int selectedIndex = 0;
        for (int index = 0; index < group.members().size(); index++) {
            PaletteMember member = group.members().get(index);
            var stack = ClientPaletteItemCodec.resolve(minecraft, member);
            if (stack.isPresent()) {
                int duplicate = -1;
                for (int entryIndex = 0; entryIndex < entries.size(); entryIndex++) {
                    if (ItemStack.isSameItemSameComponents(entries.get(entryIndex).stack, stack.get())) {
                        duplicate = entryIndex;
                        break;
                    }
                }
                if (duplicate >= 0) {
                    if (index == match.get().memberIndex()) {
                        selectedIndex = duplicate;
                    }
                    continue;
                }
                if (index == match.get().memberIndex()) {
                    selectedIndex = entries.size();
                }
                entries.add(new PaletteEntry(member, stack.get()));
            }
        }
        if (!PaletteWheelRules.canOpen(entries.size())) {
            return;
        }
        WheelLease claimed = ClientInteractionRuntime.wheel().claim(
                OWNER, WheelPriority.PALETTE_WHEEL, ClientPaletteRuntime::onScroll, ClientPaletteRuntime::revoked
        ).orElse(null);
        if (claimed == null) {
            return;
        }
        lease = claimed;
        radial = new RadialMenuSnapshot<>(entries, selectedIndex);
        ClientInteractionRuntime.hud().showRadial(
                OWNER, hudSnapshot(radial), animationMode(), nowMillis
        );
        showSelectedName();
    }

    private static synchronized boolean onScroll(double amountX, double amountY) {
        if (radial == null || lease == null || !lease.active()) {
            return false;
        }
        double amount = amountY != 0.0 ? amountY : amountX;
        int steps = SCROLL.add(amount);
        if (steps != 0) {
            int selectionSteps = -steps;
            radial = radial.rotate(selectionSteps);
            ClientInteractionRuntime.hud().rotateRadial(
                    OWNER, hudSnapshot(radial), selectionSteps, ClientInteractionRuntime.nowMillis()
            );
            radial.selected().ifPresent(ClientPaletteRuntime::replaceSelectedSlot);
            showSelectedName();
        }
        return true;
    }

    private static void showSelectedName() {
        if (radial == null) {
            return;
        }
        radial.selected().ifPresent(selected ->
                ClientInteractionRuntime.hud().showNumeric(OWNER, selected.stack.getHoverName())
        );
    }

    private static void replaceSelectedSlot(PaletteEntry entry) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.gameMode == null || !minecraft.player.isCreative()) {
            return;
        }
        ItemStack replacement = entry.stack.copy();
        int slot = minecraft.player.getInventory().getSelectedSlot();
        minecraft.player.getInventory().setSelectedItem(replacement);
        minecraft.gameMode.handleCreativeModeItemAdd(replacement, 36 + slot);
    }

    private static synchronized void close(boolean animate) {
        WheelLease previous = lease;
        lease = null;
        if (previous != null) {
            previous.close();
        }
        radial = null;
        SCROLL.reset();
        ClientInteractionRuntime.hud().hideNumeric(OWNER);
        if (animate) {
            ClientInteractionRuntime.hud().closeRadial(OWNER, ClientInteractionRuntime.nowMillis());
        } else {
            ClientInteractionRuntime.hud().hideRadial(OWNER);
        }
    }

    private static synchronized void revoked() {
        lease = null;
        radial = null;
        SCROLL.reset();
        ClientInteractionRuntime.hud().hideRadial(OWNER);
        ClientInteractionRuntime.hud().hideNumeric(OWNER);
    }

    private static RadialMenuSnapshot<RadialHudEntry> hudSnapshot(RadialMenuSnapshot<PaletteEntry> source) {
        List<RadialHudEntry> entries = source.entries().stream()
                .map(entry -> new RadialHudEntry(entry.stack.getHoverName(), entry.stack))
                .toList();
        return new RadialMenuSnapshot<>(entries, source.selectedIndex());
    }

    private static RadialAnimationMode animationMode() {
        return switch (ClientConfigRuntime.configManager().client().paletteAnimation()) {
            case CLOCKWISE -> RadialAnimationMode.CLOCKWISE;
            case EXPAND -> RadialAnimationMode.EXPAND;
            case OFF -> RadialAnimationMode.OFF;
        };
    }

    private enum Layer { PRIMARY, SECONDARY }

    private record PaletteEntry(PaletteMember member, ItemStack stack) {
        private PaletteEntry {
            stack = stack.copy();
        }
    }
}
