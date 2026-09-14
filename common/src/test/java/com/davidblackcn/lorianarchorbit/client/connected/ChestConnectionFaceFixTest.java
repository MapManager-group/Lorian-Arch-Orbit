package com.davidblackcn.lorianarchorbit.client.connected;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.TrappedChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

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
        assertFalse(ChestConnectionFaceFix.shouldRenderConnectionFace(
                chest(Blocks.CHEST, ChestType.SINGLE, Direction.NORTH),
                true,
                unexpectedCombineResult()
        ));
    }

    @Test
    public void validLeftAndRightPartnersCombineAsDouble() {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            BlockState left = chest(Blocks.CHEST, ChestType.LEFT, facing);
            BlockState right = chest(Blocks.CHEST, ChestType.RIGHT, facing);

            assertFalse(shouldRender(left, right, true, true));
            assertFalse(shouldRender(right, left, true, true));
        }
    }

    @Test
    public void missingPartnerNeedsConnectionFaceForBothHalves() {
        assertTrue(shouldRender(
                chest(Blocks.CHEST, ChestType.LEFT, Direction.NORTH),
                Blocks.AIR.defaultBlockState(),
                false,
                true
        ));
        assertTrue(shouldRender(
                chest(Blocks.CHEST, ChestType.RIGHT, Direction.NORTH),
                Blocks.AIR.defaultBlockState(),
                false,
                true
        ));
    }

    @Test
    public void matchingPartnerStateWithoutBlockEntityNeedsConnectionFace() {
        assertTrue(shouldRender(
                chest(Blocks.CHEST, ChestType.LEFT, Direction.NORTH),
                chest(Blocks.CHEST, ChestType.RIGHT, Direction.NORTH),
                false,
                true
        ));
    }

    @Test
    public void noneCombineResultNeedsConnectionFace() {
        assertTrue(ChestConnectionFaceFix.shouldRenderConnectionFace(
                chest(Blocks.CHEST, ChestType.LEFT, Direction.NORTH),
                true,
                noneCombineResult()
        ));
    }

    @Test
    public void singleOrSameSidePartnerNeedsConnectionFace() {
        BlockState left = chest(Blocks.CHEST, ChestType.LEFT, Direction.NORTH);
        assertTrue(shouldRender(
                left, chest(Blocks.CHEST, ChestType.SINGLE, Direction.NORTH), true, true
        ));
        assertTrue(shouldRender(
                left, chest(Blocks.CHEST, ChestType.LEFT, Direction.NORTH), true, true
        ));

        BlockState right = chest(Blocks.CHEST, ChestType.RIGHT, Direction.NORTH);
        assertTrue(shouldRender(
                right, chest(Blocks.CHEST, ChestType.RIGHT, Direction.NORTH), true, true
        ));
    }

    @Test
    public void wrongFacingNeedsConnectionFace() {
        assertTrue(shouldRender(
                chest(Blocks.CHEST, ChestType.LEFT, Direction.NORTH),
                chest(Blocks.CHEST, ChestType.RIGHT, Direction.EAST),
                true,
                true
        ));
    }

    @Test
    public void incompatibleChestBlockNeedsConnectionFace() {
        assertTrue(shouldRender(
                chest(Blocks.CHEST, ChestType.LEFT, Direction.NORTH),
                chest(Blocks.TRAPPED_CHEST, ChestType.RIGHT, Direction.NORTH),
                true,
                true
        ));
    }

    @Test
    public void unavailablePartnerChunkDoesNotNeedConnectionFace() {
        assertFalse(ChestConnectionFaceFix.shouldRenderConnectionFace(
                chest(Blocks.CHEST, ChestType.LEFT, Direction.NORTH),
                false,
                unexpectedCombineResult()
        ));
    }

    private static boolean shouldRender(
            BlockState state,
            BlockState partner,
            boolean partnerEntityPresent,
            boolean partnerChunkAvailable
    ) {
        DoubleBlockCombiner.NeighborCombineResult<? extends ChestBlockEntity> combined =
                combineWithVanillaRules(state, partner, partnerEntityPresent);
        return ChestConnectionFaceFix.shouldRenderConnectionFace(
                state, partnerChunkAvailable, combined
        );
    }

    private static DoubleBlockCombiner.NeighborCombineResult<? extends ChestBlockEntity>
            combineWithVanillaRules(
                    BlockState state,
                    BlockState partner,
                    boolean partnerEntityPresent
            ) {
        BlockPos partnerPos = ChestBlock.getConnectedBlockPos(ORIGIN, state);
        Map<BlockPos, BlockState> states = Map.of(ORIGIN, state, partnerPos, partner);
        Map<BlockPos, BlockEntity> entities = new HashMap<>();
        entities.put(ORIGIN, blockEntity(ORIGIN, state));
        if (partnerEntityPresent && partner.getBlock() instanceof ChestBlock) {
            entities.put(partnerPos, blockEntity(partnerPos, partner));
        }

        ChestBlock chest = (ChestBlock) state.getBlock();
        return DoubleBlockCombiner.combineWithNeigbour(
                chest.blockEntityType(),
                ChestBlock::getBlockType,
                ChestBlock::getConnectedDirection,
                ChestBlock.FACING,
                state,
                level(states, entities),
                ORIGIN,
                (level, pos) -> false
        );
    }

    private static LevelAccessor level(
            Map<BlockPos, BlockState> states,
            Map<BlockPos, BlockEntity> entities
    ) {
        return (LevelAccessor) Proxy.newProxyInstance(
                ChestConnectionFaceFixTest.class.getClassLoader(),
                new Class<?>[]{LevelAccessor.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "getBlockState" -> states.getOrDefault(
                            (BlockPos) arguments[0], Blocks.AIR.defaultBlockState()
                    );
                    case "getBlockEntity" -> entities.get((BlockPos) arguments[0]);
                    case "toString" -> "ChestConnectionFaceFixTestLevel";
                    default -> throw new UnsupportedOperationException(method.toString());
                }
        );
    }

    private static ChestBlockEntity blockEntity(BlockPos pos, BlockState state) {
        if (state.is(Blocks.TRAPPED_CHEST)) {
            return new TrappedChestBlockEntity(pos, state);
        }
        return new ChestBlockEntity(pos, state);
    }

    private static DoubleBlockCombiner.NeighborCombineResult<ChestBlockEntity>
            unexpectedCombineResult() {
        return new DoubleBlockCombiner.NeighborCombineResult<>() {
            @Override
            public <T> T apply(
                    DoubleBlockCombiner.Combiner<? super ChestBlockEntity, T> combiner
            ) {
                throw new AssertionError("Combine result should not be inspected");
            }
        };
    }

    private static DoubleBlockCombiner.NeighborCombineResult<ChestBlockEntity>
            noneCombineResult() {
        return new DoubleBlockCombiner.NeighborCombineResult<>() {
            @Override
            public <T> T apply(
                    DoubleBlockCombiner.Combiner<? super ChestBlockEntity, T> combiner
            ) {
                return combiner.acceptNone();
            }
        };
    }

    private static BlockState chest(Block block, ChestType type, Direction facing) {
        return block.defaultBlockState()
                .setValue(ChestBlock.TYPE, type)
                .setValue(ChestBlock.FACING, facing);
    }
}
