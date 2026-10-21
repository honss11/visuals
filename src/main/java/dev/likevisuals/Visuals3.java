package dev.likevisuals;

import net.minecraft.block.BlockState;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.WorldChunk;
import org.joml.Matrix4f;

import java.util.*;

public final class Visuals3 {
    public static void register() {
        Modules.add(new ShaderSky()); Modules.add(new Seasons()); Modules.add(new DamageHolo()); Modules.add(new Peek());
        Modules.add(new AudioRadar()); Modules.add(new LiquidUI()); Modules.add(new WindTrails()); Modules.add(new ItemHolo());
        Modules.add(new ChunkScanner()); Modules.add(new Tracers()); Modules.add(new StorageESP()); Modules.add(new Zoom());
        Modules.add(new LowHealth()); Modules.add(new Waypoints()); Modules.add(new TargetESP());
    }

    // ================================================================= shared helpers
    static void billboard(MatrixStack ms, Vec3d cam, Vec3d pos, String s, int color, float scale, boolean bg) {
        var mc = U.mc(); TextRenderer tr = mc.textRenderer;
        var vcp = mc.getBufferBuilders().getEntityVertexConsumers();
        ms.push();
        ms.translate(pos.x - cam.x, pos.y - cam.y, pos.z - cam.z);
        ms.multiply(mc.gameRenderer.getCamera().getRotation());
        float sc = 0.025f * scale;
        ms.scale(-sc, -sc, sc);
        Matrix4f m = ms.peek().getPositionMatrix();
        Text t = D.t(s);
        tr.draw(t, -tr.getWidth(t) / 2f, 0, color, false, m, vcp, TextRenderer.TextLayerType.SEE_THROUGH, bg ? 0x66000000 : 0, 0xF000F0);
        vcp.draw();
        ms.pop();
    }

    static int hsb(float h, float s, float b) { return 0xFF000000 | (java.awt.Color.HSBtoRGB(h - (float) Math.floor(h), s, b) & 0xFFFFFF); }
    static int mulC(int c, float k) {
        int r = Math.min(255, (int) (((c >> 16) & 255) * k)), g = Math.min(255, (int) (((c >> 8) & 255) * k)), b = Math.min(255, (int) ((c & 255) * k));
        return (c & 0xFF000000) | (r << 16) | (g << 8) | b;
    }
    static float smooth(double a, double b, double x) { double t = Math.max(0, Math.min(1, (x - a) / (b - a))); return (float) (t * t * (3 - 2 * t)); }

    // ================================================================= hooks used by mixins / Visuals
    public static Integer skySeasonOrShader() {
        ShaderSky s = Modules.get(ShaderSky.class);
        if (s != null && s.enabled) return s.horizon() & 0xFFFFFF;
        Seasons se = Modules.get(Seasons.class);
        if (se != null && se.enabled && se.tintSky.b) return se.sky();
        return null;
    }
    public static int grass(int orig, int x, int z) { Seasons s = Modules.get(Seasons.class); return s != null && s.enabled ? s.recolor(orig, x, z, false) : orig; }
    public static int foliage(int orig, int x, int z) { Seasons s = Modules.get(Seasons.class); return s != null && s.enabled ? s.recolor(orig, x, z, true) : orig; }
    public static boolean liquidOn() { return Modules.on(LiquidUI.class); }
    public static void onSound(SoundInstance si) { AudioRadar a = Modules.get(AudioRadar.class); if (a != null && a.enabled) a.add(si); }

    // ================================================================= SHADER SKY
    static class ShaderSky extends Mod {
        static final double[] EL = {-90, -30, -8, 0, 5, 12, 20, 30, 42, 56, 72, 90};
        final Setting style = mode("Небо", 0, "Сакура: закат", "Сакура: день", "Неон", "Аврора", "Галактика", "Радуга", "Лунная ночь", "Рассвет");
        final Setting bright = num("Яркость", 1.0, 0.4, 1.5, 0.05), speed = num("Скорость анимации", 1.0, 0.0, 4.0, 0.1);
        final Setting stars = bool("Звёзды", true), count = num("Количество звёзд", 260, 50, 600, 10).showIf(() -> stars.b);
        final Setting body = bool("Солнце / Луна", true), aurora = bool("Сияние (аврора)", true);

        ShaderSky() { super("Shader Sky", "Шейдерное небо: градиенты, звёзды, аврора, луна (рисуется поверх ванильного)", Cat.VISUAL); }

        int[] pal(float t) {
            switch (style.mode) {
                case 0: return new int[]{0xFF241448, 0xFFB45FA0, 0xFFFFA8B8};
                case 1: return new int[]{0xFFFFC2DA, 0xFFFFE0EC, 0xFFFFF6FA};
                case 2: { float h = (t * 0.04f) % 1f; return new int[]{hsb(h, 0.9f, 0.30f), hsb(h + 0.15f, 0.85f, 0.65f), hsb(h + 0.3f, 0.7f, 1f)}; }
                case 3: return new int[]{0xFF03060F, 0xFF0A1E38, 0xFF14485A};
                case 4: return new int[]{0xFF08031A, 0xFF2A0E5C, 0xFF8A3CB4};
                case 5: { float h = (t * 0.05f) % 1f; return new int[]{hsb(h, 0.6f, 0.9f), hsb(h + 0.2f, 0.5f, 1f), hsb(h + 0.4f, 0.4f, 1f)}; }
                case 6: return new int[]{0xFF03050E, 0xFF0B1630, 0xFF2C4470};
                default: return new int[]{0xFF3A4A9A, 0xFFFF8FA8, 0xFFFFD6A0};
            }
        }
        int horizon() { return mulC(pal(Sakura.time() * speed.f())[2], bright.f()); }
        int colorAt(int[] p, double el) {
            if (el <= 0) return U.lerpColor(p[2], 0xFF000000, 0.55f * (float) Math.min(1, -el / 30));
            float t = (float) (el / 90.0);
            return U.lerpColor(U.lerpColor(p[2], p[1], smooth(0, 0.45, t)), p[0], smooth(0.45, 1, t));
        }
        static Vec3d dir(Vec3d cam, double R, double el, double az) {
            return new Vec3d(cam.x + R * Math.cos(el) * Math.cos(az), cam.y + R * Math.sin(el), cam.z + R * Math.cos(el) * Math.sin(az));
        }

