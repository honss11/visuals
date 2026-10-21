package dev.likevisuals;

import dev.likevisuals.mixin.HandledScreenAccessor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.*;

public final class Visuals2 {
    public static void register() {
        Modules.add(new BlockOverlay()); Modules.add(new ChinaHat()); Modules.add(new HitRange()); Modules.add(new HitboxCustomizer());
        Modules.add(new JumpCircle()); Modules.add(new Prediction()); Modules.add(new Trails());
        Modules.add(new ItemHighlighter()); Modules.add(new BurHelper());
    }

    static List<PlayerEntity> players(boolean self, boolean others) {
        List<PlayerEntity> l = new ArrayList<>();
        var mc = U.mc();
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player) { if (self && !mc.options.getPerspective().isFirstPerson()) l.add(p); }
            else if (others) l.add(p);
        }
        return l;
    }

    // ================================================================ BlockOverlay
    static class BlockOverlay extends Mod {
        final Setting style = mode("Вид", 2, "Заливка", "Контур", "Заливка + контур", "Пульсация");
        final Setting col = color("Цвет", 270);
        final Setting alpha = num("Прозрачность", 0.3, 0.05, 0.9, 0.05);
        final Setting thick = num("Толщина", 2, 1, 6, 0.5);
        BlockOverlay() { super("Block Overlay", "Подсветка блока под прицелом с настройкой цвета и вида", Cat.VISUAL); }
        @Override public void onWorld(MatrixStack ms, Vec3d cam, float td) {
            var mc = U.mc();
            if (!(mc.crosshairTarget instanceof BlockHitResult br) || br.getType() == HitResult.Type.MISS) return;
            BlockPos bp = br.getBlockPos();
            var sh = mc.world.getBlockState(bp).getOutlineShape(mc.world, bp);
            if (sh.isEmpty()) return;
            Box b = sh.getBoundingBox().offset(bp).expand(0.003);
            float a = alpha.f() * (style.mode == 3 ? (float) (Math.sin(System.currentTimeMillis() / 300.0) * 0.4 + 0.6) : 1f);
            int c = col.rgb();
            R3.begin(ms, cam, true);
            if (style.mode != 1) { var q = R3.quads(); R3.boxFill(q, b, U.argb(c, a)); q.draw(); }
            if (style.mode != 0) { var l = R3.lines(thick.f()); R3.boxLines(l, b, c); l.draw(); }
            R3.end(ms);
        }
    }

    // ================================================================ ChinaHat
    static class ChinaHat extends Mod {
        final Setting style = mode("Вид", 0, "Конус", "Конус с каймой", "Радужный");
        final Setting size = num("Размер", 1.0, 0.5, 2.0, 0.05);
        final Setting col = color("Цвет", 340);
        final Setting others = bool("На других игроках", false);
        ChinaHat() { super("China Hat", "Рисует китайскую шляпу над головой игрока", Cat.VISUAL); }
        @Override public void onWorld(MatrixStack ms, Vec3d cam, float td) {
            R3.begin(ms, cam, true);
            var t = R3.tris(); var l = R3.lines(2f);
            for (PlayerEntity p : players(true, others.b)) {
                Vec3d b = R3.lerpPos(p, td).add(0, p.getHeight() - 0.08, 0);
                double r = 0.55 * size.d(), h = 0.32 * size.d();
                int seg = 28;
                for (int i = 0; i < seg; i++) {
                    double a0 = Math.PI * 2 * i / seg, a1 = Math.PI * 2 * (i + 1) / seg;
                    int c = style.mode == 2 ? col.rgb(i * 3) : col.rgb();
                    int c2 = style.mode == 2 ? col.rgb(i * 3 + 3) : col.rgb();
                    t.v(b.add(0, h, 0), U.argb(c, 0.95f));
                    t.v(b.add(Math.cos(a0) * r, 0, Math.sin(a0) * r), U.argb(c, 0.75f));
                    t.v(b.add(Math.cos(a1) * r, 0, Math.sin(a1) * r), U.argb(c2, 0.75f));
                    if (style.mode == 1) l.line(b.add(Math.cos(a0) * r, 0, Math.sin(a0) * r), b.add(Math.cos(a1) * r, 0, Math.sin(a1) * r), 0xFFFFFFFF);
                }
            }
            t.draw(); l.draw();
            R3.end(ms);
        }
    }

    // ================================================================ HitRange
    static class HitRange extends Mod {
        final Setting style = mode("Вид", 0, "Кольцо", "Заливка", "Пульсация");
        final Setting radius = num("Дальность удара", 3.0, 2.0, 6.0, 0.1);
        final Setting col = color("Цвет", 180);
        final Setting weapon = bool("Только с оружием в руке", false);
        HitRange() { super("Hit Range", "Показывает дальность удара вокруг игрока", Cat.VISUAL); }
        @Override public void onWorld(MatrixStack ms, Vec3d cam, float td) {
            var p = U.p();
            if (weapon.b && !(p.getMainHandStack().getItem().toString().contains("sword") || p.getMainHandStack().getItem().toString().contains("axe"))) return;
            Vec3d c = R3.lerpPos(p, td).add(0, 0.03, 0);
            double r = radius.d() + (style.mode == 2 ? Math.sin(System.currentTimeMillis() / 250.0) * 0.08 : 0);
            R3.begin(ms, cam, true);
            var q = R3.quads(); R3.ringFill(q, c, r - 0.05, r, 64, U.argb(col.rgb(), 0.9f), U.argb(col.rgb(1), 0.9f)); q.draw();
            if (style.mode == 1) { var t = R3.tris(); R3.discFill(t, c, r, 64, U.argb(col.rgb(), 0.15f)); t.draw(); }
            R3.end(ms);
        }
    }

    // ================================================================ HitboxCustomizer
    static class HitboxCustomizer extends Mod {
        final Setting who = mode("Цели", 0, "Игроки", "Мобы", "Животные", "Предметы", "Все");
        final Setting style = mode("Вид", 2, "Контур", "Заливка", "Оба");
        final Setting col = color("Цвет", 120);
        final Setting thick = num("Толщина", 1.5, 1, 5, 0.5);
        final Setting through = bool("Сквозь стены", true);
        HitboxCustomizer() { super("Hitbox Customizer", "Отображает хитбоксы сущностей с настройкой цвета и вида", Cat.VISUAL); }
        boolean ok(Entity e) {
            switch (who.mode) {
                case 0: return e instanceof PlayerEntity && e != U.p();
                case 1: return e instanceof Monster; case 2: return e instanceof AnimalEntity; case 3: return e instanceof ItemEntity;
                default: return e != U.p() && (e instanceof LivingEntity || e instanceof ItemEntity);
            }
        }
        @Override public void onWorld(MatrixStack ms, Vec3d cam, float td) {
            R3.begin(ms, cam, !through.b);
            var l = R3.lines(thick.f()); var q = R3.quads();
            int i = 0;
            for (Entity e : U.mc().world.getEntities()) {
                if (!ok(e) || e.squaredDistanceTo(cam) > 90 * 90) continue;
                Box b = e.getBoundingBox().offset(R3.lerpPos(e, td).subtract(e.getPos()));
                int c = Friends.is(e) ? 0xFF55FF88 : col.rgb(i++ * 2);
                if (style.mode != 1) R3.boxLines(l, b, c);
                if (style.mode != 0) R3.boxFill(q, b, U.argb(c, 0.22f));
            }
            q.draw(); l.draw();
            R3.end(ms);
        }
    }

    // ================================================================ JumpCircle
    static class JumpCircle extends Mod {
        final Setting style = mode("Вид", 0, "Кольцо", "Заливка", "Двойное");
        final Setting col = color("Цвет", 360);
        final Setting speed = num("Скорость", 1.0, 0.4, 2.5, 0.1);
        final List<Object[]> list = new ArrayList<>();
        boolean was = true;
        JumpCircle() { super("Jump Circle", "Рисует круг под игроком после прыжка", Cat.VISUAL); }
        @Override public void onTick() {
            var p = U.p();
            if (was && !p.isOnGround() && p.getVelocity().y > 0.1) list.add(new Object[]{p.getPos(), System.currentTimeMillis()});
            was = p.isOnGround();
        }
        @Override public void onWorld(MatrixStack ms, Vec3d cam, float td) {
            long now = System.currentTimeMillis();
            list.removeIf(o -> (now - (Long) o[1]) / 1000f * speed.f() >= 1f);
            if (list.isEmpty()) return;
            R3.begin(ms, cam, true);
            var q = R3.quads(); var t = R3.tris();
            for (Object[] o : list) {
                float age = (now - (Long) o[1]) / 1000f * speed.f();
                Vec3d c = ((Vec3d) o[0]).add(0, 0.02, 0);
                double r = 0.3 + age * 2.4;
                int c1 = U.argb(col.rgb(), 1f - age), c2 = U.argb(col.rgb(2), (1f - age) * 0.2f);
                R3.ringFill(q, c, r - 0.12, r, 48, c2, c1);
                if (style.mode == 1) R3.discFill(t, c, r, 48, U.argb(col.rgb(), (1f - age) * 0.25f));
                if (style.mode == 2) R3.ringFill(q, c, Math.max(0, r * 0.6 - 0.1), r * 0.6, 48, c2, c1);
            }
            q.draw(); t.draw();
            R3.end(ms);
        }
    }

    // ================================================================ Prediction
    static class Prediction extends Mod {
        final Setting col = color("Цвет линии", 190), hit = color("Цвет попадания", 0);
        final Setting mine = bool("Мои броски", true), others = bool("Чужие снаряды", true);
        final Setting thick = num("Толщина", 2, 1, 5, 0.5);
        Prediction() { super("Prediction", "Предсказывает траекторию снарядов и подсвечивает попадание", Cat.VISUAL); }

        @Override public void onWorld(MatrixStack ms, Vec3d cam, float td) {
            var mc = U.mc(); var p = U.p();
            R3.begin(ms, cam, false);
            var l = R3.lines(thick.f()); var q = R3.quads();
            if (mine.b) {
                ItemStack s = p.getMainHandStack();
                double speed = 0, grav = 0.03;
                if (s.isOf(Items.ENDER_PEARL) || s.isOf(Items.SNOWBALL) || s.isOf(Items.EGG)) speed = 1.5;
                else if (s.isOf(Items.BOW) && p.isUsingItem()) { float pr = BowItem.getPullProgress(p.getItemUseTime()); if (pr > 0.1f) { speed = pr * 3.0; grav = 0.05; } }
                if (speed > 0) sim(l, q, p.getCameraPosVec(td).subtract(0, 0.1, 0), p.getRotationVec(td).multiply(speed), grav, p);
            }
            if (others.b) for (Entity e : mc.world.getEntities()) {
                if (!(e instanceof ProjectileEntity) || e.getVelocity().lengthSquared() < 0.01) continue;
                boolean arrow = e instanceof net.minecraft.entity.projectile.PersistentProjectileEntity;
                sim(l, q, e.getPos(), e.getVelocity(), arrow ? 0.05 : 0.03, e);
            }
            l.draw(); q.draw();
            R3.end(ms);
        }

        void sim(R3.Batch l, R3.Batch q, Vec3d pos, Vec3d vel, double grav, Entity owner) {
            var w = U.mc().world; int c = col.rgb(); Vec3d last = pos;
            for (int i = 0; i < 160; i++) {
                Vec3d next = last.add(vel);
                BlockHitResult bh = w.raycast(new RaycastContext(last, next, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, owner));
                Vec3d end = bh.getType() != HitResult.Type.MISS ? bh.getPos() : next;
                Entity hitE = null;
                for (Entity e : w.getOtherEntities(owner, new Box(last, end).expand(1.0), x -> x instanceof LivingEntity && x.isAlive() && x != U.p()))
                    if (e.getBoundingBox().expand(0.3).raycast(last, end).isPresent()) { hitE = e; break; }
                l.line(last, end, U.argb(c, 1f - i / 200f));
                if (hitE != null) { R3.boxLines(l, hitE.getBoundingBox(), hit.rgb()); return; }
                if (bh.getType() != HitResult.Type.MISS) { R3.ringFill(q, end.add(0, 0.03, 0), 0.0, 0.35, 20, U.argb(c, 0.6f), U.argb(c, 0.6f)); return; }
                last = next; vel = vel.multiply(0.99).add(0, -grav, 0);
            }
        }
    }

    // ================================================================ Trails
    static class Trails extends Mod {
        final Setting style = mode("Вид", 1, "Частицы", "Лента");
        final Setting ptype = mode("Частицы", 2, Visuals.PN).showIf(() -> style.mode == 0);
        final Setting col = color("Цвет ленты", 360).showIf(() -> style.mode == 1);
        final Setting len = num("Длина ленты", 40, 10, 120, 5).showIf(() -> style.mode == 1);
        final ArrayDeque<Vec3d> pts = new ArrayDeque<>();
        Trails() { super("Trails", "Рисует ленту-след позади игрока", Cat.VISUAL); }
        @Override public void onDisable() { pts.clear(); }
        @Override public void onTick() {
            var p = U.p();
            if (style.mode == 0) {
                if (p.getVelocity().horizontalLengthSquared() > 0.003) U.particle(Visuals.PE[ptype.mode], p.getX(), p.getY() + 0.1, p.getZ(), 0, 0.01, 0);
                return;
            }
            pts.addLast(p.getPos());
            while (pts.size() > len.i()) pts.pollFirst();
        }
        @Override public void onWorld(MatrixStack ms, Vec3d cam, float td) {
            if (style.mode != 1 || pts.size() < 2) return;
            R3.begin(ms, cam, true);
            var q = R3.quads();
            Vec3d[] a = pts.toArray(new Vec3d[0]);
            for (int i = 0; i < a.length - 1; i++) {
                float t0 = i / (float) a.length, t1 = (i + 1) / (float) a.length;
                Vec3d p0 = a[i], p1 = a[i + 1];
                int c0 = U.argb(col.rgb(i), t0), c1 = U.argb(col.rgb(i + 1), t1);
                q.v(p0.add(0, 0.1, 0), c0); q.v(p0.add(0, 0.8, 0), c0); q.v(p1.add(0, 0.8, 0), c1); q.v(p1.add(0, 0.1, 0), c1);
            }
            q.draw();
            R3.end(ms);
        }
    }

    // ================================================================ ItemHighlighter
    static class ItemHighlighter extends Mod {
        final Setting what = mode("Что подсвечивать", 0, "Ценное", "Зачарованное", "Еда", "Всё");
        final Setting col = color("Цвет", 50);
        ItemHighlighter() { super("Item Highlighter", "Подсвечивает важные предметы в инвентаре и хотбаре", Cat.VISUAL); }
        boolean match(ItemStack s) {
            if (s.isEmpty()) return false;
            boolean valuable = s.isOf(Items.TOTEM_OF_UNDYING) || s.isOf(Items.GOLDEN_APPLE) || s.isOf(Items.ENCHANTED_GOLDEN_APPLE) || s.isOf(Items.ENDER_PEARL) || s.isOf(Items.ELYTRA)
                || s.isOf(Items.NETHERITE_INGOT) || s.isOf(Items.DIAMOND) || s.isOf(Items.FIREWORK_ROCKET);
            boolean ench = s.hasEnchantments();
            boolean food = s.get(net.minecraft.component.DataComponentTypes.FOOD) != null;
            switch (what.mode) { case 0: return valuable; case 1: return ench; case 2: return food; default: return valuable || ench || food; }
        }
        @Override public void onHud(DrawContext c, float td) {
            var p = U.p(); int x0 = c.getScaledWindowWidth() / 2 - 88, y = c.getScaledWindowHeight() - 19;
            for (int i = 0; i < 9; i++) if (match(p.getInventory().getStack(i))) c.drawBorder(x0 + i * 20 - 1, y - 1, 18, 18, col.rgb(i));
        }
    }

    public static void highlightSlots(HandledScreen<?> hs, DrawContext ctx) {
        ItemHighlighter h = Modules.get(ItemHighlighter.class);
        if (h == null || !h.enabled) return;
        try {
            var acc = (HandledScreenAccessor) hs;
            int ox = acc.getGuiX(), oy = acc.getGuiY();
            for (Slot s : hs.getScreenHandler().slots) {
                if (!h.match(s.getStack())) continue;
                ctx.fill(ox + s.x, oy + s.y, ox + s.x + 16, oy + s.y + 16, U.argb(h.col.rgb(s.id), 0.30f));
                ctx.drawBorder(ox + s.x - 1, oy + s.y - 1, 18, 18, h.col.rgb(s.id));
            }
        } catch (Throwable t) { h.enabled = false; }
    }

    // ================================================================ BurHelper
    static class BurHelper extends Mod {
        final Setting area = mode("Зона", 0, "3x3", "5x5", "7x7");
        final Setting depth = num("Глубина", 3, 1, 7, 1);
        final Setting col = color("Цвет", 30);
        BurHelper() { super("Bur Helper", "Показывает зону копания бульдозера и похожих предметов", Cat.VISUAL); }
        @Override public void onWorld(MatrixStack ms, Vec3d cam, float td) {
            if (!(U.mc().crosshairTarget instanceof BlockHitResult br) || br.getType() == HitResult.Type.MISS) return;
            BlockPos bp = br.getBlockPos(); Direction f = br.getSide(); int r = area.mode + 1, d = depth.i() - 1;
            double x0 = bp.getX(), y0 = bp.getY(), z0 = bp.getZ(), x1 = x0 + 1, y1 = y0 + 1, z1 = z0 + 1;
            if (f.getAxis() != Direction.Axis.X) { x0 -= r; x1 += r; } else if (f.getOffsetX() > 0) x0 -= d; else x1 += d;
            if (f.getAxis() != Direction.Axis.Y) { y0 -= r; y1 += r; } else if (f.getOffsetY() > 0) y0 -= d; else y1 += d;
            if (f.getAxis() != Direction.Axis.Z) { z0 -= r; z1 += r; } else if (f.getOffsetZ() > 0) z0 -= d; else z1 += d;
            Box b = new Box(x0, y0, z0, x1, y1, z1);
            R3.begin(ms, cam, false);
            var l = R3.lines(2f); R3.boxLines(l, b, col.rgb()); l.draw();
            var q = R3.quads(); R3.boxFill(q, b, U.argb(col.rgb(), 0.10f)); q.draw();
            R3.end(ms);
        }
    }
}
