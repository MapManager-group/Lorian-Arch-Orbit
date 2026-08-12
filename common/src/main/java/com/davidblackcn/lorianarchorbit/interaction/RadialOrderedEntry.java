package com.davidblackcn.lorianarchorbit.interaction;

public record RadialOrderedEntry<T>(T value, int sourceIndex, int relativeIndex, boolean selected) {
}