        @Override public void onWorld(MatrixStack ms, Vec3d cam, float td) {
            var mc = U.mc();
            double R = Math.max(64, Math.min(240, mc.options.getClampedViewDistance() * 16 - 16));
            float t = Sakura.time() * speed.f();
            int[] pal = pal(t);
            R3.begin(ms, cam, true);
            var q = R3.quads();
            int LON = 36;
            for (int i = 0; i < EL.length - 1; i++) {
                double e0 = Math.toRadians(EL[i]), e1 = Math.toRadians(EL[i + 1]);
                int c0 = mulC(colorAt(pal, EL[i]), bright.f()), c1 = mulC(colorAt(pal, EL[i + 1]), bright.f());
                for (int j = 0; j < LON; j++) {
                    double a0 = Math.PI * 2 * j / LON, a1 = Math.PI * 2 * (j + 1) / LON;
                    q.v(dir(cam, R, e0, a0), c0); q.v(dir(cam, R, e0, a1), c0); q.v(dir(cam, R, e1, a1), c1); q.v(dir(cam, R, e1, a0), c1);
                }
            }
            if (aurora.b && (style.mode == 3 || style.mode == 4)) {
                for (int j = 0; j < 72; j++) {
                    double a0 = Math.PI * 2 * j / 72, a1 = Math.PI * 2 * (j + 1) / 72;
                    float w0 = (float) ((0.5 + 0.5 * Math.sin(a0 * 3 + t * 0.6)) * (0.5 + 0.5 * Math.sin(a0 * 7 - t * 0.4)));
                    float w1 = (float) ((0.5 + 0.5 * Math.sin(a1 * 3 + t * 0.6)) * (0.5 + 0.5 * Math.sin(a1 * 7 - t * 0.4)));
                    int c0 = hsb((float) (0.36 + 0.28 * Math.sin(a0 * 2 + t * 0.2)), 0.7f, 1f), c1 = hsb((float) (0.36 + 0.28 * Math.sin(a1 * 2 + t * 0.2)), 0.7f, 1f);
                    double lo = Math.toRadians(16), hi = Math.toRadians(58 + 8 * Math.sin(a0 * 5 + t));
                    q.v(dir(cam, R * 0.99, lo, a0), U.argb(c0, w0 * 0.6f)); q.v(dir(cam, R * 0.99, lo, a1), U.argb(c1, w1 * 0.6f));
                    q.v(dir(cam, R * 0.99, hi, a1), U.argb(c1, 0f)); q.v(dir(cam, R * 0.99, hi, a0), U.argb(c0, 0f));
                }
            }
            if (stars.b && style.mode != 1) {
                Random r = new Random(7777);
                for (int i = 0; i < count.i(); i++) {
                    double az = r.nextDouble() * Math.PI * 2, el = Math.asin(r.nextDouble() * 0.97 + 0.02);
                    Vec3d c = dir(cam, R * 0.985, el, az);
                    Vec3d n = c.subtract(cam).normalize();
                    Vec3d u = n.crossProduct(new Vec3d(0, 1, 0)).normalize(), v = n.crossProduct(u);
                    double s = R * (0.0022 + 0.0035 * r.nextDouble());
                    float tw = (float) (0.5 + 0.5 * Math.sin(t * 2 + i));
                    int col = U.argb(r.nextBoolean() ? 0xFFFFFF : 0xFFE0F0, 0.35f + 0.65f * tw);
                    q.v(c.add(u.multiply(-s)).add(v.multiply(-s)), col); q.v(c.add(u.multiply(s)).add(v.multiply(-s)), col);
                    q.v(c.add(u.multiply(s)).add(v.multiply(s)), col); q.v(c.add(u.multiply(-s)).add(v.multiply(s)), col);
                }
            }
            q.draw();
            if (body.b) {
                var tr = R3.tris();
                double az = 0.9, el = Math.toRadians(34);
                Vec3d d = new Vec3d(Math.cos(el) * Math.cos(az), Math.sin(el), Math.cos(el) * Math.sin(az));
                boolean day = style.mode == 1 || style.mode == 7 || style.mode == 0;
                int core = day ? 0xFFFFF6E0 : 0xFFF4F8FF, glow = day ? 0xFFFFC0D8 : 0xFF9FB8FF;
                for (int g = 3; g >= 0; g--) disc(tr, cam, d, R, R * (0.05 + g * 0.045), U.argb(glow, 0.10f + 0.04f * (3 - g)), U.argb(glow, 0.0f));
                disc(tr, cam, d, R, R * 0.05, core, core);
                tr.draw();
            }
            R3.end(ms);
        }
        static void disc(R3.Batch t, Vec3d cam, Vec3d d, double R, double rad, int cc, int ce) {
            Vec3d c = cam.add(d.multiply(R * 0.98));
            Vec3d u = d.crossProduct(new Vec3d(0, 1, 0)).normalize(), v = d.crossProduct(u);
            for (int k = 0; k < 28; k++) {
                double a0 = Math.PI * 2 * k / 28, a1 = Math.PI * 2 * (k + 1) / 28;
                t.v(c, cc);
                t.v(c.add(u.multiply(Math.cos(a0) * rad)).add(v.multiply(Math.sin(a0) * rad)), ce);
                t.v(c.add(u.multiply(Math.cos(a1) * rad)).add(v.multiply(Math.sin(a1) * rad)), ce);
            }
        }
    }

    // ================================================================= SEASONS
    static class Seasons extends Mod {
        final Setting mode = mode("Сезон", 0, "Авто (по месяцу)", "Весна", "Лето", "Осень", "Зима", "Сакура");
        final Setting power = num("Сила цвета", 1.0, 0.2, 1.0, 0.05);
        final Setting particles = bool("Частицы сезона", true), tintSky = bool("Оттенок неба", true);
        int last = -2;
        Seasons() { super("Dynamic Seasons", "Сезонные цвета травы и листвы, оттенок неба и частицы", Cat.VISUAL); }

        int season() {
            if (mode.mode > 0) return mode.mode - 1;
            int m = java.time.LocalDate.now().getMonthValue();
            return m >= 3 && m <= 5 ? 0 : m >= 6 && m <= 8 ? 1 : m >= 9 && m <= 11 ? 2 : 3;
        }
        int sky() { int[] c = {0xAEDCF5, 0x6FB4FF, 0xE8B27A, 0xC9D8E8, 0xF2B9D2}; return c[season()]; }

        int recolor(int orig, int x, int z, boolean leaves) {
            int s = season(), h = Math.abs(x * 31 + z * 17) % 4, t;
            int[][] grass = {{0x7FE05A}, {0x3FB13A}, {0xB8A23A}, {0xDDE8F0}, {0x8EE06A}};
            int[][] foli = {{0x9BEF7A, 0x86E06A, 0xA8F58A, 0x92E874}, {0x2E9A2E, 0x38A838, 0x2A8C2A, 0x34A034}, {0xE8741E, 0xD9441A, 0xF2B01E, 0xB5541C}, {0xEAF4FF, 0xDDEBFA, 0xF4FAFF, 0xE2EEF9}, {0xFFB7D5, 0xFF9EC4, 0xFFD6E7, 0xFFA8CC}};
            t = leaves ? foli[s][h] : grass[s][0];
            return U.lerpColor(0xFF000000 | orig, 0xFF000000 | t, power.f()) & 0xFFFFFF;
        }

        @Override public void onEnable() { reload(); last = season(); }
        @Override public void onDisable() { reload(); last = -2; }
        void reload() { try { U.mc().worldRenderer.reload(); } catch (Throwable ignored) {} }

        @Override public void onTick() {
            int s = season();
            if (s != last) { last = s; reload(); }
            if (!particles.b) return;
            var p = U.p();
            for (int i = 0; i < 3; i++) {
                double x = p.getX() + U.rnd(10), y = p.getY() + 2 + U.RNG.nextDouble() * 8, z = p.getZ() + U.rnd(10);
                switch (s) {
                    case 0: case 4: U.particle(ParticleTypes.CHERRY_LEAVES, x, y, z, 0, -0.02, 0); break;
                    case 2: U.particle(new DustParticleEffect(U.RNG.nextBoolean() ? 0xD9741E : 0xB5541C, 1.0f), x, y, z, 0.02, -0.04, 0); break;
                    case 3: U.particle(ParticleTypes.SNOWFLAKE, x, y, z, 0, -0.04, 0); break;
                    default: if (U.RNG.nextInt(6) == 0) U.particle(ParticleTypes.GLOW, x, y - 5, z, 0, 0.01, 0);
                }
            }
        }
    }

