package dev.likevisuals;

import net.minecraft.client.gui.DrawContext;

/** A HUD element that can be moved, scaled and toggled in the HUD editor. */
public abstract class HudMod extends Mod {
    public float hx, hy, hs = 1f;
    public final float defX, defY;
    public int w = 80, h = 20;
    protected final Setting bgStyle, bgAlpha;

    protected HudMod(String name, String desc, Cat cat, float dx, float dy) {
        super(name, desc, cat);
        hx = defX = dx; hy = defY = dy;
        bgStyle = mode("Background", 0, "Global", "Dark", "Glass", "Light", "None");
        bgAlpha = num("Background opacity", 1.0, 0.1, 1.0, 0.05);
    }

    protected abstract void render(DrawContext c, float td, boolean preview);

    public int px() {
        int sw = U.mc().getWindow().getScaledWidth();
        return Math.max(0, Math.min(Math.max(0, sw - (int) (w * hs)), (int) (hx * sw)));
    }
    public int py() {
        int sh = U.mc().getWindow().getScaledHeight();
        return Math.max(0, Math.min(Math.max(0, sh - (int) (h * hs)), (int) (hy * sh)));
    }

    @Override public void onHud(DrawContext c, float td) { draw(c, td, false); }

    public void draw(DrawContext c, float td, boolean preview) {
        var ms = c.getMatrices();
        ms.push();
        ms.translate(px(), py(), 0);
        ms.scale(hs, hs, 1f);
        try { render(c, td, preview); } finally { ms.pop(); }
    }

    public void reset() { hx = defX; hy = defY; hs = 1f; }

    /** Background panel: global HUD style (Client Color) unless overridden per element. */
    protected void panel(DrawContext c, int x, int y, int ww, int hh, int r) {
        float a = bgAlpha.f() * ClientMods.hudAlpha();
        int style = bgStyle.mode == 0 ? ClientMods.hudStyle() : bgStyle.mode - 1;
        switch (style) {
            case 0: // dark, like a flat pill
                D.rrect(c, x - 1, y - 1, ww + 2, hh + 2, r + 1, D.a(0xFFFFFFFF, 0.08f * a));
                D.rrect(c, x, y, ww, hh, r, D.a(0xFF0E0E15, 0.90f * a));
                break;
            case 1: // glass
                D.rrect(c, x - 1, y - 1, ww + 2, hh + 2, r + 1, D.a(0xFFFFFFFF, 0.14f * a));
                D.rrectV(c, x, y, ww, hh, r, D.a(0xFF7A849C, 0.55f * a), D.a(0xFF566078, 0.55f * a));
                break;
            case 2: // light
                D.rrect(c, x, y + 1, ww, hh, r, D.a(0xFF000000, 0.15f * a));
                D.rrect(c, x, y, ww, hh, r, D.a(0xFFF4F4FA, 0.88f * a));
                break;
            default: break;
        }
    }

    /** Small rounded label with a coloured dot; returns its width. */
    protected int pill(DrawContext c, int x, int y, String text, int dot) {
        int pw = D.w(text) + 22;
        panel(c, x, y, pw, 16, 8);
        D.circle(c, x + 9, y + 8, 2, dot);
        D.text(c, text, x + 15, y + 4, hudText());
        return pw;
    }

    /** Pill with an icon on the left. */
    protected int iconPill(DrawContext c, int x, int y, int icon, String text, int iconCol) {
        int pw = D.w(text) + 28;
        panel(c, x, y, pw, 18, 9);
        Icons.draw(c, icon, x + 7, y + 4, iconCol);
        D.text(c, text, x + 21, y + 5, hudText());
        return pw;
    }

    protected int hudText() { return (bgStyle.mode == 0 ? ClientMods.hudStyle() : bgStyle.mode - 1) == 2 ? 0xFF1E1228 : 0xFFFFFFFF; }
}
