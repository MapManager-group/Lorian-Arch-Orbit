package com.davidblackcn.lorianarchorbit.client.connected;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.server.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class ConnectedTextureModelFixerTest {
    @Test
    public void classifiesStandardVanillaConnectedBlocks() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        assertEquals(ConnectionFixKind.WALL,
                ConnectedTextureModelFixer.kind(Blocks.COBBLESTONE_WALL.defaultBlockState()));
        assertEquals(ConnectionFixKind.DOOR,
                ConnectedTextureModelFixer.kind(Blocks.OAK_DOOR.defaultBlockState()));
        assertEquals(ConnectionFixKind.PISTON,
                ConnectedTextureModelFixer.kind(Blocks.PISTON.defaultBlockState()
                        .setValue(PistonBaseBlock.EXTENDED, true)));
        assertEquals(ConnectionFixKind.PISTON,
                ConnectedTextureModelFixer.kind(Blocks.PISTON_HEAD.defaultBlockState()
                        .setValue(PistonHeadBlock.SHORT, false)));
        assertEquals(ConnectionFixKind.NETHER_PORTAL,
                ConnectedTextureModelFixer.kind(Blocks.NETHER_PORTAL.defaultBlockState()));
        assertNull(ConnectedTextureModelFixer.kind(Blocks.PISTON_HEAD.defaultBlockState()
                .setValue(PistonHeadBlock.SHORT, true)));
        assertNull(ConnectedTextureModelFixer.kind(Blocks.STONE.defaultBlockState()));
    }

    @Test
    public void doorConnectionCopiesTheOppositeHorizontalEndFace() {
        assertEquals(Direction.DOWN, ConnectedTextureModelFixer.doorSourceDirection(Direction.UP));
        assertEquals(Direction.UP, ConnectedTextureModelFixer.doorSourceDirection(Direction.DOWN));
        assertThrows(IllegalArgumentException.class,
                () -> ConnectedTextureModelFixer.doorSourceDirection(Direction.NORTH));
    }

    @Test
    public void pistonConnectionUsesExactBaseAndHeadContactFacesForEveryDirection() {
        for (Direction facing : Direction.values()) {
            var base = ConnectedTextureModelFixer.pistonBaseConnectionBounds(facing);
            var head = ConnectedTextureModelFixer.pistonHeadConnectionBounds(facing);
            assertEquals(facing, base.direction());
            assertEquals(facing.getOpposite(), head.direction());
            assertTrue(switch (facing.getAxis()) {
                case X -> base.minX() == base.maxX() && head.minX() == head.maxX();
                case Y -> base.minY() == base.maxY() && head.minY() == head.maxY();
                case Z -> base.minZ() == base.maxZ() && head.minZ() == head.maxZ();
            });
            assertTrue(ConnectedTextureModelFixer.pistonSideDirection(facing).getAxis() != facing.getAxis());
        }
    }

    @Test
    public void pistonHeadUsesTheMiddleQuarterOfTheOppositeFaceUv() {
        assertEquals(6.0F, UVPair.unpackU(ConnectedTextureModelFixer.centerCropUv(
                UVPair.pack(0, 0), 8, 8
        )));
        assertEquals(6.0F, UVPair.unpackV(ConnectedTextureModelFixer.centerCropUv(
                UVPair.pack(0, 0), 8, 8
        )));
        assertEquals(10.0F, UVPair.unpackU(ConnectedTextureModelFixer.centerCropUv(
                UVPair.pack(16, 16), 8, 8
        )));
        assertEquals(10.0F, UVPair.unpackV(ConnectedTextureModelFixer.centerCropUv(
                UVPair.pack(16, 16), 8, 8
        )));
    }

    @Test
    public void netherPortalEdgesUseExactFourTexelCropsForBothAxes() {
        var xFaces = ConnectedTextureModelFixer.netherPortalConnectionFaces(Direction.Axis.X);
        var zFaces = ConnectedTextureModelFixer.netherPortalConnectionFaces(Direction.Axis.Z);

        assertEquals(4, xFaces.size());
        assertEquals(4, zFaces.size());
        assertTrue(xFaces.stream().allMatch(face -> face.uScale() == 1 || face.uScale() == 0.25F));
        assertTrue(xFaces.stream().allMatch(face -> face.vScale() == 1 || face.vScale() == 0.25F));
        assertTrue(zFaces.stream().allMatch(face -> face.uScale() == 0.25F));
        assertTrue(zFaces.stream().allMatch(face -> face.vScale() == 1));
        assertEquals(6.0F, UVPair.unpackU(ConnectedTextureModelFixer.centerCropUv(
                UVPair.pack(0, 0), 8, 8, 0.25F, 1
        )));
        assertEquals(0.0F, UVPair.unpackV(ConnectedTextureModelFixer.centerCropUv(
                UVPair.pack(0, 0), 8, 8, 0.25F, 1
        )));
    }
}
