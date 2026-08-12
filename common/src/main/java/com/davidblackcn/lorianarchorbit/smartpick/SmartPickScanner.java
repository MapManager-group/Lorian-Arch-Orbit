package com.davidblackcn.lorianarchorbit.smartpick;

import com.davidblackcn.lorianarchorbit.config.SmartPickMode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.ToIntFunction;

public final class SmartPickScanner {
    private static final List<Offset> ADJACENT_OFFSETS = buildAdjacentOffsets();

    private SmartPickScanner() {
    }

    public static <T, K> SmartPickScanResult<T> scan(
            SmartPickMode mode,
            int radius,
            int contextLimit,
            SmartPickDirection targetFace,
            SmartPickSampleSource<T> source,
            Function<T, K> identity,
            Function<T, String> registryId,
            ToIntFunction<T> historyWeight
    ) {
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(targetFace, "targetFace");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(identity, "identity");
        Objects.requireNonNull(registryId, "registryId");
        Objects.requireNonNull(historyWeight, "historyWeight");
        if (radius < 1 || radius > 3) {
            throw new IllegalArgumentException("radius must be between 1 and 3");
        }
        if (contextLimit < 1) {
            throw new IllegalArgumentException("contextLimit must be positive");
        }

        long started = System.nanoTime();
        Map<K, MutableCandidate<T>> candidates = new LinkedHashMap<>();
        Counters counters = new Counters();
        if (mode == SmartPickMode.ADJACENT) {
            accept(source.sample(0, 0, 0), new Offset(0, 0, 0), false,
                    candidates, counters, identity, registryId);
            for (Offset offset : ADJACENT_OFFSETS) {
                accept(source.sample(offset.x, offset.y, offset.z), offset, true,
                        candidates, counters, identity, registryId);
            }
        } else {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    for (int x = -radius; x <= radius; x++) {
                        Offset offset = new Offset(x, y, z);
                        accept(source.sample(x, y, z), offset, true,
                                candidates, counters, identity, registryId);
                    }
                }
            }
        }

        boolean contextual = mode == SmartPickMode.CONTEXT;
        List<SmartPickCandidate<T>> ordered = candidates.values().stream().map(candidate -> {
            int score = contextual ? score(candidate, targetFace, historyWeight.applyAsInt(candidate.value)) : 0;
            return new SmartPickCandidate<>(
                    candidate.value, candidate.registryId, candidate.occurrences,
                    candidate.nearestDistanceSquared, score, candidate.center
            );
        }).sorted(order(contextual)).toList();
        if (contextual && ordered.size() > contextLimit) {
            ordered = List.copyOf(ordered.subList(0, contextLimit));
        }
        return new SmartPickScanResult<>(ordered, new SmartPickScanStats(
                counters.visited,
                counters.loaded,
                counters.valid,
                candidates.size(),
                contextual ? candidates.size() : 0,
                System.nanoTime() - started
        ));
    }

    public static List<int[]> adjacentOffsets() {
        return ADJACENT_OFFSETS.stream().map(offset -> new int[]{offset.x, offset.y, offset.z}).toList();
    }

    private static <T, K> void accept(
            SmartPickSample<T> sample,
            Offset offset,
            boolean countVisit,
            Map<K, MutableCandidate<T>> candidates,
            Counters counters,
            Function<T, K> identity,
            Function<T, String> registryId
    ) {
        if (countVisit) {
            counters.visited++;
        }
        if (!sample.loaded()) {
            return;
        }
        if (countVisit) {
            counters.loaded++;
        }
        if (sample.value().isEmpty()) {
            return;
        }
        if (countVisit) {
            counters.valid++;
        }
        T value = sample.value().get();
        K key = Objects.requireNonNull(identity.apply(value), "candidate identity");
        MutableCandidate<T> candidate = candidates.computeIfAbsent(key,
                ignored -> new MutableCandidate<>(value, Objects.requireNonNull(registryId.apply(value), "registry id")));
        candidate.add(offset);
    }

    private static int score(
            MutableCandidate<?> candidate,
            SmartPickDirection face,
            int historyWeight
    ) {
        int score = candidate.occurrences * 100;
        score += Math.max(0, 60 - candidate.nearestDistanceSquared * 8);
        if (candidate.sameSurface(face)) {
            score += 45;
        }
        if (candidate.front(face)) {
            score += 20;
        }
        if (candidate.behindOnly(face)) {
            score -= 140 * candidate.minimumBehindDepth(face);
        }
        if (candidate.contact) {
            score += 25;
        }
        if (candidate.edge) {
            score += 15;
        }
        score += Math.max(0, historyWeight) * 35;
        return score;
    }

    private static Comparator<SmartPickCandidate<?>> order(boolean contextual) {
        Comparator<SmartPickCandidate<?>> comparator = Comparator
                .comparing(SmartPickCandidate<?>::center).reversed();
        if (contextual) {
            comparator = comparator.thenComparing(
                    Comparator.comparingInt((SmartPickCandidate<?> candidate) -> candidate.score()).reversed()
            );
        }
        return comparator
                .thenComparingInt(SmartPickCandidate::nearestDistanceSquared)
                .thenComparing(Comparator.comparingInt(
                        (SmartPickCandidate<?> candidate) -> candidate.occurrences()).reversed())
                .thenComparing(SmartPickCandidate::registryId);
    }

    private static List<Offset> buildAdjacentOffsets() {
        List<Offset> offsets = new ArrayList<>(18);
        offsets.addAll(List.of(
                new Offset(1, 0, 0), new Offset(-1, 0, 0),
                new Offset(0, 1, 0), new Offset(0, -1, 0),
                new Offset(0, 0, 1), new Offset(0, 0, -1)
        ));
        for (int y = -1; y <= 1; y++) {
            for (int z = -1; z <= 1; z++) {
                for (int x = -1; x <= 1; x++) {
                    int nonZero = (x == 0 ? 0 : 1) + (y == 0 ? 0 : 1) + (z == 0 ? 0 : 1);
                    if (nonZero == 2) {
                        offsets.add(new Offset(x, y, z));
                    }
                }
            }
        }
        return List.copyOf(offsets);
    }

    private record Offset(int x, int y, int z) {
        int distanceSquared() {
            return x * x + y * y + z * z;
        }

        int normalCoordinate(SmartPickDirection direction) {
            return x * direction.x() + y * direction.y() + z * direction.z();
        }
    }

    private static final class MutableCandidate<T> {
        private final T value;
        private final String registryId;
        private final List<Offset> positions = new ArrayList<>();
        private int occurrences;
        private int nearestDistanceSquared = Integer.MAX_VALUE;
        private boolean center;
        private boolean contact;
        private boolean edge;

        private MutableCandidate(T value, String registryId) {
            this.value = value;
            this.registryId = registryId;
        }

        private void add(Offset offset) {
            positions.add(offset);
            occurrences++;
            nearestDistanceSquared = Math.min(nearestDistanceSquared, offset.distanceSquared());
            center |= offset.distanceSquared() == 0;
            int nonZero = (offset.x == 0 ? 0 : 1) + (offset.y == 0 ? 0 : 1) + (offset.z == 0 ? 0 : 1);
            contact |= offset.distanceSquared() == 1;
            edge |= nonZero == 2 && Math.max(Math.abs(offset.x), Math.max(Math.abs(offset.y), Math.abs(offset.z))) == 1;
        }

        private boolean sameSurface(SmartPickDirection direction) {
            return positions.stream().anyMatch(position -> position.normalCoordinate(direction) == 0);
        }

        private boolean front(SmartPickDirection direction) {
            return positions.stream().anyMatch(position -> position.normalCoordinate(direction) > 0);
        }

        private boolean behindOnly(SmartPickDirection direction) {
            return positions.stream().allMatch(position -> position.normalCoordinate(direction) < 0);
        }

        private int minimumBehindDepth(SmartPickDirection direction) {
            return positions.stream().mapToInt(position -> -position.normalCoordinate(direction))
                    .filter(depth -> depth > 0).min().orElse(0);
        }
    }

    private static final class Counters {
        private int visited;
        private int loaded;
        private int valid;
    }
}
