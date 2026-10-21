package dev.likevisuals;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ClickGui extends Screen {
    private static Mod.Cat cat = Mod.Cat.COMBAT;
    private static int view; // 0 = modules, 1 = theme, 2 = configs
    private static final int SB = 134, ROW_H = 40;
    private static final int[] HUD_SAMPLE = {0xFF0E0E15, 0xFF68728A, 0xFFF4F4FA, 0x33FFFFFF};

    private boolean editHud;
    private String search = "";
    private float scroll, tScroll, maxScroll, dScroll, dMax, dT, appear;
    private Mod open, binding;
    private Setting drag; private int dragX, dragW;
    private HudMod dragHud, hoverHud; private int offX, offY;
    private int pendKey = -1; private long pendTime;
    private final long start = System.currentTimeMillis();
    private final float[] catAnim = new float[Mod.Cat.values().length + 3];
    private int W, H, x0, y0;

    public ClickGui() { this(false); }
    public ClickGui(boolean edit) { super(Text.literal("LikeVisuals")); editHud = edit; }

    @Override protected void init() {
        W = Math.min(width - 24, 660); H = Math.min(height - 24, 410);
        x0 = (width - W) / 2; y0 = (height - H) / 2;
    }
    @Override public boolean shouldPause() { return false; }
    @Override public void renderBackground(DrawContext c, int mx, int my, float d) {} // fully transparent: the world stays visible
    @Override public void removed() { Modules.save(); }

    // ---------- helpers ----------
    private static float ease(float t) { t = Math.max(0, Math.min(1, t)); return 1 - (1 - t) * (1 - t) * (1 - t); }
    private int ac(int c) { return D.a(c, appear); }
    private boolean in(double mx, double my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    private boolean inPanel(double mx, double my) { return !editHud && in(mx, my, x0, y0, W, H); }

    private List<Mod> visible() {
        List<Mod> l = new ArrayList<>();
        String q = search.trim().toLowerCase();
        for (Mod m : Modules.ALL) {
            if (q.isEmpty()) { if (m.cat == cat) l.add(m); }
            else if (m.name.toLowerCase().contains(q) || m.desc.toLowerCase().contains(q) || m.cat.label.toLowerCase().contains(q)) l.add(m);
        }
        return l;
    }
    private int cx() { return x0 + SB + 16; }
    private int cw() { return W - SB - 32; }
    private int top() { return y0 + 58; }
    private int bottom() { return y0 + H - 14; }
    private int drawerW() { return 262; }
    private int drawerX() { return x0 + W - 8 - (int) (drawerW() * ease(dT)); }
    private int catY(int i) { return y0 + 82 + i * 25; }
    private int btnY(int i) { return y0 + H - 84 + i * 24; }

    static String keyName(int key) {
        if (key <= 0) return "-";
        try { return InputUtil.fromKeyCode(key, 0).getLocalizedText().getString().toUpperCase(); } catch (Exception e) { return "KEY" + key; }
    }

    // ---------- HUD dragging ----------
    private boolean overHud(HudMod m, double mx, double my) {
        return in(mx, my, m.px(), m.py(), Math.max(12, (int) (m.w * m.hs)), Math.max(12, (int) (m.h * m.hs)));
    }
    private HudMod hudAt(double mx, double my) {
        for (int i = Modules.ALL.size() - 1; i >= 0; i--) {
            Mod m = Modules.ALL.get(i);
            if (m instanceof HudMod h && (h.enabled || editHud) && overHud(h, mx, my)) return h;
        }
        return null;
    }

    // ---------- render ----------
    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        Theme.update(ClientMods.themeIndex(), ClientMods.guiAlpha());
        appear = ease((System.currentTimeMillis() - start) / 220f);
        scroll += (tScroll - scroll) * 0.25f;
        dT += ((open != null && view == 0 && !editHud ? 1 : 0) - dT) * 0.22f;
        long now = System.currentTimeMillis();
        if (pendKey >= 0 && now - pendTime > 90) { appendKey(pendKey); pendKey = -1; }
        int a1 = ClientMods.accent(0), a2 = ClientMods.accent(1);

        hoverHud = dragHud != null ? dragHud : (editHud || !inPanel(mx, my) ? hudAt(mx, my) : null);
        if (editHud) {
            for (Mod m : Modules.ALL) if (m instanceof HudMod h && !h.enabled) {
                try { h.draw(ctx, delta, true); } catch (Throwable ignored) {}
                ctx.fill(h.px(), h.py(), h.px() + Math.max(12, (int) (h.w * h.hs)), h.py() + Math.max(12, (int) (h.h * h.hs)), 0x88000000);
            }
            for (Mod m : Modules.ALL) if (m instanceof HudMod h) {
                int x = h.px(), y = h.py(), ww = Math.max(12, (int) (h.w * h.hs)), hh = Math.max(12, (int) (h.h * h.hs));
                ctx.drawBorder(x - 1, y - 1, ww + 2, hh + 2, D.a(h.enabled ? a1 : 0xFFFFFFFF, h == hoverHud ? 1f : 0.5f));
            }
            drawHudTag(ctx);
            int bw = 470, bx = width / 2 - bw / 2, by = height - 34;
            D.rrect(ctx, bx - 1, by - 1, bw + 2, 26, 13, D.a(a1, 0.7f));
            D.rrect(ctx, bx, by, bw, 24, 12, 0xE61A1A22);
            String help = "Drag HUD elements   |   Scroll - resize   |   Right click - on/off   |   Tab/Esc - back";
            D.text(ctx, help, width / 2 - D.w(help) / 2, by + 8, 0xFFFFFFFF);
            return;
        }
        if (hoverHud != null) {
            int x = hoverHud.px(), y = hoverHud.py(), ww = Math.max(12, (int) (hoverHud.w * hoverHud.hs)), hh = Math.max(12, (int) (hoverHud.h * hoverHud.hs));
            ctx.drawBorder(x - 1, y - 1, ww + 2, hh + 2, D.a(a1, 1f));
            drawHudTag(ctx);
        }

        if (ClientMods.guiPetals()) Sakura.overlay(ctx, width, height, appear * 0.8f);

        var ms = ctx.getMatrices();
        ms.push();
        ms.translate(width / 2f, height / 2f, 0);
        float s = 0.94f + 0.06f * appear;
        ms.scale(s, s, 1);
        ms.translate(-width / 2f, -height / 2f, 0);

        D.rrect(ctx, x0 - 3, y0 + 3, W + 6, H + 6, 18, D.a(0xFF000000, 0.18f * appear));
        D.rrect(ctx, x0 - 1, y0 - 1, W + 2, H + 2, 15, D.a(0xFFFFFFFF, 0.16f * appear));
        D.rrectV(ctx, x0, y0, W, H, 14, ac(Theme.panelT), ac(Theme.panelB));

        drawSidebar(ctx, mx, my, a1, a2);
        drawHeader(ctx, now, a1, a2);
        if (view == 1) drawTheme(ctx, mx, my, a1, a2);
        else if (view == 2) drawConfigs(ctx, mx, my, a1, a2);
        else { drawRows(ctx, mx, my, a1, a2); drawDrawer(ctx, mx, my, a1, a2); }
        ms.pop();
    }

    private void drawHudTag(DrawContext ctx) {
        if (hoverHud == null) return;
        String tag = hoverHud.name + (hoverHud.enabled ? "" : " (off)") + "   x" + String.format(java.util.Locale.ROOT, "%.2f", hoverHud.hs);
        int x = hoverHud.px(), y = hoverHud.py(), tw = D.w(tag) + 12, ty = y > 18 ? y - 17 : y + Math.max(12, (int) (hoverHud.h * hoverHud.hs)) + 3;
        D.rrect(ctx, x, ty, tw, 14, 7, 0xDD16161E);
        D.text(ctx, tag, x + 6, ty + 3, 0xFFFFFFFF);
    }

    private void drawSidebar(DrawContext ctx, int mx, int my, int a1, int a2) {
        D.rrectV(ctx, x0 + 6, y0 + 6, SB - 6, H - 12, 12, ac(Theme.sideT), ac(Theme.sideB));
        int cxm = x0 + 6 + (SB - 6) / 2;
        Logo.draw(ctx, cxm - 15, y0 + 14, 30);
        String nm = "LikeVisuals";
        D.gradText(ctx, nm, cxm - D.w(nm) / 2, y0 + 48, ac(Theme.nameOn), ac(a1));
        String v = "1.21.4";
        D.text(ctx, v, cxm - D.w(v) / 2, y0 + 60, ac(a1));

        Mod.Cat[] cats = Mod.Cat.values();
        for (int i = 0; i < cats.length; i++) {
            Mod.Cat c = cats[i];
            int ry = catY(i), rx = x0 + 14, rw = SB - 22;
            boolean sel = view == 0 && !editHud && search.isEmpty() && c == cat, hov = in(mx, my, rx, ry, rw, 22);
            catAnim[i] += ((sel ? 1 : hov ? 0.4f : 0) - catAnim[i]) * 0.25f;
            if (catAnim[i] > 0.02f) D.rrectV(ctx, rx, ry, rw, 22, 8, ac(U.lerpColor(0x00000000, D.a(a1, 0.85f), catAnim[i])), ac(U.lerpColor(0x00000000, D.a(a2, 0.85f), catAnim[i])));
            D.circle(ctx, rx + 12, ry + 11, 3, ac(sel ? 0xFFFFFFFF : D.a(c.color, 0.9f)));
            D.text(ctx, c.label, rx + 24, ry + 7, ac(sel ? 0xFFFFFFFF : Theme.label));
            int on = 0; for (Mod m : Modules.ALL) if (m.cat == c && m.enabled) on++;
            if (on > 0) { String cs = String.valueOf(on); D.text(ctx, cs, rx + rw - 8 - D.w(cs), ry + 7, ac(sel ? 0xFFFFFFFF : Theme.dim)); }
        }
        String[] lab = {"Theme", "Configs", "Edit HUD"};
        for (int i = 0; i < 3; i++) {
            int ry = btnY(i), rx = x0 + 14, rw = SB - 22;
            boolean sel = (i == 0 && view == 1) || (i == 1 && view == 2), hov = in(mx, my, rx, ry, rw, 20);
            int k = cats.length + i;
            catAnim[k] += ((sel ? 1 : hov ? 0.5f : 0) - catAnim[k]) * 0.25f;
            D.rrect(ctx, rx, ry, rw, 20, 10, ac(U.lerpColor(Theme.chipOff, D.a(a1, 0.9f), catAnim[k])));
            D.text(ctx, lab[i], rx + rw / 2 - D.w(lab[i]) / 2, ry + 5, ac(sel || hov ? 0xFFFFFFFF : Theme.label));
        }
    }

    private void drawHeader(DrawContext ctx, long now, int a1, int a2) {
        int sx = cx(), sy = y0 + 14, sw = cw();
        String title = view == 1 ? "Theme" : view == 2 ? "Configs" : search.isEmpty() ? cat.label : "Search";
        String sub = view == 1 ? "Colors for the GUI and the HUD" : view == 2 ? "Save, load and delete your setups" : search.isEmpty() ? "Manage your modules" : "Results for your query";
        D.textS(ctx, title, sx, sy, ac(Theme.text), 1.25f);
        D.text(ctx, sub, sx, sy + 16, ac(Theme.muted));
        if (view == 1) return;
        int fw = 150, fx = sx + sw - fw;
        D.rrect(ctx, fx, sy, fw, 22, 11, ac(Theme.field));
        D.circle(ctx, fx + 13, sy + 10, 4, ac(Theme.muted));
        D.circle(ctx, fx + 13, sy + 10, 2, ac(Theme.field | 0xFF000000));
        ctx.fill(fx + 16, sy + 13, fx + 19, sy + 16, ac(Theme.muted));
        String ph = view == 2 ? "Config name..." : "Search...";
        String txt = search.isEmpty() ? ph : D.clip(search, fw - 40) + ((now / 450) % 2 == 0 ? "|" : "");
        D.text(ctx, txt, fx + 24, sy + 6, ac(search.isEmpty() ? Theme.dim : Theme.text));
        if (!search.isEmpty()) { D.circle(ctx, fx + fw - 11, sy + 11, 6, ac(Theme.chip)); D.text(ctx, "x", fx + fw - 11 - D.w("x") / 2, sy + 6, ac(Theme.label)); }
    }

    private void drawRows(DrawContext ctx, int mx, int my, int a1, int a2) {
        List<Mod> list = visible();
        int cxs = cx(), cw = cw(), topY = top(), botY = bottom();
        maxScroll = Math.max(0, list.size() * ROW_H - (botY - topY));
        tScroll = Math.max(0, Math.min(maxScroll, tScroll));
        scroll = Math.max(0, Math.min(maxScroll, scroll));
        ctx.enableScissor(cxs - 4, topY - 2, cxs + cw + 4, botY);
        for (int i = 0; i < list.size(); i++) {
            Mod m = list.get(i);
            int y = topY + i * ROW_H - (int) scroll;
            if (y + ROW_H < topY || y > botY) continue;
            boolean hov = in(mx, my, cxs, y, cw, ROW_H) && my >= topY && my < botY && !(open != null && mx >= drawerX() - 4);
            m.anim += ((m.enabled ? 1 : 0) - m.anim) * 0.22f;
            m.hover += ((hov ? 1 : 0) - m.hover) * 0.25f;
            if (m.hover > 0.02f) D.rrect(ctx, cxs - 4, y + 1, cw + 8, ROW_H - 2, 9, ac(D.a(Theme.rowHover, m.hover)));
            ctx.fill(cxs, y + ROW_H - 1, cxs + cw, y + ROW_H, ac(Theme.rowLine));
            if (open == m) D.rrect(ctx, cxs - 4, y + 9, 2, ROW_H - 18, 1, ac(a1));

            D.text(ctx, D.clip(m.name, cw - 150), cxs + 2, y + 8, ac(U.lerpColor(Theme.nameOff, Theme.nameOn, m.anim)));
            D.text(ctx, D.clip(m.desc, cw - 140), cxs + 2, y + 22, ac(Theme.muted));

            int sw = 32, sh = 16, sx = cxs + cw - sw - 2, sy = y + (ROW_H - sh) / 2;
            D.rrectV(ctx, sx, sy, sw, sh, 8, ac(U.lerpColor(Theme.chipOff, a1, m.anim)), ac(U.lerpColor(Theme.chipOff, a2, m.anim)));
            D.circle(ctx, sx + 8 + (int) ((sw - 16) * m.anim), sy + 8, 6, ac(0xFFFFFFFF));
            boolean bnd = binding == m;
            String bt = bnd ? "..." : keyName(m.key);
            int bw = Math.max(18, D.w(bt) + 12);
            D.rrect(ctx, sx - 10 - bw, sy + 1, bw, 14, 7, ac(bnd ? D.a(a1, 0.8f) : Theme.chip));
            D.text(ctx, bt, sx - 10 - bw + bw / 2 - D.w(bt) / 2, sy + 3, ac(bnd ? 0xFFFFFFFF : Theme.muted));
            if (!m.settings.isEmpty()) for (int k = 0; k < 3; k++) D.circle(ctx, sx - 22 - bw - k * 5, sy + 8, 1, ac(Theme.dim));
        }
        ctx.disableScissor();
        if (list.isEmpty()) D.text(ctx, "Nothing found", cxs + 4, topY + 8, ac(Theme.dim));
        if (maxScroll > 0) {
            int th = botY - topY, bh = (int) Math.max(26, th * th / (th + maxScroll));
            int by = topY + (int) ((th - bh) * (scroll / maxScroll));
            D.rrect(ctx, cxs + cw + 6, by, 3, bh, 1, ac(D.a(a1, 0.75f)));
        }
        D.text(ctx, "LMB toggle  |  RMB settings  |  click the key chip to bind  |  .bind <module/item> <key> in chat", cxs, y0 + H - 12, ac(Theme.dim));
    }

    // ---------- theme tab ----------
    private int themeRowH(Setting s, ClientMods.ClientColor cc) { return (s == cc.theme || s == cc.hudStyle) ? 62 : rowH(s); }

    private void drawTheme(DrawContext ctx, int mx, int my, int a1, int a2) {
        var cc = ClientMods.color();
        if (cc == null) return;
        int cxs = cx(), cw = cw(), topY = top(), botY = bottom();
        int total = 0; for (Setting s : cc.settings) total += themeRowH(s, cc);
        maxScroll = Math.max(0, total - (botY - topY));
        tScroll = Math.max(0, Math.min(maxScroll, tScroll)); scroll = Math.max(0, Math.min(maxScroll, scroll));
        ctx.enableScissor(cxs - 4, topY - 2, cxs + cw + 4, botY);
        int y = topY - (int) scroll;
        for (Setting s : cc.settings) {
            if (s == cc.theme || s == cc.hudStyle) {
                D.text(ctx, s.name, cxs, y + 2, ac(Theme.label));
                int n = s.modes.length, sw = Math.min(58, (cw - (n - 1) * 8) / n);
                for (int i = 0; i < n; i++) {
                    int x = cxs + i * (sw + 8), sy = y + 18;
                    boolean sel = s.mode == i;
                    D.rrect(ctx, x - 2, sy - 2, sw + 4, 34, 11, ac(sel ? D.a(a1, 0.95f) : 0x00000000));
                    D.rrect(ctx, x, sy, sw, 30, 9, ac(s == cc.theme ? Theme.SAMPLE[i] : HUD_SAMPLE[i]));
                    D.rrect(ctx, x + 6, sy + 12, sw - 12, 6, 3, ac(D.a(a1, 0.8f)));
                    D.text(ctx, s.modes[i], x + sw / 2 - D.w(s.modes[i]) / 2, sy + 36, ac(sel ? Theme.nameOn : Theme.muted));
                }
            } else drawRow(ctx, s, cxs, y, cw, a1, a2);
            y += themeRowH(s, cc);
        }
        ctx.disableScissor();
        // rainbow shortcut
        D.text(ctx, "Tip: drag the accent color slider to its far right for an animated rainbow.", cxs, y0 + H - 12, ac(Theme.dim));
    }

    private boolean clickTheme(double mx, double my, int btn) {
        var cc = ClientMods.color();
        if (cc == null) return false;
        int cxs = cx(), cw = cw(), y = top() - (int) scroll;
        for (Setting s : cc.settings) {
            int h = themeRowH(s, cc);
            if (my >= y && my < y + h && mx >= cxs && mx < cxs + cw) {
                if (s == cc.theme || s == cc.hudStyle) {
                    int n = s.modes.length, sw = Math.min(58, (cw - (n - 1) * 8) / n);
                    for (int i = 0; i < n; i++) if (in(mx, my, cxs + i * (sw + 8), y + 18, sw, 30)) { s.mode = i; return true; }
                } else clickRow(s, cxs, y, cw, mx, my, btn);
                return true;
            }
            y += h;
        }
        return false;
    }

    // ---------- configs tab ----------
    private List<String> cfgList = new ArrayList<>();
    private long cfgRefresh;
    private int cfgRowY(int i) { return top() + 40 + i * 36; }

    private void drawConfigs(DrawContext ctx, int mx, int my, int a1, int a2) {
        long now = System.currentTimeMillis();
        if (now - cfgRefresh > 1000) { cfgList = Modules.listConfigs(); cfgRefresh = now; }
        int cxs = cx(), cw = cw();
        D.rrect(ctx, cxs - 4, top(), cw + 8, 32, 10, ac(D.a(Theme.rowHover, 1f)));
        D.text(ctx, search.isEmpty() ? "Type a name in the field above, then press Save" : "Save as: " + search, cxs + 6, top() + 11, ac(Theme.label));
        int bw = 80;
        boolean hov = in(mx, my, cxs + cw - bw, top() + 5, bw, 22);
        D.rrect(ctx, cxs + cw - bw, top() + 5, bw, 22, 11, ac(hov ? a1 : D.a(a1, 0.75f)));
        D.text(ctx, "Save", cxs + cw - bw + bw / 2 - D.w("Save") / 2, top() + 11, 0xFFFFFFFF);
        for (int i = 0; i < cfgList.size(); i++) {
            int y = cfgRowY(i);
            if (y + 30 > bottom()) break;
            ctx.fill(cxs, y + 33, cxs + cw, y + 34, ac(Theme.rowLine));
            D.text(ctx, cfgList.get(i), cxs + 2, y + 10, ac(Theme.nameOn));
            String[] bl = {"Load", "Overwrite", "Delete"};
            int bx = cxs + cw;
            for (int b = 2; b >= 0; b--) {
                int w = D.w(bl[b]) + 18; bx -= w + 6;
                boolean h2 = in(mx, my, bx, y + 5, w, 22);
                int base = b == 2 ? 0xFFFF5C7A : a1;
                D.rrect(ctx, bx, y + 5, w, 22, 11, ac(h2 ? D.a(base, 0.9f) : D.a(base, 0.35f)));
                D.text(ctx, bl[b], bx + 9, y + 11, 0xFFFFFFFF);
            }
        }
        if (cfgList.isEmpty()) D.text(ctx, "No saved configs yet", cxs + 2, top() + 50, ac(Theme.dim));
        D.text(ctx, "Files: .minecraft/config/likevisuals/  |  chat: .cfg save|load <name>", cxs, y0 + H - 12, ac(Theme.dim));
    }

    private boolean clickConfigs(double mx, double my) {
        int cxs = cx(), cw = cw(), bw = 80;
        if (in(mx, my, cxs + cw - bw, top() + 5, bw, 22)) {
            if (!search.trim().isEmpty()) { Modules.saveNamed(search.trim()); cfgRefresh = 0; U.chat("Config saved: " + search.trim()); }
            return true;
        }
        for (int i = 0; i < cfgList.size(); i++) {
            int y = cfgRowY(i);
            if (y + 30 > bottom()) break;
            String[] bl = {"Load", "Overwrite", "Delete"};
            int bx = cxs + cw;
            for (int b = 2; b >= 0; b--) {
                int w = D.w(bl[b]) + 18; bx -= w + 6;
                if (in(mx, my, bx, y + 5, w, 22)) {
                    String n = cfgList.get(i);
                    if (b == 0) { Modules.loadNamed(n); U.chat("Config loaded: " + n); }
                    else if (b == 1) { Modules.saveNamed(n); U.chat("Config overwritten: " + n); }
                    else { Modules.deleteNamed(n); cfgRefresh = 0; }
                    return true;
                }
            }
        }
        return false;
    }

    // ---------- settings drawer ----------
    private int rowH(Setting s) {
        switch (s.kind) { case HEADER: return 26; case NUM: case COLOR: return 38; default: return 28; }
    }

    private void drawDrawer(DrawContext ctx, int mx, int my, int a1, int a2) {
        if (open == null) return;
        Mod m = open;
        int dw = drawerW(), dx = drawerX(), dy = top() - 6, dh = bottom() - dy + 4;
        D.rrect(ctx, dx - 1, dy - 1, dw + 2, dh + 2, 13, ac(D.a(0xFFFFFFFF, 0.18f)));
        D.rrectV(ctx, dx, dy, dw, dh, 12, ac(Theme.panelT | 0xE0000000), ac(Theme.panelB | 0xE0000000));
        D.text(ctx, m.name, dx + 14, dy + 10, ac(Theme.nameOn));
        D.text(ctx, m.cat.label, dx + 14, dy + 24, ac(m.cat.color));
        boolean hx = in(mx, my, dx + dw - 26, dy + 7, 18, 18);
        D.circle(ctx, dx + dw - 17, dy + 16, 8, ac(hx ? D.a(a1, 0.7f) : Theme.chip));
        D.text(ctx, "x", dx + dw - 17 - D.w("x") / 2, dy + 11, ac(Theme.nameOn));
        ctx.fill(dx + 12, dy + 40, dx + dw - 12, dy + 41, ac(Theme.divider));

        int areaTop = dy + 46, areaBot = dy + dh - 6;
        int total = 56;
        for (Setting s : m.settings) if (s.visible.getAsBoolean()) total += rowH(s);
        dMax = Math.max(0, total - (areaBot - areaTop));
        dScroll = Math.max(0, Math.min(dMax, dScroll));
        ctx.enableScissor(dx, areaTop, dx + dw, areaBot);
        int y = areaTop - (int) dScroll, x = dx + 14, w = dw - 28;
        for (Setting s : m.settings) {
            if (!s.visible.getAsBoolean()) continue;
            drawRow(ctx, s, x, y, w, a1, a2);
            y += rowH(s);
        }
        D.text(ctx, "Bind", x, y + 9, ac(Theme.label));
        String bt = binding == m ? "press a key..." : keyName(m.key);
        int bw = D.w(bt) + 18;
        D.rrect(ctx, x + w - bw, y + 4, bw, 19, 9, ac(binding == m ? D.a(a1, 0.7f) : Theme.chip));
        D.text(ctx, bt, x + w - bw + 9, y + 9, ac(Theme.nameOn));
        y += 28;
        D.text(ctx, "Bind mode", x, y + 9, ac(Theme.label));
        String bm = m.bindMode.mode == 0 ? "Toggle" : "Hold"; int bmw = D.w(bm) + 18;
        D.rrect(ctx, x + w - bmw, y + 4, bmw, 19, 9, ac(Theme.chip));
        D.text(ctx, bm, x + w - bmw + 9, y + 9, ac(Theme.nameOn));
        ctx.disableScissor();
    }

    private void drawRow(DrawContext ctx, Setting s, int x, int y, int w, int a1, int a2) {
        switch (s.kind) {
            case HEADER: {
                int tw = D.w(s.name);
                ctx.fill(x, y + 14, x + (w - tw) / 2 - 8, y + 15, ac(Theme.divider));
                ctx.fill(x + (w + tw) / 2 + 8, y + 14, x + w, y + 15, ac(Theme.divider));
                D.text(ctx, s.name, x + (w - tw) / 2, y + 9, ac(Theme.dim));
                break;
            }
            case BOOL: {
                D.text(ctx, s.name, x, y + 9, ac(Theme.label));
                int sw = 32, sh = 16, sx = x + w - sw, sy = y + 6;
                D.rrectV(ctx, sx, sy, sw, sh, 8, ac(s.b ? a1 : Theme.chipOff), ac(s.b ? a2 : Theme.chipOff));
                D.circle(ctx, sx + (s.b ? sw - 8 : 8), sy + 8, 6, ac(0xFFFFFFFF));
                break;
            }
            case MODE: {
                D.text(ctx, s.name, x, y + 9, ac(Theme.label));
                String v = s.modes[s.mode]; int vw = D.w(v) + 28;
                D.rrect(ctx, x + w - vw, y + 4, vw, 19, 9, ac(Theme.chip));
                D.text(ctx, "<", x + w - vw + 7, y + 9, ac(Theme.muted));
                D.text(ctx, ">", x + w - 13, y + 9, ac(Theme.muted));
                D.text(ctx, v, x + w - vw + 14, y + 9, ac(Theme.nameOn));
                break;
            }
            default: {
                D.text(ctx, s.name, x, y + 5, ac(Theme.label));
                String v = s.valueText();
                D.text(ctx, v, x + w - D.w(v), y + 5, ac(Theme.nameOn));
                int ty = y + 26; float t = (float) ((s.v - s.min) / (s.max - s.min));
                if (s.kind == Setting.Kind.COLOR) {
                    for (int i = 0; i < 40; i++) {
                        int c1 = x + i * w / 40, c2 = x + (i + 1) * w / 40;
                        ctx.fill(c1, ty - 2, c2, ty + 3, ac(0xFF000000 | (java.awt.Color.HSBtoRGB(i / 40f, 0.5f, 1f) & 0xFFFFFF)));
                    }
                } else {
                    D.rrect(ctx, x, ty - 2, w, 5, 2, ac(Theme.chipOff));
                    D.rrectV(ctx, x, ty - 2, Math.max(5, (int) (w * t)), 5, 2, ac(a1), ac(a2));
                }
                int kx = x + (int) (w * t);
                D.circle(ctx, kx, ty, 8, ac(D.a(a1, 0.25f)));
                D.circle(ctx, kx, ty, 5, ac(0xFFFFFFFF));
                break;
            }
        }
    }

    // ---------- input ----------
    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (binding != null) { binding = null; return true; }

        // HUD elements: drag directly on the screen (outside the panel, or anywhere in edit mode)
        if (editHud || !inPanel(mx, my)) {
            HudMod h = hudAt(mx, my);
            if (h != null) {
                if (btn == 0) { dragHud = h; offX = (int) mx - h.px(); offY = (int) my - h.py(); }
                else if (btn == 1) h.toggle();
                return true;
            }
            if (editHud) return true;
            if (!inPanel(mx, my)) return super.mouseClicked(mx, my, btn);
        }

        // search clear
        int fx = cx() + cw() - 150, sy = y0 + 14;
        if (view != 1 && in(mx, my, fx, sy, 150, 22)) {
            if (!search.isEmpty() && in(mx, my, fx + 150 - 20, sy, 20, 22)) { search = ""; tScroll = 0; }
            return true;
        }

        if (view == 0 && open != null && dT > 0.5f) {
            int dw = drawerW(), dx = drawerX(), dy = top() - 6, dh = bottom() - dy + 4;
            if (in(mx, my, dx, dy, dw, dh)) {
                if (in(mx, my, dx + dw - 26, dy + 7, 18, 18)) { open = null; return true; }
                int areaTop = dy + 46, y = areaTop - (int) dScroll, x = dx + 14, w = dw - 28;
                if (my < areaTop) return true;
                for (Setting s : open.settings) {
                    if (!s.visible.getAsBoolean()) continue;
                    int h = rowH(s);
                    if (my >= y && my < y + h) clickRow(s, x, y, w, mx, my, btn);
                    y += h;
                }
                if (my >= y && my < y + 28) binding = open;
                y += 28;
                if (my >= y && my < y + 28) open.bindMode.cycle(1);
                return true;
            }
        }

        Mod.Cat[] cats = Mod.Cat.values();
        for (int i = 0; i < cats.length; i++)
            if (in(mx, my, x0 + 14, catY(i), SB - 22, 22)) { cat = cats[i]; view = 0; search = ""; tScroll = 0; open = null; return true; }
        if (in(mx, my, x0 + 14, btnY(0), SB - 22, 20)) { view = view == 1 ? 0 : 1; search = ""; open = null; tScroll = 0; return true; }
        if (in(mx, my, x0 + 14, btnY(1), SB - 22, 20)) { view = view == 2 ? 0 : 2; search = ""; open = null; cfgRefresh = 0; tScroll = 0; return true; }
        if (in(mx, my, x0 + 14, btnY(2), SB - 22, 20)) { editHud = true; return true; }

        if (view == 1) return clickTheme(mx, my, btn);
        if (view == 2) return clickConfigs(mx, my);

        List<Mod> list = visible();
        int cxs = cx(), cw = cw();
        if (my >= top() && my < bottom() && mx >= cxs - 4 && mx < cxs + cw + 4) {
            for (int i = 0; i < list.size(); i++) {
                int y = top() + i * ROW_H - (int) scroll;
                if (!in(mx, my, cxs - 4, y, cw + 8, ROW_H)) continue;
                Mod m = list.get(i);
                int kx = cxs + cw - 34, ky = y + (ROW_H - 16) / 2, bw = Math.max(18, D.w(keyName(m.key)) + 12);
                if (btn == 0 && in(mx, my, kx - 10 - bw, ky, bw, 16)) { binding = m; return true; }
                if (btn == 0) m.toggle();
                else if (btn == 1) { open = (open == m) ? null : m; dScroll = 0; }
                return true;
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    private void clickRow(Setting s, int x, int y, int w, double mx, double my, int btn) {
        switch (s.kind) {
            case BOOL: s.b = !s.b; break;
            case MODE: s.cycle(btn == 1 || mx < x + w - D.w(s.modes[s.mode]) - 14 ? -1 : 1); break;
            case NUM: case COLOR: if (my >= y + 16) { drag = s; dragX = x; dragW = w; applyDrag(mx); } break;
            default: break;
        }
    }

    private void applyDrag(double mx) {
        if (drag == null) return;
        double t = Math.max(0, Math.min(1, (mx - dragX) / dragW));
        double v = drag.min + t * (drag.max - drag.min);
        if (drag.step > 0) v = Math.round(v / drag.step) * drag.step;
        drag.v = Math.max(drag.min, Math.min(drag.max, v));
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (dragHud != null) {
            float nx = (float) (mx - offX) / width, ny = (float) (my - offY) / height;
            int ww = (int) (dragHud.w * dragHud.hs), hh = (int) (dragHud.h * dragHud.hs);
            if (Math.abs(nx * width + ww / 2f - width / 2f) < 5) nx = (width / 2f - ww / 2f) / width;
            if (Math.abs(ny * height + hh / 2f - height / 2f) < 5) ny = (height / 2f - hh / 2f) / height;
            dragHud.hx = Math.max(0, Math.min(1, nx)); dragHud.hy = Math.max(0, Math.min(1, ny));
            return true;
        }
        if (drag != null) { applyDrag(mx); return true; }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override public boolean mouseReleased(double mx, double my, int btn) { drag = null; dragHud = null; return super.mouseReleased(mx, my, btn); }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmt, double vAmt) {
        if (editHud || !inPanel(mx, my)) {
            HudMod h = hudAt(mx, my);
            if (h != null) { h.hs = Math.max(0.5f, Math.min(3f, h.hs + (float) vAmt * 0.05f)); return true; }
            if (editHud) return true;
        }
        if (view == 0 && open != null && mx >= drawerX()) dScroll = Math.max(0, Math.min(dMax, dScroll - (float) vAmt * 26));
        else tScroll = Math.max(0, Math.min(maxScroll, tScroll - (float) vAmt * 38));
        return true;
    }

    private void appendKey(int key) {
        if (search.length() >= 28) return;
        if (key >= GLFW.GLFW_KEY_A && key <= GLFW.GLFW_KEY_Z) search += (char) ('a' + key - GLFW.GLFW_KEY_A);
        else if (key >= GLFW.GLFW_KEY_0 && key <= GLFW.GLFW_KEY_9) search += (char) ('0' + key - GLFW.GLFW_KEY_0);
        else if (key == GLFW.GLFW_KEY_SPACE) search += ' ';
        tScroll = 0; open = null;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (binding != null) {
            if (key == GLFW.GLFW_KEY_ESCAPE) { binding = null; return true; }
            binding.key = (key == GLFW.GLFW_KEY_DELETE || key == GLFW.GLFW_KEY_BACKSPACE) ? -1 : key;
            binding = null;
            return true;
        }
        if (key == GLFW.GLFW_KEY_TAB) { editHud = !editHud; return true; }
        if (key == GLFW.GLFW_KEY_ESCAPE) { if (editHud) editHud = false; else if (open != null) open = null; else close(); return true; }
        if (key == GLFW.GLFW_KEY_RIGHT_SHIFT) { close(); return true; }
        if (key == GLFW.GLFW_KEY_BACKSPACE) { if (!search.isEmpty()) { search = search.substring(0, search.length() - 1); tScroll = 0; } return true; }
        if ((mods & GLFW.GLFW_MOD_CONTROL) != 0 && key == GLFW.GLFW_KEY_V) {
            String clip = U.mc().keyboard.getClipboard().replaceAll("[\\r\\n]", " ");
            String all = search + clip;
            search = all.substring(0, Math.min(28, all.length()));
            return true;
        }
        if ((mods & GLFW.GLFW_MOD_CONTROL) == 0 && ((key >= GLFW.GLFW_KEY_A && key <= GLFW.GLFW_KEY_Z) || (key >= GLFW.GLFW_KEY_0 && key <= GLFW.GLFW_KEY_9) || key == GLFW.GLFW_KEY_SPACE)) {
            pendKey = key; pendTime = System.currentTimeMillis(); // fallback if the char event never arrives
        }
        return true;
    }

    @Override
    public boolean charTyped(char c, int mods) {
        pendKey = -1;
        if (!editHud && binding == null && c >= 32 && c != 127 && search.length() < 28) { search += c; tScroll = 0; open = null; }
        return true;
    }
}
