package com.davidblackcn.lorianarchorbit.palette.hueblocks;

/** Standard sRGB to OkLAB conversion; all calculations use finite doubles. */
public record HueColor(double l, double a, double b) {
    public HueColor {
        if (!Double.isFinite(l) || !Double.isFinite(a) || !Double.isFinite(b)) {
            throw new IllegalArgumentException("Non-finite color");
        }
    }

    public static HueColor fromRgb(int rgb) {
        double r = linear((rgb >> 16) & 255);
        double g = linear((rgb >> 8) & 255);
        double b = linear(rgb & 255);
        double x = Math.cbrt(.4122214708 * r + .5363325363 * g + .0514459929 * b);
        double y = Math.cbrt(.2119034982 * r + .6806995451 * g + .1073969566 * b);
        double z = Math.cbrt(.0883024619 * r + .2817188376 * g + .6299787005 * b);
        return new HueColor(.2104542553 * x + .7936177850 * y - .0040720468 * z,
                1.9779984951 * x - 2.4285922050 * y + .4505937099 * z,
                .0259040371 * x + .7827717662 * y - .8086757660 * z);
    }

    private static double linear(int channel) {
        double c = channel / 255.0;
        return c <= .04045 ? c / 12.92 : Math.pow((c + .055) / 1.055, 2.4);
    }

    public HueColor mix(HueColor other, double t) {
        return new HueColor(l + (other.l - l) * t, a + (other.a - a) * t, b + (other.b - b) * t);
    }

    public double distanceSquared(HueColor other) {
        return Math.pow(l - other.l, 2) + Math.pow(a - other.a, 2) + Math.pow(b - other.b, 2);
    }
}
