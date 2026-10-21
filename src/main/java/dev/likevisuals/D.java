package dev.likevisuals;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class D {
    public static final Identifier FONT = Identifier.of("likevisuals", "sakura");

    public static Text t(String s) { return Text.literal(s).setStyle(Style.EMPTY.withFont(FONT)); }

    public static int a(int c, float m) {
        int al = (int) (((c >>> 24) & 255) * Math.max(0, Math.min(1, m)));
        return (Math.max(al, 6) << 24) | (c & 0xFFFFFF);
    }

    private static int inset(int r, int i) {
        double dy = r - i - 0.5;
        return r - (int) Math.round(Math.sqrt(Math.max(0, r * r - dy * dy)));
    }

    public static void rrect(DrawContext c, int x, int y, int w, int h, int r, int col) { rrectV(c, x, y, w, h, r, col, col); }

    public static void rrectV(DrawContext c, int x, int y, int w, int h, int r, int top, int bot) {
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        for (int i = 0; i < h; i++) {
            int in = (i < r) ? inset(r, i) : (i >= h - r) ? inset(r, h - 1 - i) : 0;
            int col = top == bot ? top : U.lerpColor(top, bot, h <= 1 ? 0 : i / (float) (h - 1));
            c.fill(x + in, y + i, x + w - in, y + i + 1, col);
        }
    }

    public static void circle(DrawContext c, int cx, int cy, int r, int col) { rrect(c, cx - r, cy - r, r * 2, r * 2, r, col); }

    public static void glow(DrawContext c, int cx, int cy, int radius, int col) {
        for (int k = 0; k < 9; k++) circle(c, cx, cy, (int) (radius * (1 - k / 9f)), a(col, 0.05f));
    }

    public static int text(DrawContext c, String s, int x, int y, int col) {
        Text t = t(s);
        c.drawText(U.mc().textRenderer, t, x, y, col, false);
        return U.mc().textRenderer.getWidth(t);
    }
    public static int w(String s) { return U.mc().textRenderer.getWidth(t(s)); }

    public static void textS(DrawContext c, String s, float x, float y, int col, float sc) {
        var ms = c.getMatrices();
        ms.push(); ms.translate(x, y, 0); ms.scale(sc, sc, 1);
        text(c, s, 0, 0, col);
        ms.pop();
    }

    public static void gradTextS(DrawContext c, String s, float x, float y, int c1, int c2, float sc) {
        var ms = c.getMatrices();
        ms.push(); ms.translate(x, y, 0); ms.scale(sc, sc, 1);
        gradText(c, s, 0, 0, c1, c2);
        ms.pop();
    }

    public static void gradText(DrawContext c, String s, int x, int y, int c1, int c2) {
        int cx = x;
        for (int i = 0; i < s.length(); i++) {
            String ch = String.valueOf(s.charAt(i));
            c.drawText(U.mc().textRenderer, t(ch), cx, y, U.lerpColor(c1, c2, i / (float) Math.max(1, s.length() - 1)), false);
            cx += w(ch);
        }
    }

    public static String clip(String s, int maxW) {
        if (w(s) <= maxW) return s;
        while (s.length() > 1 && w(s + "...") > maxW) s = s.substring(0, s.length() - 1);
        return s + "...";
    }
}
