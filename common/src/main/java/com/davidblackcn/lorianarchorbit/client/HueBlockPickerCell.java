package com.davidblackcn.lorianarchorbit.client;

import com.davidblackcn.lorianarchorbit.palette.hueblocks.HueGradient;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/** A real Button retains keyboard selection and narration while drawing only an item cell. */
final class HueBlockPickerCell extends Button {
    final HueGradient.Candidate candidate;

    HueBlockPickerCell(HueGradient.Candidate candidate, int x, int y, OnPress picked) {
        super(x, y, HueBlockPickerLayout.CELL, HueBlockPickerLayout.CELL,
                label(candidate), picked, DEFAULT_NARRATION);
        this.candidate = candidate;
        setTooltip(Tooltip.create(getMessage()));
    }

    private static Component label(HueGradient.Candidate candidate) {
        return HueGradientScreen.text("picker_item_hint", HueBlocksRuntime.stack(candidate).getHoverName(),
                candidate.itemId(), candidate.block().texture());
    }

    @Override protected void extractContents(GuiGraphicsExtractor graphics, int mx, int my, float tick) {
        int x = getX(), y = getY();
        if (isHoveredOrFocused()) graphics.fill(x, y, x + width, y + height, 0xAA4D6A7D);
        graphics.item(HueBlocksRuntime.stack(candidate), x + 2, y + 1);
        graphics.fill(x + 3, y + 18, x + width - 3, y + 19, 0xFF000000 | candidate.block().rgb());
        if (isFocused()) WorkbenchFlatButton.border(graphics, x, y, width, height, 0xFFD0D0D0);
    }
}
