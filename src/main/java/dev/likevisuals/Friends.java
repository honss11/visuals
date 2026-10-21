package dev.likevisuals;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class Friends {
    private static final Set<String> NAMES = new LinkedHashSet<>();
    private static boolean loaded;

    private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("likevisuals_friends.txt"); }

    private static void ensure() {
        if (loaded) return;
        loaded = true;
        try { if (Files.exists(file())) for (String s : Files.readAllLines(file())) if (!s.isBlank()) NAMES.add(s.trim().toLowerCase()); } catch (IOException ignored) {}
    }
    private static void save() { try { Files.write(file(), new ArrayList<>(NAMES)); } catch (IOException ignored) {} }

    public static boolean is(Entity e) { return e instanceof PlayerEntity p && isName(p.getName().getString()); }
    public static boolean isName(String n) { ensure(); return NAMES.contains(n.toLowerCase()); }

    /** True when the hit must be cancelled (friend protection is on). */
    public static boolean blocks(Entity e) {
        Tools.FriendsMod m = Modules.get(Tools.FriendsMod.class);
        return m != null && m.enabled && m.protect.b && is(e);
    }

    public static boolean add(String n) { ensure(); boolean r = NAMES.add(n.toLowerCase()); save(); return r; }
    public static boolean remove(String n) { ensure(); boolean r = NAMES.remove(n.toLowerCase()); save(); return r; }
    public static boolean toggle(String n) { if (isName(n)) { remove(n); return false; } add(n); return true; }
    public static void clear() { ensure(); NAMES.clear(); save(); }
    public static List<String> list() { ensure(); return new ArrayList<>(NAMES); }
}
