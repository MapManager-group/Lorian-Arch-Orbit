package com.davidblackcn.lorianarchorbit.client.connected;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.object.chest.ChestModel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.junit.jupiter.api.Test;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

public final class SealedDoubleChestModelTest {
    private static final List<String> PARTS = List.of("bottom", "lid", "lock");

    @Test
    public void sealedLeftAddsOnlyTheWestConnectionFaces() {
        assertSealedModel(ChestType.LEFT, Direction.WEST, 0.0F, List.of(
                new UvBounds(29.0F, 43.0F, 33.0F, 43.0F),
                new UvBounds(29.0F, 43.0F, 14.0F, 19.0F),
                new UvBounds(2.0F, 3.0F, 1.0F, 5.0F)
        ));
    }

    @Test
    public void sealedRightAddsOnlyTheEastConnectionFaces() {
        assertSealedModel(ChestType.RIGHT, Direction.EAST, 16.0F, List.of(
                new UvBounds(0.0F, 14.0F, 33.0F, 43.0F),
                new UvBounds(0.0F, 14.0F, 14.0F, 19.0F),
                new UvBounds(0.0F, 1.0F, 1.0F, 5.0F)
        ));
    }

    private static void assertSealedModel(
            ChestType type,
            Direction connectionFace,
            float seamPlane,
            List<UvBounds> expectedUvBounds
    ) {
        ChestModel vanilla = vanillaModel(type);
        ChestModel sealed = SealedDoubleChestModel.create(type);
        assertEquals(vanilla.root().getInitialPose(), sealed.root().getInitialPose(), "root");

        for (int partIndex = 0; partIndex < PARTS.size(); partIndex++) {
            String partName = PARTS.get(partIndex);
            ModelPart vanillaPart = vanilla.root().getChild(partName);
            ModelPart sealedPart = sealed.root().getChild(partName);
            assertEquals(vanillaPart.getInitialPose(), sealedPart.getInitialPose(), partName);

            ModelPart.Cube vanillaCube = onlyCube(vanillaPart);
            ModelPart.Cube sealedCube = onlyCube(sealedPart);
            assertCubeBoundsEqual(vanillaCube, sealedCube, partName);
            assertEquals(vanillaCube.polygons.length + 1, sealedCube.polygons.length, partName);

            for (ModelPart.Polygon vanillaFace : vanillaCube.polygons) {
                ModelPart.Polygon sealedFace = polygon(sealedCube, vanillaFace.normal());
                assertPolygonEqual(vanillaFace, sealedFace, partName);
            }

            ModelPart.Polygon connection = polygon(sealedCube, connectionFace);
            assertConnectionPlane(connection, seamPlane, partName);
            assertUvMatchesOppositeVanillaFace(
                    connection,
                    polygon(vanillaCube, connectionFace.getOpposite()),
                    partName
            );
            assertUvBounds(connection, expectedUvBounds.get(partIndex), partName);
        }

        for (float open : List.of(0.0F, 0.5F, 1.0F)) {
            vanilla.setupAnim(open);
            sealed.setupAnim(open);
            for (String partName : PARTS) {
                assertPartTransformEqual(
                        vanilla.root().getChild(partName),
                        sealed.root().getChild(partName),
                        partName + " at open=" + open
                );
            }
        }
    }

    private static ChestModel vanillaModel(ChestType type) {
        return switch (type) {
            case LEFT -> new ChestModel(ChestModel.createDoubleBodyLeftLayer().bakeRoot());
            case RIGHT -> new ChestModel(ChestModel.createDoubleBodyRightLayer().bakeRoot());
            case SINGLE -> throw new IllegalArgumentException("Expected a double chest type");
        };
    }

    private static void assertCubeBoundsEqual(
            ModelPart.Cube expected,
            ModelPart.Cube actual,
            String message
    ) {
        assertEquals(expected.minX, actual.minX, message);
        assertEquals(expected.minY, actual.minY, message);
        assertEquals(expected.minZ, actual.minZ, message);
        assertEquals(expected.maxX, actual.maxX, message);
        assertEquals(expected.maxY, actual.maxY, message);
        assertEquals(expected.maxZ, actual.maxZ, message);
    }

    private static void assertPolygonEqual(
            ModelPart.Polygon expected,
            ModelPart.Polygon actual,
            String message
    ) {
        assertNormal(expected.normal(), actual.normal(), message);
        assertEquals(expected.vertices().length, actual.vertices().length, message);
        for (int index = 0; index < expected.vertices().length; index++) {
            ModelPart.Vertex expectedVertex = expected.vertices()[index];
            ModelPart.Vertex actualVertex = actual.vertices()[index];
            assertEquals(expectedVertex.x(), actualVertex.x(), message);
            assertEquals(expectedVertex.y(), actualVertex.y(), message);
            assertEquals(expectedVertex.z(), actualVertex.z(), message);
            assertEquals(expectedVertex.u(), actualVertex.u(), message);
            assertEquals(expectedVertex.v(), actualVertex.v(), message);
        }
    }

