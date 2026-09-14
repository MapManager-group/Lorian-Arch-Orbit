package com.davidblackcn.lorianarchorbit.client.connected;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class ChestConnectionFaceFixTest {
    private static final BlockPos ORIGIN = BlockPos.ZERO;

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    public void singleChestNeverNeedsConnectionFace() {
        assertFalse(shouldRender(
                chest(Blocks.CHEST, ChestType.SINGLE, Direction.NORTH),
                chest(Blocks.CHEST, ChestType.SINGLE, Direction.NORTH),
                ORIGIN,
                true
        ));
    }

    @Test
    public void validLeftAndRightPartnersDoNotNeedConnectionFaces() {
        BlockState left = chest(Blocks.CHEST, ChestType.LEFT, Direction.NORTH);
        BlockState right = chest(Blocks.CHEST, ChestType.RIGHT, Direction.NORTH);
        BlockPos rightPos = ChestBlock.getConnectedBlockPos(ORIGIN, left);

        assertFalse(shouldRender(left, right, rightPos, true));
        assertFalse(ChestConnectionFaceFix.shouldRenderConnectionFace(
                right, rightPos, left, ORIGIN, true
        ));
    }

    @Test
    public void missingPartnerNeedsConnectionFaceForBothHalves() {
        assertTrue(shouldRender(
                chest(Blocks.CHEST, ChestType.LEFT, Direction.NORTH),
                Blocks.AIR.defaultBlockState(),
                ORIGIN.relative(Direction.EAST),
                true
        ));
        BlockState right = chest(Blocks.CHEST, ChestType.RIGHT, Direction.NORTH);
        assertTrue(shouldRender(
                right,
                Blocks.AIR.defaultBlockState(),
                ChestBlock.getConnectedBlockPos(ORIGIN, right),
                true
        ));
    }

    @Test
    public void singleOrSameSidePartnerNeedsConnectionFace() {
        BlockState left = chest(Blocks.CHEST, ChestType.LEFT, Direction.NORTH);
        BlockPos partnerPos = ChestBlock.getConnectedBlockPos(ORIGIN, left);

        assertTrue(shouldRender(
                left, chest(Blocks.CHEST, ChestType.SINGLE, Direction.NORTH), partnerPos, true
        ));
        assertTrue(shouldRender(
                left, chest(Blocks.CHEST, ChestType.LEFT, Direction.NORTH), partnerPos, true
        ));

        BlockState right = chest(Blocks.CHEST, ChestType.RIGHT, Direction.NORTH);
        assertTrue(shouldRender(
                right,
                chest(Blocks.CHEST, ChestType.RIGHT, Direction.NORTH),
                ChestBlock.getConnectedBlockPos(ORIGIN, right),
                true
        ));
    }

    @Test
    public void wrongFacingOrOneWayPartnerNeedsConnectionFace() {
        BlockState left = chest(Blocks.CHEST, ChestType.LEFT, Direction.NORTH);
        BlockPos partnerPos = ChestBlock.getConnectedBlockPos(ORIGIN, left);

        assertTrue(shouldRender(
                left, chest(Blocks.CHEST, ChestType.RIGHT, Direction.EAST), partnerPos, true
        ));

        BlockState pointsAway = chest(Blocks.CHEST, ChestType.RIGHT, Direction.SOUTH);
        assertFalse(ChestBlock.getConnectedBlockPos(partnerPos, pointsAway).equals(ORIGIN));
        assertTrue(shouldRender(left, pointsAway, partnerPos, true));
    }

    @Test
    public void incompatibleChestBlockNeedsConnectionFace() {
        BlockState left = chest(Blocks.CHEST, ChestType.LEFT, Direction.NORTH);
        BlockPos partnerPos = ChestBlock.getConnectedBlockPos(ORIGIN, left);
        assertTrue(shouldRender(
                left,
                chest(Blocks.TRAPPED_CHEST, ChestType.RIGHT, Direction.NORTH),
                partnerPos,
                true
        ));
    }

    @Test
    public void unavailablePartnerChunkDoesNotNeedConnectionFace() {
        BlockState left = chest(Blocks.CHEST, ChestType.LEFT, Direction.NORTH);
        assertFalse(shouldRender(
                left,
                Blocks.AIR.defaultBlockState(),
                ChestBlock.getConnectedBlockPos(ORIGIN, left),
                false
        ));
    }

    private static boolean shouldRender(
            BlockState state,
            BlockState partner,
            BlockPos partnerPos,
            boolean partnerChunkAvailable
    ) {
        return ChestConnectionFaceFix.shouldRenderConnectionFace(
                state, ORIGIN, partner, partnerPos, partnerChunkAvailable
        );
    }

    private static BlockState chest(Block block, ChestType type, Direction facing) {
        return block.defaultBlockState()
                .setValue(ChestBlock.TYPE, type)
                .setValue(ChestBlock.FACING, facing);
    }
}
