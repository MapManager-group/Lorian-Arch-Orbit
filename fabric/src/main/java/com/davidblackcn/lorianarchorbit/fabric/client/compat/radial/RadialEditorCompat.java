package com.davidblackcn.lorianarchorbit.fabric.client.compat.radial;

import dev.velolib.radial.api.RadialSlot;
import dev.velolib.radial.mode.base.IconEnabledSlotMode;
import dev.velolib.radial.ui.screen.SlotEditorScreen;
import dev.velolib.radial.ui.screen.iconpicker.IconPickerScreen;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.function.Consumer;

/** The two binary-incompatible editor APIs shipped for Minecraft 26.2. */
final class RadialEditorCompat {
    private static final Method ICON_ROW;
    private static final Constructor<IconPickerScreen> PICKER;
    private static final boolean HAS_INITIAL_ICON;

    static {
        try {
            Method iconRow;
            try {
                iconRow = IconEnabledSlotMode.class.getDeclaredMethod("buildIconRow",
                        Screen.class, RadialSlot.class, int.class, LinearLayout.class);
            } catch (NoSuchMethodException oldApi) {
                iconRow = IconEnabledSlotMode.class.getDeclaredMethod("buildIconRow",
                        SlotEditorScreen.class, RadialSlot.class, int.class, LinearLayout.class);
            }
            iconRow.setAccessible(true);
            ICON_ROW = iconRow;
            Constructor<IconPickerScreen> picker;
            boolean hasInitialIcon;
            try {
                picker = IconPickerScreen.class.getConstructor(Screen.class, String.class, Consumer.class);
                hasInitialIcon = true;
            } catch (NoSuchMethodException oldApi) {
                picker = IconPickerScreen.class.getConstructor(Screen.class, Consumer.class);
                hasInitialIcon = false;
            }
            PICKER = picker;
            HAS_INITIAL_ICON = hasInitialIcon;
        } catch (ReflectiveOperationException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private RadialEditorCompat() { }

    static void buildIconRow(IconEnabledSlotMode mode, Screen screen, RadialSlot slot,
                             int width, LinearLayout container) {
        try {
            ICON_ROW.invoke(mode, screen, slot, width, container);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot build Radial icon editor", exception);
        }
    }

    static Screen createPicker(Screen parent, String initialIcon, Consumer<String> selection) {
        try {
            return HAS_INITIAL_ICON ? PICKER.newInstance(parent, initialIcon, selection)
                    : PICKER.newInstance(parent, selection);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot open Radial item picker", exception);
        }
    }
}
