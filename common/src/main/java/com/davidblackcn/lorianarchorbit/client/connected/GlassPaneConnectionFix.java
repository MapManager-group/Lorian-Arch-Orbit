package com.davidblackcn.lorianarchorbit.client.connected;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class GlassPaneConnectionFix {
    private GlassPaneConnectionFix() {
    }

    public static boolean shouldRenderSharedFace(
            BlockState state, BlockState adjacentState, Direction direction
    ) {
        // Vanilla drops the entire vertical face between equal pane blocks. Keep it only when
        // different horizontal outlines leave part of that face visibly exposed.
        return direction.getAxis() == Direction.Axis.Y
                && state.getBlock() == adjacentState.getBlock()
                && isGlassPane(state)
                && horizontalConnectionsDiffer(state, adjacentState);
    }

    static boolean isGlassPane(BlockState state) {
        return state.getBlock() == Blocks.GLASS_PANE
                || state.getBlock() instanceof StainedGlassPaneBlock;
    }

    static boolean horizontalConnectionsDiffer(BlockState first, BlockState second) {
        return first.getValue(CrossCollisionBlock.NORTH) != second.getValue(CrossCollisionBlock.NORTH)
                || first.getValue(CrossCollisionBlock.EAST) != second.getValue(CrossCollisionBlock.EAST)
                || first.getValue(CrossCollisionBlock.SOUTH) != second.getValue(CrossCollisionBlock.SOUTH)
                || first.getValue(CrossCollisionBlock.WEST) != second.getValue(CrossCollisionBlock.WEST);
    }
}
