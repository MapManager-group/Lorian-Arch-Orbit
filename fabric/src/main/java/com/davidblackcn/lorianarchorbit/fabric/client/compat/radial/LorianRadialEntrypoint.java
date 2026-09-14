package com.davidblackcn.lorianarchorbit.fabric.client.compat.radial;

import com.davidblackcn.lorianarchorbit.LorianArchOrbit;
import dev.velolib.radial.api.RadialApiEntrypoint;
import dev.velolib.radial.api.SlotModeRegistry;
import net.minecraft.resources.Identifier;

public final class LorianRadialEntrypoint implements RadialApiEntrypoint {
    @Override
    public void registerSlotModes() {
        SlotModeRegistry.register(
                Identifier.fromNamespaceAndPath(LorianArchOrbit.MOD_ID, "pick_item"),
                new PickItemSlotMode()
        );
    }
}
