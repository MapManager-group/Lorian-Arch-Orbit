package com.davidblackcn.lorianarchorbit.client.connected;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

public final class ChestConnectionFaceFix {
    private static final DoubleBlockCombiner.Combiner<ChestBlockEntity, Boolean> IS_DOUBLE =
            new DoubleBlockCombiner.Combiner<>() {
                @Override
                public Boolean acceptDouble(ChestBlockEntity first, ChestBlockEntity second) {
                    return true;
                }

                @Override
                public Boolean acceptSingle(ChestBlockEntity single) {
                    return false;
                }

                @Override
                public Boolean acceptNone() {
                    return false;
                }
            };

    private ChestConnectionFaceFix() {
    }

    public static boolean shouldRenderConnectionFace(Level level, BlockPos pos, BlockState state) {
        if (level == null || !isConnectedChest(state)) return false;

        BlockPos partnerPos = ChestBlock.getConnectedBlockPos(pos, state);
        if (!level.hasChunkAt(partnerPos)) return false;

        ChestBlock chest = (ChestBlock) state.getBlock();
        return shouldRenderConnectionFace(state, true, chest.combine(state, level, pos, true));
    }

    static boolean shouldRenderConnectionFace(
            BlockState state,
            boolean partnerChunkAvailable,
            DoubleBlockCombiner.NeighborCombineResult<? extends ChestBlockEntity> combined
    ) {
        if (!isConnectedChest(state) || !partnerChunkAvailable) return false;
        return !combined.apply(IS_DOUBLE);
    }

    private static boolean isConnectedChest(BlockState state) {
        return state.getBlock() instanceof ChestBlock
                && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE;
    }
}
