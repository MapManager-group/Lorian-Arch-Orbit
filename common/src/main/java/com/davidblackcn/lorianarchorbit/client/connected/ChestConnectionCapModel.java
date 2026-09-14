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
        Direction missingFace = missingFace(type);
        float x = type == ChestType.RIGHT ? 1.0F : 0.0F;

        // These are the exact cube coordinates and texture origins used by the Minecraft 26.2
        // ChestModel double-left/right layers. Each cap retains only the omitted connection face.
        ModelPart bottom = part(copyOppositeFaceUv(
                0, 19, x, 0.0F, 1.0F, 15.0F, 10.0F, 14.0F, missingFace
        ));
        ModelPart lid = part(copyOppositeFaceUv(
                0, 0, x, 0.0F, 0.0F, 15.0F, 5.0F, 14.0F, missingFace
        ));
        PartPose lidPose = PartPose.offset(0.0F, 9.0F, 1.0F);
        lid.loadPose(lidPose);
        lid.setInitialPose(lidPose);

        float lockX = type == ChestType.RIGHT ? 15.0F : 0.0F;
        ModelPart lock = part(copyOppositeFaceUv(
                0, 0, lockX, -2.0F, 14.0F, 1.0F, 4.0F, 1.0F, missingFace
        ));
        lock.loadPose(lidPose);
        lock.setInitialPose(lidPose);
        ModelPart root = new ModelPart(List.of(), Map.of(
                "bottom", bottom,
                "lid", lid,
                "lock", lock
        ));
        return new ChestConnectionCapModel(root);
    }

    static Direction missingFace(ChestType type) {
        return switch (type) {
            case LEFT -> Direction.WEST;
            case RIGHT -> Direction.EAST;
            case SINGLE -> throw new IllegalArgumentException("A single chest has no connection face");
        };
    }

    static ModelPart.Cube copyOppositeFaceUv(
            int textureU,
            int textureV,
            float x,
            float y,
            float z,
            float width,
            float height,
            float depth,
            Direction missingFace
    ) {
        ModelPart.Cube target = faceCube(
                textureU, textureV, x, y, z, width, height, depth, missingFace
        );
        ModelPart.Cube source = faceCube(
                textureU, textureV, x, y, z, width, height, depth, missingFace.getOpposite()
        );
        ModelPart.Polygon targetFace = target.polygons[0];
        ModelPart.Vertex[] targetVertices = targetFace.vertices();
        ModelPart.Vertex[] sourceVertices = source.polygons[0].vertices();
        ModelPart.Vertex[] copied = new ModelPart.Vertex[targetVertices.length];
        for (int index = 0; index < targetVertices.length; index++) {
            ModelPart.Vertex position = targetVertices[index];
            // WEST and EAST use different vertex orders. Match the physical corner so
            // the opposite exterior texture is copied without mirroring or crossing UVs.
            ModelPart.Vertex uv = findMatchingSideVertex(position, sourceVertices);
            copied[index] = new ModelPart.Vertex(
                    position.x(), position.y(), position.z(), uv.u(), uv.v()
            );
        }
        target.polygons[0] = new ModelPart.Polygon(copied, targetFace.normal());
        return target;
    }

    private static ModelPart.Vertex findMatchingSideVertex(
            ModelPart.Vertex target,
            ModelPart.Vertex[] sourceVertices
    ) {
        for (ModelPart.Vertex source : sourceVertices) {
            if (Float.compare(target.y(), source.y()) == 0
                    && Float.compare(target.z(), source.z()) == 0) {
                return source;
            }
        }
        throw new IllegalStateException("Opposite chest face has no matching Y/Z vertex");
    }

    static ModelPart.Cube faceCube(
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

    private static ModelPart part(ModelPart.Cube... cubes) {
        return new ModelPart(List.of(cubes), Map.of());
    }
}
