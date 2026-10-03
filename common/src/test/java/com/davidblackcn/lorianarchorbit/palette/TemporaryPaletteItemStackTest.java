package com.davidblackcn.lorianarchorbit.palette;

import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Uses only the game registry and stack value objects; does not construct or start a client. */
class TemporaryPaletteItemStackTest {
    @Test
    void nativeStackBackupPreservesCountNameDamageAndEmptySlotsAcrossRepeatedWrites() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        var session = new TemporaryPaletteSession<ItemStack>(ItemStack::copy);
        Object player = new Object();
        ItemStack tool = stack(Items.DIAMOND_PICKAXE, 1, DataComponentMap.builder()
                .set(DataComponents.MAX_DAMAGE, 1561).set(DataComponents.DAMAGE, 0).build());
        tool.set(DataComponents.CUSTOM_NAME, Component.literal("Original builder's tool"));
        tool.setDamageValue(17);
        ItemStack expected = tool.copy();
        session.remember(player, 0, tool);
        session.remember(player, 1, stack(Items.DIAMOND, 32, DataComponentMap.EMPTY));
        session.remember(player, 9, ItemStack.EMPTY);
        tool.setDamageValue(50);
        tool.set(DataComponents.CUSTOM_NAME, Component.literal("changed"));
        session.remember(player, 0, stack(Items.STONE, 1, DataComponentMap.EMPTY));
        var originals = session.originals(player);
        assertTrue(ItemStack.isSameItemSameComponents(expected, originals.get(0)));
        assertEquals(17, originals.get(0).getDamageValue());
        assertEquals(32, originals.get(1).getCount());
        assertTrue(originals.get(9).isEmpty());
        originals.get(0).setDamageValue(90);
        assertEquals(17, session.originals(player).get(0).getDamageValue());
    }

    private static ItemStack stack(Item item, int count, DataComponentMap defaults) {
        // 26.2 binds component prototypes during resource loading. Supply isolated prototypes for value-only tests.
        Holder.Reference<Item> holder = Holder.Reference.createIntrusive(BuiltInRegistries.ITEM, item);
        holder.bindComponents(defaults);
        return new ItemStack(holder, count);
    }
}
