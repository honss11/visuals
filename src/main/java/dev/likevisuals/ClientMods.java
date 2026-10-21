package dev.likevisuals;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ClientMods {
    public static ClientSounds sounds;
    private static ClientColor cc;

    public static void register() {
        cc = new ClientColor(); Modules.add(cc);
        sounds = new ClientSounds(); Modules.add(sounds);
    }

    /** Accent colour (index 0 = primary, 1 = secondary for gradients). */
    public static int accent(int i) {
        if (cc == null) return i == 0 ? 0xFFFFB7D5 : 0xFFD9B8FF;
        if (cc.col.rainbow()) return cc.col.rgb(i * 8);
        return 0xFF000000 | (java.awt.Color.HSBtoRGB((float) (((cc.col.v + i * cc.shift.d()) % 360) / 360.0), 0.45f, 1f) & 0xFFFFFF);
    }

    public static boolean titleOn() { return cc != null && cc.title.b; }
    public static int themeIndex() { return cc == null ? 0 : cc.theme.mode; }
    public static float guiAlpha() { return cc == null ? 0.85f : cc.guiAlpha.f(); }
    public static int hudStyle() { return cc == null ? 0 : cc.hudStyle.mode; }
    public static float hudAlpha() { return cc == null ? 0.9f : cc.hudAlpha.f(); }
    static ClientColor color() { return cc; }
    public static boolean guiPetals() { return cc == null || cc.petals.b; }

    static class ClientColor extends Mod {
        final Setting col = color("Accent color", 335);
        final Setting shift = num("Gradient shift", 30, 0, 180, 5);
        final Setting theme = mode("GUI style", 0, Theme.NAMES);
        final Setting guiAlpha = num("GUI opacity", 0.85, 0.2, 1.0, 0.05);
        final Setting hudStyle = mode("HUD style", 0, "Dark", "Glass", "Light", "Transparent");
        final Setting hudAlpha = num("HUD opacity", 0.9, 0.2, 1.0, 0.05);
        final Setting title = bool("Sakura main menu", true);
        final Setting petals = bool("GUI petals", false);
        ClientColor() { super("Client Color", "Accent color, GUI style and HUD style", Cat.CLIENT); enabled = true; }
    }

    static class ClientSounds extends Mod {
        static final SoundEvent[] S = {SoundEvents.UI_BUTTON_CLICK.value(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundEvents.ENTITY_ITEM_PICKUP};
        final Setting snd = mode("Звук", 1, "Клик", "Колокольчик", "Орб", "Подбор");
        ClientSounds() { super("Client Sounds", "Звук при включении и выключении модулей", Cat.CLIENT); }
        void play(boolean on) { U.sound(S[snd.mode], on ? 1.3f : 0.8f); }
    }

    static class Hud extends Mod {
        final Setting wm = bool("Ватермарка", true), fps = bool("FPS", true), coords = bool("Координаты", true), ping = bool("Пинг", true),
            speed = bool("Скорость", false), time = bool("Время", false), ip = bool("IP сервера", false), list = bool("Список модулей", true);
        Hud() { super("Hud", "Скрываемые элементы HUD: ватермарка, координаты, FPS, список модулей", Cat.CLIENT); enabled = true; }

        @Override public void onHud(DrawContext c, float td) {
            var mc = U.mc(); var p = U.p(); var tr = mc.textRenderer;
            int a1 = ClientMods.accent(0), a2 = ClientMods.accent(1);
            int y = 6;
            if (wm.b) {
                String name = Utils.protect(p.getName().getString());
                int w = D.w("LikeVisuals") + D.w(name) + 40;
                D.rrectV(c, 6, y, w, 16, 6, 0xCC141424, 0xCC0C0C18);
                D.rrect(c, 6, y + 14, w, 2, 0, a1);
                D.gradText(c, "LikeVisuals", 12, y + 4, a1, a2);
                D.text(c, name, 12 + D.w("LikeVisuals") + 12, y + 4, 0xFFFFFFFF);
                y += 22;
            }
            List<String> lines = new ArrayList<>();
            if (fps.b) lines.add("FPS " + mc.getCurrentFps());
            if (ping.b && mc.getNetworkHandler() != null) { PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(p.getUuid()); lines.add("Ping " + (e == null ? 0 : e.getLatency()) + " ms"); }
            if (coords.b && !Utils.streamerHides(false)) lines.add(String.format(Locale.ROOT, "XYZ %.0f %.0f %.0f", p.getX(), p.getY(), p.getZ()));
            if (speed.b) lines.add(String.format(Locale.ROOT, "%.1f b/s", Math.hypot(p.getX() - p.prevX, p.getZ() - p.prevZ) * 20));
            if (time.b) lines.add(java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")));
            if (ip.b && !Utils.streamerHides(true)) { var s = mc.getCurrentServerEntry(); lines.add(s == null ? "Singleplayer" : s.address); }
            for (String s : lines) {
                D.rrect(c, 6, y, D.w(s) + 10, 12, 4, 0x99101020);
                D.rrect(c, 6, y + 2, 2, 8, 1, a1);
                D.text(c, s, 11, y + 2, 0xFFFFFFFF);
                y += 14;
            }
            if (list.b) {
                List<Mod> en = new ArrayList<>();
                for (Mod m : Modules.ALL) if (m.enabled && m != this && m.cat != Cat.CLIENT) en.add(m);
                en.sort((x, z) -> D.w(z.name) - D.w(x.name));
                int ry = 6, i = 0, W = c.getScaledWindowWidth();
                for (Mod m : en) {
                    int w = D.w(m.name) + 10;
                    D.rrect(c, W - w - 6, ry, w, 12, 4, 0x99101020);
                    D.rrect(c, W - 8, ry + 2, 2, 8, 1, U.lerpColor(a1, a2, (i % 8) / 7f));
                    D.text(c, m.name, W - w - 1, ry + 2, U.lerpColor(a1, a2, (i % 8) / 7f));
                    ry += 14; i++;
                }
            }
        }
    }
}
