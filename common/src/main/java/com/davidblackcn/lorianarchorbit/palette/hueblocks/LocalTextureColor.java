package com.davidblackcn.lorianarchorbit.palette.hueblocks;

import com.google.gson.JsonObject;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;

/** Whole-sprite, alpha-weighted linear sRGB mean. No background or biome is assumed. */
public final class LocalTextureColor {
    public static final int ALGORITHM = 1;
    public record Color(int rgb, boolean opaque) { }
    private record Frame(int index, int time) { }
    private LocalTextureColor() { }

    public static Color calculate(byte[] png, JsonObject animation) throws IOException {
        // Inspect dimensions before allocating a decoded image; high-resolution packs are bounded.
        try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(png))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IOException("Unsupported texture image");
            var reader = readers.next();
            try {
                reader.setInput(input);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width < 1 || height < 1 || (long) width * height > 16_777_216)
                    throw new IOException("Texture exceeds 16 megapixels");
                return calculate(reader.read(0), animation);
            } finally { reader.dispose(); }
        }
    }

    static Color calculate(BufferedImage image, JsonObject animation) {
        int width = image.getWidth(), height = image.getHeight(), fw = width, fh = height;
        List<Frame> frames = new ArrayList<>();
        if (animation != null) {
            boolean hasWidth = animation.has("width"), hasHeight = animation.has("height");
            fw = hasWidth ? animation.get("width").getAsInt() : hasHeight ? width : Math.min(width, height);
            fh = hasHeight ? animation.get("height").getAsInt() : hasWidth ? height : Math.min(width, height);
            if (fw < 1 || fh < 1 || width % fw != 0 || height % fh != 0)
                throw new IllegalArgumentException("Invalid animation frame dimensions");
            int count = (width / fw) * (height / fh);
            int time = animation.has("frametime") ? animation.get("frametime").getAsInt() : 1;
            if (time < 1) throw new IllegalArgumentException("Invalid animation duration");
            if (animation.has("frames")) {
                if (animation.getAsJsonArray("frames").size() > 10000) throw new IllegalArgumentException("Too many animation frames");
                for (var entry : animation.getAsJsonArray("frames")) {
                    int index = entry.isJsonObject() ? entry.getAsJsonObject().get("index").getAsInt() : entry.getAsInt();
                    int duration = entry.isJsonObject() && entry.getAsJsonObject().has("time")
                            ? entry.getAsJsonObject().get("time").getAsInt() : time;
                    if (index < 0 || index >= count || duration < 1) throw new IllegalArgumentException("Invalid animation frame");
                    frames.add(new Frame(index, duration));
                }
            } else for (int i = 0; i < count; i++) frames.add(new Frame(i, time));
        } else frames.add(new Frame(0, 1));
        if (frames.isEmpty()) throw new IllegalArgumentException("Empty animation");
        double red = 0, green = 0, blue = 0, weight = 0;
        boolean opaque = true;
        // Aggregate repeat frames before visiting pixels (including animation time weighting).
        var durations = new java.util.TreeMap<Integer, Long>();
        for (var frame : frames) durations.merge(frame.index(), (long) frame.time(), Long::sum);
        for (var frame : durations.entrySet()) {
            int ox = frame.getKey() % (width / fw) * fw, oy = frame.getKey() / (width / fw) * fh;
            for (int y = 0; y < fh; y++) for (int x = 0; x < fw; x++) {
                int pixel = image.getRGB(ox + x, oy + y), alpha = pixel >>> 24;
                opaque &= alpha == 255;
                double w = alpha / 255.0 * frame.getValue();
                red += linear((pixel >> 16) & 255) * w;
                green += linear((pixel >> 8) & 255) * w;
                blue += linear(pixel & 255) * w; weight += w;
            }
        }
        if (weight == 0) throw new IllegalArgumentException("Fully transparent texture");
        return new Color(channel(red / weight) << 16 | channel(green / weight) << 8 | channel(blue / weight), opaque);
    }
    private static double linear(int channel) {
        double value = channel / 255.0;
        return value <= .04045 ? value / 12.92 : Math.pow((value + .055) / 1.055, 2.4);
    }
    private static int channel(double value) {
        return (int) Math.round(255 * (value <= .0031308 ? value * 12.92 : 1.055 * Math.pow(value, 1 / 2.4) - .055));
    }
}
