package com.davidblackcn.lorianarchorbit.client;

/** Pure geometry: framebuffer pixels -> vanilla GUI units -> palette canvas units. */
public record PaletteViewport(int width, int height, double pixelScale, double guiScale,
                              double offsetX, double offsetY) {
    public static final int REFERENCE_WIDTH = 768;
    public static final int REFERENCE_HEIGHT = 408;

    public static PaletteViewport calculate(int pixelWidth, int pixelHeight, double guiScale) {
        if (pixelWidth <= 0 || pixelHeight <= 0 || !Double.isFinite(guiScale) || guiScale <= 0) {
            throw new IllegalArgumentException("positive viewport dimensions and GUI scale required");
        }
        double preferred = Math.min(pixelWidth / 768.0, pixelHeight / 408.0);
        double readable = Math.min(1.5, Math.min(pixelWidth / 320.0, pixelHeight / 180.0));
        double scale = Math.max(preferred, readable);
        int width = Math.max(320, Math.min(REFERENCE_WIDTH, (int) Math.floor(pixelWidth / scale + 1e-9)));
        int height = Math.max(180, Math.min(REFERENCE_HEIGHT, (int) Math.floor(pixelHeight / scale + 1e-9)));
        return new PaletteViewport(width, height, scale, guiScale,
                (pixelWidth - width * scale) / (2 * guiScale),
                (pixelHeight - height * scale) / (2 * guiScale));
    }

    public double scale() { return pixelScale / guiScale; }
    public double localX(double x) { return (x - offsetX) / scale(); }
    public double localY(double y) { return (y - offsetY) / scale(); }
    public double guiX(double x) { return offsetX + x * scale(); }
    public double guiY(double y) { return offsetY + y * scale(); }
    // MouseHandler uses ceil(framebuffer / GUI scale), while GuiRenderer projects the exact quotient.
    private double mouseRatioX() {
        double extent = width * scale() + offsetX * 2;
        return extent / Math.ceil(extent - 1e-9);
    }
    private double mouseRatioY() {
        double extent = height * scale() + offsetY * 2;
        return extent / Math.ceil(extent - 1e-9);
    }
    public double mouseX(double x) { return localX(x * mouseRatioX()); }
    public double mouseY(double y) { return localY(y * mouseRatioY()); }
    public double mouseDeltaX(double delta) { return delta * mouseRatioX() / scale(); }
    public double mouseDeltaY(double delta) { return delta * mouseRatioY() / scale(); }
    public boolean containsMouse(double x, double y) {
        return contains(x * mouseRatioX(), y * mouseRatioY());
    }
    public boolean contains(double x, double y) {
        double localX = localX(x);
        double localY = localY(y);
        return localX >= 0 && localY >= 0 && localX < width && localY < height;
    }
}
