package com.davidblackcn.lorianarchorbit.client.connected;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.server.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class ConnectedTextureModelFixerTest {
    @Test
    public void classifiesStandardVanillaConnectedBlocks() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        assertEquals(ConnectionFixKind.WALL,
                ConnectedTextureModelFixer.kind(Blocks.COBBLESTONE_WALL.defaultBlockState()));
        assertEquals(ConnectionFixKind.DOOR,
                ConnectedTextureModelFixer.kind(Blocks.OAK_DOOR.defaultBlockState()));
        assertNull(ConnectedTextureModelFixer.kind(Blocks.STONE.defaultBlockState()));
    }

    @Test
    public void doorConnectionCopiesTheOppositeHorizontalEndFace() {
        assertEquals(Direction.DOWN, ConnectedTextureModelFixer.doorSourceDirection(Direction.UP));
        assertEquals(Direction.UP, ConnectedTextureModelFixer.doorSourceDirection(Direction.DOWN));
        assertThrows(IllegalArgumentException.class,
                () -> ConnectedTextureModelFixer.doorSourceDirection(Direction.NORTH));
    }
}
