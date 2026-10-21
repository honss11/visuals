package dev.likevisuals;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public abstract class Mod {
    public enum Cat {
        COMBAT("Бой", 0xFFFF5C7A), VISUAL("Визуалы", 0xFF8F7CFF), HUD("HUD", 0xFF6FD6FF), PLAYER("Игрок", 0xFF4DD6A5),
        UTIL("Утилиты", 0xFFFFB84D), FT("Хелперы", 0xFF4DB8FF), CLIENT("Клиент", 0xFFFFB7D5);
        public final String label; public final int color;
        Cat(String l, int c) { label = l; color = c; }
    }

    public final String name, desc;
    public final Cat cat;
    public int key = -1;
    public boolean enabled, oneShot, wasDown;
    public float anim, hover;
    public final List<Setting> settings = new ArrayList<>();
    public final Setting bindMode = Setting.mode("Режим бинда", 0, "Переключатель", "Удержание");

    protected Mod(String name, String desc, Cat cat) { this.name = name; this.desc = desc; this.cat = cat; }

    protected Setting bool(String n, boolean d) { Setting s = Setting.bool(n, d); settings.add(s); return s; }
    protected Setting num(String n, double d, double mi, double ma, double st) { Setting s = Setting.num(n, d, mi, ma, st); settings.add(s); return s; }
    protected Setting mode(String n, int d, String... m) { Setting s = Setting.mode(n, d, m); settings.add(s); return s; }
    protected Setting color(String n, double hue) { Setting s = Setting.color(n, hue); settings.add(s); return s; }
    protected Setting header(String n) { Setting s = Setting.header(n); settings.add(s); return s; }

    public void toggle() { setEnabled(!enabled); }

    public void setEnabled(boolean v) {
        try {
            if (oneShot) { if (v) onEnable(); enabled = false; Modules.toggled(this); return; }
            if (v == enabled) return;
            enabled = v;
            if (v) onEnable(); else onDisable();
            Modules.toggled(this);
        } catch (Throwable t) { LikeVisuals.LOG.error("module " + name, t); enabled = false; }
    }

    public void onEnable() {}
    public void onDisable() {}
    public void onTick() {}
    public void onHud(DrawContext ctx, float td) {}
    public void onWorld(MatrixStack ms, Vec3d cam, float td) {}
    public void onAttack(Entity e) {}
    public void onTotemPop(Entity e) {}
}
