package com.davidblackcn.lorianarchorbit.client.connected;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

public final class ChestConnectionFaceFix {
    private ChestConnectionFaceFix() {
    }

    public static boolean shouldRenderConnectionFace(Level level, BlockPos pos, BlockState state) {
        if (level == null || !isConnectedChest(state)) return false;

        BlockPos partnerPos = ChestBlock.getConnectedBlockPos(pos, state);
        if (!level.hasChunkAt(partnerPos)) return false;

        return shouldRenderConnectionFace(
                state,
                pos,
                level.getBlockState(partnerPos),
                partnerPos,
                true
        );
    }

    static boolean shouldRenderConnectionFace(
            BlockState state,
            BlockPos pos,
            BlockState partner,
            BlockPos partnerPos,
            boolean partnerChunkAvailable
    ) {
        if (!isConnectedChest(state) || !partnerChunkAvailable) return false;
        return !isValidPartner(state, pos, partner, partnerPos);
    }

    static boolean isValidPartner(
            BlockState state,
            BlockPos pos,
            BlockState partner,
            BlockPos partnerPos
    ) {
        if (!(state.getBlock() instanceof ChestBlock chest)
                || !(partner.getBlock() instanceof ChestBlock partnerChest)) {
            return false;
        }

        ChestType type = state.getValue(ChestBlock.TYPE);
        ChestType partnerType = partner.getValue(ChestBlock.TYPE);
        return type != ChestType.SINGLE
                && partnerType == type.getOpposite()
                && chest.chestCanConnectTo(partner)
                && partnerChest.chestCanConnectTo(state)
                && state.getValue(ChestBlock.FACING) == partner.getValue(ChestBlock.FACING)
                && ChestBlock.getConnectedBlockPos(pos, state).equals(partnerPos)
                && ChestBlock.getConnectedBlockPos(partnerPos, partner).equals(pos);
    }

    private static boolean isConnectedChest(BlockState state) {
        return state.getBlock() instanceof ChestBlock
                && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE;
    }
}
