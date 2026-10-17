package com.davidblackcn.lorianarchorbit.palette;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.function.IntFunction;
import java.util.function.BiConsumer;

/** Memory-only palette and first-write inventory backup, scoped to the current player instance. */
public final class TemporaryPaletteSession<T> {
    public static final int INVENTORY_SIZE = 36;
    private final UnaryOperator<T> copy;
    private List<String> palette = List.of();
    private final Map<Integer, T> originals = new LinkedHashMap<>();
    private Object inventoryOwner;

    public TemporaryPaletteSession(UnaryOperator<T> copy) {
        this.copy = Objects.requireNonNull(copy);
    }

    public void setPalette(List<String> items) {
        List<String> snapshot = List.copyOf(items);
        if (snapshot.stream().anyMatch(String::isBlank)) throw new IllegalArgumentException("Blank item ID");
        palette = snapshot; // Every sample, including repeated blocks, is a wheel position.
    }

    public List<String> palette() { return palette; }

    public void bindInventory(Object owner) {
        if (owner != inventoryOwner) {
            originals.clear();
            inventoryOwner = owner;
        }
    }

    public void remember(Object owner, int slot, T original) {
        Objects.requireNonNull(owner);
        menuSlot(slot);
        bindInventory(owner);
        if (!originals.containsKey(slot)) originals.put(slot, copy.apply(Objects.requireNonNull(original)));
    }

    public boolean hasBackup(Object owner) {
        bindInventory(owner);
        return !originals.isEmpty();
    }

    public Map<Integer, T> originals(Object owner) {
        bindInventory(owner);
        Map<Integer, T> snapshot = new LinkedHashMap<>();
        originals.forEach((slot, item) -> snapshot.put(slot, copy.apply(item)));
        return Collections.unmodifiableMap(snapshot);
    }

    public void clearBackup() { originals.clear(); }

    public boolean applyInventory(Object owner, List<T> items, IntFunction<T> readSlot, BiConsumer<Integer, T> writeSlot) {
        if (owner == null || items.isEmpty() || items.size() > INVENTORY_SIZE) return false;
        for (int slot = 0; slot < items.size(); slot++) remember(owner, slot, readSlot.apply(slot));
        for (int slot = 0; slot < items.size(); slot++) writeSlot.accept(slot, copy.apply(items.get(slot)));
        return true;
    }

    public boolean restoreInventory(Object owner, BiConsumer<Integer, T> writeSlot) {
        if (!hasBackup(owner)) return false;
        originals(owner).forEach(writeSlot);
        clearBackup();
        return true;
    }

    public void reset() {
        palette = List.of();
        originals.clear();
        inventoryOwner = null;
    }

    /** Inventory indices 0–8 are hotbar slots; creative menu slots 9–35 are the main inventory. */
    public static int menuSlot(int inventorySlot) {
        if (inventorySlot < 0 || inventorySlot >= INVENTORY_SIZE) throw new IllegalArgumentException("Invalid inventory slot");
        return inventorySlot < 9 ? inventorySlot + 36 : inventorySlot;
    }
}