    // ================================================================= DAMAGE HOLOGRAMS
    static class DamageHolo extends Mod {
        static class H { Vec3d pos; String text; int color; long t0; double vx, vz; }
        final Setting dur = num("Время жизни (сек)", 1.4, 0.5, 4, 0.1), scale = num("Размер", 1.2, 0.5, 3, 0.1);
        final Setting heal = bool("Показывать лечение", false), mine = bool("Только мои удары", false), self = bool("Мой урон тоже", false);
        final Setting style = mode("Стиль", 0, "Числа", "Числа + сердечки");
        final List<H> list = new ArrayList<>(); final Map<Integer, Float> hp = new HashMap<>();
        int tick;
        DamageHolo() { super("Damage Holograms", "Всплывающие цифры урона над целью (3D, читаемы под любым углом)", Cat.VISUAL); }

        @Override public void onTick() {
            var mc = U.mc(); var p = mc.player; long now = System.currentTimeMillis();
            Set<Integer> live = new HashSet<>();
            for (Entity e : mc.world.getEntities()) {
                if (!(e instanceof LivingEntity le) || e.squaredDistanceTo(p) > 48 * 48) continue;
                if (e == p && !self.b) continue;
                live.add(e.getId());
                float h = le.getHealth() + le.getAbsorptionAmount();
                Float prev = hp.put(e.getId(), h);
                if (prev == null) continue;
                float d = prev - h;
                boolean recent = Modules.lastTarget == e && now - Modules.lastTargetTime < 1500;
                if (d > 0.05f && (!mine.b || recent)) add(le, "-" + fmt(d), d < 3 ? 0xFFFFFFFF : d < 6 ? 0xFFFFE066 : d < 10 ? 0xFFFF9A3C : 0xFFFF4D4D);
                else if (d < -0.05f && heal.b) add(le, "+" + fmt(-d), 0xFF7CFFB0);
            }
            if (++tick % 100 == 0) hp.keySet().retainAll(live);
        }
        String fmt(float d) { return Math.abs(d - Math.round(d)) < 0.05 ? String.valueOf(Math.round(d)) : String.format(Locale.ROOT, "%.1f", d); }
        void add(LivingEntity e, String s, int col) {
            H h = new H(); h.pos = new Vec3d(e.getX(), e.getY() + e.getHeight() + 0.2, e.getZ()); h.text = (style.mode == 1 ? "\u2764 " : "") + s; h.color = col; h.t0 = System.currentTimeMillis();
            h.vx = U.rnd(0.4); h.vz = U.rnd(0.4); list.add(h);
        }
        @Override public void onWorld(MatrixStack ms, Vec3d cam, float td) {
            long now = System.currentTimeMillis();
            list.removeIf(h -> (now - h.t0) / 1000.0 > dur.d());
            for (H h : list) {
                double age = (now - h.t0) / 1000.0, k = age / dur.d();
                Vec3d p = h.pos.add(h.vx * age, Math.sin(Math.min(1, k) * Math.PI * 0.5) * 1.0, h.vz * age);
                float a = k < 0.6 ? 1f : (float) (1 - (k - 0.6) / 0.4);
                billboard(ms, cam, p, h.text, U.argb(h.color, Math.max(0.05f, a)), scale.f() * (k < 0.12 ? 1.4f - (float) k * 3.3f : 1f), false);
            }
        }
    }

    // ================================================================= LIDLESS PEEK
    static class Peek extends Mod {
        final Map<BlockPos, List<ItemStack>> cache = new HashMap<>();
        BlockPos look;
        final Setting held = bool("Шалкер в руке", true), merge = bool("Сгруппировать предметы", true), scale = num("Размер", 1.0, 0.6, 2.0, 0.1);
        Peek() { super("Lidless Peek", "Содержимое сундука/шалкера рядом с прицелом без открытия (после первого открытия)", Cat.VISUAL); }

        @Override public void onTick() {
            var mc = U.mc();
            if (mc.currentScreen == null) {
                if (mc.crosshairTarget instanceof BlockHitResult br && br.getType() != HitResult.Type.MISS) look = br.getBlockPos();
            } else if (mc.currentScreen instanceof HandledScreen<?> hs && look != null) {
                var h = hs.getScreenHandler();
                int size = -1;
                if (h instanceof GenericContainerScreenHandler g) size = g.getRows() * 9; else if (h instanceof ShulkerBoxScreenHandler) size = 27;
                if (size > 0) { List<ItemStack> l = new ArrayList<>(); for (int i = 0; i < size; i++) l.add(h.slots.get(i).getStack().copy()); cache.put(look.toImmutable(), l); }
            }
        }

        List<ItemStack> current() {
            var mc = U.mc();
            if (mc.crosshairTarget instanceof BlockHitResult br && br.getType() != HitResult.Type.MISS) {
                List<ItemStack> l = cache.get(br.getBlockPos());
                if (l != null) return l;
            }
            if (held.b) {
                ItemStack s = mc.player.getMainHandStack();
                if (s.getItem() instanceof BlockItem bi && bi.getBlock() instanceof ShulkerBoxBlock) {
                    var cc = s.get(DataComponentTypes.CONTAINER);
                    if (cc != null) return cc.streamNonEmpty().toList();
                }
            }
            return null;
        }

        @Override public void onHud(DrawContext c, float td) {
            if (U.mc().currentScreen != null) return;
            List<ItemStack> l = current();
            if (l == null) return;
            List<ItemStack> show = new ArrayList<>(); List<Integer> counts = new ArrayList<>();
            if (merge.b) {
                LinkedHashMap<Item, Integer> m = new LinkedHashMap<>();
                for (ItemStack s : l) if (!s.isEmpty()) m.merge(s.getItem(), s.getCount(), Integer::sum);
                for (var e : m.entrySet()) { show.add(new ItemStack(e.getKey())); counts.add(e.getValue()); }
            } else for (ItemStack s : l) if (!s.isEmpty()) { show.add(s); counts.add(s.getCount()); }
            int cols = Math.max(1, Math.min(9, show.size())), rows = Math.max(1, (show.size() + 8) / 9);
            int ww = cols * 20 + 10, hh = rows * 20 + 20;
            int sw = c.getScaledWindowWidth(), sh = c.getScaledWindowHeight();
            var ms = c.getMatrices();
            ms.push(); ms.translate(sw / 2f + 18, sh / 2f - hh * scale.f() / 2f, 0); ms.scale(scale.f(), scale.f(), 1f);
            int a1 = ClientMods.accent(0), a2 = ClientMods.accent(1);
            D.rrectV(c, -1, -1, ww + 2, hh + 2, 10, D.a(a1, 0.7f), D.a(a2, 0.7f));
            D.rrectV(c, 0, 0, ww, hh, 9, 0xD8241830, 0xD8180D22);
            D.text(c, show.isEmpty() ? "Пусто" : "Содержимое (" + l.stream().filter(s -> !s.isEmpty()).count() + ")", 6, 4, 0xFFFFFFFF);
            for (int i = 0; i < show.size() && i < 27; i++) {
                int x = 5 + (i % 9) * 20, y = 16 + (i / 9) * 20;
                D.rrect(c, x, y, 18, 18, 4, 0x33FFFFFF);
                c.drawItem(show.get(i), x + 1, y + 1);
                int n = counts.get(i);
                if (n > 1) { String s = n >= 1000 ? (n / 1000) + "k" : String.valueOf(n); D.text(c, s, x + 17 - D.w(s), y + 9, 0xFFFFFFFF); }
            }
            ms.pop();
        }
    }

