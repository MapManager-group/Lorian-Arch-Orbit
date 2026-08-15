package com.davidblackcn.lorianarchorbit.client.connected;

import net.minecraft.client.renderer.FaceInfo;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.WallSide;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ConnectedTextureModelFixer {
    private static final float EPSILON = 0.0001F;
    private static final Map<BedFootKey, BakedQuad> BED_FOOT_FACES = new ConcurrentHashMap<>();

    private ConnectedTextureModelFixer() {
    }

    public static void beginModelBake() {
        BED_FOOT_FACES.clear();
    }

    public static BlockStateModel wrap(BlockState state, BlockStateModel original) {
        ConnectionFixKind kind = kind(state);
        if (kind == null) return original;
        List<BlockStateModelPart> parts = collectParts(original);
        List<BakedQuad> existing = collectQuads(parts);
        List<BakedQuad> unculled = collectUnculledQuads(parts);
        if (kind == ConnectionFixKind.BED && state.getValue(BedBlock.PART) == BedPart.HEAD) {
            return wrapBedHead(state, original, parts, existing);
        }
        List<BakedQuad> additions = switch (kind) {
            case WALL -> wallFaces(state, existing);
            case BED -> bedFootFaces(state, existing);
            case DOOR -> doorFaces(state, existing);
            case PISTON -> pistonFaces(state, existing, unculled);
            case NETHER_PORTAL -> netherPortalFaces(state, existing);
            case END_PORTAL -> List.of();
        };
        if (additions.isEmpty()) return original;
        return new WrappedModel(original, new AddedPart(
                additions, original, ambientOcclusion(parts, existing.getFirst())
        ), kind);
    }

    static ConnectionFixKind kind(BlockState state) {
        // Client data-pack tags are not guaranteed to be bound during the initial model bake.
        // These property sets use the vanilla property instances and uniquely identify the
        // standard connected model contracts while still covering compatible mod blocks.
        if (standardWall(state)) return ConnectionFixKind.WALL;
        if (standardBed(state)) return ConnectionFixKind.BED;
        if (standardDoor(state)) return ConnectionFixKind.DOOR;
        if (standardExtendedPistonBase(state) || standardExtendedPistonHead(state)) return ConnectionFixKind.PISTON;
        if (state.getBlock() instanceof NetherPortalBlock && state.hasProperty(NetherPortalBlock.AXIS)) {
            return ConnectionFixKind.NETHER_PORTAL;
        }
        return null;
    }

    private static boolean standardWall(BlockState state) {
        return state.hasProperty(WallBlock.UP) && state.hasProperty(WallBlock.NORTH)
                && state.hasProperty(WallBlock.SOUTH) && state.hasProperty(WallBlock.WEST)
                && state.hasProperty(WallBlock.EAST);
    }

    private static boolean standardBed(BlockState state) {
        return state.hasProperty(BedBlock.PART) && state.hasProperty(BedBlock.FACING);
    }

    private static boolean standardDoor(BlockState state) {
        return state.hasProperty(DoorBlock.HALF) && state.hasProperty(DoorBlock.FACING)
                && state.hasProperty(DoorBlock.HINGE) && state.hasProperty(DoorBlock.OPEN);
    }

    private static boolean standardExtendedPistonBase(BlockState state) {
        return state.getBlock() instanceof PistonBaseBlock
                && state.hasProperty(DirectionalBlock.FACING)
                && state.hasProperty(PistonBaseBlock.EXTENDED)
                && state.getValue(PistonBaseBlock.EXTENDED);
    }

    private static boolean standardExtendedPistonHead(BlockState state) {
        return state.getBlock() instanceof PistonHeadBlock
                && state.hasProperty(DirectionalBlock.FACING)
                && state.hasProperty(PistonHeadBlock.SHORT)
                && !state.getValue(PistonHeadBlock.SHORT);
    }

    private static List<BakedQuad> wallFaces(BlockState state, List<BakedQuad> existing) {
        List<BakedQuad> result = new ArrayList<>(4);
        addWallFace(result, existing, Direction.NORTH, state.getValue(WallBlock.NORTH));
        addWallFace(result, existing, Direction.SOUTH, state.getValue(WallBlock.SOUTH));
        addWallFace(result, existing, Direction.WEST, state.getValue(WallBlock.WEST));
        addWallFace(result, existing, Direction.EAST, state.getValue(WallBlock.EAST));
        return result;
    }

    private static void addWallFace(
            List<BakedQuad> result, List<BakedQuad> existing, Direction arm, WallSide side
    ) {
        if (side == WallSide.NONE) return;
        float height = side == WallSide.TALL ? 1.0F : 0.875F;
        Bounds bounds = switch (arm) {
            case NORTH -> new Bounds(5 / 16F, 0, 0.5F, 11 / 16F, height, 0.5F, Direction.SOUTH);
            case SOUTH -> new Bounds(5 / 16F, 0, 0.5F, 11 / 16F, height, 0.5F, Direction.NORTH);
            case WEST -> new Bounds(0.5F, 0, 5 / 16F, 0.5F, height, 11 / 16F, Direction.EAST);
            case EAST -> new Bounds(0.5F, 0, 5 / 16F, 0.5F, height, 11 / 16F, Direction.WEST);
            default -> throw new IllegalArgumentException("Wall arm must be horizontal");
        };
        Bounds source = switch (arm) {
            case NORTH -> new Bounds(5 / 16F, 0, 0, 11 / 16F, height, 0, Direction.NORTH);
            case SOUTH -> new Bounds(5 / 16F, 0, 1, 11 / 16F, height, 1, Direction.SOUTH);
            case WEST -> new Bounds(0, 0, 5 / 16F, 0, height, 11 / 16F, Direction.WEST);
            case EAST -> new Bounds(1, 0, 5 / 16F, 1, height, 11 / 16F, Direction.EAST);
            default -> throw new IllegalArgumentException("Wall arm must be horizontal");
        };
        addIfMissing(result, existing, exactReference(existing, source, arm), bounds);
    }

    private static List<BakedQuad> bedFootFaces(BlockState state, List<BakedQuad> existing) {
        Direction facing = state.getValue(BedBlock.FACING);
        Direction seam = facing;
        Bounds bounds = edgeBounds(seam, 3 / 16F, 9 / 16F, 0, 1);
        List<BakedQuad> result = new ArrayList<>(1);
        Direction sourceDirection = seam.getOpposite();
        BakedQuad reference = exactReference(
                existing, edgeBounds(sourceDirection, 3 / 16F, 9 / 16F, 0, 1), sourceDirection
        );
        if (reference != null) BED_FOOT_FACES.put(new BedFootKey(state.getBlock(), facing), reference);
        addIfMissing(result, existing, reference, bounds);
        return result;
    }

    private static BlockStateModel wrapBedHead(
            BlockState state, BlockStateModel original, List<BlockStateModelPart> parts,
            List<BakedQuad> existing
    ) {
        if (existing.isEmpty()) return original;
        Direction facing = state.getValue(BedBlock.FACING);
        Bounds bounds = edgeBounds(facing.getOpposite(), 3 / 16F, 9 / 16F, 0, 1);
        if (existing.stream().anyMatch(quad -> covers(quad, bounds))) return original;
        DeferredBedHeadPart addition = new DeferredBedHeadPart(
                new BedFootKey(state.getBlock(), facing), bounds, original,
                ambientOcclusion(parts, existing.getFirst())
        );
        return new WrappedModel(original, addition, ConnectionFixKind.BED);
    }

    private static List<BakedQuad> doorFaces(BlockState state, List<BakedQuad> existing) {
        float minX = 1, minZ = 1, maxX = 0, maxZ = 0;
        for (BakedQuad quad : existing) {
            for (int vertex = 0; vertex < BakedQuad.VERTEX_COUNT; vertex++) {
                Vector3fc position = quad.position(vertex);
                minX = Math.min(minX, position.x());
                minZ = Math.min(minZ, position.z());
                maxX = Math.max(maxX, position.x());
                maxZ = Math.max(maxZ, position.z());
            }
        }
        if (maxX <= minX || maxZ <= minZ) return List.of();
        boolean upper = state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER;
        float y = upper ? 0 : 1;
        Bounds bounds = new Bounds(minX, y, minZ, maxX, y, maxZ, upper ? Direction.DOWN : Direction.UP);
        List<BakedQuad> result = new ArrayList<>(1);
        addIfMissing(result, existing, bestReference(existing, doorSourceDirection(bounds.direction)), bounds);
        return result;
    }

    private static List<BakedQuad> pistonFaces(
            BlockState state, List<BakedQuad> existing, List<BakedQuad> unculled
    ) {
        Direction facing = state.getValue(DirectionalBlock.FACING);
        List<BakedQuad> result = new ArrayList<>(1);
        if (state.getBlock() instanceof PistonBaseBlock) {
            Bounds bounds = pistonBaseConnectionBounds(facing);
            BakedQuad reference = existing.stream().filter(quad -> covers(quad, bounds)).findFirst().orElse(null);
            if (reference == null) reference = bestReference(existing, pistonSideDirection(facing));
            addIfUncovered(result, unculled, reference, bounds);
        } else {
            Bounds bounds = pistonHeadConnectionBounds(facing);
            BakedQuad reference = bestReference(existing, facing.getOpposite());
            if (reference != null && existing.stream().noneMatch(quad -> covers(quad, bounds))) {
                result.add(bakeCenterCrop(bounds, reference));
            }
        }
        return result;
    }

    private static List<BakedQuad> netherPortalFaces(BlockState state, List<BakedQuad> existing) {
        Direction.Axis axis = state.getValue(NetherPortalBlock.AXIS);
        BakedQuad reference = bestReference(existing, axis == Direction.Axis.X ? Direction.NORTH : Direction.EAST);
        if (reference == null) return List.of();
        List<BakedQuad> result = new ArrayList<>(4);
        for (PortalFace face : netherPortalConnectionFaces(axis)) {
            addCroppedIfMissing(result, existing, reference, face);
        }
        return result;
    }

    /**
     * The portal slab is four texels thick. Every added edge preserves that density by taking a
     * centred 4x16 or 16x4 slice from an existing full 16x16 portal face, never stretching a
     * whole face across the narrow edge.
     */
    static List<PortalFace> netherPortalConnectionFaces(Direction.Axis axis) {
        return switch (axis) {
            case X -> List.of(
                    new PortalFace(new Bounds(0, 0, 6 / 16F, 0, 1, 10 / 16F, Direction.WEST), 1 / 4F, 1),
                    new PortalFace(new Bounds(1, 0, 6 / 16F, 1, 1, 10 / 16F, Direction.EAST), 1 / 4F, 1),
                    new PortalFace(new Bounds(0, 0, 6 / 16F, 1, 0, 10 / 16F, Direction.DOWN), 1, 1 / 4F),
                    new PortalFace(new Bounds(0, 1, 6 / 16F, 1, 1, 10 / 16F, Direction.UP), 1, 1 / 4F)
            );
            case Z -> List.of(
                    new PortalFace(new Bounds(6 / 16F, 0, 0, 10 / 16F, 1, 0, Direction.NORTH), 1 / 4F, 1),
                    new PortalFace(new Bounds(6 / 16F, 0, 1, 10 / 16F, 1, 1, Direction.SOUTH), 1 / 4F, 1),
                    new PortalFace(new Bounds(6 / 16F, 0, 0, 10 / 16F, 0, 1, Direction.DOWN), 1 / 4F, 1),
                    new PortalFace(new Bounds(6 / 16F, 1, 0, 10 / 16F, 1, 1, Direction.UP), 1 / 4F, 1)
            );
            case Y -> throw new IllegalArgumentException("Nether portals only support horizontal axes");
        };
    }

    static Direction pistonSideDirection(Direction facing) {
        return facing.getAxis() == Direction.Axis.Y ? Direction.NORTH : Direction.UP;
    }

    static Bounds pistonBaseConnectionBounds(Direction facing) {
        float plane = facing.getAxisDirection() == Direction.AxisDirection.NEGATIVE ? 4 / 16F : 12 / 16F;
        return switch (facing.getAxis()) {
            case X -> new Bounds(plane, 0, 0, plane, 1, 1, facing);
            case Y -> new Bounds(0, plane, 0, 1, plane, 1, facing);
            case Z -> new Bounds(0, 0, plane, 1, 1, plane, facing);
        };
    }

    static Bounds pistonHeadConnectionBounds(Direction facing) {
        float plane = facing.getAxisDirection() == Direction.AxisDirection.NEGATIVE ? 20 / 16F : -4 / 16F;
        Direction direction = facing.getOpposite();
        return switch (facing.getAxis()) {
            case X -> new Bounds(plane, 6 / 16F, 6 / 16F, plane, 10 / 16F, 10 / 16F, direction);
            case Y -> new Bounds(6 / 16F, plane, 6 / 16F, 10 / 16F, plane, 10 / 16F, direction);
            case Z -> new Bounds(6 / 16F, 6 / 16F, plane, 10 / 16F, 10 / 16F, plane, direction);
        };
    }

    static Direction doorSourceDirection(Direction connectionDirection) {
        if (connectionDirection.getAxis() != Direction.Axis.Y) {
            throw new IllegalArgumentException("Door connection face must be horizontal");
        }
        return connectionDirection.getOpposite();
    }

    private static Bounds edgeBounds(Direction direction, float minY, float maxY, float minAcross, float maxAcross) {
        return switch (direction) {
            case NORTH -> new Bounds(minAcross, minY, 0, maxAcross, maxY, 0, Direction.NORTH);
            case SOUTH -> new Bounds(minAcross, minY, 1, maxAcross, maxY, 1, Direction.SOUTH);
            case WEST -> new Bounds(0, minY, minAcross, 0, maxY, maxAcross, Direction.WEST);
            case EAST -> new Bounds(1, minY, minAcross, 1, maxY, maxAcross, Direction.EAST);
            default -> throw new IllegalArgumentException("Connection direction must be horizontal");
        };
    }

    private static void addIfMissing(
            List<BakedQuad> result, List<BakedQuad> existing, BakedQuad reference, Bounds bounds
    ) {
        if (reference != null && existing.stream().noneMatch(quad -> covers(quad, bounds))) {
            result.add(bake(bounds, reference));
        }
    }

    private static void addIfUncovered(
            List<BakedQuad> result, List<BakedQuad> existing, BakedQuad reference, Bounds bounds
    ) {
        if (reference != null && existing.stream().noneMatch(quad -> covers(quad, bounds))) {
            result.add(bake(bounds, reference));
        }
    }

    private static void addCroppedIfMissing(
            List<BakedQuad> result, List<BakedQuad> existing, BakedQuad reference, PortalFace face
    ) {
        if (existing.stream().noneMatch(quad -> covers(quad, face.bounds))) {
            result.add(bakeCenteredCrop(face.bounds, reference, face.uScale, face.vScale));
        }
    }

    private static boolean covers(BakedQuad quad, Bounds bounds) {
        if (quad.direction() != bounds.direction) return false;
        float minX = 1, minY = 1, minZ = 1, maxX = 0, maxY = 0, maxZ = 0;
        for (int vertex = 0; vertex < BakedQuad.VERTEX_COUNT; vertex++) {
            Vector3fc position = quad.position(vertex);
            minX = Math.min(minX, position.x());
            minY = Math.min(minY, position.y());
            minZ = Math.min(minZ, position.z());
            maxX = Math.max(maxX, position.x());
            maxY = Math.max(maxY, position.y());
            maxZ = Math.max(maxZ, position.z());
        }
        return close(minX, bounds.minX) && close(minY, bounds.minY) && close(minZ, bounds.minZ)
                && close(maxX, bounds.maxX) && close(maxY, bounds.maxY) && close(maxZ, bounds.maxZ);
    }

    private static BakedQuad bake(Bounds bounds, BakedQuad reference) {
        Vector3f from = new Vector3f(bounds.minX, bounds.minY, bounds.minZ);
        Vector3f to = new Vector3f(bounds.maxX, bounds.maxY, bounds.maxZ);
        FaceInfo face = FaceInfo.fromFacing(bounds.direction);
        Vector3fc[] positions = new Vector3fc[4];
        long[] packedUvs = new long[4];
        for (int vertex = 0; vertex < 4; vertex++) {
            positions[vertex] = face.getVertexInfo(vertex).select(from, to);
            packedUvs[vertex] = reference.packedUV(vertex);
        }
        return new BakedQuad(
                positions[0], positions[1], positions[2], positions[3],
                packedUvs[0], packedUvs[1], packedUvs[2], packedUvs[3],
                bounds.direction, reference.materialInfo()
        );
    }

    private static BakedQuad bakeCenterCrop(Bounds bounds, BakedQuad reference) {
        return bakeCenteredCrop(bounds, reference, 1 / 4F, 1 / 4F);
    }

    private static BakedQuad bakeCenteredCrop(
            Bounds bounds, BakedQuad reference, float uScale, float vScale
    ) {
        Vector3f from = new Vector3f(bounds.minX, bounds.minY, bounds.minZ);
        Vector3f to = new Vector3f(bounds.maxX, bounds.maxY, bounds.maxZ);
        FaceInfo face = FaceInfo.fromFacing(bounds.direction);
        Vector3fc[] positions = new Vector3fc[4];
        long[] packedUvs = new long[4];
        float centerU = 0;
        float centerV = 0;
        for (int vertex = 0; vertex < 4; vertex++) {
            centerU += UVPair.unpackU(reference.packedUV(vertex));
            centerV += UVPair.unpackV(reference.packedUV(vertex));
        }
        centerU /= 4;
        centerV /= 4;
        for (int vertex = 0; vertex < 4; vertex++) {
            positions[vertex] = face.getVertexInfo(vertex).select(from, to);
            packedUvs[vertex] = centerCropUv(reference.packedUV(vertex), centerU, centerV, uScale, vScale);
        }
        return new BakedQuad(
                positions[0], positions[1], positions[2], positions[3],
                packedUvs[0], packedUvs[1], packedUvs[2], packedUvs[3],
                bounds.direction, reference.materialInfo()
        );
    }

    static long centerCropUv(long packedUv, float centerU, float centerV) {
        return centerCropUv(packedUv, centerU, centerV, 1 / 4F, 1 / 4F);
    }

    static long centerCropUv(long packedUv, float centerU, float centerV, float uScale, float vScale) {
        float u = UVPair.unpackU(packedUv);
        float v = UVPair.unpackV(packedUv);
        return UVPair.pack(centerU + (u - centerU) * uScale, centerV + (v - centerV) * vScale);
    }

    private static BakedQuad bestReference(List<BakedQuad> quads, Direction direction) {
        return quads.stream().filter(quad -> quad.direction() == direction)
                .max(java.util.Comparator.comparingDouble(ConnectedTextureModelFixer::area)).orElse(null);
    }

    private static BakedQuad exactReference(
            List<BakedQuad> quads, Bounds source, Direction fallbackDirection
    ) {
        return quads.stream().filter(quad -> covers(quad, source)).findFirst()
                .orElseGet(() -> bestReference(quads, fallbackDirection));
    }

    private static double area(BakedQuad quad) {
        Vector3fc first = quad.position(0);
        Vector3fc second = quad.position(1);
        Vector3fc fourth = quad.position(3);
        return new Vector3f(second).sub(first).cross(new Vector3f(fourth).sub(first)).length();
    }

    private static List<BlockStateModelPart> collectParts(BlockStateModel model) {
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(RandomSource.create(0), parts);
        return parts;
    }

    private static List<BakedQuad> collectQuads(List<BlockStateModelPart> parts) {
        List<BakedQuad> quads = new ArrayList<>();
        for (BlockStateModelPart part : parts) {
            quads.addAll(part.getQuads(null));
            for (Direction direction : Direction.values()) quads.addAll(part.getQuads(direction));
        }
        return quads;
    }

    private static List<BakedQuad> collectUnculledQuads(List<BlockStateModelPart> parts) {
        List<BakedQuad> quads = new ArrayList<>();
        for (BlockStateModelPart part : parts) {
            quads.addAll(part.getQuads(null));
        }
        return quads;
    }

    private static boolean ambientOcclusion(List<BlockStateModelPart> parts, BakedQuad reference) {
        for (BlockStateModelPart part : parts) {
            if (part.getQuads(null).contains(reference)) return part.useAmbientOcclusion();
            for (Direction direction : Direction.values()) {
                if (part.getQuads(direction).contains(reference)) return part.useAmbientOcclusion();
            }
        }
        return true;
    }

    private static boolean close(float first, float second) {
        return Math.abs(first - second) <= EPSILON;
    }

    record Bounds(
            float minX, float minY, float minZ, float maxX, float maxY, float maxZ, Direction direction
    ) {
    }

    record PortalFace(Bounds bounds, float uScale, float vScale) {
    }

    private record WrappedModel(
            BlockStateModel original, BlockStateModelPart addition, ConnectionFixKind kind
    ) implements BlockStateModel {
        @Override
        public void collectParts(RandomSource random, List<BlockStateModelPart> output) {
            original.collectParts(random, output);
            if (ConnectedTextureRuntime.enabled(kind)) output.add(addition);
        }

        @Override
        public net.minecraft.client.resources.model.sprite.Material.Baked particleMaterial() {
            return original.particleMaterial();
        }

        @Override
        public int materialFlags() {
            if (!ConnectedTextureRuntime.enabled(kind)) {
                return original.materialFlags();
            }
            return original.materialFlags() | addition.materialFlags();
        }
    }

    private static final class AddedPart implements BlockStateModelPart {
        private final List<BakedQuad> quads;
        private final BlockStateModel original;
        private final boolean ambientOcclusion;

        private AddedPart(
                List<BakedQuad> quads, BlockStateModel original, boolean ambientOcclusion
        ) {
            this.quads = List.copyOf(quads);
            this.original = original;
            this.ambientOcclusion = ambientOcclusion;
        }

        @Override
        public List<BakedQuad> getQuads(Direction direction) {
            return direction == null ? quads : List.of();
        }

        @Override
        public boolean useAmbientOcclusion() {
            return ambientOcclusion;
        }

        @Override
        public net.minecraft.client.resources.model.sprite.Material.Baked particleMaterial() {
            return original.particleMaterial();
        }

        @Override
        public int materialFlags() {
            return quads.stream().map(BakedQuad::materialInfo).mapToInt(BakedQuad.MaterialInfo::flags)
                    .reduce(0, (left, right) -> left | right);
        }
    }

    private static final class DeferredBedHeadPart implements BlockStateModelPart {
        private final BedFootKey key;
        private final Bounds bounds;
        private final BlockStateModel original;
        private final boolean ambientOcclusion;

        private DeferredBedHeadPart(
                BedFootKey key, Bounds bounds, BlockStateModel original, boolean ambientOcclusion
        ) {
            this.key = key;
            this.bounds = bounds;
            this.original = original;
            this.ambientOcclusion = ambientOcclusion;
        }

        @Override
        public List<BakedQuad> getQuads(Direction direction) {
            if (direction != null) return List.of();
            BakedQuad reference = BED_FOOT_FACES.get(key);
            return reference == null ? List.of() : List.of(bake(bounds, reference));
        }

        @Override
        public boolean useAmbientOcclusion() {
            return ambientOcclusion;
        }

        @Override
        public net.minecraft.client.resources.model.sprite.Material.Baked particleMaterial() {
            return original.particleMaterial();
        }

        @Override
        public int materialFlags() {
            BakedQuad reference = BED_FOOT_FACES.get(key);
            return reference == null ? 0 : reference.materialInfo().flags();
        }
    }

    private record BedFootKey(Block block, Direction facing) {
    }
}
