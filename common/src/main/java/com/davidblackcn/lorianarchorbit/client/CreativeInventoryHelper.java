package com.davidblackcn.lorianarchorbit.client;

import com.davidblackcn.lorianarchorbit.palette.TemporaryPaletteSession;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import java.util.List;

public final class CreativeInventoryHelper {
    private static final TemporaryPaletteSession<ItemStack> SESSION = new TemporaryPaletteSession<>(ItemStack::copy);
    private CreativeInventoryHelper() {
    }

    public static boolean replaceSelectedSlot(Minecraft minecraft, ItemStack stack) {
        if (minecraft.player == null
                || minecraft.gameMode == null
                || !minecraft.player.isCreative()
                || stack.isEmpty()) {
            return false;
        }
        int slot = minecraft.player.getInventory().getSelectedSlot();
        writeSlot(minecraft, slot, stack);
        return true;
    }

    public static List<String> temporaryPalette() { return SESSION.palette(); }
    public static void setTemporaryPalette(List<String> items) { SESSION.setPalette(items); }
    public static void syncPlayer(Minecraft minecraft) { SESSION.bindInventory(minecraft.player); }
    public static void clearSession() { SESSION.reset(); }

    public static boolean canApply(Minecraft minecraft) {
        return minecraft.player != null && minecraft.gameMode != null && minecraft.player.isCreative();
    }

    public static boolean applyGradient(Minecraft minecraft, List<ItemStack> stacks) {
        if (!canApply(minecraft) || stacks.isEmpty() || stacks.size() > TemporaryPaletteSession.INVENTORY_SIZE
                || stacks.stream().anyMatch(ItemStack::isEmpty)) return false;
        return SESSION.applyInventory(minecraft.player, stacks, minecraft.player.getInventory()::getItem,
                (slot, stack) -> writeSlot(minecraft, slot, stack));
    }

    public static boolean replaceSelectedSlotWithBackup(Minecraft minecraft, ItemStack stack) {
        if (!canApply(minecraft) || stack.isEmpty()) return false;
        int slot = minecraft.player.getInventory().getSelectedSlot();
        SESSION.remember(minecraft.player, slot, minecraft.player.getInventory().getItem(slot));
        return replaceSelectedSlot(minecraft, stack);
    }

    public static boolean canRestore(Minecraft minecraft) {
        return canApply(minecraft) && SESSION.hasBackup(minecraft.player);
    }

    public static boolean restore(Minecraft minecraft) {
        if (!canRestore(minecraft)) return false;
        return SESSION.restoreInventory(minecraft.player, (slot, stack) -> writeSlot(minecraft, slot, stack));
    }

    private static void writeSlot(Minecraft minecraft, int slot, ItemStack stack) {
        ItemStack replacement = stack.copy();
        minecraft.player.getInventory().setItem(slot, replacement);
        minecraft.gameMode.handleCreativeModeItemAdd(replacement, TemporaryPaletteSession.menuSlot(slot));
    }
}
