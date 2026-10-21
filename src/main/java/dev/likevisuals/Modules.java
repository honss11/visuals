package dev.likevisuals;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class Modules {
    public static final List<Mod> ALL = new ArrayList<>();
    private static final Map<Class<?>, Mod> BY_CLASS = new HashMap<>();
    public static Entity lastTarget;
    public static long lastTargetTime;
    private static boolean wasGuiKey;
    public static boolean quiet;

    public static void add(Mod m) { ALL.add(m); BY_CLASS.put(m.getClass(), m); }

    @SuppressWarnings("unchecked")
    public static <T extends Mod> T get(Class<T> c) { return (T) BY_CLASS.get(c); }
    public static boolean on(Class<? extends Mod> c) { Mod m = BY_CLASS.get(c); return m != null && m.enabled; }

    public static void init() {
        Visuals3.register();
        HudMods.register();
        HudMods2.register();
        Combat.register();
        Visuals.register();
        Visuals2.register();
        PlayerMods.register();
        Utils.register();
        Tools.register();
        FtHelper.register();
        ClientMods.register();
        load();
    }

    public static void toggled(Mod m) {
        if (!quiet) HudMods2.toggleNotice(m);
        if (ClientMods.sounds != null && ClientMods.sounds.enabled && U.in()) ClientMods.sounds.play(m.enabled);
    }

    public static void tick(MinecraftClient mc) {
        if (mc.currentScreen instanceof net.minecraft.client.gui.screen.TitleScreen && ClientMods.titleOn()) mc.setScreen(new SakuraTitle());
        long h = mc.getWindow().getHandle();
        boolean g = InputUtil.isKeyPressed(h, GLFW.GLFW_KEY_RIGHT_SHIFT);
        if (g && !wasGuiKey && mc.currentScreen == null && mc.world != null) mc.setScreen(new ClickGui());
        wasGuiKey = g;

        if (mc.currentScreen == null) {
            for (Mod m : ALL) {
                if (m.key <= 0) continue;
                boolean d = InputUtil.isKeyPressed(h, m.key);
                if (m.bindMode.mode == 1) { if (d != m.enabled) m.setEnabled(d); }
                else if (d && !m.wasDown) m.toggle();
                m.wasDown = d;
            }
        }
        if (!U.in()) return;
        for (Mod m : ALL) {
            if (!m.enabled) continue;
            try { m.onTick(); } catch (Throwable t) { LikeVisuals.LOG.error("tick " + m.name, t); m.enabled = false; }
        }
    }

    public static void hud(DrawContext ctx, net.minecraft.client.render.RenderTickCounter tc) {
        if (!U.in() || U.mc().options.hudHidden) return;
        float td = tc.getTickDelta(false);
        for (Mod m : ALL) if (m.enabled) try { m.onHud(ctx, td); } catch (Throwable t) { LikeVisuals.LOG.error("hud " + m.name, t); m.enabled = false; }
    }

    public static void world(MatrixStack ms, Vec3d cam, float td) {
        if (!U.in()) return;
        for (Mod m : ALL) if (m.enabled) try { m.onWorld(ms, cam, td); } catch (Throwable t) { LikeVisuals.LOG.error("world " + m.name, t); m.enabled = false; }
    }

    public static void attack(Entity e) {
        lastTarget = e; lastTargetTime = System.currentTimeMillis();
        for (Mod m : ALL) if (m.enabled) try { m.onAttack(e); } catch (Throwable t) { LikeVisuals.LOG.error("attack " + m.name, t); }
    }

    public static void totemPop(Entity e) {
        for (Mod m : ALL) if (m.enabled) try { m.onTotemPop(e); } catch (Throwable t) { LikeVisuals.LOG.error("pop " + m.name, t); }
    }

    // ---------------------------------------------------------------- config
    private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("likevisuals.properties"); }
    private static Path dir() {
        Path d = FabricLoader.getInstance().getConfigDir().resolve("likevisuals");
        try { Files.createDirectories(d); } catch (IOException ignored) {}
        return d;
    }
    private static String safe(String n) { return n.replaceAll("[^\\p{L}\\p{N}_\\- ]", "_").trim(); }

    private static Properties toProps() {
        Properties p = new Properties();
        for (Mod m : ALL) {
            p.setProperty(m.name + ".on", String.valueOf(m.enabled));
            p.setProperty(m.name + ".key", String.valueOf(m.key));
            p.setProperty(m.name + ".bm", String.valueOf(m.bindMode.mode));
            if (m instanceof HudMod hm) {
                p.setProperty(m.name + ".hx", String.valueOf(hm.hx));
                p.setProperty(m.name + ".hy", String.valueOf(hm.hy));
                p.setProperty(m.name + ".hs", String.valueOf(hm.hs));
            }
            for (Setting s : m.settings) {
                if (s.kind == Setting.Kind.HEADER) continue;
                String v = s.kind == Setting.Kind.BOOL ? String.valueOf(s.b) : s.kind == Setting.Kind.MODE ? String.valueOf(s.mode) : String.valueOf(s.v);
                p.setProperty(m.name + ".s." + s.name, v);
            }
        }
        return p;
    }

    private static void fromProps(Properties p, boolean live) {
        for (Mod m : ALL) {
            try {
                String k = p.getProperty(m.name + ".key"); if (k != null) m.key = Integer.parseInt(k);
                String bm = p.getProperty(m.name + ".bm"); if (bm != null) m.bindMode.mode = Integer.parseInt(bm);
                if (m instanceof HudMod hm) {
                    String a = p.getProperty(m.name + ".hx"), b = p.getProperty(m.name + ".hy"), c = p.getProperty(m.name + ".hs");
                    if (a != null) hm.hx = Float.parseFloat(a);
                    if (b != null) hm.hy = Float.parseFloat(b);
                    if (c != null) hm.hs = Float.parseFloat(c);
                }
                for (Setting s : m.settings) {
                    String v = p.getProperty(m.name + ".s." + s.name);
                    if (v == null) continue;
                    if (s.kind == Setting.Kind.BOOL) s.b = Boolean.parseBoolean(v);
                    else if (s.kind == Setting.Kind.MODE) s.mode = Math.max(0, Math.min(s.modes.length - 1, Integer.parseInt(v)));
                    else if (s.kind != Setting.Kind.HEADER) s.v = Double.parseDouble(v);
                }
                String on = p.getProperty(m.name + ".on");
                if (on != null && !m.oneShot && m.bindMode.mode == 0) {
                    boolean want = Boolean.parseBoolean(on);
                    if (live) { if (want != m.enabled) m.setEnabled(want); } else m.enabled = want;
                }
            } catch (Exception ignored) {}
        }
    }

    public static void save() {
        try (var w = Files.newBufferedWriter(file())) { toProps().store(w, "LikeVisuals"); } catch (IOException ignored) {}
    }

    private static void load() {
        if (!Files.exists(file())) return;
        Properties p = new Properties();
        try (var r = Files.newBufferedReader(file())) { p.load(r); } catch (IOException e) { return; }
        fromProps(p, false);
    }

    public static void saveNamed(String name) {
        String n = safe(name);
        if (n.isEmpty()) return;
        try (var w = Files.newBufferedWriter(dir().resolve(n + ".properties"))) { toProps().store(w, "LikeVisuals config " + n); } catch (IOException ignored) {}
    }

    public static boolean loadNamed(String name) {
        Path f = dir().resolve(safe(name) + ".properties");
        if (!Files.exists(f)) return false;
        Properties p = new Properties();
        try (var r = Files.newBufferedReader(f)) { p.load(r); } catch (IOException e) { return false; }
        quiet = true;
        try { fromProps(p, true); } finally { quiet = false; }
        save();
        return true;
    }

    public static boolean deleteNamed(String name) {
        try { return Files.deleteIfExists(dir().resolve(safe(name) + ".properties")); } catch (IOException e) { return false; }
    }

    public static List<String> listConfigs() {
        List<String> l = new ArrayList<>();
        try (var s = Files.list(dir())) {
            s.filter(f -> f.toString().endsWith(".properties")).forEach(f -> { String n = f.getFileName().toString(); l.add(n.substring(0, n.length() - 11)); });
        } catch (IOException ignored) {}
        Collections.sort(l);
        return l;
    }
}
