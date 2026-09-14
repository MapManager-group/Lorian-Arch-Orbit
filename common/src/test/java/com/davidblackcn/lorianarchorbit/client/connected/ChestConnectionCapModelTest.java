package com.davidblackcn.lorianarchorbit.client.connected;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.object.chest.ChestModel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class ChestConnectionCapModelTest {
    @Test
    public void leftCapUsesVanillaWestUvWithRightSprite() {
        ChestConnectionCapModel model = ChestConnectionCapModel.create(ChestType.LEFT);

        assertEquals(ChestType.RIGHT, ChestConnectionCapModel.spriteType(ChestType.LEFT));
        assertVanillaPartPoses(model, ChestType.LEFT);
        assertFace(onlyCube(model.root().getChild("bottom")), Direction.WEST, 0.0F,
                0.0F, 10.0F, 1.0F, 15.0F, 0.0F, 14.0F, 33.0F, 43.0F);
        assertFace(onlyCube(model.root().getChild("lid")), Direction.WEST, 0.0F,
                0.0F, 5.0F, 0.0F, 14.0F, 0.0F, 14.0F, 14.0F, 19.0F);
        assertFace(onlyCube(model.root().getChild("lock")), Direction.WEST, 0.0F,
                -2.0F, 2.0F, 14.0F, 15.0F, 0.0F, 1.0F, 1.0F, 5.0F);
    }

    @Test
    public void rightCapUsesVanillaEastUvWithLeftSprite() {
        ChestConnectionCapModel model = ChestConnectionCapModel.create(ChestType.RIGHT);

        assertEquals(ChestType.LEFT, ChestConnectionCapModel.spriteType(ChestType.RIGHT));
        assertVanillaPartPoses(model, ChestType.RIGHT);
        assertFace(onlyCube(model.root().getChild("bottom")), Direction.EAST, 16.0F,
                0.0F, 10.0F, 1.0F, 15.0F, 29.0F, 43.0F, 33.0F, 43.0F);
        assertFace(onlyCube(model.root().getChild("lid")), Direction.EAST, 16.0F,
                0.0F, 5.0F, 0.0F, 14.0F, 29.0F, 43.0F, 14.0F, 19.0F);
        assertFace(onlyCube(model.root().getChild("lock")), Direction.EAST, 16.0F,
                -2.0F, 2.0F, 14.0F, 15.0F, 2.0F, 3.0F, 1.0F, 5.0F);
    }

    @Test
    public void lidAndLockUseTheVanillaAnimationHierarchy() {
        ChestConnectionCapModel model = ChestConnectionCapModel.create(ChestType.LEFT);
        model.setupAnim(0.5F);

        assertEquals(0.0F, model.root().getChild("bottom").xRot);
        assertEquals(-(float) Math.PI / 4.0F, model.root().getChild("lid").xRot, 0.000001F);
        assertEquals(-(float) Math.PI / 4.0F, model.root().getChild("lock").xRot, 0.000001F);
    }

    private static void assertFace(
            ModelPart.Cube cube,
            Direction direction,
            float planeX,
            float minY,
            float maxY,
            float minZ,
            float maxZ,
            float minPixelU,
            float maxPixelU,
            float minPixelV,
            float maxPixelV
    ) {
        assertEquals(1, cube.polygons.length);
        assertEquals(minY, cube.minY);
        assertEquals(maxY, cube.maxY);
        assertEquals(minZ, cube.minZ);
        assertEquals(maxZ, cube.maxZ);
        ModelPart.Polygon polygon = cube.polygons[0];
        assertEquals(direction.getUnitVec3f(), polygon.normal());
        float actualMinU = Float.POSITIVE_INFINITY;
        float actualMaxU = Float.NEGATIVE_INFINITY;
        float actualMinV = Float.POSITIVE_INFINITY;
        float actualMaxV = Float.NEGATIVE_INFINITY;
        for (ModelPart.Vertex vertex : polygon.vertices()) {
            assertEquals(planeX, vertex.x());
            actualMinU = Math.min(actualMinU, vertex.u());
            actualMaxU = Math.max(actualMaxU, vertex.u());
            actualMinV = Math.min(actualMinV, vertex.v());
            actualMaxV = Math.max(actualMaxV, vertex.v());
        }
        assertEquals(minPixelU / 64.0F, actualMinU);
        assertEquals(maxPixelU / 64.0F, actualMaxU);
        assertEquals(minPixelV / 64.0F, actualMinV);
        assertEquals(maxPixelV / 64.0F, actualMaxV);
    }

    private static void assertVanillaPartPoses(ChestConnectionCapModel cap, ChestType type) {
        ChestModel vanilla = switch (type) {
            case LEFT -> new ChestModel(ChestModel.createDoubleBodyLeftLayer().bakeRoot());
            case RIGHT -> new ChestModel(ChestModel.createDoubleBodyRightLayer().bakeRoot());
            case SINGLE -> throw new IllegalArgumentException("Expected a double chest type");
        };
        for (String partName : List.of("bottom", "lid", "lock")) {
            assertEquals(
                    vanilla.root().getChild(partName).getInitialPose(),
                    cap.root().getChild(partName).getInitialPose(),
                    partName
            );
        }
    }

    private static ModelPart.Cube onlyCube(ModelPart part) {
        List<ModelPart.Cube> cubes = new ArrayList<>();
        part.visit(new PoseStack(), (pose, path, index, cube) -> cubes.add(cube));
        assertEquals(1, cubes.size());
        return cubes.getFirst();
    }
}
