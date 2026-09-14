package com.davidblackcn.lorianarchorbit.client.connected;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.object.chest.ChestModel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.ChestType;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ChestConnectionCapModel extends ChestModel {
    private static final int TEXTURE_WIDTH = 64;
    private static final int TEXTURE_HEIGHT = 64;
    private static final ChestConnectionCapModel LEFT = create(ChestType.LEFT);
    private static final ChestConnectionCapModel RIGHT = create(ChestType.RIGHT);

    private ChestConnectionCapModel(ModelPart root) {
        super(root);
    }

    public static ChestConnectionCapModel select(ChestType type) {
        return switch (type) {
            case LEFT -> LEFT;
            case RIGHT -> RIGHT;
            case SINGLE -> throw new IllegalArgumentException("A single chest has no connection face");
        };
    }

    static ChestConnectionCapModel create(ChestType type) {
        Direction connectionFace = connectionFace(type);
        float x = type == ChestType.RIGHT ? 1.0F : 0.0F;

        // Coordinates, texture offsets, and poses exactly match the Minecraft 26.2
        // double-left/right layers. Each cube retains only Vanilla's omitted face.
        ModelPart bottom = part(faceCube(
                0, 19, x, 0.0F, 1.0F, 15.0F, 10.0F, 14.0F, connectionFace
        ));
        PartPose lidPose = PartPose.offset(0.0F, 9.0F, 1.0F);
        ModelPart lid = part(faceCube(
                0, 0, x, 0.0F, 0.0F, 15.0F, 5.0F, 14.0F, connectionFace
        ), lidPose);
        float lockX = type == ChestType.RIGHT ? 15.0F : 0.0F;
        ModelPart lock = part(faceCube(
                0, 0, lockX, -2.0F, 14.0F, 1.0F, 4.0F, 1.0F, connectionFace
        ), lidPose);

        return new ChestConnectionCapModel(new ModelPart(List.of(), Map.of(
                "bottom", bottom,
                "lid", lid,
                "lock", lock
        )));
    }

    static Direction connectionFace(ChestType type) {
        return switch (type) {
            case LEFT -> Direction.WEST;
            case RIGHT -> Direction.EAST;
            case SINGLE -> throw new IllegalArgumentException("A single chest has no connection face");
        };
    }

    public static ChestType spriteType(ChestType type) {
        return switch (type) {
            case LEFT -> ChestType.RIGHT;
            case RIGHT -> ChestType.LEFT;
            case SINGLE -> throw new IllegalArgumentException("A single chest has no cap sprite");
        };
    }

    private static ModelPart.Cube faceCube(
            int textureU,
            int textureV,
            float x,
            float y,
            float z,
            float width,
            float height,
            float depth,
            Direction face
    ) {
        return new ModelPart.Cube(
                textureU,
                textureV,
                x,
                y,
                z,
                width,
                height,
                depth,
                0.0F,
                0.0F,
                0.0F,
                false,
                TEXTURE_WIDTH,
                TEXTURE_HEIGHT,
                Set.of(face)
        );
    }

    private static ModelPart part(ModelPart.Cube cube) {
        return new ModelPart(List.of(cube), Map.of());
    }

    private static ModelPart part(ModelPart.Cube cube, PartPose pose) {
        ModelPart part = part(cube);
        part.loadPose(pose);
        part.setInitialPose(pose);
        return part;
    }
}
