package com.davidblackcn.lorianarchorbit.client;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public record RadialHudEntry(Component label, ItemStack icon) {
    public RadialHudEntry {
        Objects.requireNonNull(label, "label");
        icon = Objects.requireNonNull(icon, "icon").copy();
    }

    public static RadialHudEntry text(Component label) {
        return new RadialHudEntry(label, ItemStack.EMPTY);
    }

    @Override
    public ItemStack icon() {
        return icon.copy();
    }
}