    // ================================================================= AUDIO RADAR
    static class AudioRadar extends Mod {
        static class S { double x, y, z; int type; long t0; }
        final List<S> sounds = new ArrayList<>();
        final Setting radius = num("Радиус кольца", 60, 30, 140, 2), life = num("Время жизни (сек)", 1.6, 0.5, 4, 0.1), range = num("Дальность звука", 32, 8, 64, 1);
        final Setting steps = bool("Шаги", true), mobs = bool("Мобы и крипер", true), combat = bool("Бой и стрелы", true), blocks = bool("Блоки и двери", true);
        static final int[] COL = {0xFF7CFFB0, 0xFFFF5A5A, 0xFFFFA040, 0xFF8FC8FF, 0xFFFFE066};

        AudioRadar() { super("Audio Radar", "Кольцо вокруг прицела: звуки превращаются в световые волны с направлением", Cat.VISUAL); }

        void add(SoundInstance si) {
            if (si.isRelative() || !U.in()) return;
            String id = si.getId().getPath();
            int type;
            if (id.contains("step") || id.contains("swim")) { if (!steps.b) return; type = 0; }
            else if (id.contains("creeper") || id.contains("explode") || id.contains("tnt") || id.contains("ghast") || id.contains("warden")) { if (!mobs.b) return; type = 1; }
            else if (id.contains("arrow") || id.contains("bow") || id.contains("crossbow") || id.contains("hurt") || id.contains("attack") || id.contains("hit") || id.contains("trident")) { if (!combat.b) return; type = 2; }
            else if (id.contains("door") || id.contains("chest") || id.contains("gate") || id.contains("barrel") || id.contains("break") || id.contains("place") || id.contains("shulker")) { if (!blocks.b) return; type = 3; }
            else if (id.startsWith("entity.") && !id.contains("experience")) { if (!mobs.b) return; type = 4; }
            else return;
            var p = U.p();
            double d = Math.sqrt(si.getX() * si.getX() * 0 + Math.pow(si.getX() - p.getX(), 2) + Math.pow(si.getZ() - p.getZ(), 2));
            if (d < 1.6 || d > range.d()) return;
            S s = new S(); s.x = si.getX(); s.y = si.getY(); s.z = si.getZ(); s.type = type; s.t0 = System.currentTimeMillis();
            sounds.add(s);
            if (sounds.size() > 60) sounds.remove(0);
        }

        @Override public void onHud(DrawContext c, float td) {
            var p = U.p(); long now = System.currentTimeMillis();
            sounds.removeIf(s -> (now - s.t0) / 1000.0 > life.d());
            int cx = c.getScaledWindowWidth() / 2, cy = c.getScaledWindowHeight() / 2;
            for (int i = 0; i < 48; i++) { double a = Math.PI * 2 * i / 48; c.fill(cx + (int) (Math.sin(a) * radius.d()), cy - (int) (Math.cos(a) * radius.d()), cx + (int) (Math.sin(a) * radius.d()) + 1, cy - (int) (Math.cos(a) * radius.d()) + 1, 0x22FFFFFF); }
            for (S s : sounds) {
                double dx = s.x - p.getX(), dz = s.z - p.getZ(), dist = Math.hypot(dx, dz);
                double rel = MathHelper.wrapDegrees((float) (Math.toDegrees(Math.atan2(-dx, dz)) - p.getYaw()));
                double age = (now - s.t0) / 1000.0 / life.d();
                float alpha = (float) (1 - age) * (float) Math.max(0.35, 1 - dist / range.d());
                double spread = 14 + 22 * Math.max(0, 1 - dist / range.d());
                for (int w = 0; w < 3; w++) {
                    double R = radius.d() + w * 4 + age * 10;
                    for (double k = -spread; k <= spread; k += 3) {
                        double a = Math.toRadians(rel + k);
                        float fall = (float) (1 - Math.abs(k) / spread) * (1f - w * 0.28f);
                        int x = cx + (int) (Math.sin(a) * R), y = cy - (int) (Math.cos(a) * R);
                        c.fill(x - 1, y - 1, x + 2, y + 2, D.a(COL[s.type], alpha * fall));
                    }
                }
            }
        }
    }

    // ================================================================= LIQUID UI
    static class LiquidUI extends Mod {
        final float[] cur = new float[5], vel = new float[5];
        final Setting numbers = bool("Числа на полосках", true), wave = bool("Волна жидкости", true);
        LiquidUI() { super("Liquid UI", "Здоровье, броня, голод и опыт — жидкие полоски с покачиванием", Cat.VISUAL); }

        void bar(DrawContext c, int i, int x, int y, int w, int h, float frac, int c1, int c2, String label) {
            vel[i] += (frac - cur[i]) * 0.10f; vel[i] *= 0.80f; cur[i] += vel[i];
            float v = Math.max(0f, Math.min(1.04f, cur[i]));
            float t = Sakura.time();
            D.rrect(c, x - 1, y - 1, w + 2, h + 2, (h + 2) / 2, D.a(0xFFFFFFFF, 0.28f));
            D.rrect(c, x, y, w, h, h / 2, 0xDD0C0810);
            int fw = (int) (w * Math.min(1f, v));
            if (fw > 2) {
                D.rrectV(c, x, y, Math.max(h, fw), h, h / 2, c1, c2);
                if (wave.b) {
                    for (int i2 = 0; i2 < Math.max(h, fw); i2 += 2) {
                        int yy = y + h / 2 + (int) (Math.sin(i2 * 0.45 + t * 4 + i) * (h * 0.22));
                        c.fill(x + i2, yy, x + i2 + 2, yy + 1, 0x44FFFFFF);
                    }
                    for (int k = 0; k < 4; k++) { int off = (int) (Math.sin(t * 5 + k) * 1.5 * (1 - k / 4f)); c.fill(x + fw - k, y + 1 + off, x + fw - k + 1, y + h - 1 + off, c1); }
                }
                c.fill(x + h / 2, y + 1, x + Math.max(h / 2 + 1, fw - h / 2), y + 2, 0x66FFFFFF);
            }
            if (numbers.b && label != null) D.text(c, label, x + w / 2 - D.w(label) / 2, y + (h - 9) / 2 + 1, 0xFFFFFFFF);
        }

        @Override public void onHud(DrawContext c, float td) {
            var p = U.p();
            int sw = c.getScaledWindowWidth(), sh = c.getScaledWindowHeight();
            float hp = p.getHealth() + p.getAbsorptionAmount(), max = Math.max(1f, p.getMaxHealth());
            boolean abs = p.getAbsorptionAmount() > 0;
            int y = sh - 48;
            bar(c, 0, sw / 2 - 91, y, 88, 10, hp / max, abs ? 0xFFFFD86B : 0xFFFF4D6A, abs ? 0xFFFFF0A0 : 0xFFFF9EB0, String.valueOf((int) Math.ceil(hp)));
            bar(c, 1, sw / 2 + 3, y, 88, 10, p.getHungerManager().getFoodLevel() / 20f, 0xFFFF9A3C, 0xFFFFD08A, String.valueOf(p.getHungerManager().getFoodLevel()));
            if (p.getArmor() > 0) bar(c, 2, sw / 2 - 91, y - 11, 88, 6, p.getArmor() / 20f, 0xFF8FA8D8, 0xFFD0E0FF, null);
            if (p.getAir() < p.getMaxAir()) bar(c, 3, sw / 2 + 3, y - 11, 88, 6, p.getAir() / (float) p.getMaxAir(), 0xFF4DB8FF, 0xFFB0E4FF, null);
            bar(c, 4, sw / 2 - 91, sh - 31, 182, 6, p.experienceProgress, 0xFF7CFF8A, 0xFFD0FFB0, null);
            if (p.experienceLevel > 0) { String l = String.valueOf(p.experienceLevel); D.text(c, l, sw / 2 - D.w(l) / 2, sh - 44 - 12 + 12 - 10 + 0, 0xFF9DFF9D); }
        }
    }

