package com.davidblackcn.lorianarchorbit.fabric.client.compat.radial;

import com.davidblackcn.lorianarchorbit.client.CreativeInventoryHelper;
import com.mojang.brigadier.StringReader;
import dev.velolib.radial.api.RadialSlot;
import dev.velolib.radial.api.SlotActionContext;
import dev.velolib.radial.mode.base.IconEnabledSlotMode;
import dev.velolib.radial.ui.screen.SlotEditorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import dev.velolib.radial.ui.screen.iconpicker.IconPickerScreen;
import dev.velolib.radial.util.EncoderUtils;
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
        Minecraft minecraft = Minecraft.getInstance();
        container.addChild(new StringWidget(Component.translatable("radial.mode.lorian_arch_orbit.target"), minecraft.font));
        EditBox target = new EditBox(minecraft.font, width, 20,
                Component.translatable("radial.mode.lorian_arch_orbit.target"));
        target.setMaxLength(32767);
        String initialTarget = targetItem(slot);
        target.setValue(initialTarget == null ? "" : initialTarget);
        // Freeze the legacy target when editing so changing only the icon cannot change the action.
        // Radial's editor owns save/cancel and restores the original value on cancel.
        slot.value = target.getValue();
        target.setResponder(value -> {
            slot.value = value;
            target.setTextColor(parseStack(minecraft, targetItem(slot)).isPresent() ? 0xFFE0E0E0 : 0xFFFF5555);
        });
        container.addChild(target);
        LinearLayout actions = LinearLayout.horizontal().spacing(4);
        actions.addChild(Button.builder(Component.translatable("radial.mode.lorian_arch_orbit.select_target"),
                button -> minecraft.setScreenAndShow(new IconPickerScreen(screen, target::setValue))).width((width - 4) / 2).build());
        actions.addChild(Button.builder(Component.translatable("radial.mode.lorian_arch_orbit.held_target"), button -> {
            if (minecraft.player != null && minecraft.level != null && !minecraft.player.getMainHandItem().isEmpty()) {
                target.setValue(EncoderUtils.toGiveCommandString(minecraft.player.getMainHandItem(), minecraft.level.registryAccess()));
            }
        }).width((width - 4) / 2).build());
        container.addChild(actions);
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
        Optional<ItemStack> stack = parseStack(minecraft, targetItem(slot));
        if (stack.isEmpty()) {
            showActionBar(minecraft, "message.lorian_arch_orbit.radial_pick_item.invalid_item");
            return;
        }
        CreativeInventoryHelper.replaceSelectedSlot(minecraft, stack.get());
    }

    private static String targetItem(RadialSlot slot) {
        // Old slots stored the action target in the icon field. Never fall back from an invalid explicit target.
        return slot.value == null || slot.value.isBlank() ? slot.itemId : slot.value;
    }

    private static Optional<ItemStack> parseStack(Minecraft minecraft, String itemId) {
        if (minecraft.level == null || itemId == null || itemId.isBlank()) {
            return Optional.empty();
        }
        try {
            StringReader reader = new StringReader(itemId.trim());
            ItemInput input = new ItemParser(minecraft.level.registryAccess()).parse(reader);
            if (reader.canRead()) return Optional.empty();
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
