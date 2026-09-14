package com.davidblackcn.lorianarchorbit.client.connected;

import com.davidblackcn.lorianarchorbit.LorianArchOrbit;
import dev.architectury.platform.Platform;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.object.chest.ChestModel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.ChestType;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SealedDoubleChestModel extends ChestModel {
    private static final int TEXTURE_WIDTH = 64;
    private static final int TEXTURE_HEIGHT = 64;
    private static final System.Logger LOGGER =
            System.getLogger(LorianArchOrbit.MOD_ID + ".chest_renderer");
    private static final Set<ChestType> LOGGED_REPLACEMENTS = EnumSet.noneOf(ChestType.class);
    private static final SealedDoubleChestModel LEFT = create(ChestType.LEFT);
    private static final SealedDoubleChestModel RIGHT = create(ChestType.RIGHT);

    private SealedDoubleChestModel(ModelPart root) {
        super(root);
    }

    public static SealedDoubleChestModel select(ChestType type) {
        SealedDoubleChestModel model = switch (type) {
            case LEFT -> LEFT;
            case RIGHT -> RIGHT;
            case SINGLE -> throw new IllegalArgumentException("A single chest is not a double chest");
        };
        logReplacement(type);
        return model;
    }

    static SealedDoubleChestModel create(ChestType type) {
        Direction connectionFace = connectionFace(type);
        float x = type == ChestType.RIGHT ? 1.0F : 0.0F;

        // Coordinates, texture offsets, and poses are copied from the Minecraft 26.2
        // ChestModel double-left/right layers. Only their omitted side is made visible.
        ModelPart.Cube bottomCube = sealedCube(
                0, 19, x, 0.0F, 1.0F, 15.0F, 10.0F, 14.0F, connectionFace
        );
        ModelPart.Cube lidCube = sealedCube(
                0, 0, x, 0.0F, 0.0F, 15.0F, 5.0F, 14.0F, connectionFace
        );
        float lockX = type == ChestType.RIGHT ? 15.0F : 0.0F;
        ModelPart.Cube lockCube = sealedCube(
                0, 0, lockX, -2.0F, 14.0F, 1.0F, 4.0F, 1.0F, connectionFace
        );

        ModelPart bottom = part(bottomCube);
        PartPose lidPose = PartPose.offset(0.0F, 9.0F, 1.0F);
        ModelPart lid = part(lidCube, lidPose);
        ModelPart lock = part(lockCube, lidPose);
        Map<String, ModelPart> children = new LinkedHashMap<>();
        children.put("bottom", bottom);
        children.put("lid", lid);
        children.put("lock", lock);
        return new SealedDoubleChestModel(new ModelPart(List.of(), children));
    }

    static Direction connectionFace(ChestType type) {
        return switch (type) {
            case LEFT -> Direction.WEST;
            case RIGHT -> Direction.EAST;
            case SINGLE -> throw new IllegalArgumentException("A single chest has no connection face");
        };
    }

    private static ModelPart.Cube sealedCube(
            int textureU,
            int textureV,
            float x,
            float y,
            float z,
            float width,
            float height,
            float depth,
            Direction connectionFace
    ) {
        ModelPart.Cube cube = new ModelPart.Cube(
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
                EnumSet.allOf(Direction.class)
        );
        copyOppositeFaceUv(cube, connectionFace);
        return cube;
    }

    private static void copyOppositeFaceUv(ModelPart.Cube cube, Direction connectionFace) {
        int targetIndex = polygonIndex(cube, connectionFace);
        ModelPart.Polygon target = cube.polygons[targetIndex];
        ModelPart.Vertex[] sourceVertices = polygon(cube, connectionFace.getOpposite()).vertices();
        ModelPart.Vertex[] copied = new ModelPart.Vertex[target.vertices().length];
        for (int index = 0; index < target.vertices().length; index++) {
            ModelPart.Vertex position = target.vertices()[index];
            ModelPart.Vertex source = matchingSideVertex(position, sourceVertices);
            copied[index] = new ModelPart.Vertex(
                    position.x(), position.y(), position.z(), source.u(), source.v()
            );
        }
        cube.polygons[targetIndex] = new ModelPart.Polygon(copied, target.normal());
    }

    private static ModelPart.Vertex matchingSideVertex(
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

    private static ModelPart.Polygon polygon(ModelPart.Cube cube, Direction direction) {
        return cube.polygons[polygonIndex(cube, direction)];
    }

    private static int polygonIndex(ModelPart.Cube cube, Direction direction) {
        for (int index = 0; index < cube.polygons.length; index++) {
            if (sameNormal(cube.polygons[index].normal(), direction)) {
                return index;
            }
        }
        throw new IllegalStateException("Chest cube has no " + direction + " face");
    }

    private static boolean sameNormal(org.joml.Vector3fc normal, Direction direction) {
        return Float.compare(normal.x(), direction.getStepX()) == 0
                && Float.compare(normal.y(), direction.getStepY()) == 0
                && Float.compare(normal.z(), direction.getStepZ()) == 0;
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

    private static void logReplacement(ChestType type) {
        if (!Platform.isDevelopmentEnvironment() || !LOGGED_REPLACEMENTS.add(type)) {
            return;
        }
        LOGGER.log(
                System.Logger.Level.INFO,
                "ChestRendererMixin loaded; ChestType=" + type
                        + "; fixChests=true; Vanilla model replaced with SEALED_" + type
        );
    }
}