    // ================================================================= WIND TRAILS
    static class WindTrails extends Mod {
        static class W { Vec3d pos, vel; long t0; float len; }
        final List<W> list = new ArrayList<>();
        final Setting density = num("Плотность", 6, 1, 20, 1), cocoon = bool("Воздушный кокон", true), storm = bool("Ветер в грозу", true), high = bool("Ветер высоко в горах", true);
        final Setting col = color("Цвет", 200);
        WindTrails() { super("Wind Trails", "Потоки воздуха: в грозу, на высоте и при падении или полёте на элитрах", Cat.VISUAL); }

        double speed() { var p = U.p(); return p.getVelocity().length(); }
        @Override public void onTick() {
            var p = U.p(); var w = U.mc().world; long now = System.currentTimeMillis();
            list.removeIf(x -> now - x.t0 > 900);
            double sp = speed();
            boolean fast = sp > 0.7 || p.isGliding();
            boolean st = storm.b && w.isThundering(), hi = high.b && p.getY() > 150;
            if (!fast && !st && !hi) return;
            Vec3d dir = fast ? p.getVelocity().normalize().multiply(-1) : new Vec3d(-0.8, 0, 0.35).normalize();
            double v = fast ? sp * 1.2 : 0.8;
            for (int i = 0; i < density.i(); i++) {
                W x = new W();
                x.pos = p.getPos().add(U.rnd(4), 0.5 + U.RNG.nextDouble() * 2.5 + (fast ? 0 : U.RNG.nextDouble() * 4), U.rnd(4)).add(dir.multiply(-3));
                x.vel = dir.multiply(v); x.t0 = now; x.len = 1.5f + U.RNG.nextFloat() * 3f; list.add(x);
            }
        }
        @Override public void onWorld(MatrixStack ms, Vec3d cam, float td) {
            long now = System.currentTimeMillis(); var p = U.p();
            R3.begin(ms, cam, false);
            var l = R3.lines(1.6f);
            for (W x : list) {
                double age = (now - x.t0) / 1000.0;
                Vec3d head = x.pos.add(x.vel.multiply(age * 20)), tail = head.subtract(x.vel.normalize().multiply(x.len));
                float a = (float) Math.sin(Math.min(1, age / 0.9) * Math.PI) * 0.5f;
                l.line(tail, head, U.argb(col.rgb(), a * 0.1f)); l.v(head, U.argb(col.rgb(), a));
                l.v(head.add(0, 0.001, 0), U.argb(col.rgb(), 0));
            }
            if (cocoon.b && (speed() > 0.9 || p.isGliding())) {
                Vec3d v = p.getVelocity().normalize(), c0 = R3.lerpPos(p, td).add(0, p.getHeight() * 0.5, 0);
                Vec3d u = v.crossProduct(Math.abs(v.y) > 0.9 ? new Vec3d(1, 0, 0) : new Vec3d(0, 1, 0)).normalize(), w2 = v.crossProduct(u);
                for (int r = 0; r < 5; r++) {
                    double off = -0.2 - r * 0.7, rad = 0.7 + r * 0.17;
                    float a = 0.55f - r * 0.1f;
                    for (int k = 0; k < 28; k++) {
                        double a0 = Math.PI * 2 * k / 28, a1 = Math.PI * 2 * (k + 1) / 28;
                        l.line(c0.add(v.multiply(off)).add(u.multiply(Math.cos(a0) * rad)).add(w2.multiply(Math.sin(a0) * rad)),
                            c0.add(v.multiply(off)).add(u.multiply(Math.cos(a1) * rad)).add(w2.multiply(Math.sin(a1) * rad)), U.argb(col.rgb(r), a));
                    }
                }
                for (int k = 0; k < 8; k++) { double a = Math.PI * 2 * k / 8; l.line(c0.add(v.multiply(0.8)), c0.add(v.multiply(-2.8)).add(u.multiply(Math.cos(a) * 1.1)).add(w2.multiply(Math.sin(a) * 1.1)), U.argb(col.rgb(), 0.25f)); }
            }
            l.draw();
            R3.end(ms);
        }
    }

    // ================================================================= SMART ITEM HOLOGRAM
    static class ItemHolo extends Mod {
        final Setting range = num("Дальность", 20, 6, 48, 1), scale = num("Размер", 1.0, 0.5, 2.5, 0.1);
        final Setting only = mode("Какие предметы", 0, "Все", "Редкие и зачарованные");
        final Setting ench = bool("Показывать зачарования", true), bg = bool("Фон", true);
        ItemHolo() { super("Smart Item Hologram", "Над выброшенным предметом: название, редкость и зачарования (читается под любым углом)", Cat.VISUAL); }
        @Override public void onWorld(MatrixStack ms, Vec3d cam, float td) {
            var p = U.p(); double t = System.currentTimeMillis() / 1000.0;
            for (Entity e : U.mc().world.getEntities()) {
                if (!(e instanceof ItemEntity ie) || e.squaredDistanceTo(p) > range.d() * range.d()) continue;
                ItemStack s = ie.getStack();
                boolean rare = s.getRarity() != net.minecraft.util.Rarity.COMMON || s.hasEnchantments();
                if (only.mode == 1 && !rare) continue;
                Integer rc = s.getRarity().getFormatting().getColorValue();
                int col = 0xFF000000 | (rc == null ? 0xFFFFFF : rc);
                Vec3d base = R3.lerpPos(e, td).add(0, 0.55 + Math.sin(t * 2 + e.getId()) * 0.06, 0);
                int line = 0;
                String name = s.getName().getString() + (s.getCount() > 1 ? "  x" + s.getCount() : "");
                billboard(ms, cam, base.add(0, 0.28 * (ench.b && s.hasEnchantments() ? 1.0 : 0.0) + 0.0, 0), name, col, 0.7f * scale.f(), bg.b);
                if (ench.b && s.hasEnchantments()) {
                    var comp = s.getEnchantments();
                    StringBuilder sb = new StringBuilder("\u2726 ");
                    int n = 0;
                    for (var en : comp.getEnchantments()) { if (n++ >= 2) { sb.append("..."); break; } sb.append(en.value().description().getString()).append(' ').append(comp.getLevel(en)).append("  "); }
                    billboard(ms, cam, base.add(0, -0.02, 0), sb.toString().trim(), 0xFFB8A0FF, 0.55f * scale.f(), bg.b);
                }
            }
        }
    }

    // ================================================================= CHUNK SCANNER
    static class ChunkScanner extends Mod {
        final Setting mode = mode("Режим", 2, "Границы чанка", "Скан по земле", "Оба");
        final Setting radius = num("Радиус (чанки)", 1, 0, 3, 1), speed = num("Скорость волны", 1.0, 0.3, 3.0, 0.1);
        final Setting col = color("Цвет", 190), height = num("Высота сетки", 24, 8, 64, 4), cell = num("Размер ячейки", 2, 1, 4, 1);
        ChunkScanner() { super("Chunk Scanner", "Лазерная сетка: волна сканирования по границам чанков и по земле", Cat.VISUAL); }

