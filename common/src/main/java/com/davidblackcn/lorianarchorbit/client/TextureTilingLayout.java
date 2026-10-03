package com.davidblackcn.lorianarchorbit.client;

/** Only visible tiles are submitted, even for the 1906-sample maximum gradient. */
record TextureTilingLayout(int first, int last, int firstCross, int lastCross, int offset, int crossOffset) {
    static TextureTilingLayout calculate(int count, int cell, int extent, int crossExtent, int offset, int crossOffset) {
        int along = Math.clamp(offset, 0, Math.max(0, count * cell - extent));
        int across = Math.clamp(crossOffset, 0, Math.max(0, 4 * cell - crossExtent));
        return new TextureTilingLayout(along / cell, Math.min(count, (along + extent + cell - 1) / cell),
                across / cell, Math.min(4, (across + crossExtent + cell - 1) / cell), along, across);
    }
}
