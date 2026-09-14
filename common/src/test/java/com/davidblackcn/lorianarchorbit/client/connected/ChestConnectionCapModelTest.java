package com.davidblackcn.lorianarchorbit.client.connected;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public final class ChestConnectionCapModelTest {
    @Test
    public void leftCapUsesEastWoodUvWithoutMirroring() {
        ChestConnectionCapModel model = ChestConnectionCapModel.create(ChestType.LEFT);

        assertFace(onlyCube(model.root().getChild("bottom")), Direction.WEST, 0.0F);
        assertUvRectangle(
                onlyCube(model.root().getChild("bottom")).polygons[0],
                0.0F, 10.0F, 1.0F, 15.0F,
                29.0F, 43.0F, 33.0F, 43.0F
        );
        assertFace(onlyCube(model.root().getChild("lid")), Direction.WEST, 0.0F);
        assertUvRectangle(
                onlyCube(model.root().getChild("lid")).polygons[0],
                0.0F, 5.0F, 0.0F, 14.0F,
                29.0F, 43.0F, 14.0F, 19.0F
        );
        assertFace(onlyCube(model.root().getChild("lock")), Direction.WEST, 0.0F);
        assertUvRectangle(
                onlyCube(model.root().getChild("lock")).polygons[0],
                -2.0F, 2.0F, 14.0F, 15.0F,
                2.0F, 3.0F, 1.0F, 5.0F
        );
    }

    @Test
    public void rightCapUsesWestWoodUvWithoutMirroring() {
        ChestConnectionCapModel model = ChestConnectionCapModel.create(ChestType.RIGHT);

        assertFace(onlyCube(model.root().getChild("bottom")), Direction.EAST, 16.0F);
        assertUvRectangle(
                onlyCube(model.root().getChild("bottom")).polygons[0],
                0.0F, 10.0F, 1.0F, 15.0F,
                14.0F, 0.0F, 33.0F, 43.0F
        );
        assertFace(onlyCube(model.root().getChild("lid")), Direction.EAST, 16.0F);
        assertUvRectangle(
                onlyCube(model.root().getChild("lid")).polygons[0],
                0.0F, 5.0F, 0.0F, 14.0F,
                14.0F, 0.0F, 14.0F, 19.0F
        );
        assertFace(onlyCube(model.root().getChild("lock")), Direction.EAST, 16.0F);
        assertUvRectangle(
                onlyCube(model.root().getChild("lock")).polygons[0],
                -2.0F, 2.0F, 14.0F, 15.0F,
                1.0F, 0.0F, 1.0F, 5.0F
        );
    }

    @Test
    public void lidCapUsesTheVanillaLidAnimationHierarchy() {
        ChestConnectionCapModel model = ChestConnectionCapModel.create(ChestType.LEFT);
        model.setupAnim(0.5F);

        assertEquals(0.0F, model.root().getChild("bottom").xRot);
        assertEquals(-(float) Math.PI / 4.0F, model.root().getChild("lid").xRot, 0.000001F);
        assertEquals(-(float) Math.PI / 4.0F, model.root().getChild("lock").xRot, 0.000001F);
    }

    private static void assertFace(ModelPart.Cube cube, Direction direction, float planeX) {
        assertEquals(1, cube.polygons.length);
        ModelPart.Polygon polygon = cube.polygons[0];
        assertEquals(direction.getUnitVec3f(), polygon.normal());
        for (ModelPart.Vertex vertex : polygon.vertices()) {
            assertEquals(planeX, vertex.x());
        }
    }

    private static void assertUvRectangle(
            ModelPart.Polygon polygon,
            float minY,
            float maxY,
            float minZ,
            float maxZ,
            float uvAtMinZ,
            float uvAtMaxZ,
            float minV,
            float maxV
    ) {
        assertVertexUv(polygon, minY, minZ, uvAtMinZ, minV);
        assertVertexUv(polygon, minY, maxZ, uvAtMaxZ, minV);
        assertVertexUv(polygon, maxY, minZ, uvAtMinZ, maxV);
        assertVertexUv(polygon, maxY, maxZ, uvAtMaxZ, maxV);
    }

    private static void assertVertexUv(
            ModelPart.Polygon polygon,
            float y,
            float z,
            float expectedPixelU,
            float expectedPixelV
    ) {
        for (ModelPart.Vertex vertex : polygon.vertices()) {
            if (Float.compare(vertex.y(), y) == 0 && Float.compare(vertex.z(), z) == 0) {
                assertEquals(expectedPixelU / 64.0F, vertex.u());
                assertEquals(expectedPixelV / 64.0F, vertex.v());
                return;
            }
        }
        fail("Missing vertex at Y=" + y + ", Z=" + z);
    }

    private static ModelPart.Cube onlyCube(ModelPart part) {
        List<ModelPart.Cube> cubes = new ArrayList<>();
        part.visit(new PoseStack(), (pose, path, index, cube) -> cubes.add(cube));
        assertEquals(1, cubes.size());
        return cubes.getFirst();
    }
}