    private static void assertConnectionPlane(
            ModelPart.Polygon connection,
            float planeX,
            String message
    ) {
        for (ModelPart.Vertex vertex : connection.vertices()) {
            assertEquals(planeX, vertex.x(), message);
        }
    }

    private static void assertUvMatchesOppositeVanillaFace(
            ModelPart.Polygon connection,
            ModelPart.Polygon vanillaExterior,
            String message
    ) {
        for (ModelPart.Vertex connectionVertex : connection.vertices()) {
            ModelPart.Vertex exteriorVertex = vertexAt(
                    vanillaExterior, connectionVertex.y(), connectionVertex.z()
            );
            assertEquals(exteriorVertex.u(), connectionVertex.u(), message);
            assertEquals(exteriorVertex.v(), connectionVertex.v(), message);
        }
    }

    private static void assertUvBounds(
            ModelPart.Polygon polygon,
            UvBounds expected,
            String message
    ) {
        float minU = Float.POSITIVE_INFINITY;
        float maxU = Float.NEGATIVE_INFINITY;
        float minV = Float.POSITIVE_INFINITY;
        float maxV = Float.NEGATIVE_INFINITY;
        for (ModelPart.Vertex vertex : polygon.vertices()) {
            minU = Math.min(minU, vertex.u());
            maxU = Math.max(maxU, vertex.u());
            minV = Math.min(minV, vertex.v());
            maxV = Math.max(maxV, vertex.v());
        }
        assertEquals(expected.minU() / 64.0F, minU, message);
        assertEquals(expected.maxU() / 64.0F, maxU, message);
        assertEquals(expected.minV() / 64.0F, minV, message);
        assertEquals(expected.maxV() / 64.0F, maxV, message);
    }

    private static void assertPartTransformEqual(
            ModelPart expected,
            ModelPart actual,
            String message
    ) {
        assertEquals(expected.x, actual.x, message);
        assertEquals(expected.y, actual.y, message);
        assertEquals(expected.z, actual.z, message);
        assertEquals(expected.xRot, actual.xRot, message);
        assertEquals(expected.yRot, actual.yRot, message);
        assertEquals(expected.zRot, actual.zRot, message);
        assertEquals(expected.xScale, actual.xScale, message);
        assertEquals(expected.yScale, actual.yScale, message);
        assertEquals(expected.zScale, actual.zScale, message);
    }

    private static ModelPart.Cube onlyCube(ModelPart part) {
        List<ModelPart.Cube> cubes = new ArrayList<>();
        part.visit(new PoseStack(), (pose, path, index, cube) -> cubes.add(cube));
        assertEquals(1, cubes.size());
        return cubes.getFirst();
    }

    private static ModelPart.Polygon polygon(ModelPart.Cube cube, Direction direction) {
        return polygon(cube, direction.getUnitVec3f());
    }

    private static ModelPart.Polygon polygon(ModelPart.Cube cube, Vector3fc normal) {
        for (ModelPart.Polygon polygon : cube.polygons) {
            if (sameNormal(polygon.normal(), normal)) {
                return polygon;
            }
        }
        fail("Missing face with normal " + normal);
        return null;
    }

    private static ModelPart.Vertex vertexAt(ModelPart.Polygon polygon, float y, float z) {
        for (ModelPart.Vertex vertex : polygon.vertices()) {
            if (Float.compare(vertex.y(), y) == 0 && Float.compare(vertex.z(), z) == 0) {
                return vertex;
            }
        }
        fail("Missing vertex at Y=" + y + ", Z=" + z);
        return null;
    }

    private static void assertNormal(Vector3fc expected, Vector3fc actual, String message) {
        assertNotNull(actual, message);
        assertEquals(expected.x(), actual.x(), message);
        assertEquals(expected.y(), actual.y(), message);
        assertEquals(expected.z(), actual.z(), message);
    }

    private static boolean sameNormal(Vector3fc first, Vector3fc second) {
        return Float.compare(first.x(), second.x()) == 0
                && Float.compare(first.y(), second.y()) == 0
                && Float.compare(first.z(), second.z()) == 0;
    }

    private record UvBounds(float minU, float maxU, float minV, float maxV) {
    }
}
