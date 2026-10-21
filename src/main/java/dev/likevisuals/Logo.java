package dev.likevisuals;

import net.minecraft.client.gui.DrawContext;

/** The LikeVisuals logo: a sakura blossom inside a gradient rounded badge. */
public final class Logo {
    public static void draw(DrawContext c, int x, int y, int s) {
        int a1 = ClientMods.accent(0), a2 = ClientMods.accent(1);
        int r = Math.max(3, s / 3);
        D.rrectV(c, x - 1, y - 1, s + 2, s + 2, r + 1, D.a(a1, 0.95f), D.a(a2, 0.95f));
        D.rrectV(c, x, y, s, s, r, 0xFF2B1634, 0xFF170B20);
        int cx = x + s / 2, cy = y + s / 2, fr = Math.max(3, (int) (s * 0.38f));
        // soft glow behind the blossom
        D.circle(c, cx, cy, Math.max(2, (int) (s * 0.42f)), D.a(a1, 0.18f));
        for (int i = 0; i < 5; i++) {
            double a = i * Math.PI * 2 / 5 - Math.PI / 2;
            int px = cx + (int) Math.round(Math.cos(a) * fr * 0.62), py = cy + (int) Math.round(Math.sin(a) * fr * 0.62);
            D.circle(c, px, py, Math.max(1, (int) (fr * 0.5)), U.lerpColor(0xFFFFE6F0, 0xFFFF9EC4, i / 4f));
        }
        D.circle(c, cx, cy, Math.max(1, (int) (fr * 0.26f)), 0xFFFFD86B);
    }
}
