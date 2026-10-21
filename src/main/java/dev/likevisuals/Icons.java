package dev.likevisuals;

import net.minecraft.client.gui.DrawContext;

/** 10x10 icons drawn with fills. */
public final class Icons {
    public static final int USER = 0, BOLT = 1, WIFI = 2, PIN = 3, CLOCK = 4, CHECK = 5, CROSS = 6, BELL = 7, HEART = 8, CHEV = 9, DIAMOND = 10, SHIELD = 11, SWAP = 12, SPEED = 13;

    public static void draw(DrawContext c, int t, int x, int y, int col) {
        switch (t) {
            case USER: D.circle(c, x + 5, y + 3, 2, col); D.rrect(c, x + 1, y + 6, 8, 4, 2, col); break;
            case BOLT: c.fill(x + 5, y, x + 7, y + 4, col); c.fill(x + 3, y + 4, x + 8, y + 6, col); c.fill(x + 3, y + 6, x + 5, y + 10, col); break;
            case WIFI: c.fill(x + 1, y + 7, x + 3, y + 10, col); c.fill(x + 4, y + 4, x + 6, y + 10, col); c.fill(x + 7, y + 1, x + 9, y + 10, col); break;
            case PIN: D.circle(c, x + 5, y + 4, 3, col); c.fill(x + 4, y + 6, x + 6, y + 10, col); D.circle(c, x + 5, y + 4, 1, 0xFF0E0E14); break;
            case CLOCK: D.circle(c, x + 5, y + 5, 5, col); D.circle(c, x + 5, y + 5, 3, 0xFF0E0E14); c.fill(x + 5, y + 3, x + 6, y + 6, col); c.fill(x + 5, y + 5, x + 8, y + 6, col); break;
            case CHECK: c.fill(x + 1, y + 5, x + 3, y + 7, col); c.fill(x + 3, y + 7, x + 5, y + 9, col); c.fill(x + 5, y + 5, x + 7, y + 7, col); c.fill(x + 7, y + 3, x + 9, y + 5, col); break;
            case CROSS: for (int i = 1; i <= 8; i++) { c.fill(x + i, y + i, x + i + 2, y + i + 1, col); c.fill(x + 9 - i, y + i, x + 11 - i, y + i + 1, col); } break;
            case BELL: D.rrect(c, x + 2, y + 1, 6, 6, 3, col); c.fill(x + 1, y + 6, x + 9, y + 8, col); D.circle(c, x + 5, y + 9, 1, col); break;
            case HEART:
                D.circle(c, x + 3, y + 3, 2, col); D.circle(c, x + 7, y + 3, 2, col);
                c.fill(x + 1, y + 4, x + 9, y + 5, col); c.fill(x + 2, y + 5, x + 8, y + 6, col); c.fill(x + 3, y + 6, x + 7, y + 7, col); c.fill(x + 4, y + 7, x + 6, y + 8, col);
                break;
            case CHEV: for (int i = 0; i < 4; i++) { c.fill(x + 2 + i, y + 1 + i, x + 4 + i, y + 2 + i, col); c.fill(x + 2 + i, y + 8 - i, x + 4 + i, y + 9 - i, col); } break;
            case DIAMOND:
                c.fill(x + 4, y + 1, x + 6, y + 3, col); c.fill(x + 3, y + 3, x + 7, y + 5, col); c.fill(x + 2, y + 5, x + 8, y + 6, col);
                c.fill(x + 3, y + 6, x + 7, y + 8, col); c.fill(x + 4, y + 8, x + 6, y + 9, col);
                break;
            case SHIELD: D.rrect(c, x + 1, y + 1, 8, 7, 2, col); c.fill(x + 3, y + 8, x + 7, y + 10, col); break;
            case SWAP: c.fill(x + 1, y + 2, x + 8, y + 3, col); c.fill(x + 6, y + 1, x + 8, y + 4, col); c.fill(x + 2, y + 7, x + 9, y + 8, col); c.fill(x + 2, y + 6, x + 4, y + 9, col); break;
            default: c.fill(x, y + 4, x + 10, y + 6, col); c.fill(x + 6, y + 2, x + 8, y + 8, col);
        }
    }
}
