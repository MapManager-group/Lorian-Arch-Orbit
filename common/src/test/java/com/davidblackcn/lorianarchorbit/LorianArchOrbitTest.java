package com.davidblackcn.lorianarchorbit;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class LorianArchOrbitTest {
    @Test
    public void exposesStableModIdentity() {
        assertEquals("lorian_arch_orbit", LorianArchOrbit.MOD_ID);
        assertEquals("Lorian’s Arch Orbit", LorianArchOrbit.MOD_NAME);
    }

    @Test
    public void registersTheFiveFrozenBuiltinFeatures() {
        assertTrue(LorianArchOrbit.features().isFrozen());
        assertEquals(Set.of(
                "reach_extension",
                "palette_wheel",
                "smart_pick",
                "connected_texture_fix",
                "invisible_blocks"
        ), LorianArchOrbit.features().ids());
        assertFalse(LorianArchOrbit.features().get("reach_extension").enabledByDefault());
        assertTrue(LorianArchOrbit.features().get("palette_wheel").enabledByDefault());
    }
}
