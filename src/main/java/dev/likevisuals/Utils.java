package dev.likevisuals;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

public final class Utils {
    public static void register() {
        Modules.add(new AutoTpAccept()); Modules.add(new AucHelper()); Modules.add(new NameProtect()); Modules.add(new StreamerMode());
        Modules.add(new ArmorNotifier());
    }

    public static void onGameMessage(Text msg) {
        AutoTpAccept a = Modules.get(AutoTpAccept.class);
        if (a != null && a.enabled) {
            String s = msg.getString().toLowerCase();
            if (s.contains("/tpaccept") || s.contains("/tpyes")) a.due = System.currentTimeMillis() + a.delay.i() * 50L;
        }
    }

    static final Pattern IP = Pattern.compile("(?i)\\b([a-z0-9-]+\\.)+(me|ru|com|net|org|world|gg|fun|su|io|xyz|club|pro)\\b|\\b\\d{1,3}(\\.\\d{1,3}){3}(:\\d+)?\\b");
    static final Pattern COORD = Pattern.compile("-?\\d{2,6}[ ,]+-?\\d{1,3}[ ,]+-?\\d{2,6}");

    public static boolean censor(String s) {
        StreamerMode sm = Modules.get(StreamerMode.class);
        return sm != null && sm.enabled && sm.hideIp.b && IP.matcher(s).find();
    }

    public static Text modifyMessage(Text msg) {
        if (!U.in()) return msg;
        Text out = msg; String s = msg.getString();
        StreamerMode sm = Modules.get(StreamerMode.class);
        boolean strm = sm != null && sm.enabled;
        if (strm || Modules.on(NameProtect.class)) {
            String me = U.p().getName().getString();
            boolean changed = false;
            if (s.contains(me)) { s = s.replace(me, protect(me)); changed = true; }
            if (strm && sm.chatFilter.b) {
                String f = s;
                if (sm.hideIp.b) f = IP.matcher(f).replaceAll("\u2588\u2588\u2588\u2588");
                if (sm.hideCoords.b) f = COORD.matcher(f).replaceAll("\u2588\u2588\u2588");
                if (!f.equals(s)) { s = f; changed = true; }
            }
            if (changed) out = Text.literal(s);
        }
        Tools.ChatTimestamps ts = Modules.get(Tools.ChatTimestamps.class);
        if (ts != null && ts.enabled) {
            String t = LocalTime.now().format(DateTimeFormatter.ofPattern(ts.fmt.mode == 0 ? "HH:mm" : "HH:mm:ss"));
            out = Text.literal("[" + t + "] ").formatted(Formatting.GRAY).append(out);
        }
        return out;
    }

    public static String protect(String name) {
        NameProtect n = Modules.get(NameProtect.class); StreamerMode sm = Modules.get(StreamerMode.class);
        if (n != null && n.enabled) return n.fake();
        if (sm != null && sm.enabled && sm.hideName.b) return sm.nick.modes[sm.nick.mode];
        return name;
    }

    public static boolean streamerHides(boolean ip) {
        StreamerMode s = Modules.get(StreamerMode.class);
        return s != null && s.enabled && (ip ? s.hideIp.b : s.hideCoords.b);
    }

    static class AutoTpAccept extends Mod {
        final Setting delay = num("Задержка (тики)", 10, 0, 100, 1);
        long due;
        AutoTpAccept() { super("Auto TP Accept", "Автоматически принимает запросы на телепорт", Cat.UTIL); }
        @Override public void onTick() { if (due > 0 && System.currentTimeMillis() >= due) { due = 0; U.cmd("tpaccept"); } }
    }

    static class AucHelper extends Mod {
        final Setting cmd = mode("Команда", 0, "ah search", "auc search", "ah", "market search");
        AucHelper() { super("Auc Helper", "Ищет предмет из руки на аукционе по бинду", Cat.UTIL); oneShot = true; }
        @Override public void onEnable() {
            if (!U.in()) return;
            ItemStack s = U.p().getMainHandStack();
            if (s.isEmpty()) { U.bar("Auc Helper: возьми предмет в руку"); return; }
            U.cmd(cmd.modes[cmd.mode] + (cmd.mode == 2 ? "" : " " + s.getItem().getName().getString()));
        }
    }

    static class NameProtect extends Mod {
        final Setting nick = mode("Заменять на", 0, "Protected", "Player", "Steve", "Скрыто", "\u2588\u2588\u2588\u2588");
        NameProtect() { super("Name Protect", "Заменяет твой ник в тексте и над головой", Cat.UTIL); }
        String fake() { return nick.modes[nick.mode]; }
    }

    static class StreamerMode extends Mod {
        final Setting hideName = bool("Скрывать ник", true), nick = mode("Заменять ник на", 0, "Streamer", "Player", "Hidden", "\u2588\u2588\u2588\u2588");
        final Setting hideIp = bool("Скрывать IP и домены сервера", true), hideCoords = bool("Скрывать координаты", true), chatFilter = bool("Фильтровать чат (IP, координаты)", true);
        StreamerMode() { super("Streamer Mode", "Прячет ник, IP, домены и координаты в HUD, скорборде, карте и чате", Cat.UTIL); }
    }

    static class ArmorDurability extends Mod {
        final Setting style = mode("Показ", 0, "Проценты", "Остаток");
        ArmorDurability() { super("Armor Durability", "Показывает броню по оставшейся прочности", Cat.UTIL); }
        @Override public void onHud(DrawContext c, float td) {
            var p = U.p(); var tr = U.mc().textRenderer;
            EquipmentSlot[] sl = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
            int x = c.getScaledWindowWidth() / 2 - 40, y = c.getScaledWindowHeight() - 90;
            for (int i = 0; i < 4; i++) {
                ItemStack s = p.getEquippedStack(sl[i]);
                if (s.isEmpty()) continue;
                c.drawItem(s, x + i * 22, y);
                if (!s.isDamageable()) continue;
                int left = s.getMaxDamage() - s.getDamage(), pct = left * 100 / s.getMaxDamage();
                String t = style.mode == 0 ? pct + "%" : String.valueOf(left);
                int col = pct > 50 ? 0xFF55FF55 : pct > 20 ? 0xFFFFD84D : 0xFFFF5555;
                D.text(c, t, x + i * 22 + 8 - D.w(t) / 2, y + 18, col);
            }
        }
    }

    static class ArmorNotifier extends Mod {
        final Setting thr = num("Порог (%)", 20, 5, 50, 1);
        final Setting where = mode("Уведомление", 2, "Чат", "Над хотбаром", "Оба");
        final boolean[] told = new boolean[4];
        int t;
        ArmorNotifier() { super("Armor Notifier", "Предупреждает, что броня скоро сломается", Cat.UTIL); }
        @Override public void onTick() {
            if (++t % 20 != 0) return;
            EquipmentSlot[] sl = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
            for (int i = 0; i < 4; i++) {
                ItemStack s = U.p().getEquippedStack(sl[i]);
                if (s.isEmpty() || !s.isDamageable()) { told[i] = false; continue; }
                int pct = (s.getMaxDamage() - s.getDamage()) * 100 / s.getMaxDamage();
                if (pct <= thr.i() && !told[i]) {
                    told[i] = true;
                    String m = "Броня скоро сломается (" + pct + "%): " + s.getItem().getName().getString();
                    if (where.mode != 1) U.chat(m);
                    if (where.mode != 0) U.bar("\u00a7c" + m);
                } else if (pct > thr.i() + 10) told[i] = false;
            }
        }
    }
}
