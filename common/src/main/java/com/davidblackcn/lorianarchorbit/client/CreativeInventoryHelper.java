package com.davidblackcn.lorianarchorbit.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

public final class CreativeInventoryHelper {
    private CreativeInventoryHelper() {
    }

    public static boolean replaceSelectedSlot(Minecraft minecraft, ItemStack stack) {
        if (minecraft.player == null
                || minecraft.gameMode == null
                || !minecraft.player.isCreative()
                || stack.isEmpty()) {
            return false;
        }
        ItemStack replacement = stack.copy();
        int slot = minecraft.player.getInventory().getSelectedSlot();
        minecraft.player.getInventory().setSelectedItem(replacement);
        minecraft.gameMode.handleCreativeModeItemAdd(replacement, 36 + slot);
        return true;
    }
}
