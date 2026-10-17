package com.davidblackcn.lorianarchorbit.palette;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import com.mojang.serialization.Codec;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaletteWheelRulesTest {
    @Test
    void emptyAndSingleMemberGroupsNeverOpen() {
        assertFalse(PaletteWheelRules.canOpen(0));
        assertFalse(PaletteWheelRules.canOpen(1));
        assertTrue(PaletteWheelRules.canOpen(2));
    }

    @Test
    void arbitraryMemberCountsRemainIntact() {
        PaletteMember member = new PaletteMember("minecraft:stone");
        PaletteGroup large = new PaletteGroup(
                "large", "Large", "minecraft:stone", Collections.nCopies(96, member)
        );

        assertEquals(96, large.members().size());
    }

    @Test
    void nativeComponentIdentityDistinguishesNameAndDamage() {
        DataComponentType<String> customName = DataComponentType.<String>builder().persistent(Codec.STRING).build();
        DataComponentType<Integer> damage = DataComponentType.<Integer>builder().persistent(Codec.INT).build();
        DataComponentPatch base = DataComponentPatch.EMPTY;
        DataComponentPatch named = DataComponentPatch.builder()
                .set(customName, "Builder")
                .build();
        DataComponentPatch damaged = DataComponentPatch.builder()
                .set(damage, 7)
                .build();

        assertFalse(base.equals(named));
        assertFalse(base.equals(damaged));
        assertTrue(named.equals(DataComponentPatch.builder()
                .set(customName, "Builder")
                .build()));
    }
}
