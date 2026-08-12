package com.davidblackcn.lorianarchorbit.palette;

public enum PaletteMatchMode {
    ITEM,
    EXACT_COMPONENTS;

    public static PaletteMatchMode parse(String value) {
        return switch (value == null ? "item" : value) {
            case "item" -> ITEM;
            case "exact_components" -> EXACT_COMPONENTS;
            default -> throw new IllegalArgumentException("Unknown palette match mode: " + value);
        };
    }

    public String serializedName() {
        return this == ITEM ? "item" : "exact_components";
    }
}
