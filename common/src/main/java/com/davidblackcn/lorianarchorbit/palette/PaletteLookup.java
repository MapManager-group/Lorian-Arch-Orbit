package com.davidblackcn.lorianarchorbit.palette;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiPredicate;

public final class PaletteLookup {
    private PaletteLookup() {
    }

    public static <T> Optional<Match<T>> find(
            List<PaletteGroup> groups,
            T candidate,
            BiPredicate<PaletteMember, T> itemMatcher,
            BiPredicate<PaletteMember, T> exactMatcher
    ) {
        Objects.requireNonNull(groups, "groups");
        Objects.requireNonNull(candidate, "candidate");
        Objects.requireNonNull(itemMatcher, "itemMatcher");
        Objects.requireNonNull(exactMatcher, "exactMatcher");
        Optional<Match<T>> exact = findMode(groups, candidate, PaletteMatchMode.EXACT_COMPONENTS, exactMatcher);
        return exact.isPresent() ? exact : findMode(groups, candidate, PaletteMatchMode.ITEM, itemMatcher);
    }

    private static <T> Optional<Match<T>> findMode(
            List<PaletteGroup> groups,
            T candidate,
            PaletteMatchMode mode,
            BiPredicate<PaletteMember, T> matcher
    ) {
        for (int groupIndex = 0; groupIndex < groups.size(); groupIndex++) {
            PaletteGroup group = groups.get(groupIndex);
            for (int memberIndex = 0; memberIndex < group.members().size(); memberIndex++) {
                PaletteMember member = group.members().get(memberIndex);
                if (member.matchMode() == mode && matcher.test(member, candidate)) {
                    return Optional.of(new Match<>(group, groupIndex, memberIndex, candidate));
                }
            }
        }
        return Optional.empty();
    }

    public record Match<T>(PaletteGroup group, int groupIndex, int memberIndex, T candidate) {
    }
}