        @Override public void onWorld(MatrixStack ms, Vec3d cam, float td) {
            var p = U.p(); var w = U.mc().world;
            double t = System.currentTimeMillis() / 1000.0 * speed.d();
            Vec3d pp = R3.lerpPos(p, td);
            int cx = (int) Math.floor(pp.x) >> 4, cz = (int) Math.floor(pp.z) >> 4, R = radius.i();
            R3.begin(ms, cam, false);
            var l = R3.lines(1.4f);
            if (mode.mode != 1) {
                double sweep = pp.y - height.d() + ((t * 10) % (height.d() * 2));
                int st = cell.i();
                for (int dx = -R; dx <= R + 1; dx++) for (int dz = -R; dz <= R; dz++) edgeX(l, (cx + dx) * 16, (cz + dz) * 16, pp.y, sweep, st);
                for (int dx = -R; dx <= R; dx++) for (int dz = -R; dz <= R + 1; dz++) edgeZ(l, (cx + dx) * 16, (cz + dz) * 16, pp.y, sweep, st);
            }
            if (mode.mode != 0) {
                double rw = (t * 6) % 26;
                for (int k = 0; k < 3; k++) {
                    double rr = rw - k * 2.0; if (rr < 1) continue;
                    float a = (float) (1 - rr / 26.0) * (0.9f - k * 0.3f);
                    Vec3d prev = null;
                    for (int s = 0; s <= 72; s++) {
                        double ang = Math.PI * 2 * s / 72;
                        int bx = (int) Math.floor(pp.x + Math.cos(ang) * rr), bz = (int) Math.floor(pp.z + Math.sin(ang) * rr);
                        double y = w.getTopY(Heightmap.Type.WORLD_SURFACE, bx, bz) + 0.05;
                        if (Math.abs(y - pp.y) > 12) y = pp.y - 0.9;
                        Vec3d cur = new Vec3d(pp.x + Math.cos(ang) * rr, y, pp.z + Math.sin(ang) * rr);
                        if (prev != null) l.line(prev, cur, U.argb(col.rgb(k), Math.max(0, a)));
                        prev = cur;
                    }
                }
            }
            l.draw();
            R3.end(ms);
        }
        void edgeX(R3.Batch l, int x, int z0, double py, double sweep, int st) {
            double h = height.d();
            for (double y = py - h; y <= py + h; y += st) { float a = alpha(y, sweep); l.line(new Vec3d(x, y, z0), new Vec3d(x, y, z0 + 16), U.argb(col.rgb(), a)); }
            for (int z = 0; z <= 16; z += st) l.line(new Vec3d(x, py - h, z0 + z), new Vec3d(x, py + h, z0 + z), U.argb(col.rgb(), 0.10f));
        }
        void edgeZ(R3.Batch l, int x0, int z, double py, double sweep, int st) {
            double h = height.d();
            for (double y = py - h; y <= py + h; y += st) { float a = alpha(y, sweep); l.line(new Vec3d(x0, y, z), new Vec3d(x0 + 16, y, z), U.argb(col.rgb(), a)); }
            for (int x = 0; x <= 16; x += st) l.line(new Vec3d(x0 + x, py - h, z), new Vec3d(x0 + x, py + h, z), U.argb(col.rgb(), 0.10f));
        }
        float alpha(double y, double sweep) { double d = (y - sweep) / 3.5; return (float) (0.07 + 0.9 * Math.exp(-d * d)); }
    }

    // ================================================================= TRACERS
    static class Tracers extends Mod {
        final Setting who = mode("Цели", 0, "Игроки", "Мобы", "Все живые");
        final Setting thick = num("Толщина", 1.5, 0.5, 4, 0.5), range = num("Дальность", 64, 8, 160, 4);
        final Setting colour = color("Цвет", 340), byDist = bool("Цвет по дистанции", false), friends = bool("Друзья зелёным", true);
        Tracers() { super("Tracers", "Линии от прицела к игрокам и мобам", Cat.VISUAL); }
        @Override public void onWorld(MatrixStack ms, Vec3d cam, float td) {
            var p = U.p();
            Vec3d from = cam.add(p.getRotationVec(td).multiply(0.6));
            R3.begin(ms, cam, false);
            var l = R3.lines(thick.f());
            int i = 0;
            for (Entity e : U.mc().world.getEntities()) {
                if (e == p || !(e instanceof LivingEntity) || e.squaredDistanceTo(p) > range.d() * range.d()) continue;
                boolean pl = e instanceof PlayerEntity;
                if ((who.mode == 0 && !pl) || (who.mode == 1 && pl)) continue;
                int c = friends.b && Friends.is(e) ? 0xFF55FF88 : byDist.b ? hsb((float) Math.min(0.33, e.distanceTo(p) / range.d() * 0.33), 0.7f, 1f) : colour.rgb(i++);
                l.line(from, R3.lerpPos(e, td).add(0, e.getHeight() * 0.5, 0), U.argb(c, 0.85f));
            }
            l.draw();
            R3.end(ms);
        }
    }

    // ================================================================= STORAGE ESP
    static class StorageESP extends Mod {
        final Setting chests = bool("Сундуки", true), barrels = bool("Бочки", true), shulkers = bool("Шалкеры", true), ender = bool("Эндер-сундуки", true);
        final Setting range = num("Радиус (чанки)", 4, 1, 8, 1), fill = bool("Заливка", true), thick = num("Толщина", 1.5, 1, 4, 0.5), through = bool("Сквозь стены", true);
        final List<Object[]> cache = new ArrayList<>(); long last;
        StorageESP() { super("Storage ESP", "Подсвечивает сундуки, бочки и шалкеры в округе", Cat.VISUAL); }
        @Override public void onWorld(MatrixStack ms, Vec3d cam, float td) {
            var mc = U.mc(); long now = System.currentTimeMillis();
            if (now - last > 600) {
                last = now; cache.clear();
                int pcx = mc.player.getChunkPos().x, pcz = mc.player.getChunkPos().z, R = range.i();
                for (int dx = -R; dx <= R; dx++) for (int dz = -R; dz <= R; dz++) {
                    WorldChunk ch = mc.world.getChunkManager().getWorldChunk(pcx + dx, pcz + dz);
                    if (ch == null) continue;
                    for (BlockEntity be : ch.getBlockEntities().values()) {
                        int type = -1;
                        if (be instanceof ChestBlockEntity && chests.b) type = 0; else if (be instanceof BarrelBlockEntity && barrels.b) type = 1;
                        else if (be instanceof ShulkerBoxBlockEntity && shulkers.b) type = 2; else if (be instanceof EnderChestBlockEntity && ender.b) type = 3;
                        if (type < 0) continue;
                        BlockPos pos = be.getPos(); BlockState st = mc.world.getBlockState(pos);
                        var sh = st.getOutlineShape(mc.world, pos);
                        Box b = (sh.isEmpty() ? new Box(0, 0, 0, 1, 1, 1) : sh.getBoundingBox()).offset(pos);
                        cache.add(new Object[]{b, type});
                    }
                }
            }
            int[] cols = {0xFFFFB84D, 0xFFB5733C, 0xFFFF7AD9, 0xFFB48CFF};
            R3.begin(ms, cam, !through.b);
            var l = R3.lines(thick.f()); var q = R3.quads();
            for (Object[] o : cache) {
                Box b = (Box) o[0]; int c = cols[(Integer) o[1]];
                R3.boxLines(l, b, c);
                if (fill.b) R3.boxFill(q, b, U.argb(c, 0.16f));
            }
            q.draw(); l.draw();
            R3.end(ms);
        }
    }

