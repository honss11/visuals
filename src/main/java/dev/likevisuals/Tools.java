package dev.likevisuals;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.registry.Registries;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class Tools {
    public static void register() {
        Modules.add(new FriendsMod()); Modules.add(new FriendToggle()); Modules.add(new ShiftTap()); Modules.add(new AddMark());
        Modules.add(new AutoCommands()); Modules.add(new AntiAFK()); Modules.add(new AutoLeave()); Modules.add(new AutoTool());
        Modules.add(new ChatTimestamps());
        loadMarks(); loadAuto(); loadBinds();
    }

    // ================================================================= marks
    public static final class Mark { public String server, dim, name; public double x, y, z; public int color; }
    private static final List<Mark> MARKS = new ArrayList<>();
    private static final int[] MARK_COLORS = {0xFFFF8FBF, 0xFF7CFFB0, 0xFFFFD86B, 0xFF8FC8FF, 0xFFC9A7FF, 0xFFFF9E6B};

    private static String serverKey() { var s = U.mc().getCurrentServerEntry(); return s == null ? "local" : s.address; }
    private static String dimKey() { return U.mc().world == null ? "" : U.mc().world.getRegistryKey().getValue().toString(); }
    private static Path marksFile() { return FabricLoader.getInstance().getConfigDir().resolve("likevisuals_marks.txt"); }

    public static List<Mark> visibleMarks() {
        List<Mark> l = new ArrayList<>();
        if (!U.in()) return l;
        String s = serverKey(), d = dimKey();
        for (Mark m : MARKS) if (m.server.equals(s) && m.dim.equals(d)) l.add(m);
        return l;
    }
    public static void addMark(String name, double x, double y, double z) {
        Mark m = new Mark(); m.server = serverKey(); m.dim = dimKey(); m.name = name; m.x = x; m.y = y; m.z = z;
        m.color = MARK_COLORS[MARKS.size() % MARK_COLORS.length];
        MARKS.add(m); saveMarks();
    }
    public static boolean removeMark(String name) {
        boolean r = MARKS.removeIf(m -> m.name.equalsIgnoreCase(name) && m.server.equals(serverKey()));
        saveMarks(); return r;
    }
    private static void saveMarks() {
        List<String> out = new ArrayList<>();
        for (Mark m : MARKS) out.add(String.join("|", m.server, m.dim, m.name.replace("|", "/"), String.valueOf(m.x), String.valueOf(m.y), String.valueOf(m.z), String.valueOf(m.color)));
        try { Files.write(marksFile(), out); } catch (IOException ignored) {}
    }
    private static void loadMarks() {
        try {
            if (!Files.exists(marksFile())) return;
            for (String ln : Files.readAllLines(marksFile())) {
                String[] p = ln.split("\\|");
                if (p.length < 7) continue;
                Mark m = new Mark(); m.server = p[0]; m.dim = p[1]; m.name = p[2]; m.x = Double.parseDouble(p[3]); m.y = Double.parseDouble(p[4]); m.z = Double.parseDouble(p[5]); m.color = Integer.parseInt(p[6]);
                MARKS.add(m);
            }
        } catch (Exception ignored) {}
    }

    // ================================================================= auto commands storage
    static final List<String> AUTO = new ArrayList<>();
    private static Path autoFile() { return FabricLoader.getInstance().getConfigDir().resolve("likevisuals_autocmd.txt"); }
    private static void saveAuto() { try { Files.write(autoFile(), AUTO); } catch (IOException ignored) {} }
    private static void loadAuto() { try { if (Files.exists(autoFile())) AUTO.addAll(Files.readAllLines(autoFile())); } catch (IOException ignored) {} }

    // ================================================================= modules
    static class FriendsMod extends Mod {
        final Setting protect = bool("Не бить друзей", true), notify = bool("Сообщения в чат", true);
        FriendsMod() { super("Friends", "Друзья: не даёт ударить друга, подсвечивает зелёным (/lv friend add ник)", Cat.COMBAT); enabled = true; }
    }

    static class FriendToggle extends Mod {
        FriendToggle() { super("Friend Toggle", "Добавляет или убирает друга: игрока в прицеле (по бинду)", Cat.COMBAT); oneShot = true; }
        @Override public void onEnable() {
            var mc = U.mc();
            Entity t = mc.crosshairTarget instanceof EntityHitResult er ? er.getEntity() : null;
            if (!(t instanceof PlayerEntity pe)) { U.bar("Friend Toggle: наведись на игрока"); return; }
            String n = pe.getName().getString();
            boolean now = Friends.toggle(n);
            U.chat(now ? n + " добавлен в друзья" : n + " удалён из друзей");
        }
    }

    static class ShiftTap extends Mod {
        final Setting style = mode("Режим", 0, "Shift Tap (приседание)", "W-Tap (отпустить вперёд)", "S-Tap (назад)");
        final Setting delay = num("Задержка после удара (тики)", 1, 0, 6, 1);
        final Setting hold = num("Длительность (тики)", 2, 1, 5, 1);
        final Setting chance = num("Шанс (%)", 100, 10, 100, 5);
        int wait = -1, active;
        ShiftTap() { super("Shift Tap", "После удара кратко приседает / отпускает W для лучшего отбрасывания", Cat.COMBAT); }
        @Override public void onAttack(Entity e) { if (U.RNG.nextInt(100) < chance.i()) { wait = delay.i(); active = 0; } }
        @Override public void onDisable() { wait = -1; active = 0; KeyBinding.updatePressedStates(); }
        @Override public void onTick() {
            var o = U.mc().options;
            if (wait > 0) { wait--; return; }
            if (wait == 0) { wait = -1; active = hold.i(); }
            if (active > 0) {
                if (style.mode == 0) o.sneakKey.setPressed(true);
                else if (style.mode == 1) o.forwardKey.setPressed(false);
                else { o.forwardKey.setPressed(false); o.backKey.setPressed(true); }
                if (--active == 0) KeyBinding.updatePressedStates();
            }
        }
    }

    static class AddMark extends Mod {
        final Setting where = mode("Куда ставить", 0, "Блок в прицеле", "Моя позиция");
        int n = 1;
        AddMark() { super("Add Mark", "Ставит метку по бинду (видна в мире, на карте и компасе)", Cat.PLAYER); oneShot = true; }
        @Override public void onEnable() {
            var mc = U.mc(); var p = mc.player;
            if (p == null) return;
            double x = p.getX(), y = p.getY(), z = p.getZ();
            if (where.mode == 0 && mc.crosshairTarget instanceof BlockHitResult br && br.getType() != HitResult.Type.MISS) { BlockPos b = br.getBlockPos(); x = b.getX() + 0.5; y = b.getY() + 1; z = b.getZ() + 0.5; }
            String nm = "Метка " + (visibleMarks().size() + n++);
            addMark(nm, x, y, z);
            U.chat("Поставлена: " + nm + "  (/lv mark remove " + nm + ")");
        }
    }

    static class AutoCommands extends Mod {
        final Setting trigger = mode("Когда", 0, "При входе в мир", "По таймеру", "После смерти", "При включении модуля");
        final Setting delay = num("Задержка до старта (сек)", 3, 0, 30, 1);
        final Setting gap = num("Пауза между командами (сек)", 1.0, 0.3, 10, 0.1);
        final Setting interval = num("Интервал таймера (сек)", 60, 10, 600, 5).showIf(() -> trigger.mode == 1);
        final ArrayDeque<String> queue = new ArrayDeque<>();
        Object lastWorld; long nextAt, timerAt; boolean wasDead;
        AutoCommands() { super("Auto Commands", "Автоматически отправляет команды (/lv auto add /home)", Cat.UTIL); }
        void fire(long now, double startDelay) { queue.clear(); for (String s : AUTO) queue.add(s); nextAt = now + (long) (startDelay * 1000); }
        @Override public void onEnable() { if (trigger.mode == 3) fire(System.currentTimeMillis(), 0); lastWorld = U.mc().world; timerAt = System.currentTimeMillis() + interval.i() * 1000L; }
        @Override public void onTick() {
            var mc = U.mc(); var p = mc.player; long now = System.currentTimeMillis();
            if (mc.world != lastWorld) { lastWorld = mc.world; if (trigger.mode == 0) fire(now, delay.d()); }
            if (trigger.mode == 1 && now >= timerAt) { timerAt = now + interval.i() * 1000L; fire(now, 0); }
            if (trigger.mode == 2) { if (p.isDead() && !wasDead) fire(now, delay.d() + 2); wasDead = p.isDead(); }
            if (!queue.isEmpty() && now >= nextAt && !p.isDead()) {
                String s = queue.poll();
                if (s.startsWith("/")) U.cmd(s.substring(1)); else U.say(s);
                nextAt = now + (long) (gap.d() * 1000);
            }
        }
    }

    static class AntiAFK extends Mod {
        final Setting action = mode("Действие", 2, "Прыжок", "Поворот", "Оба");
        final Setting interval = num("Интервал (сек)", 20, 5, 120, 1);
        long next; int jump;
        AntiAFK() { super("Anti AFK", "Не даёт кикнуть за бездействие: прыгает и поворачивает камеру", Cat.UTIL); }
        @Override public void onTick() {
            var p = U.p(); long now = System.currentTimeMillis();
            if (jump > 0) { U.mc().options.jumpKey.setPressed(true); if (--jump == 0) KeyBinding.updatePressedStates(); return; }
            if (now < next) return;
            next = now + interval.i() * 1000L;
            if (action.mode != 1) jump = 2;
            if (action.mode != 0) p.setYaw(p.getYaw() + (U.RNG.nextBoolean() ? 6 : -6));
        }
    }

    static class AutoLeave extends Mod {
        final Setting hp = num("Выйти при HP ниже", 4, 1, 19, 1);
        final Setting totemOk = bool("Не выходить, если есть тотем в руке", true);
        AutoLeave() { super("Auto Leave", "Выходит с сервера при критически низком здоровье", Cat.UTIL); }
        @Override public void onTick() {
            var p = U.p();
            if (p.getHealth() > hp.f() || p.isDead()) return;
            if (totemOk.b && p.getOffHandStack().isOf(net.minecraft.item.Items.TOTEM_OF_UNDYING)) return;
            p.networkHandler.getConnection().disconnect(Text.literal("LikeVisuals: низкое здоровье"));
            enabled = false;
        }
    }

    static class AutoTool extends Mod {
        final Setting back = bool("Возвращать слот", true);
        int prev = -1;
        AutoTool() { super("Auto Tool", "Сам выбирает лучший инструмент для ломаемого блока", Cat.PLAYER); }
        @Override public void onTick() {
            var mc = U.mc(); var p = mc.player;
            boolean mining = mc.options.attackKey.isPressed() && mc.currentScreen == null && mc.crosshairTarget instanceof BlockHitResult br && br.getType() != HitResult.Type.MISS;
            if (!mining) { if (prev >= 0 && back.b) p.getInventory().selectedSlot = prev; prev = -1; return; }
            BlockState st = mc.world.getBlockState(((BlockHitResult) mc.crosshairTarget).getBlockPos());
            int best = p.getInventory().selectedSlot; float bs = p.getInventory().getStack(best).getMiningSpeedMultiplier(st);
            for (int i = 0; i < 9; i++) { float s = p.getInventory().getStack(i).getMiningSpeedMultiplier(st); if (s > bs + 0.01f) { bs = s; best = i; } }
            if (best != p.getInventory().selectedSlot) { if (prev < 0) prev = p.getInventory().selectedSlot; p.getInventory().selectedSlot = best; }
        }
    }

    static class ChatTimestamps extends Mod {
        final Setting fmt = mode("Формат", 0, "HH:mm", "HH:mm:ss");
        ChatTimestamps() { super("Chat Timestamps", "Добавляет время к сообщениям чата", Cat.UTIL); }
    }

    // ================================================================= item binds (.bind)
    public static final class ItemBind { public String kw; public int key; public boolean down; }
    public static final List<ItemBind> BINDS = new ArrayList<>();
    private static Path bindsFile() { return FabricLoader.getInstance().getConfigDir().resolve("likevisuals_binds.txt"); }
    private static void saveBinds() { List<String> o = new ArrayList<>(); for (ItemBind b : BINDS) o.add(b.key + "|" + b.kw); try { Files.write(bindsFile(), o); } catch (IOException ignored) {} }
    private static void loadBinds() {
        try { if (!Files.exists(bindsFile())) return; for (String l : Files.readAllLines(bindsFile())) { String[] p = l.split("\\|", 2); if (p.length < 2) continue; ItemBind b = new ItemBind(); b.key = Integer.parseInt(p[0]); b.kw = p[1]; BINDS.add(b); } } catch (Exception ignored) {}
    }

    public static boolean matches(ItemStack s, String kw) {
        if (s.isEmpty()) return false;
        if (s.getName().getString().toLowerCase().contains(kw)) return true;
        return Registries.ITEM.getId(s.getItem()).getPath().replace('_', ' ').contains(kw);
    }

    static int parseKey(String raw) {
        String s = raw.toLowerCase().trim();
        if (s.equals("none") || s.equals("-") || s.equals("off")) return -1;
        Map<String, String> al = Map.ofEntries(Map.entry("lshift", "left.shift"), Map.entry("rshift", "right.shift"), Map.entry("lctrl", "left.control"), Map.entry("rctrl", "right.control"),
            Map.entry("lalt", "left.alt"), Map.entry("ralt", "right.alt"), Map.entry("esc", "escape"), Map.entry("caps", "caps.lock"), Map.entry("bksp", "backspace"),
            Map.entry("pgup", "page.up"), Map.entry("pgdn", "page.down"), Map.entry("del", "delete"), Map.entry("ins", "insert"), Map.entry("np0", "keypad.0"));
        try { return InputUtil.fromTranslationKey("key.keyboard." + al.getOrDefault(s, s)).getCode(); } catch (Exception e) { return -2; }
    }
    private static String norm(String s) { return s.toLowerCase().replaceAll("[^a-z0-9\\p{L}]", ""); }

    private static Mod findModule(String name) {
        String n = norm(name);
        if (n.isEmpty()) return null;
        for (Mod m : Modules.ALL) if (norm(m.name).equals(n)) return m;
        Mod found = null;
        for (Mod m : Modules.ALL) if (norm(m.name).startsWith(n)) { if (found != null) return null; found = m; }
        return found;
    }

    private static void bindHelp() {
        U.chat(".bind <module or item> <key>   - bind a module (autoswap) or an item (trap, ender pearl)");
        U.chat(".bind item <name> <key>   .bind module <name> <key>   .bind <name> none");
        U.chat(".bind list   .bind remove <name|key>   .bind clear");
    }

    private static void bindCmd(String body) {
        String[] a = body.trim().split("\\s+");
        if (a.length < 2 || a[1].equalsIgnoreCase("help")) { bindHelp(); return; }
        String sub = a[1].toLowerCase();
        if (sub.equals("list")) {
            for (Mod m : Modules.ALL) if (m.key > 0) U.chat("[module] " + m.name + " = " + ClickGui.keyName(m.key));
            for (ItemBind b : BINDS) U.chat("[item] " + b.kw + " = " + ClickGui.keyName(b.key));
            return;
        }
        if (sub.equals("clear")) { BINDS.clear(); saveBinds(); U.chat("Item binds cleared"); return; }
        if (sub.equals("remove") || sub.equals("del")) {
            String arg = rest(a, 2).toLowerCase();
            int k = parseKey(arg); boolean any = false;
            if (k > 0) { any |= BINDS.removeIf(b -> b.key == k); for (Mod m : Modules.ALL) if (m.key == k) { m.key = -1; any = true; } }
            else { final String q = arg; any |= BINDS.removeIf(b -> b.kw.equals(q)); Mod m = findModule(arg); if (m != null && m.key > 0) { m.key = -1; any = true; } }
            saveBinds(); U.chat(any ? "Bind removed" : "Nothing to remove"); return;
        }
        int start = 1; boolean forceItem = false, forceModule = false;
        if (sub.equals("item")) { forceItem = true; start = 2; } else if (sub.equals("module")) { forceModule = true; start = 2; }
        if (a.length - start < 2) { bindHelp(); return; }
        String keyStr = a[a.length - 1];
        String name = String.join(" ", Arrays.copyOfRange(a, start, a.length - 1)).toLowerCase();
        int key = parseKey(keyStr);
        if (key == -2) { U.chat("Unknown key: " + keyStr); return; }
        Mod mod = forceItem ? null : findModule(name);
        if (mod != null || forceModule) {
            if (mod == null) { U.chat("Module not found: " + name); return; }
            mod.key = key; U.chat(key < 0 ? mod.name + " unbound" : mod.name + " bound to " + ClickGui.keyName(key));
            HudMods2.info("Bind", mod.name + (key < 0 ? " unbound" : " -> " + ClickGui.keyName(key)));
            return;
        }
        BINDS.removeIf(b -> b.kw.equals(name));
        if (key > 0) { ItemBind b = new ItemBind(); b.kw = name; b.key = key; BINDS.add(b); }
        saveBinds();
        U.chat(key < 0 ? "Item bind removed: " + name : "Item \"" + name + "\" bound to " + ClickGui.keyName(key));
        HudMods2.info("Bind", "Item " + name + (key < 0 ? " unbound" : " -> " + ClickGui.keyName(key)));
    }

    /** Handles chat lines that start with "." — returns true when the message was consumed. */
    public static boolean onChat(String msg) {
        if (!msg.startsWith(".") || msg.length() < 2) return false;
        String body = msg.substring(1).trim();
        if (body.isEmpty()) return false;
        String first = body.split("\\s+")[0].toLowerCase();
        try {
            switch (first) {
                case "bind": bindCmd(body); return true;
                case "binds": bindCmd("bind list"); return true;
                case "friend": case "f": case "mark": case "m": case "wp": case "cfg": case "config": case "auto": case "hud": handle(body); return true;
                default: return false;
            }
        } catch (Throwable t) { U.chat("Error: " + t.getMessage()); return true; }
    }

    // ================================================================= /lv command
    public static void commands(CommandDispatcher<FabricClientCommandSource> d) {
        d.register(ClientCommandManager.literal("lv")
            .executes(ctx -> { help(); return 1; })
            .then(ClientCommandManager.argument("args", StringArgumentType.greedyString()).executes(ctx -> {
                try { handle(StringArgumentType.getString(ctx, "args")); } catch (Throwable t) { U.chat("Ошибка: " + t.getMessage()); }
                return 1;
            })));
    }

    private static void help() {
        U.chat("Команды /lv:");
        U.chat("friend add|remove|list|clear <ник>   mark add|remove|list|clear <имя>");
        U.chat("cfg save|load|delete|list <имя>   auto add|remove|list|clear <команда>   hud");
    }

    private static String rest(String[] a, int from) { return a.length <= from ? "" : String.join(" ", Arrays.copyOfRange(a, from, a.length)); }

    private static void handle(String line) {
        String[] a = line.trim().split("\\s+");
        if (a.length == 0 || a[0].isEmpty()) { help(); return; }
        String sub = a[0].toLowerCase(), act = a.length > 1 ? a[1].toLowerCase() : "", arg = rest(a, 2);
        switch (sub) {
            case "friend": case "f":
                switch (act) {
                    case "add": U.chat(Friends.add(arg) ? arg + " добавлен в друзья" : arg + " уже в друзьях"); break;
                    case "remove": U.chat(Friends.remove(arg) ? arg + " удалён" : arg + " не найден"); break;
                    case "clear": Friends.clear(); U.chat("Список друзей очищен"); break;
                    default: U.chat("Друзья: " + (Friends.list().isEmpty() ? "(пусто)" : String.join(", ", Friends.list())));
                }
                break;
            case "mark": case "m": case "wp":
                switch (act) {
                    case "add": { var p = U.p(); addMark(arg.isEmpty() ? "Метка " + (visibleMarks().size() + 1) : arg, p.getX(), p.getY(), p.getZ()); U.chat("Метка поставлена"); break; }
                    case "remove": U.chat(removeMark(arg) ? "Метка удалена" : "Метка не найдена"); break;
                    case "clear": MARKS.removeIf(m -> m.server.equals(serverKey())); saveMarks(); U.chat("Метки очищены"); break;
                    default: for (Mark m : visibleMarks()) U.chat(m.name + ": " + (int) m.x + " " + (int) m.y + " " + (int) m.z);
                }
                break;
            case "cfg": case "config":
                switch (act) {
                    case "save": Modules.saveNamed(arg); U.chat("Конфиг сохранён: " + arg); break;
                    case "load": U.chat(Modules.loadNamed(arg) ? "Конфиг загружен: " + arg : "Конфиг не найден: " + arg); break;
                    case "delete": U.chat(Modules.deleteNamed(arg) ? "Конфиг удалён" : "Конфиг не найден"); break;
                    default: U.chat("Конфиги: " + (Modules.listConfigs().isEmpty() ? "(пусто)" : String.join(", ", Modules.listConfigs())));
                }
                break;
            case "auto":
                switch (act) {
                    case "add": if (!arg.isEmpty()) { AUTO.add(arg); saveAuto(); U.chat("Добавлено: " + arg); } break;
                    case "remove": try { AUTO.remove(Integer.parseInt(arg.trim()) - 1); saveAuto(); U.chat("Удалено"); } catch (Exception e) { U.chat("Укажи номер строки"); } break;
                    case "clear": AUTO.clear(); saveAuto(); U.chat("Список очищен"); break;
                    default: for (int i = 0; i < AUTO.size(); i++) U.chat((i + 1) + ". " + AUTO.get(i));
                }
                break;
            case "hud": U.mc().send(() -> U.mc().setScreen(new ClickGui(true))); break;
            default: help();
        }
    }
}
