package com.davidblackcn.lorianarchorbit.fabric.client.compat.radial;

import com.davidblackcn.lorianarchorbit.client.CreativeInventoryHelper;
import com.mojang.brigadier.StringReader;
import dev.velolib.radial.api.RadialSlot;
import dev.velolib.radial.api.SlotActionContext;
import dev.velolib.radial.mode.base.IconEnabledSlotMode;
import dev.velolib.radial.ui.screen.SlotEditorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public final class PickItemSlotMode extends IconEnabledSlotMode {
    @Override
    public Component getTranslatedName() {
        return Component.translatable("radial.mode.lorian_arch_orbit.pick_item");
    }

    @Override
    public void buildEditorWidgets(SlotEditorScreen screen, RadialSlot slot, int width, LinearLayout container) {
        buildIconRow(screen, slot, width, container);
    }

    @Override
    public void performAction(RadialSlot slot, SlotActionContext context) {
        context.closeScreen();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.gameMode == null || !minecraft.player.isCreative()) {
            showActionBar(minecraft, "message.lorian_arch_orbit.radial_pick_item.not_creative");
            return;
        }
        Optional<ItemStack> stack = parseStack(minecraft, slot.itemId);
        if (stack.isEmpty()) {
            showActionBar(minecraft, "message.lorian_arch_orbit.radial_pick_item.invalid_item");
            return;
        }
        CreativeInventoryHelper.replaceSelectedSlot(minecraft, stack.get());
    }

    private static Optional<ItemStack> parseStack(Minecraft minecraft, String itemId) {
        if (minecraft.level == null || itemId == null || itemId.isBlank()) {
            return Optional.empty();
        }
        try {
            ItemInput input = new ItemParser(minecraft.level.registryAccess()).parse(new StringReader(itemId));
            ItemStack stack = new ItemStack(input.item(), 1);
            stack.applyComponentsAndValidate(input.components());
            return stack.isEmpty() ? Optional.empty() : Optional.of(stack);
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    private static void showActionBar(Minecraft minecraft, String translationKey) {
        minecraft.gui.hud.setOverlayMessage(Component.translatable(translationKey), false);
    }
}
