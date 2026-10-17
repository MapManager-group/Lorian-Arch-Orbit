package com.davidblackcn.lorianarchorbit.client;

/** Two entries form a split landing page; three/four entries form a two-by-two grid. */
record EditorHomeLayout(int left, int top, int columns, int rows, int cardWidth, int cardHeight, int gap) {
    static EditorHomeLayout calculate(int width, int height, int count) {
        if (width < 320 || height < 180 || count < 1) throw new IllegalArgumentException("Invalid editor home bounds");
        int columns = count == 1 ? 1 : 2;
        int rows = (count + columns - 1) / columns;
        int gap = width < 600 || height < 270 ? 10 : 16;
        // Half the previous reference card's width and height; only shrink further to fit small windows.
        int cardWidth = Math.min(176, (width - 24 - gap * (columns - 1)) / columns);
        int cardHeight = Math.min(163, (height - 48 - gap * (rows - 1)) / rows);
        int gridWidth = cardWidth * columns + gap * (columns - 1);
        int gridHeight = cardHeight * rows + gap * (rows - 1);
        return new EditorHomeLayout((width - gridWidth) / 2, 32 + (height - 48 - gridHeight) / 2,
                columns, rows, cardWidth, cardHeight, gap);
    }
    int x(int index) { return left + index % columns * (cardWidth + gap); }
    int y(int index) { return top + index / columns * (cardHeight + gap); }
}
