package dev.likevisuals;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.RotationAxis;

import java.util.Random;

/** Procedural sakura scenery used by the title screen and the click GUI. */
public final class Sakura {
    public static final int[] PINKS = {0xFFFFB7D5, 0xFFFF8FBF, 0xFFFFD6E7, 0xFFFF9EC4, 0xFFFFC9DE};

    public static float time() { return (System.currentTimeMillis() % 3_600_000L) / 1000f; }

    // ---------------------------------------------------------------- petals
    public static void petals(DrawContext c, int w, int h, int count, float alpha) {
        float t = time();
        for (int i = 0; i < count; i++) {
            float sp = 16 + (i * 13 % 24), ph = (i * 97 % 1000) / 1000f, x0 = (i * 61 % 1000) / 1000f * w;
            float y = ((t * sp + ph * (h + 40)) % (h + 40)) - 20;
            float x = (float) ((x0 + Math.sin(t * 0.7 + i) * 26 + t * 9) % w);
            if (x < 0) x += w;
            float rot = t * (0.6f + (i % 5) * 0.25f) + i, s = 3.2f + (i % 4) * 1.1f, flip = (float) Math.cos(t * 1.3 + i);
            petal(c, x, y, s, rot, flip, D.a(PINKS[i % PINKS.length], alpha * (0.55f + (i % 3) * 0.2f)));
        }
    }

    public static void petal(DrawContext c, float x, float y, float s, float rot, float flip, int col) {
        var ms = c.getMatrices();
        ms.push();
        ms.translate(x, y, 0);
        ms.multiply(RotationAxis.POSITIVE_Z.rotation(rot));
        ms.scale(Math.max(0.25f, Math.abs(flip)), 1f, 1f);
        D.rrect(c, (int) -s, (int) (-s * 0.55f), (int) (s * 2), (int) (s * 1.1f) + 1, (int) (s * 0.55f), col);
        ms.pop();
    }

    // ---------------------------------------------------------------- flowers / branches
    public static void flower(DrawContext c, int cx, int cy, int r, int col) {
        for (int i = 0; i < 5; i++) {
            double a = i * Math.PI * 2 / 5 - Math.PI / 2;
            D.circle(c, cx + (int) (Math.cos(a) * r * 0.62), cy + (int) (Math.sin(a) * r * 0.62), Math.max(1, (int) (r * 0.48)), col);
        }
        D.circle(c, cx, cy, Math.max(1, (int) (r * 0.28)), 0xFFFFE08A);
    }

    public static void cluster(DrawContext c, int cx, int cy, int radius, long seed, float alpha) {
        Random rnd = new Random(seed);
        float t = time();
        int sx = (int) (Math.sin(t * 0.8 + seed) * 2), sy = (int) (Math.cos(t * 0.6 + seed) * 1.5);
        for (int i = 0; i < 16; i++) {
            double a = rnd.nextDouble() * Math.PI * 2, d = Math.sqrt(rnd.nextDouble()) * radius;
            int r = 4 + rnd.nextInt(Math.max(1, radius / 3));
            D.circle(c, cx + sx + (int) (Math.cos(a) * d), cy + sy + (int) (Math.sin(a) * d), r, D.a(PINKS[rnd.nextInt(PINKS.length)], alpha * 0.92f));
        }
        for (int i = 0; i < 4; i++) {
            double a = rnd.nextDouble() * Math.PI * 2, d = rnd.nextDouble() * radius * 0.8;
            flower(c, cx + sx + (int) (Math.cos(a) * d), cy + sy + (int) (Math.sin(a) * d), 5 + rnd.nextInt(3), D.a(0xFFFFE6F0, alpha));
        }
    }

    public static void branch(DrawContext c, float x0, float y0, float x1, float y1, float bend, float th0, float th1, int col) {
        int n = (int) Math.max(12, Math.hypot(x1 - x0, y1 - y0) / 3);
        for (int i = 0; i <= n; i++) {
            float t = i / (float) n;
            float x = x0 + (x1 - x0) * t, y = y0 + (y1 - y0) * t + (float) Math.sin(t * Math.PI) * bend;
            D.circle(c, (int) x, (int) y, Math.max(1, (int) (th0 + (th1 - th0) * t)), col);
        }
    }

