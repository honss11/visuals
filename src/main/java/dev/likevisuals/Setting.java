package dev.likevisuals;

import java.awt.Color;
import java.util.Locale;
import java.util.function.BooleanSupplier;

public class Setting {
    public enum Kind { BOOL, NUM, MODE, COLOR, HEADER }
    public final String name;
    public final Kind kind;
    public boolean b;
    public double v, min, max, step = 1;
    public String[] modes;
    public int mode;
    public BooleanSupplier visible = () -> true;

    private Setting(String n, Kind k) { name = n; kind = k; }

    public static Setting bool(String n, boolean d) { Setting s = new Setting(n, Kind.BOOL); s.b = d; return s; }
    public static Setting num(String n, double d, double mi, double ma, double st) { Setting s = new Setting(n, Kind.NUM); s.v = d; s.min = mi; s.max = ma; s.step = st; return s; }
    public static Setting mode(String n, int d, String... m) { Setting s = new Setting(n, Kind.MODE); s.mode = d; s.modes = m; return s; }
    public static Setting color(String n, double hue) { Setting s = new Setting(n, Kind.COLOR); s.v = hue; s.min = 0; s.max = 360; s.step = 1; return s; }
    public static Setting header(String n) { return new Setting(n, Kind.HEADER); }

    public Setting showIf(BooleanSupplier c) { visible = c; return this; }

    public boolean on() { return b; }
    public double d() { return v; }
    public float f() { return (float) v; }
    public int i() { return (int) Math.round(v); }
    public int m() { return mode; }
    public boolean rainbow() { return kind == Kind.COLOR && v >= 359.5; }

    /** ARGB (opaque) of a COLOR setting. offset shifts the hue for rainbow gradients. */
    public int rgb(int offset) {
        float h = rainbow() ? ((System.currentTimeMillis() / 18 + offset * 20L) % 360) / 360f : (float) (v / 360.0);
        return 0xFF000000 | (Color.HSBtoRGB(h, 0.62f, 1f) & 0xFFFFFF);
    }
    public int rgb() { return rgb(0); }

    public String valueText() {
        switch (kind) {
            case BOOL: return b ? "ВКЛ" : "ВЫКЛ";
            case MODE: return modes[mode];
            case COLOR: return rainbow() ? "Радуга" : (int) v + "°";
            default: return step >= 1 ? String.valueOf(i()) : String.format(Locale.ROOT, "%.2f", v);
        }
    }
    public void cycle(int dir) { if (modes != null) mode = (mode + dir + modes.length) % modes.length; }
}