    // ================================================================= ZOOM
    static class Zoom extends Mod {
        final Setting fov = num("FOV при зуме", 30, 30, 90, 1);
        final Setting smooth = bool("Плавно", true);
        int orig = -1; float curFov;
        Zoom() { super("Zoom", "Приближение камеры (по умолчанию удержание C; бинд меняется)", Cat.VISUAL); key = org.lwjgl.glfw.GLFW.GLFW_KEY_C; bindMode.mode = 1; }
        @Override public void onEnable() { var o = U.mc().options.getFov(); orig = o.getValue(); curFov = orig; }
        @Override public void onDisable() { if (orig > 0) U.mc().options.getFov().setValue(orig); orig = -1; }
        @Override public void onTick() {
            var o = U.mc().options.getFov();
            if (orig < 0) return;
            float target = fov.f();
            curFov = smooth.b ? curFov + (target - curFov) * 0.35f : target;
            o.setValue(Math.max(30, Math.min(110, Math.round(curFov))));
        }
    }

    // ================================================================= LOW HEALTH
    static class LowHealth extends Mod {
        final Setting hp = num("Порог HP", 7, 2, 19, 1), pulse = bool("Пульсация", true), size = num("Ширина края", 60, 20, 160, 5);
        final Setting col = color("Цвет", 350);
        LowHealth() { super("Low Health Alert", "Красное свечение по краям экрана при низком здоровье", Cat.VISUAL); }
        @Override public void onHud(DrawContext c, float td) {
            var p = U.p();
            if (p.getHealth() > hp.f()) return;
            float k = 1f - p.getHealth() / hp.f();
            float a = (0.25f + 0.5f * k) * (pulse.b ? (float) (0.7 + 0.3 * Math.sin(System.currentTimeMillis() / 180.0)) : 1f);
            int sw = c.getScaledWindowWidth(), sh = c.getScaledWindowHeight(), s = size.i(), col1 = U.argb(col.rgb(), a), col0 = U.argb(col.rgb(), 0f);
            c.fillGradient(0, 0, sw, s, col1, col0); c.fillGradient(0, sh - s, sw, sh, col0, col1);
            for (int i = 0; i < s; i++) { int al = (int) (((col1 >>> 24) & 255) * (1 - i / (float) s)); int cc = (al << 24) | (col.rgb() & 0xFFFFFF); c.fill(i, s, i + 1, sh - s, cc); c.fill(sw - i - 1, s, sw - i, sh - s, cc); }
        }
    }

    // ================================================================= WAYPOINTS
    static class Waypoints extends Mod {
        final Setting beam = bool("Луч в небо", true), label = bool("Подпись с расстоянием", true), max = num("Макс. расстояние", 2000, 50, 10000, 50), scale = num("Размер подписи", 1.0, 0.5, 2.5, 0.1);
        Waypoints() { super("Waypoints", "Метки в мире: луч, название и расстояние (/lv mark add)", Cat.VISUAL); enabled = true; }
        @Override public void onWorld(MatrixStack ms, Vec3d cam, float td) {
            var p = U.p(); List<Tools.Mark> marks = Tools.visibleMarks();
            if (marks.isEmpty()) return;
            if (beam.b) {
                R3.begin(ms, cam, false);
                var q = R3.quads();
                for (Tools.Mark m : marks) {
                    double d = Math.sqrt(p.squaredDistanceTo(m.x, m.y, m.z)); if (d > max.d()) continue;
                    double wd = 0.18 + Math.min(1.0, d / 80.0) * 0.1, h = 80;
                    int c0 = U.argb(m.color, 0.85f), c1 = U.argb(m.color, 0f);
                    q.v(new Vec3d(m.x - wd, m.y, m.z), c0); q.v(new Vec3d(m.x + wd, m.y, m.z), c0); q.v(new Vec3d(m.x + wd, m.y + h, m.z), c1); q.v(new Vec3d(m.x - wd, m.y + h, m.z), c1);
                    q.v(new Vec3d(m.x, m.y, m.z - wd), c0); q.v(new Vec3d(m.x, m.y, m.z + wd), c0); q.v(new Vec3d(m.x, m.y + h, m.z + wd), c1); q.v(new Vec3d(m.x, m.y + h, m.z - wd), c1);
                }
                q.draw(); R3.end(ms);
            }
            if (label.b) for (Tools.Mark m : marks) {
                double d = Math.sqrt(p.squaredDistanceTo(m.x, m.y, m.z)); if (d > max.d()) continue;
                double k = Math.max(1.0, Math.min(6.0, d / 12.0));
                billboard(ms, cam, new Vec3d(m.x, m.y + 2.4 + k * 0.1, m.z), m.name + "  " + (int) d + "m", m.color, (float) (scale.d() * Math.min(3.0, 0.8 + k * 0.35)), true);
            }
        }
    }

    // ================================================================= TARGET ESP
    static class TargetESP extends Mod {
        final Setting style = mode("Вид", 0, "Кольцо", "Пирамида", "Круг на земле", "Нимб", "Сфера", "Кристаллы", "Лазеры", "HP-дуга", "Стрелка-маркер");
        final Setting col = color("Цвет", 335), size = num("Размер", 1.0, 0.5, 2.5, 0.1), speed = num("Скорость", 1.0, 0.2, 4, 0.1);
        final Setting hpCol = bool("Цвет по здоровью", false), glow = bool("Свечение", true), friends = bool("Друзья зелёным", true);
        TargetESP() { super("Target ESP", "Подсветка цели: 9 эффектов, цвет по здоровью, свечение", Cat.VISUAL); }

        Entity target() {
            Entity t = Modules.lastTarget;
            if (t != null && System.currentTimeMillis() - Modules.lastTargetTime < 6000 && t.isAlive() && !t.isRemoved()) return t;
            if (U.mc().crosshairTarget instanceof net.minecraft.util.hit.EntityHitResult er && er.getEntity() instanceof LivingEntity) return er.getEntity();
            return null;
        }
        int c(int i, Entity t) {
            if (friends.b && Friends.is(t)) return 0xFF55FF88;
            if (hpCol.b && t instanceof LivingEntity le) return U.lerpColor(0xFFFF4D6A, 0xFF7CFFB0, Math.max(0, Math.min(1, le.getHealth() / Math.max(1f, le.getMaxHealth()))));
            return col.rgb(i);
        }