    // ---------------------------------------------------------------- scene pieces
    public static void sky(DrawContext c, int w, int h) {
        int mid = (int) (h * 0.62);
        c.fillGradient(0, 0, w, mid, 0xFF24153F, 0xFFC4679F);
        c.fillGradient(0, mid, w, h, 0xFFC4679F, 0xFFFFB9B0);
        float t = time();
        for (int i = 0; i < 50; i++) {
            int x = (i * 733) % Math.max(1, w), y = (i * 271) % Math.max(1, (int) (h * 0.42));
            float tw = (float) (Math.sin(t * 1.5 + i * 1.7) * 0.5 + 0.5);
            int sz = i % 3 == 0 ? 2 : 1;
            c.fill(x, y, x + sz, y + sz, D.a(0xFFFFFFFF, 0.25f + 0.55f * tw));
        }
        int mx = (int) (w * 0.72), my = (int) (h * 0.27);
        D.glow(c, mx, my, 150, 0xFFFFB7D5);
        D.circle(c, mx, my, 38, 0xFFFFF3F8);
        D.circle(c, mx - 9, my - 6, 8, 0x22E8B8D0);
        D.circle(c, mx + 12, my + 9, 6, 0x22E8B8D0);
    }

    public static void mountains(DrawContext c, int w, int h) {
        int cx = (int) (w * 0.28), by = (int) (h * 0.76), ht = (int) (h * 0.32), hw = (int) (h * 0.46);
        for (int x = Math.max(0, cx - hw); x < Math.min(w, cx + hw); x += 2) {
            float dx = Math.abs(x - cx) / (float) hw;
            int y = by - (int) (ht * Math.pow(1 - dx, 1.25));
            c.fill(x, y, x + 2, by, 0xFF7B4A8E);
            c.fill(x, y, x + 2, y + (int) (ht * 0.2 * (1 - dx) + 2), 0xFFFBE4EE);
        }
        int[] cols = {0xFF5A3470, 0xFF442658, 0xFF2F1840};
        float[] base = {0.72f, 0.79f, 0.87f}, amp = {26, 22, 16};
        for (int l = 0; l < 3; l++)
            for (int x = 0; x < w; x += 3) {
                int y = (int) (h * base[l] + Math.sin(x * 0.006 * (1 + l * 0.6) + l * 2.1) * amp[l] + Math.sin(x * 0.017 + l) * amp[l] * 0.35);
                c.fill(x, y, x + 3, h, cols[l]);
                c.fill(x, y, x + 3, y + 2, D.a(0xFFFF9EC4, 0.25f));
            }
    }

    public static void trees(DrawContext c, int w, int h) {
        int trunk = 0xFF2A1428;
        branch(c, w * 0.05f, h + 10, w * 0.085f, h * 0.52f, 14, 15, 7, trunk);
        branch(c, w * 0.085f, h * 0.52f, w * 0.30f, h * 0.36f, -26, 6, 2, trunk);
        branch(c, w * 0.10f, h * 0.60f, w * 0.24f, h * 0.55f, 10, 5, 2, trunk);
        branch(c, w * 0.085f, h * 0.50f, w * 0.17f, h * 0.24f, 18, 5, 2, trunk);
        branch(c, w + 10, h * 0.10f, w * 0.70f, h * 0.20f, 18, 8, 2, trunk);
        branch(c, w * 0.86f, h * 0.14f, w * 0.80f, h * 0.36f, -10, 4, 1, trunk);
        cluster(c, (int) (w * 0.30), (int) (h * 0.36), 46, 11, 1f);
        cluster(c, (int) (w * 0.23), (int) (h * 0.55), 38, 12, 1f);
        cluster(c, (int) (w * 0.17), (int) (h * 0.24), 44, 13, 1f);
        cluster(c, (int) (w * 0.09), (int) (h * 0.42), 40, 14, 1f);
        cluster(c, (int) (w * 0.10), (int) (h * 0.64), 34, 15, 1f);
        cluster(c, (int) (w * 0.70), (int) (h * 0.20), 48, 16, 1f);
        cluster(c, (int) (w * 0.80), (int) (h * 0.36), 36, 17, 1f);
        cluster(c, (int) (w * 0.90), (int) (h * 0.10), 42, 18, 1f);
        cluster(c, (int) (w * 0.78), (int) (h * 0.14), 34, 19, 1f);
    }

    /** Full title-screen scenery. */
    public static void scene(DrawContext c, int w, int h) {
        sky(c, w, h);
        mountains(c, w, h);
        trees(c, w, h);
        petals(c, w, h, 90, 1f);
        c.fillGradient(0, 0, w, h / 4, 0x66100818, 0x00000000);
        c.fillGradient(0, h * 3 / 4, w, h, 0x00000000, 0x77100818);
    }

    /** Light decoration behind the in-game GUI. */
    public static void overlay(DrawContext c, int w, int h, float a) {
        petals(c, w, h, 55, a);
        cluster(c, 20, 10, 60, 31, a * 0.9f);
        cluster(c, w - 16, h - 6, 60, 32, a * 0.9f);
    }
}
