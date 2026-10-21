package dev.likevisuals;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;

public class SakuraTitle extends Screen {
    private static final String[] LABELS = {"Одиночная игра", "Сетевая игра", "Настройки", "Выйти из игры"};
    private final float[] hover = new float[LABELS.length];
    private final long start = System.currentTimeMillis();

    public SakuraTitle() { super(Text.literal("LikeVisuals")); }

    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public void renderBackground(DrawContext c, int mx, int my, float d) {}

    private int bw() { return 230; }
    private int bh() { return 30; }
    private int bx() { return width / 2 - bw() / 2; }
    private int by(int i) { return (int) (height * 0.50) + i * (bh() + 10); }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        float appear = Math.min(1f, (System.currentTimeMillis() - start) / 700f);
        int a1 = ClientMods.accent(0), a2 = ClientMods.accent(1);
        Sakura.scene(ctx, width, height);

        // logo
        float t = Sakura.time();
        int cx = width / 2, ly = (int) (height * 0.17 + Math.sin(t * 1.2) * 2);
        D.glow(ctx, cx, ly + 26, 150, D.a(0xFFFF8FBF, appear));
        String logo = "LikeVisuals";
        float sc = 4.4f;
        float lw = D.w(logo) * sc;
        D.textS(ctx, logo, cx - lw / 2f + 2, ly + 3, D.a(0xFF4A1838, 0.55f * appear), sc);
        D.gradTextS(ctx, logo, cx - lw / 2f, ly, D.a(0xFFFFF0F6, appear), D.a(a2, appear), sc);
        Sakura.flower(ctx, (int) (cx - lw / 2f - 26), ly + 22, 14, D.a(0xFFFFB7D5, appear));
        Sakura.flower(ctx, (int) (cx + lw / 2f + 26), ly + 22, 14, D.a(0xFFFFB7D5, appear));
        String sub = "S A K U R A   E D I T I O N";
        D.text(ctx, sub, cx - D.w(sub) / 2, ly + 56, D.a(0xFFFFE6F0, 0.9f * appear));

        // buttons
        for (int i = 0; i < LABELS.length; i++) {
            int x = bx(), y = by(i), w = bw(), h = bh();
            boolean hov = mx >= x && mx < x + w && my >= y && my < y + h;
            hover[i] += ((hov ? 1 : 0) - hover[i]) * 0.22f;
            float hv = hover[i];
            int ex = (int) (hv * 6), yy = y - (int) (hv * 1);
            D.rrect(ctx, x - ex, yy + 4, w + ex * 2, h, 15, D.a(0xFF1A0820, 0.35f * appear));
            D.rrectV(ctx, x - ex - 1, yy - 1, w + ex * 2 + 2, h + 2, 16, D.a(U.lerpColor(0xFFFFD6E7, a1, hv), (0.55f + 0.4f * hv) * appear), D.a(U.lerpColor(0xFFE6C8FF, a2, hv), (0.55f + 0.4f * hv) * appear));
            D.rrectV(ctx, x - ex, yy, w + ex * 2, h, 15, D.a(U.lerpColor(0xFF4A2A58, 0xFF6A3A78, hv), 0.92f * appear), D.a(U.lerpColor(0xFF331A40, 0xFF4E2A5C, hv), 0.92f * appear));
            D.text(ctx, LABELS[i], cx - D.w(LABELS[i]) / 2, yy + (h - 9) / 2, D.a(0xFFFFFFFF, appear));
            if (hv > 0.05f) Sakura.petal(ctx, x - ex + 16, yy + h / 2f, 4.5f, t * 2 + i, 1f, D.a(0xFFFFB7D5, hv * appear));
            if (hv > 0.05f) Sakura.petal(ctx, x + w + ex - 16, yy + h / 2f, 4.5f, -t * 2 + i, 1f, D.a(0xFFFFB7D5, hv * appear));
        }

        D.text(ctx, "LikeVisuals  |  Minecraft 1.21.4  |  Fabric", 10, height - 14, D.a(0xFFFFE6F0, 0.8f * appear));
        String hint = "Right Shift - меню клиента (в игре)";
        D.text(ctx, hint, width - D.w(hint) - 10, height - 14, D.a(0xFFFFE6F0, 0.8f * appear));
        String user = U.mc().getSession().getUsername();
        D.text(ctx, user, width - D.w(user) - 12, 10, D.a(0xFFFFFFFF, 0.9f * appear));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn != 0) return false;
        for (int i = 0; i < LABELS.length; i++) {
            if (mx >= bx() && mx < bx() + bw() && my >= by(i) && my < by(i) + bh()) {
                var mc = U.mc();
                switch (i) {
                    case 0: mc.setScreen(new SelectWorldScreen(this)); break;
                    case 1: mc.setScreen(new MultiplayerScreen(this)); break;
                    case 2: mc.setScreen(new OptionsScreen(this, mc.options)); break;
                    default: mc.scheduleStop();
                }
                return true;
            }
        }
        return false;
    }
}
