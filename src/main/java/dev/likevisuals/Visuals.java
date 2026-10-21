package dev.likevisuals;

import dev.likevisuals.mixin.SimpleOptionAccessor;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.Arm;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public final class Visuals {
    public static final String[] PN = {"Сердца", "Крит", "Звёзды", "Огонь", "Души", "Снег", "Искры", "Портал", "Облако"};
    public static final ParticleEffect[] PE = {ParticleTypes.HEART, ParticleTypes.CRIT, ParticleTypes.END_ROD, ParticleTypes.FLAME, ParticleTypes.SOUL_FIRE_FLAME,
        ParticleTypes.SNOWFLAKE, ParticleTypes.ELECTRIC_SPARK, ParticleTypes.PORTAL, ParticleTypes.CLOUD};

    public static void register() {
        Modules.add(new FullBright()); Modules.add(new Ambience()); Modules.add(new TimeChanger()); Modules.add(new WeatherChanger());
        Modules.add(new AspectRatio()); Modules.add(new ViewModel()); Modules.add(new SwingAnimation()); Modules.add(new Crosshair());
        Modules.add(new NoRender()); Modules.add(new Optimization()); Modules.add(new Particles()); Modules.add(new WorldParticles());
        Modules.add(new TotemParticles()); Modules.add(new HitBubble()); Modules.add(new HitColor()); Modules.add(new KillEffects());
        Modules.add(new SelfNameTag());
    }

    // ===== hooks for mixins (keep mixins thin) =====
    public static Integer skyRgb() {
        Ambience a = Modules.get(Ambience.class);
        if (a != null && a.enabled) return a.rgb();
        return Visuals3.skySeasonOrShader();
    }
    public static long forcedTime() { TimeChanger t = Modules.get(TimeChanger.class); return t != null && t.enabled ? t.forced() : -1; }
    public static void applyAspect(Matrix4f m) {
        AspectRatio a = Modules.get(AspectRatio.class);
        if (a == null || !a.enabled) return;
        var w = U.mc().getWindow();
        float cur = (float) w.getFramebufferWidth() / Math.max(1, w.getFramebufferHeight());
        m.scale(cur / a.target(), 1f, 1f);
    }
    public static void applyViewModel(MatrixStack m) {
        ViewModel v = Modules.get(ViewModel.class);
        if (v == null || !v.enabled) return;
        m.translate(v.x.f(), v.y.f(), v.z.f());
        m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(v.rx.f()));
        m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(v.ry.f()));
        m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(v.rz.f()));
        float s = v.scale.f(); m.scale(s, s, s);
    }
    public static boolean applySwing(MatrixStack m, Arm arm, float f) {
        SwingAnimation s = Modules.get(SwingAnimation.class);
        if (s == null || !s.enabled || s.style.mode == 0) return false;
        f = (float) Math.pow(Math.max(0f, Math.min(1f, f)), 1.0 / s.speed.d());
        float side = arm == Arm.RIGHT ? 1 : -1, p = s.power.f();
        float sw = (float) Math.sin(f * Math.PI) * p, sq = (float) Math.sin(Math.sqrt(f) * Math.PI) * p;
        switch (s.style.mode) {
            case 1: m.translate(0, -0.15f * sw, -0.1f * sw); m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-85f * sw)); break;
            case 2: m.translate(0, 0, -0.75f * sw); break;
            case 3: m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * f * 360f * p)); m.translate(0, -0.1f * sw, -0.2f * sw); break;
            case 4: m.translate(side * 0.2f * sq, -0.15f * sq, -0.3f * sq); m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * 25f * sq)); break;
            case 5: m.translate(side * -0.3f * sw, 0, -0.4f * sw); m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * -70f * sw)); break;
            case 6: m.translate(side * 0.15f * sw, -0.1f * sw, -0.25f * sw); m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * -60f * sw)); m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-50f * sw)); break;
            case 7: m.translate(0, 0.1f * sw, -0.5f * sw); m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-120f * sq)); break;
            case 8: m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * f * 360f * p)); m.translate(0, -0.1f * sw, -0.3f * sw); m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-30f * sw)); break;
            case 9: m.translate(0, 0.35f * sw, -0.3f * sw); m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-50f * sw)); break;
            case 10: m.translate(side * -0.2f * sw, 0.05f * sw, -0.25f * sw); m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * 55f * sw)); m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * 20f * sw)); break;
            case 11: m.translate((float) Math.sin(f * 40) * 0.03f * sw, 0, -0.15f * sw); m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) Math.sin(f * 50) * 8f * sw)); break;
            case 12: m.translate(side * -0.2f * sw, -0.2f * sw, -0.35f * sw); m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * -35f * sw)); m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * -20f * sw)); break;
            default: m.translate(side * 0.35f * (float) Math.sin(f * Math.PI * 2) * p, -0.1f * sw, -0.2f * sw); m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * 25f * (float) Math.sin(f * Math.PI * 2) * p)); break;
        }
        return true;
    }
    public static boolean hideVanillaCrosshair() { Crosshair c = Modules.get(Crosshair.class); return c != null && c.enabled && c.hideVanilla.b; }
    public static boolean noRender(int what) {
        NoRender n = Modules.get(NoRender.class);
        if (n == null || !n.enabled) return false;
        return n.flags[what].b;
    }

    // ================================================================= modules
    static class FullBright extends Mod {
        final Setting style = mode("Режим", 0, "Ночное зрение", "Гамма");
        boolean applied; double orig = -1;
        FullBright() { super("Full Bright", "Ночное зрение для полной яркости в тёмных местах", Cat.VISUAL); }
        @Override public void onTick() {
            var p = U.p();
            if (style.mode == 0) {
                var cur = p.getStatusEffect(StatusEffects.NIGHT_VISION);
                if (cur == null || cur.getDuration() < 400) { p.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 1000, 0, false, false, false)); applied = true; }
            } else {
                var o = U.mc().options.getGamma();
                if (orig < 0) orig = o.getValue();
                @SuppressWarnings("unchecked") var acc = (SimpleOptionAccessor<Double>) (Object) o;
                acc.setRawValue(16.0);
            }
        }
        @Override public void onDisable() {
            var p = U.p();
            if (p != null && applied) p.removeStatusEffect(StatusEffects.NIGHT_VISION);
            applied = false;
            if (orig >= 0) { @SuppressWarnings("unchecked") var acc = (SimpleOptionAccessor<Double>) (Object) U.mc().options.getGamma(); acc.setRawValue(orig); orig = -1; }
        }
    }

    static class Ambience extends Mod {
        final Setting style = mode("Стиль неба", 0, "Свой цвет", "Закат", "Неон", "Радуга", "Космос");
        final Setting col = color("Цвет", 280).showIf(() -> style.mode == 0);
        Ambience() { super("Ambience", "Модернизирует небо: меняет его цвет", Cat.VISUAL); }
        int rgb() {
            switch (style.mode) {
                case 1: return U.lerpColor(0xFFFF7A3D, 0xFFB03CFF, (float) (Math.sin(System.currentTimeMillis() / 3000.0) * 0.5 + 0.5)) & 0xFFFFFF;
                case 2: return U.lerpColor(0xFF00F0FF, 0xFFFF00E6, (float) (Math.sin(System.currentTimeMillis() / 1500.0) * 0.5 + 0.5)) & 0xFFFFFF;
                case 3: return java.awt.Color.HSBtoRGB((System.currentTimeMillis() % 12000) / 12000f, 0.6f, 1f) & 0xFFFFFF;
                case 4: return 0x14083A;
                default: return col.rgb() & 0xFFFFFF;
            }
        }
    }

    static class TimeChanger extends Mod {
        final Setting style = mode("Время суток", 1, "Рассвет", "День", "Полдень", "Закат", "Ночь", "Полночь", "Свой", "Вращение");
        final Setting custom = num("Тики", 6000, 0, 24000, 100).showIf(() -> style.mode == 6);
        final Setting speed = num("Скорость вращения", 4, 1, 40, 1).showIf(() -> style.mode == 7);
        TimeChanger() { super("Time Changer", "Фиксирует время суток только у тебя на клиенте", Cat.VISUAL); }
        long forced() {
            long[] t = {23000, 1000, 6000, 12500, 14500, 18000};
            if (style.mode < 6) return t[style.mode];
            if (style.mode == 6) return custom.i();
            return (System.currentTimeMillis() / 50 * speed.i()) % 24000;
        }
    }

    static class WeatherChanger extends Mod {
        final Setting w = mode("Погода", 0, "Ясно", "Дождь", "Гроза");
        WeatherChanger() { super("Weather Changer", "Меняет погоду на солнце, дождь или грозу (клиентская)", Cat.VISUAL); }
        @Override public void onTick() {
            var wd = U.mc().world;
            wd.setRainGradient(w.mode == 0 ? 0f : 1f);
            wd.setThunderGradient(w.mode == 2 ? 1f : 0f);
        }
    }

    static class AspectRatio extends Mod {
        final Setting preset = mode("Пропорции", 0, "4:3", "5:4", "16:10", "1:1", "21:9", "Свои");
        final Setting custom = num("Значение", 1.78, 0.5, 3.0, 0.01).showIf(() -> preset.mode == 5);
        AspectRatio() { super("Aspect Ratio", "Меняет соотношение сторон экрана", Cat.VISUAL); }
        float target() { float[] r = {4f / 3, 5f / 4, 16f / 10, 1f, 21f / 9}; return preset.mode < 5 ? r[preset.mode] : custom.f(); }
    }

    static class ViewModel extends Mod {
        final Setting x = num("X", 0, -1.5, 1.5, 0.01), y = num("Y", 0, -1.5, 1.5, 0.01), z = num("Z", 0, -1.5, 1.5, 0.01);
        final Setting scale = num("Размер", 1, 0.3, 1.6, 0.01);
        final Setting rx = num("Поворот X", 0, -180, 180, 1), ry = num("Поворот Y", 0, -180, 180, 1), rz = num("Поворот Z", 0, -180, 180, 1);
        ViewModel() { super("View Model", "Изменяет положение, размер и поворот рук", Cat.VISUAL); }
    }

    static class SwingAnimation extends Mod {
        final Setting style = mode("Анимация", 6, "Ванильная", "Рубящая", "Тычок", "Круговая", "Мягкая", "Размах", "Катана", "Бросок", "Спираль", "Прыжок", "Щит", "Дрожание", "Лепесток сакуры", "Метла");
        final Setting power = num("Сила", 1.0, 0.3, 1.8, 0.05);
        final Setting speed = num("Скорость", 1.0, 0.5, 2.0, 0.05);
        SwingAnimation() { super("Swing Animation", "Меняет анимацию удара мечом и предметами", Cat.VISUAL); }
    }

    static class Crosshair extends Mod {
        final Setting style = mode("Вид", 0, "Крест", "Точка", "Круг", "Квадрат", "Динамический");
        final Setting size = num("Размер", 5, 1, 14, 1), gap = num("Зазор", 2, 0, 10, 1), thick = num("Толщина", 1, 1, 4, 1);
        final Setting col = color("Цвет", 360);
        final Setting hideVanilla = bool("Скрыть ванильный", true);
        Crosshair() { super("Crosshair", "Кастомный прицел с расширенными настройками", Cat.VISUAL); }
        @Override public void onHud(DrawContext c, float td) {
            int cx = c.getScaledWindowWidth() / 2, cy = c.getScaledWindowHeight() / 2, s = size.i(), t = thick.i(), h = t / 2, col = this.col.rgb();
            int g = gap.i();
            switch (style.mode) {
                case 1: c.fill(cx - t, cy - t, cx + t, cy + t, col); break;
                case 2: for (int a = 0; a < 24; a++) { double r = a * Math.PI / 12; int px = cx + (int) (Math.cos(r) * (s + g)), py = cy + (int) (Math.sin(r) * (s + g)); c.fill(px, py, px + t, py + t, col); } break;
                case 3: { int d = s + g; c.fill(cx - d, cy - d, cx + d, cy - d + t, col); c.fill(cx - d, cy + d - t, cx + d, cy + d, col); c.fill(cx - d, cy - d, cx - d + t, cy + d, col); c.fill(cx + d - t, cy - d, cx + d, cy + d, col); break; }
                default:
                    if (style.mode == 4) { var p = U.p(); g += (int) (p.getVelocity().horizontalLength() * 40) + (int) ((1 - Math.min(1, p.getAttackCooldownProgress(0.5f))) * 6); }
                    c.fill(cx - g - s, cy - h, cx - g, cy - h + t, col); c.fill(cx + g, cy - h, cx + g + s, cy - h + t, col);
                    c.fill(cx - h, cy - g - s, cx - h + t, cy - g, col); c.fill(cx - h, cy + g, cx - h + t, cy + g + s, col);
            }
        }
    }

    static class NoRender extends Mod {
        final Setting[] flags = {bool("Босс-бар", true), bool("Огонь на экране", true), bool("Тряска при уроне", false), bool("Скорборд", false)};
        NoRender() { super("No Render", "Убирает бос-бар, огонь, тряску камеры и другие элементы", Cat.VISUAL); }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static void setOpt(net.minecraft.client.option.SimpleOption o, Object val) { o.setValue(val); }

    static class Optimization extends Mod {
        final Setting lvl = mode("Уровень", 0, "Лёгкий", "Средний", "Максимум FPS");
        Object view, pm, gm, cm, ent; boolean saved;
        Optimization() { super("Optimization", "Снижает дистанцию чанков, частицы и графику ради FPS", Cat.VISUAL); }
        @Override public void onEnable() {
            var o = U.mc().options;
            view = o.getViewDistance().getValue(); pm = o.getParticles().getValue(); gm = o.getGraphicsMode().getValue();
            cm = o.getCloudRenderMode().getValue(); ent = o.getEntityDistanceScaling().getValue(); saved = true;
            int[] v = {10, 8, 6};
            setOpt(o.getViewDistance(), Math.min((Integer) view, v[lvl.mode]));
            setOpt(o.getParticles(), ((Enum<?>) pm).getDeclaringClass().getEnumConstants()[lvl.mode == 0 ? 1 : 2]);
            setOpt(o.getGraphicsMode(), GraphicsMode.FAST);
            setOpt(o.getCloudRenderMode(), lvl.mode == 2 ? CloudRenderMode.OFF : CloudRenderMode.FAST);
            setOpt(o.getEntityDistanceScaling(), lvl.mode == 2 ? 0.5 : lvl.mode == 1 ? 0.75 : 1.0);
        }
        @Override public void onDisable() {
            if (!saved) return;
            var o = U.mc().options;
            setOpt(o.getViewDistance(), view); setOpt(o.getParticles(), pm); setOpt(o.getGraphicsMode(), gm);
            setOpt(o.getCloudRenderMode(), cm); setOpt(o.getEntityDistanceScaling(), ent);
            saved = false;
        }
    }

    static class Particles extends Mod {
        final Setting type = mode("Частицы", 1, PN), amount = num("Количество", 12, 1, 50, 1), power = num("Разлёт", 0.35, 0.1, 1.2, 0.05);
        Particles() { super("Particles", "Частицы при ударах с физикой разлёта", Cat.VISUAL); }
        @Override public void onAttack(Entity e) {
            for (int i = 0; i < amount.i(); i++)
                U.particle(PE[type.mode], e.getX(), e.getBodyY(0.5), e.getZ(), U.rnd(power.d()), Math.abs(U.rnd(power.d())) + 0.1, U.rnd(power.d()));
        }
    }

    static class WorldParticles extends Mod {
        static final ParticleEffect[] W = {ParticleTypes.SNOWFLAKE, ParticleTypes.END_ROD, ParticleTypes.HEART, ParticleTypes.CHERRY_LEAVES, ParticleTypes.ASH, ParticleTypes.GLOW, ParticleTypes.FLAME, ParticleTypes.SOUL};
        final Setting type = mode("Тип", 0, "Снег", "Звёзды", "Сердца", "Лепестки", "Пепел", "Свечение", "Огоньки", "Души");
        final Setting amount = num("Плотность", 4, 1, 20, 1), radius = num("Радиус", 12, 4, 30, 1);
        WorldParticles() { super("World Particles", "Добавляет частицы в мир: снег, звёзды, лепестки и др.", Cat.VISUAL); }
        @Override public void onTick() {
            var p = U.p();
            for (int i = 0; i < amount.i(); i++)
                U.particle(W[type.mode], p.getX() + U.rnd(radius.d()), p.getY() + U.RNG.nextDouble() * radius.d() * 0.6, p.getZ() + U.rnd(radius.d()), U.rnd(0.01), -0.02, U.rnd(0.01));
        }
    }

    static class TotemParticles extends Mod {
        final Setting style = mode("Эффект", 0, "Тотем", "Цветной взрыв", "Фейерверк", "Души");
        final Setting col = color("Цвет", 300).showIf(() -> style.mode == 1);
        final Setting amount = num("Количество", 30, 5, 100, 5);
        TotemParticles() { super("Totem Particles", "Показывает разлёт частиц при срабатывании тотема", Cat.VISUAL); }
        @Override public void onTotemPop(Entity e) {
            for (int i = 0; i < amount.i(); i++) {
                double vx = U.rnd(0.5), vy = U.RNG.nextDouble() * 0.6, vz = U.rnd(0.5);
                ParticleEffect pe = style.mode == 0 ? ParticleTypes.TOTEM_OF_UNDYING : style.mode == 1 ? new DustParticleEffect(col.rgb() & 0xFFFFFF, 1.4f) : style.mode == 2 ? ParticleTypes.FIREWORK : ParticleTypes.SOUL_FIRE_FLAME;
                U.particle(pe, e.getX(), e.getBodyY(0.6), e.getZ(), vx, vy, vz);
            }
        }
    }

    static class HitBubble extends Mod {
        final Setting style = mode("Эффект", 0, "Портал", "Пузыри", "Сфера из звёзд", "Обратный портал");
        final Setting amount = num("Количество", 25, 5, 80, 5);
        HitBubble() { super("Hit Bubble", "Создаёт порталы и пузыри вокруг цели при ударе", Cat.VISUAL); }
        @Override public void onAttack(Entity e) {
            ParticleEffect[] pe = {ParticleTypes.PORTAL, ParticleTypes.BUBBLE, ParticleTypes.END_ROD, ParticleTypes.REVERSE_PORTAL};
            for (int i = 0; i < amount.i(); i++) {
                double th = U.RNG.nextDouble() * Math.PI * 2, ph = Math.acos(2 * U.RNG.nextDouble() - 1), r = 0.9;
                double dx = Math.sin(ph) * Math.cos(th), dy = Math.cos(ph), dz = Math.sin(ph) * Math.sin(th);
                U.particle(pe[style.mode], e.getX() + dx * r, e.getBodyY(0.5) + dy * r, e.getZ() + dz * r, dx * 0.1, dy * 0.1, dz * 0.1);
            }
        }
    }

    static class HitColor extends Mod {
        static class R { Vec3d pos; long t0; int c; }
        final Setting style = mode("Эффект", 0, "Цветной всплеск", "Кольцо-волна", "Искры + кольцо", "Радужный взрыв", "Кровь", "Сердечки");
        final Setting col = color("Цвет", 0), amount = num("Количество", 24, 5, 80, 1), psize = num("Размер частиц", 1.2, 0.5, 3, 0.1), power = num("Разлёт", 0.25, 0.05, 0.8, 0.05);
        final Setting ring = bool("Кольцо на цели", true), flash = bool("Подсветка тела цветом", true);
        final java.util.List<R> rings = new java.util.ArrayList<>();
        Entity flashE; long flashT;
        HitColor() { super("Hit Color", "Цветной эффект при попадании: всплеск, кольцо-волна, подсветка тела", Cat.VISUAL); }
        int pick(int i) {
            switch (style.mode) {
                case 3: return java.awt.Color.HSBtoRGB(((i * 37 + System.currentTimeMillis() / 10) % 360) / 360f, 0.7f, 1f) & 0xFFFFFF;
                case 4: return U.RNG.nextBoolean() ? 0xB01020 : 0xE02030;
                default: return col.rgb(i) & 0xFFFFFF;
            }
        }
        @Override public void onAttack(Entity e) {
            for (int i = 0; i < amount.i(); i++) {
                double y = e.getY() + U.RNG.nextDouble() * e.getHeight();
                double vx = U.rnd(power.d()), vy = U.RNG.nextDouble() * power.d(), vz = U.rnd(power.d());
                ParticleEffect pe;
                if (style.mode == 5) pe = ParticleTypes.HEART;
                else if (style.mode == 2 && i % 3 == 0) pe = ParticleTypes.ELECTRIC_SPARK;
                else pe = new DustParticleEffect(pick(i), psize.f());
                U.particle(pe, e.getX() + U.rnd(0.4), y, e.getZ() + U.rnd(0.4), vx, vy, vz);
            }
            if (ring.b || style.mode == 1 || style.mode == 2) { R r = new R(); r.pos = e.getPos().add(0, e.getHeight() * 0.5, 0); r.t0 = System.currentTimeMillis(); r.c = 0xFF000000 | pick(1); rings.add(r); }
            flashE = e; flashT = System.currentTimeMillis();
        }
        @Override public void onWorld(MatrixStack ms, Vec3d cam, float td) {
            long now = System.currentTimeMillis();
            rings.removeIf(r -> now - r.t0 > 700);
            boolean fl = flash.b && flashE != null && flashE.isAlive() && now - flashT < 450;
            if (rings.isEmpty() && !fl) return;
            R3.begin(ms, cam, false);
            var q = R3.quads();
            for (R r : rings) {
                double age = (now - r.t0) / 700.0, rad = 0.3 + age * 1.8;
                float a = (float) (1 - age);
                R3.ringFill(q, r.pos, rad - 0.08, rad, 40, U.argb(r.c, a), U.argb(r.c, a * 0.4f));
                R3.ringFill(q, r.pos.add(0, 0.3, 0), rad * 0.8 - 0.05, rad * 0.8, 40, U.argb(r.c, a * 0.7f), U.argb(r.c, a * 0.3f));
            }
            if (fl) R3.boxFill(q, flashE.getBoundingBox().offset(R3.lerpPos(flashE, td).subtract(flashE.getPos())).expand(0.04), U.argb(0xFF000000 | pick(0), 0.45f * (1f - (now - flashT) / 450f)));
            q.draw();
            R3.end(ms);
        }
    }

    static class KillEffects extends Mod {
        final Setting style = mode("Эффект", 0, "Молния", "Души", "Фейерверк", "Сердца", "Тотем");
        Entity done;
        KillEffects() { super("Kill Effects", "Частицы или молния при убийстве игрока", Cat.VISUAL); }
        @Override public void onTick() {
            Entity t = Modules.lastTarget;
            if (t == null || t == done || System.currentTimeMillis() - Modules.lastTargetTime > 4000) return;
            if (!(t instanceof LivingEntity le) || !(le.isDead() || le.getHealth() <= 0)) return;
            done = t;
            double x = t.getX(), y = t.getY(), z = t.getZ();
            if (style.mode == 0) {
                var w = U.mc().world;
                LightningEntity b = new LightningEntity(EntityType.LIGHTNING_BOLT, w);
                b.setPosition(x, y, z); b.setId(-6000 - U.RNG.nextInt(1000)); w.addEntity(b);
            } else {
                ParticleEffect pe = style.mode == 1 ? ParticleTypes.SOUL : style.mode == 2 ? ParticleTypes.FIREWORK : style.mode == 3 ? ParticleTypes.HEART : ParticleTypes.TOTEM_OF_UNDYING;
                for (int i = 0; i < 40; i++) U.particle(pe, x, y + 1, z, U.rnd(0.4), U.RNG.nextDouble() * 0.5, U.rnd(0.4));
            }
        }
    }

    static class SelfNameTag extends Mod {
        final Setting scale = num("Размер", 1.0, 0.5, 2.5, 0.1);
        final Setting hp = bool("Показывать HP", true);
        final Setting bg = bool("Фон", true);
        SelfNameTag() { super("Self Name Tag", "Показывает собственный ник над головой в третьем лице", Cat.VISUAL); }
        @Override public void onWorld(MatrixStack ms, Vec3d cam, float td) {
            var mc = U.mc(); var p = U.p();
            if (mc.options.getPerspective().isFirstPerson()) return;
            Vec3d pos = R3.lerpPos(p, td);
            String s = Utils.protect(p.getName().getString()) + (hp.b ? "  " + (int) Math.ceil(p.getHealth()) + "\u2764" : "");
            TextRenderer tr = mc.textRenderer;
            var vcp = mc.getBufferBuilders().getEntityVertexConsumers();
            ms.push();
            ms.translate(pos.x - cam.x, pos.y + p.getHeight() + 0.55 - cam.y, pos.z - cam.z);
            ms.multiply(mc.gameRenderer.getCamera().getRotation());
            float sc = 0.025f * scale.f(); ms.scale(-sc, -sc, sc);
            Matrix4f m = ms.peek().getPositionMatrix();
            tr.draw(D.t(s), -tr.getWidth(D.t(s)) / 2f, 0, 0xFFFFFFFF, false, m, vcp, TextRenderer.TextLayerType.SEE_THROUGH, bg.b ? 0x66000000 : 0, 0xF000F0);
            vcp.draw();
            ms.pop();
        }
    }
}
