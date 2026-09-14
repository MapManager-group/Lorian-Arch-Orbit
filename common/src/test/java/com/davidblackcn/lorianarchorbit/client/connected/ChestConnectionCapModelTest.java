package com.davidblackcn.lorianarchorbit.client.connected;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class ChestConnectionCapModelTest {
    @Test
    public void leftAndRightCapsContainOnlyTheirMissingFaceWithOppositeUv() {
        assertCap(ChestType.LEFT, 0.0F, Direction.WEST);
        assertCap(ChestType.RIGHT, 1.0F, Direction.EAST);
    }

    @Test
    public void lidCapUsesTheVanillaLidAnimationHierarchy() {
        ChestConnectionCapModel model = ChestConnectionCapModel.create(ChestType.LEFT);
        model.setupAnim(0.5F);

        assertEquals(0.0F, model.root().getChild("bottom").xRot);
        assertEquals(-(float) Math.PI / 4.0F, model.root().getChild("lid").xRot, 0.000001F);
        assertEquals(-(float) Math.PI / 4.0F, model.root().getChild("lock").xRot, 0.000001F);
    }

    private static void assertCap(ChestType type, float x, Direction missingFace) {
        ChestConnectionCapModel model = ChestConnectionCapModel.create(type);
        ModelPart.Cube bottom = onlyCube(model.root().getChild("bottom"));
        ModelPart.Cube lid = onlyCube(model.root().getChild("lid"));
        ModelPart.Cube lock = onlyCube(model.root().getChild("lock"));

        assertEquals(1, bottom.polygons.length);
        assertEquals(1, lid.polygons.length);
        assertEquals(1, lock.polygons.length);
        assertEquals(missingFace.getUnitVec3f(), bottom.polygons[0].normal());

        float targetX = missingFace == Direction.WEST ? x : x + 15.0F;
        for (ModelPart.Vertex vertex : bottom.polygons[0].vertices()) {
            assertEquals(targetX, vertex.x());
        }

        ModelPart.Cube oppositeBottom = ChestConnectionCapModel.faceCube(
                0, 19, x, 0.0F, 1.0F, 15.0F, 10.0F, 14.0F, missingFace.getOpposite()
        );
        assertUvEquals(oppositeBottom.polygons[0], bottom.polygons[0]);

        ModelPart.Cube oppositeLid = ChestConnectionCapModel.faceCube(
                0, 0, x, 0.0F, 0.0F, 15.0F, 5.0F, 14.0F, missingFace.getOpposite()
        );
        assertUvEquals(oppositeLid.polygons[0], lid.polygons[0]);

        float lockX = type == ChestType.RIGHT ? 15.0F : 0.0F;
        ModelPart.Cube oppositeLock = ChestConnectionCapModel.faceCube(
                0, 0, lockX, -2.0F, 14.0F, 1.0F, 4.0F, 1.0F, missingFace.getOpposite()
        );
        assertUvEquals(oppositeLock.polygons[0], lock.polygons[0]);
    }

    private static ModelPart.Cube onlyCube(ModelPart part) {
        List<ModelPart.Cube> cubes = cubes(part);
        assertEquals(1, cubes.size());
        return cubes.getFirst();
    }

    private static List<ModelPart.Cube> cubes(ModelPart part) {
        List<ModelPart.Cube> cubes = new ArrayList<>();
        part.visit(new PoseStack(), (pose, path, index, cube) -> cubes.add(cube));
        return cubes;
    }

    private static void assertUvEquals(ModelPart.Polygon expected, ModelPart.Polygon actual) {
        assertEquals(expected.vertices().length, actual.vertices().length);
        for (int index = 0; index < expected.vertices().length; index++) {
            assertEquals(expected.vertices()[index].u(), actual.vertices()[index].u());
            assertEquals(expected.vertices()[index].v(), actual.vertices()[index].v());
        }
    }
}