        @Override public void onWorld(MatrixStack ms, Vec3d cam, float td) {
            Entity t = target();
            if (t == null) return;
            Vec3d pos = R3.lerpPos(t, td);
            double w = t.getWidth() * 0.9 * size.d(), h = t.getHeight(), time = System.currentTimeMillis() / 1000.0 * speed.d();
            R3.begin(ms, cam, false);
            var q = R3.quads(); var tr = R3.tris(); var l = R3.lines(2f);
            switch (style.mode) {
                case 0: {
                    double y = (Math.sin(time * 2.2) * 0.5 + 0.5) * h;
                    for (int k = 0; k < 7; k++) {
                        double yy = y - k * 0.04 * (Math.cos(time * 2.2) >= 0 ? 1 : -1);
                        R3.ringFill(q, pos.add(0, Math.max(0, Math.min(h, yy)), 0), w - 0.04, w + 0.04, 36, U.argb(c(k, t), 0.9f - k * 0.12f), U.argb(c(k + 1, t), 0.9f - k * 0.12f));
                    }
                    break;
                }
                case 1: case 8: {
                    boolean down = style.mode == 8;
                    Vec3d top = pos.add(0, h + 0.65 * size.d(), 0), apex = pos.add(0, h + (down ? 0.2 : 0.15), 0);
                    double r = (down ? 0.18 : 0.22) * size.d(), rot = time * 2.5;
                    if (down) { Vec3d tip = pos.add(0, h + 0.25 + Math.sin(time * 3) * 0.08, 0); Vec3d bs = tip.add(0, 0.55 * size.d(), 0);
                        for (int i = 0; i < 6; i++) { double a0 = rot + Math.PI * 2 * i / 6, a1 = rot + Math.PI * 2 * (i + 1) / 6; tr.v(tip, U.argb(c(0, t), 0.95f)); tr.v(bs.add(Math.cos(a0) * r, 0, Math.sin(a0) * r), U.argb(c(i, t), 0.8f)); tr.v(bs.add(Math.cos(a1) * r, 0, Math.sin(a1) * r), U.argb(c(i + 1, t), 0.8f)); } }
                    else for (int i = 0; i < 4; i++) { double a0 = rot + Math.PI / 2 * i, a1 = rot + Math.PI / 2 * (i + 1);
                        tr.v(apex, U.argb(c(0, t), 0.95f)); tr.v(top.add(Math.cos(a0) * r, 0, Math.sin(a0) * r), U.argb(c(i, t), 0.8f)); tr.v(top.add(Math.cos(a1) * r, 0, Math.sin(a1) * r), U.argb(c(i + 1, t), 0.8f)); }
                    break;
                }
                case 2: {
                    double rot = time * 1.5;
                    for (int i = 0; i < 12; i++) { double a0 = rot + Math.PI * 2 * i / 12, a1 = a0 + Math.PI * 2 / 12 * 0.6; Vec3d g = pos.add(0, 0.03, 0);
                        q.quad(g.add(Math.cos(a0) * (w + 0.3), 0, Math.sin(a0) * (w + 0.3)), g.add(Math.cos(a1) * (w + 0.3), 0, Math.sin(a1) * (w + 0.3)), g.add(Math.cos(a1) * (w + 0.45), 0, Math.sin(a1) * (w + 0.45)), g.add(Math.cos(a0) * (w + 0.45), 0, Math.sin(a0) * (w + 0.45)), U.argb(c(i, t), 0.9f)); }
                    break;
                }
                case 3: R3.ringFill(q, pos.add(0, h + 0.3 + Math.sin(time * 3) * 0.04, 0), 0.2 * size.d(), 0.3 * size.d(), 32, U.argb(c(0, t), 0.95f), U.argb(c(3, t), 0.95f)); break;
                case 4: {
                    Vec3d ctr = pos.add(0, h / 2, 0); double r = Math.max(t.getWidth(), h) * 0.75 * size.d(), rot = time;
                    for (int la = 1; la < 6; la++) { double el = Math.PI * la / 6 - Math.PI / 2, rr = r * Math.cos(el), yy = r * Math.sin(el); R3.ringLines(l, ctr.add(0, yy, 0), rr, 32, U.argb(c(la, t), 0.8f)); }
                    for (int m = 0; m < 8; m++) { double az = rot + Math.PI * m / 8; Vec3d prev = null;
                        for (int k = 0; k <= 16; k++) { double el = Math.PI * k / 16 - Math.PI / 2; Vec3d p2 = ctr.add(Math.cos(el) * Math.cos(az) * r, Math.sin(el) * r, Math.cos(el) * Math.sin(az) * r); if (prev != null) l.line(prev, p2, U.argb(c(m, t), 0.6f)); prev = p2; } }
                    break;
                }
                case 5: {
                    for (int k = 0; k < 3; k++) {
                        double a = time * 1.6 + k * Math.PI * 2 / 3; Vec3d ctr = pos.add(Math.cos(a) * (w + 0.4), h * 0.5 + Math.sin(time * 2 + k) * 0.3, Math.sin(a) * (w + 0.4)); double s = 0.16 * size.d();
                        Vec3d top = ctr.add(0, s * 1.6, 0), bot = ctr.add(0, -s * 1.6, 0);
                        Vec3d[] eq = {ctr.add(s, 0, 0), ctr.add(0, 0, s), ctr.add(-s, 0, 0), ctr.add(0, 0, -s)};
                        for (int i = 0; i < 4; i++) { tr.tri(top, eq[i], eq[(i + 1) % 4], U.argb(c(k, t), 0.9f)); tr.tri(bot, eq[i], eq[(i + 1) % 4], U.argb(c(k + 2, t), 0.8f)); }
                    }
                    break;
                }
                case 6: {
                    for (int k = 0; k < 4; k++) { double a = time * 1.2 + k * Math.PI / 2, wd = 0.04 + 0.02 * Math.sin(time * 4 + k); double bx = pos.x + Math.cos(a) * (w + 0.6), bz = pos.z + Math.sin(a) * (w + 0.6);
                        q.v(new Vec3d(bx - wd, pos.y, bz), U.argb(c(k, t), 0.9f)); q.v(new Vec3d(bx + wd, pos.y, bz), U.argb(c(k, t), 0.9f)); q.v(new Vec3d(bx + wd, pos.y + 4, bz), U.argb(c(k, t), 0f)); q.v(new Vec3d(bx - wd, pos.y + 4, bz), U.argb(c(k, t), 0f)); }
                    R3.ringFill(q, pos.add(0, 0.03, 0), w + 0.55, w + 0.65, 40, U.argb(c(0, t), 0.8f), U.argb(c(2, t), 0.8f));
                    break;
                }
                default: {
                    float frac = t instanceof LivingEntity le ? Math.max(0, Math.min(1, le.getHealth() / Math.max(1f, le.getMaxHealth()))) : 1f;
                    Vec3d g = pos.add(0, 0.04, 0);
                    R3.ringFill(q, g, w + 0.3, w + 0.5, 48, 0x55FFFFFF, 0x55FFFFFF);
                    int n = (int) (48 * frac);
                    for (int i = 0; i < n; i++) { double a0 = Math.PI * 2 * i / 48, a1 = Math.PI * 2 * (i + 1) / 48; int cc = U.argb(U.lerpColor(0xFFFF4D6A, 0xFF7CFFB0, frac), 0.95f);
                        q.quad(g.add(Math.cos(a0) * (w + 0.3), 0, Math.sin(a0) * (w + 0.3)), g.add(Math.cos(a1) * (w + 0.3), 0, Math.sin(a1) * (w + 0.3)), g.add(Math.cos(a1) * (w + 0.5), 0, Math.sin(a1) * (w + 0.5)), g.add(Math.cos(a0) * (w + 0.5), 0, Math.sin(a0) * (w + 0.5)), cc); }
                }
            }
            if (glow.b) R3.boxFill(q, t.getBoundingBox().offset(pos.subtract(t.getPos())).expand(0.05), U.argb(c(0, t), 0.10f));
            q.draw(); tr.draw(); l.draw();
            R3.end(ms);
        }
    }
}
