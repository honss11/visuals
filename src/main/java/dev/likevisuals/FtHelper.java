package dev.likevisuals;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Item helpers for server-specific items (identified by the item's display name).
 * Shapes: 0 = cube (trap), 1 = wall (plast), 2 = ring (area effect), 3 = line (thrown projectile).
 * Sizes and durations are approximations and can be tuned in the settings.
 */
public final class FtHelper {
    public static void register() {
        Modules.add(new Helper("FT Helper", "Помощник предметов FunTime: зоны, таймеры и траектории",
            new String[]{"Трапка", "Пласт", "Дезориентация", "Явная пыль", "Снежок заморозки"},
            new String[][]{{"трап"}, {"пласт"}, {"дезориент"}, {"явная пыль", "яв. пыль"}, {"снежок", "заморож"}},
            new int[]{0, 1, 2, 2, 3}, new double[]{15, 20, 12, 10, 3}, new double[]{5, 5, 10, 10, 0}));
        Modules.add(new Helper("RW Helper", "Помощник предметов ReallyWorld: зоны, таймеры и траектории",
            new String[]{"Трапка", "Взрывная трапка", "Пласт", "Дезориентация", "Явная пыль", "Божья аура", "Огненный смерч", "Снежок"},
            new String[][]{{"трап"}, {"взрывная"}, {"пласт"}, {"дезориент"}, {"явная пыль"}, {"божья аура", "аура"}, {"смерч"}, {"снежок"}},
            new int[]{0, 0, 1, 2, 2, 2, 2, 3}, new double[]{15, 15, 20, 12, 10, 8, 10, 3}, new double[]{5, 7, 5, 10, 10, 6, 8, 0}));
    }

    static class Zone { int type; Vec3d pos, end; double yaw, dur; long t0; }

    static class Helper extends Mod {
        final String[] names; final String[][] keys; final int[] shapes;
        final Setting[] item, dur, size;
        final Setting timer, thick, clientColor, fillZone;
        final List<Zone> zones = new ArrayList<>();
        boolean wasUse;

        Helper(String name, String desc, String[] names, String[][] keys, int[] shapes, double[] durs, double[] sizes) {
            super(name, desc, Cat.FT);
            this.names = names; this.keys = keys; this.shapes = shapes;
            item = new Setting[names.length]; dur = new Setting[names.length]; size = new Setting[names.length];
            header("Предметы");
            for (int i = 0; i < names.length; i++) item[i] = bool(names[i], true);
            header("Время действия (сек)");
            for (int i = 0; i < names.length; i++) dur[i] = num(names[i] + " (сек)", durs[i], 1, 60, 1);
            header("Размер зоны");
            for (int i = 0; i < names.length; i++) if (shapes[i] != 3) size[i] = num(names[i] + " (размер)", sizes[i], 2, 20, 1);
            header("Отображение");
            timer = bool("Таймеры на экране", true);
            fillZone = bool("Заливка зон", true);
            thick = num("Жирность линии", 2.5, 1, 6, 0.5);
            clientColor = bool("Цвет клиента", true);
        }

        int color(int i) { return clientColor.b ? ClientMods.accent(i % 2) : 0xFF6FD6FF; }

        int typeOf(ItemStack s) {
            if (s.isEmpty()) return -1;
            String n = s.getName().getString().toLowerCase(Locale.ROOT);
            for (int t = 0; t < keys.length; t++) for (String k : keys[t]) if (n.contains(k)) return item[t].b ? t : -1;
            return -1;
        }

        @Override public void onTick() {
            var mc = U.mc(); var p = U.p(); long now = System.currentTimeMillis();
            zones.removeIf(z -> now - z.t0 > z.dur * 1000);
            boolean use = mc.options.useKey.isPressed() && mc.currentScreen == null;
            if (use && !wasUse) {
                int t = typeOf(p.getMainHandStack());
                if (t < 0) t = typeOf(p.getOffHandStack());
                if (t >= 0) {
                    Zone z = new Zone(); z.type = t; z.t0 = now; z.dur = dur[t].d(); z.yaw = p.getYaw();
                    Vec3d pos = p.getPos();
                    int sh = shapes[t];
                    if (sh == 0 || sh == 1) {
                        if (mc.crosshairTarget instanceof BlockHitResult br && br.getType() != HitResult.Type.MISS) { BlockPos bp = br.getBlockPos().offset(br.getSide()); pos = Vec3d.ofBottomCenter(bp); }
                        else pos = p.getPos().add(p.getRotationVec(1f).multiply(2, 0, 2));
                    }
                    z.pos = pos;
                    if (sh == 3) {
                        Vec3d s = p.getEyePos(), e = s.add(p.getRotationVec(1f).multiply(40));
                        var bh = mc.world.raycast(new RaycastContext(s, e, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, p));
                        z.pos = s.subtract(0, 0.2, 0); z.end = bh.getType() != HitResult.Type.MISS ? bh.getPos() : e;
                        z.dur = Math.min(z.dur, 3);
                    }
                    zones.add(z);
                }
            }
            wasUse = use;
        }

        @Override public void onHud(DrawContext c, float td) {
            if (!timer.b || zones.isEmpty()) return;
            long now = System.currentTimeMillis();
            int x = 8, y = c.getScaledWindowHeight() / 2 - 70;
            int ww = 130, hh = zones.size() * 14 + 22;
            D.rrectV(c, x - 4, y - 4, ww, hh, 9, 0xCC241830, 0xCC180D22);
            Logo.draw(c, x, y - 1, 12);
            D.text(c, name, x + 17, y, ClientMods.accent(0));
            y += 16;
            for (Zone z : zones) {
                double left = z.dur - (now - z.t0) / 1000.0;
                float frac = (float) Math.max(0, Math.min(1, left / z.dur));
                D.text(c, names[z.type] + String.format(Locale.ROOT, "  %.1f", Math.max(0, left)), x, y, 0xFFFFFFFF);
                D.rrect(c, x, y + 10, ww - 14, 2, 1, 0x55000000);
                D.rrect(c, x, y + 10, Math.max(1, (int) ((ww - 14) * frac)), 2, 1, color(z.type));
                y += 14;
            }
        }

        @Override public void onWorld(MatrixStack ms, Vec3d cam, float td) {
            if (zones.isEmpty()) return;
            long now = System.currentTimeMillis();
            R3.begin(ms, cam, false);
            var l = R3.lines(thick.f()); var q = R3.quads(); var t = R3.tris();
            for (Zone z : zones) {
                float life = 1f - (float) ((now - z.t0) / 1000.0 / z.dur);
                int c = color(z.type), sh = shapes[z.type];
                double s = size[z.type] == null ? 0 : size[z.type].d();
                switch (sh) {
                    case 0: {
                        double h = s / 2;
                        Box b = new Box(z.pos.x - h, z.pos.y, z.pos.z - h, z.pos.x + h, z.pos.y + s, z.pos.z + h);
                        R3.boxLines(l, b, c); if (fillZone.b) R3.boxFill(q, b, U.argb(c, 0.12f));
                        break;
                    }
                    case 1: {
                        boolean alongX = Math.abs(Math.sin(Math.toRadians(z.yaw))) > 0.7;
                        double h = s / 2;
                        Box b = alongX ? new Box(z.pos.x - 0.1, z.pos.y, z.pos.z - h, z.pos.x + 0.1, z.pos.y + s, z.pos.z + h) : new Box(z.pos.x - h, z.pos.y, z.pos.z - 0.1, z.pos.x + h, z.pos.y + s, z.pos.z + 0.1);
                        R3.boxLines(l, b, c); if (fillZone.b) R3.boxFill(q, b, U.argb(c, 0.2f));
                        break;
                    }
                    case 3: l.line(z.pos, z.end, U.argb(c, Math.max(0.2f, life))); R3.ringFill(q, z.end.add(0, 0.03, 0), 0, 0.4, 16, U.argb(c, 0.6f), U.argb(c, 0.6f)); break;
                    default: {
                        R3.ringLines(l, z.pos.add(0, 0.05, 0), s, 64, c);
                        if (fillZone.b) R3.discFill(t, z.pos.add(0, 0.04, 0), s, 64, U.argb(c, 0.08f * life + 0.03f));
                    }
                }
            }
            q.draw(); t.draw(); l.draw();
            R3.end(ms);
        }
    }
}
