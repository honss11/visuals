package dev.likevisuals;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

import java.util.*;

public final class HudMods2 {
    public static void register() {
        Modules.add(new Notifications()); Modules.add(new PlayerCard()); Modules.add(new Points()); Modules.add(new UseTracker());
        Modules.add(new ItemBinds()); Modules.add(new Hotbar()); Modules.add(new Session()); Modules.add(new FpsGraph()); Modules.add(new Nearby());
    }

    public static boolean hotbarOn() { return Modules.on(Hotbar.class); }
    public static boolean hotbarBars() { Hotbar h = Modules.get(Hotbar.class); return h != null && h.enabled && h.bars.b; }

    // ================================================================= notifications
    static class Notif { String title, sub; int icon, col; long t0; }
    static final List<Notif> NOTES = new ArrayList<>();

    public static void info(String title, String sub) { push(title, sub, Icons.BELL, 0xFFFFFFFF); }
    static void push(String title, String sub, int icon, int col) {
        Notifications n = Modules.get(Notifications.class);
        if (n == null || !n.enabled) return;
        Notif x = new Notif(); x.title = title; x.sub = sub; x.icon = icon; x.col = col; x.t0 = System.currentTimeMillis();
        NOTES.add(0, x);
        while (NOTES.size() > n.max.i()) NOTES.remove(NOTES.size() - 1);
    }
    public static void toggleNotice(Mod m) {
        Notifications n = Modules.get(Notifications.class);
        if (n == null || !n.enabled || !n.toggles.b || m.oneShot || m instanceof HudMod || m.cat == Mod.Cat.CLIENT || !U.in()) return;
        push("Module " + m.name, m.enabled ? "enabled" : "disabled", m.enabled ? Icons.CHECK : Icons.CROSS, m.enabled ? 0xFF7CFFB0 : 0xFFFF6A7A);
    }

    static class Notifications extends HudMod {
        final Setting toggles = bool("Module toggles", true), max = num("Max notifications", 5, 1, 8, 1), life = num("Duration (sec)", 3.0, 1, 8, 0.5);
        Notifications() { super("Notifications", "Pop-ups when modules turn on/off and for events", Cat.HUD, 0.008f, 0.76f); enabled = true; }
        @Override protected void render(DrawContext c, float td, boolean pv) {
            long now = System.currentTimeMillis();
            NOTES.removeIf(x -> (now - x.t0) / 1000.0 > life.d() + 0.4);
            List<Notif> list = new ArrayList<>(NOTES);
            if (list.isEmpty() && pv) { Notif d = new Notif(); d.title = "Module Example"; d.sub = "enabled"; d.icon = Icons.CHECK; d.col = 0xFF7CFFB0; d.t0 = now; list.add(d); }
            int y = 0, mw = 120;
            for (Notif x : list) {
                double age = (now - x.t0) / 1000.0;
                int ww = Math.max(D.w(x.title), D.w(x.sub)) + 44, hh = x.sub.isEmpty() ? 22 : 30;
                double in = Math.min(1, age / 0.25), out = age > life.d() ? Math.min(1, (age - life.d()) / 0.4) : 0;
                int off = (int) ((-(1 - (1 - Math.pow(1 - in, 3)))) * (ww + 20) - out * (ww + 20));
                if (pv) off = 0;
                panel(c, off, y, ww, hh, 10);
                D.circle(c, off + 15, y + hh / 2, 8, D.a(x.col, 0.22f));
                Icons.draw(c, x.icon, off + 10, y + hh / 2 - 5, x.col);
                D.text(c, x.title, off + 30, y + (x.sub.isEmpty() ? 7 : 5), hudText());
                if (!x.sub.isEmpty()) D.text(c, x.sub, off + 30, y + 16, D.a(hudText(), 0.65f));
                mw = Math.max(mw, ww); y += hh + 4;
            }
            w = mw; h = Math.max(22, y);
        }
    }

    // ================================================================= player card
    static class PlayerCard extends HudMod {
        float shown = -1;
        final Setting avatar = bool("Avatar", true);
        PlayerCard() { super("Player Card", "Your avatar, name and animated health bar", Cat.HUD, 0.008f, 0.145f); enabled = true; }
        @Override protected void render(DrawContext c, float td, boolean pv) {
            if (!U.in()) return;
            ClientPlayerEntity p = U.p();
            float hp = p.getHealth(), ab = p.getAbsorptionAmount(), max = Math.max(1f, p.getMaxHealth());
            if (shown < 0) shown = hp;
            shown += (hp - shown) * 0.2f;
            int ww = 124, hh = 36, a1 = ClientMods.accent(0), a2 = ClientMods.accent(1), tc = hudText();
            panel(c, 0, 0, ww, hh, 11);
            int tx = 8;
            if (avatar.b) { tx = 36; try { PlayerSkinDrawer.draw(c, p.getSkinTextures(), 6, 6, 24); } catch (Throwable t) { D.circle(c, 18, 18, 11, D.a(a1, 0.6f)); } }
            D.text(c, D.clip(Utils.protect(p.getName().getString()), ww - tx - 40), tx, 6, tc);
            String hs = (int) Math.ceil(hp) + (ab > 0 ? "+" + (int) ab : "");
            D.text(c, hs, ww - 8 - D.w(hs), 6, ab > 0 ? 0xFFFFD86B : a1);
            float pct = Math.max(0f, Math.min(1f, shown / max));
            D.rrect(c, tx, 23, ww - tx - 8, 5, 2, D.a(0xFF000000, 0.45f));
            D.rrectV(c, tx, 23, Math.max(3, (int) ((ww - tx - 8) * pct)), 5, 2, U.lerpColor(0xFFFF4D6A, a1, pct), U.lerpColor(0xFFFF8F9F, a2, pct));
            w = ww; h = hh;
        }
    }

    // ================================================================= points
    static class Points extends HudMod {
        final Setting count = num("Max points", 4, 1, 8, 1), coordsOn = bool("Show coordinates", true);
        Points() { super("Points", "List of your marks with distance (/lv mark add)", Cat.HUD, 0.42f, 0.10f); }
        @Override protected void render(DrawContext c, float td, boolean pv) {
            if (!U.in()) return;
            var p = U.p();
            List<Tools.Mark> l = new ArrayList<>(Tools.visibleMarks());
            l.sort(Comparator.comparingDouble(m -> p.squaredDistanceTo(m.x, m.y, m.z)));
            if (l.isEmpty() && pv) { Tools.Mark m = new Tools.Mark(); m.name = "Base"; m.x = 124; m.y = 68; m.z = -340; m.color = 0xFFC9A7FF; l.add(m); }
            int n = Math.min(l.size(), count.i()), mw = 130, rowH = coordsOn.b ? 24 : 16;
            for (int i = 0; i < n; i++) mw = Math.max(mw, D.w(l.get(i).name) + 80);
            int hh = 24 + n * rowH + 4;
            panel(c, 0, 0, mw, hh, 10);
            Icons.draw(c, Icons.PIN, 8, 7, ClientMods.accent(0));
            D.text(c, "Points", 24, 6, hudText());
            ctxLine(c, mw);
            for (int i = 0; i < n; i++) {
                Tools.Mark m = l.get(i);
                int y = 26 + i * rowH;
                double d = Math.sqrt(p.squaredDistanceTo(m.x, m.y, m.z));
                String ds = d < 1000 ? (int) d + "m" : String.format(Locale.ROOT, "%.1fkm", d / 1000);
                Icons.draw(c, Icons.DIAMOND, 8, y + 2, m.color);
                D.text(c, m.name, 24, y, hudText());
                if (coordsOn.b) D.textS(c, Utils.streamerHides(false) ? "hidden" : "X " + (int) m.x + " Y " + (int) m.y + " Z " + (int) m.z, 24, y + 11, D.a(hudText(), 0.55f), 0.75f);
                D.text(c, ds, mw - 8 - D.w(ds), y, D.a(hudText(), 0.7f));
            }
            w = mw; h = hh;
        }
        void ctxLine(DrawContext c, int mw) { c.fill(8, 21, mw - 8, 22, D.a(0xFFFFFFFF, 0.10f)); }
    }

    // ================================================================= use tracker
    static class UseTracker extends HudMod {
        final Setting hold = num("Hold target (sec)", 8, 2, 20, 1);
        UseTracker() { super("Use Tracker", "Target's active effects and the item it is using", Cat.HUD, 0.70f, 0.40f); }
        static String roman(int n) { String[] r = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"}; return n >= 0 && n < r.length ? r[n] : String.valueOf(n); }
        @Override protected void render(DrawContext c, float td, boolean pv) {
            if (!U.in()) return;
            LivingEntity t = HudMods.target(hold.i());
            if (!(t instanceof PlayerEntity)) { if (!pv) return; t = U.p(); }
            List<String[]> rows = new ArrayList<>();
            for (StatusEffectInstance e : t.getStatusEffects()) {
                int sec = e.getDuration() / 20;
                rows.add(new String[]{e.getEffectType().value().getName().getString() + " " + roman(e.getAmplifier() + 1), e.isInfinite() ? "inf" : sec + "s", e.getEffectType().value().isBeneficial() ? "g" : "r"});
            }
            if (t.isUsingItem()) rows.add(new String[]{t.getActiveItem().getName().getString(), String.format(Locale.ROOT, "%.1fs", t.getItemUseTimeLeft() / 20f), "a"});
            if (rows.isEmpty()) { if (!pv) return; rows.add(new String[]{"No active effects", "", "a"}); }
            int mw = 140;
            String nm = Utils.protect(t.getName().getString());
            mw = Math.max(mw, D.w("Use Tracker") + D.w(nm) + 50);
            for (String[] r : rows) mw = Math.max(mw, D.w(r[0]) + D.w(r[1]) + 46);
            int hh = 26 + rows.size() * 20 + 4;
            panel(c, 0, 0, mw, hh, 10);
            Icons.draw(c, Icons.CROSS, 8, 7, ClientMods.accent(0));
            D.text(c, "Use Tracker", 24, 6, hudText());
            D.text(c, nm, mw - 8 - D.w(nm), 6, D.a(hudText(), 0.7f));
            c.fill(8, 21, mw - 8, 22, D.a(0xFFFFFFFF, 0.10f));
            int y = 26;
            for (String[] r : rows) {
                int dot = r[2].equals("g") ? 0xFF7CFFB0 : r[2].equals("r") ? 0xFFFF6A7A : ClientMods.accent(1);
                D.circle(c, 12, y + 7, 2, dot);
                D.text(c, r[0], 22, y + 2, hudText());
                if (!r[1].isEmpty()) { int cw = D.w(r[1]) + 10; D.rrect(c, mw - 8 - cw, y, cw, 14, 7, D.a(ClientMods.accent(0), 0.28f)); D.text(c, r[1], mw - 8 - cw + 5, y + 2, hudText()); }
                y += 20;
            }
            w = mw; h = hh;
        }
    }

    // ================================================================= item binds (.bind)
    static class ItemBinds extends HudMod {
        final Setting armor = bool("Armor slots", true), showKeys = bool("Key letters", true);
        int step, prevSel, swapIdx; boolean swapped;
        ItemBinds() { super("Item Binds", "Bind items to keys: .bind trap g  (uses the item on key press)", Cat.HUD, 0.008f, 0.58f); enabled = true; }

        @Override public void onTick() {
            var mc = U.mc(); var p = mc.player; long hnd = mc.getWindow().getHandle();
            if (step > 0) {
                step--;
                if (step == 1) mc.interactionManager.interactItem(p, Hand.MAIN_HAND);
                else if (step == 0) restore(p);
                return;
            }
            if (mc.currentScreen != null) return;
            for (Tools.ItemBind b : Tools.BINDS) {
                boolean d = b.key > 0 && InputUtil.isKeyPressed(hnd, b.key);
                if (d && !b.down) trigger(b.kw);
                b.down = d;
            }
        }
        void trigger(String kw) {
            var p = U.p();
            int idx = U.find(s -> Tools.matches(s, kw));
            if (idx < 0) { info("Item Bind", "No item found: " + kw); return; }
            prevSel = p.getInventory().selectedSlot; swapped = false;
            if (idx < 9) p.getInventory().selectedSlot = idx;
            else { U.click(U.slotId(idx), prevSel, SlotActionType.SWAP); swapped = true; swapIdx = idx; }
            step = 3;
        }
        void restore(ClientPlayerEntity p) {
            if (swapped) { U.click(U.slotId(swapIdx), prevSel, SlotActionType.SWAP); swapped = false; }
            p.getInventory().selectedSlot = prevSel;
        }
        ItemStack stackFor(String kw) {
            var p = U.p();
            int idx = U.find(s -> Tools.matches(s, kw));
            return idx >= 0 ? p.getInventory().getStack(idx) : ItemStack.EMPTY;
        }

        @Override protected void render(DrawContext c, float td, boolean pv) {
            if (!U.in()) return;
            var p = U.p(); var tr = U.mc().textRenderer;
            int a1 = ClientMods.accent(0);
            List<Tools.ItemBind> binds = Tools.BINDS;
            int n = Math.min(binds.size(), 6);
            int slots = Math.max(n, armor.b ? 4 : 0);
            int ww = Math.max(60, slots * 30 + 6), hh = 6;
            if (n > 0 || pv) hh += 36;
            if (armor.b) hh += 30;
            if (hh <= 6) hh = 24;
            int y = 4;
            if (n > 0 || pv) {
                for (int i = 0; i < Math.max(1, n); i++) {
                    int x = 6 + i * 30;
                    Tools.ItemBind b = i < n ? binds.get(i) : null;
                    ItemStack s = b == null ? ItemStack.EMPTY : stackFor(b.kw);
                    panel(c, x, y + 8, 26, 26, 8);
                    c.drawBorder(x, y + 8, 26, 26, D.a(a1, 0.55f));
                    if (!s.isEmpty()) { c.drawItem(s, x + 5, y + 13); float cd = p.getItemCooldownManager().getCooldownProgress(s, td); if (cd > 0) { D.rrect(c, x + 2, y + 31, 22, 2, 1, 0x55000000); D.rrect(c, x + 2, y + 31, Math.max(1, (int) (22 * (1 - cd))), 2, 1, a1); } }
                    if (showKeys.b) {
                        String k = b == null ? "-" : ClickGui.keyName(b.key); if (k.length() > 3) k = k.substring(0, 3);
                        int kw = Math.max(14, D.w(k) + 8);
                        D.rrect(c, x + 13 - kw / 2, y, kw, 11, 5, D.a(a1, 0.9f));
                        D.text(c, k, x + 13 - D.w(k) / 2, y + 1, 0xFFFFFFFF);
                    }
                }
                y += 38;
            }
            if (armor.b) {
                var inv = p.getInventory();
                net.minecraft.entity.EquipmentSlot[] sl = {net.minecraft.entity.EquipmentSlot.HEAD, net.minecraft.entity.EquipmentSlot.CHEST, net.minecraft.entity.EquipmentSlot.LEGS, net.minecraft.entity.EquipmentSlot.FEET};
                for (int i = 0; i < 4; i++) {
                    int x = 6 + i * 30;
                    ItemStack s = p.getEquippedStack(sl[i]);
                    panel(c, x, y, 26, 26, 13);
                    int col = 0x55FFFFFF;
                    if (!s.isEmpty()) {
                        c.drawItem(s, x + 5, y + 5);
                        if (s.isDamageable()) { int pct = (s.getMaxDamage() - s.getDamage()) * 100 / s.getMaxDamage(); col = pct > 50 ? 0xFF7CFFB0 : pct > 20 ? 0xFFFFD86B : 0xFFFF6A7A; D.rrect(c, x + 4, y + 23, Math.max(1, 18 * pct / 100), 2, 1, col); }
                    }
                }
            }
            w = ww; h = hh;
        }
    }

    // ================================================================= hotbar
    static class Hotbar extends HudMod {
        final Setting bars = bool("Status bars", true), wave = bool("Liquid wave", true), off = bool("Offhand slot", true), nums = bool("Slot numbers", false);
        final float[] cur = new float[5], slotAnim = new float[9];
        Hotbar() { super("Hotbar", "Custom hotbar with health, armor, food and XP bars (hides the vanilla ones)", Cat.HUD, 0.33f, 0.86f); }

        void bar(DrawContext c, int i, int x, int y, int bw, int bh, float frac, int c1, int c2, String label) {
            cur[i] += (frac - cur[i]) * 0.2f;
            float v = Math.max(0f, Math.min(1f, cur[i]));
            D.rrect(c, x, y, bw, bh, bh / 2, D.a(0xFF000000, 0.55f));
            int fw = (int) (bw * v);
            if (fw > 0) {
                D.rrectV(c, x, y, Math.max(bh, fw), bh, bh / 2, c1, c2);
                if (wave.b && bh >= 6) for (int k = 0; k < Math.max(bh, fw); k += 2) { int yy = y + bh / 2 + (int) (Math.sin(k * 0.5 + Sakura.time() * 4 + i) * (bh * 0.2)); c.fill(x + k, yy, x + k + 2, yy + 1, 0x40FFFFFF); }
            }
            if (label != null && bh >= 8) D.text(c, label, x + bw / 2 - D.w(label) / 2, y + (bh - 9) / 2, 0xFFFFFFFF);
        }

        @Override protected void render(DrawContext c, float td, boolean pv) {
            if (!U.in()) return;
            var p = U.p(); var tr = U.mc().textRenderer;
            int ww = 9 * 22 + 8, barsH = bars.b ? 26 : 0, a1 = ClientMods.accent(0), a2 = ClientMods.accent(1);
            if (bars.b) {
                int half = (ww - 6) / 2;
                float hp = p.getHealth() + p.getAbsorptionAmount(), max = Math.max(1f, p.getMaxHealth());
                boolean abs = p.getAbsorptionAmount() > 0;
                if (p.getArmor() > 0) bar(c, 2, 0, 0, half, 5, p.getArmor() / 20f, 0xFF8FA8D8, 0xFFD0E0FF, null);
                if (p.getAir() < p.getMaxAir()) bar(c, 3, ww - half, 0, half, 5, p.getAir() / (float) p.getMaxAir(), 0xFF4DB8FF, 0xFFB0E4FF, null);
                bar(c, 0, 0, 7, half, 10, hp / max, abs ? 0xFFFFD86B : 0xFFFF4D6A, abs ? 0xFFFFF0A0 : 0xFFFF9EB0, String.valueOf((int) Math.ceil(hp)));
                bar(c, 1, ww - half, 7, half, 10, p.getHungerManager().getFoodLevel() / 20f, 0xFFFF9A3C, 0xFFFFD08A, String.valueOf(p.getHungerManager().getFoodLevel()));
                bar(c, 4, 0, 19, ww, 6, p.experienceProgress, 0xFF7CFF8A, 0xFFD0FFB0, null);
                if (p.experienceLevel > 0) D.textS(c, String.valueOf(p.experienceLevel), ww / 2f - D.w(String.valueOf(p.experienceLevel)) * 0.35f, 18.5f, 0xFFFFFFFF, 0.7f);
            }
            panel(c, 0, barsH, ww, 28, 10);
            int sel = p.getInventory().selectedSlot;
            for (int i = 0; i < 9; i++) {
                slotAnim[i] += ((i == sel ? 1 : 0) - slotAnim[i]) * 0.3f;
                float k = slotAnim[i];
                int x = 4 + i * 22, y = barsH + 3 - (int) (k * 2);
                D.rrectV(c, x, y, 20, 22, 6, U.lerpColor(D.a(0xFFFFFFFF, 0.07f), D.a(a1, 0.85f), k), U.lerpColor(D.a(0xFFFFFFFF, 0.04f), D.a(a2, 0.85f), k));
                ItemStack s = p.getInventory().getStack(i);
                if (!s.isEmpty()) {
                    c.drawItem(s, x + 2, y + 3); c.drawStackOverlay(tr, s, x + 2, y + 3);
                }
                if (nums.b) D.textS(c, String.valueOf(i + 1), x + 2, y, D.a(0xFFFFFFFF, 0.55f), 0.7f);
            }
            if (off.b) {
                ItemStack o = p.getOffHandStack();
                panel(c, -28, barsH + 3, 24, 24, 8);
                if (!o.isEmpty()) c.drawItem(o, -26, barsH + 5);
            }
            w = ww; h = barsH + 28;
        }
    }

    // ================================================================= session
    static class Session extends HudMod {
        long start = System.currentTimeMillis(); int hits, deaths; double walked; boolean wasDead;
        Session() { super("Session", "Play time, hits, deaths and distance walked", Cat.HUD, 0.86f, 0.80f); }
        @Override public void onEnable() { start = System.currentTimeMillis(); hits = 0; deaths = 0; walked = 0; }
        @Override public void onAttack(net.minecraft.entity.Entity e) { hits++; }
        @Override public void onTick() {
            var p = U.p();
            walked += Math.hypot(p.getX() - p.prevX, p.getZ() - p.prevZ);
            if (p.isDead() && !wasDead) deaths++;
            wasDead = p.isDead();
        }
        @Override protected void render(DrawContext c, float td, boolean pv) {
            long s = (System.currentTimeMillis() - start) / 1000;
            String[][] rows = {{"Time", String.format(Locale.ROOT, "%d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60)}, {"Hits", String.valueOf(hits)}, {"Deaths", String.valueOf(deaths)}, {"Walked", walked >= 1000 ? String.format(Locale.ROOT, "%.1fkm", walked / 1000) : (int) walked + "m"}};
            int ww = 112, hh = 24 + rows.length * 14 + 2;
            panel(c, 0, 0, ww, hh, 10);
            Icons.draw(c, Icons.CLOCK, 8, 7, ClientMods.accent(0));
            D.text(c, "Session", 24, 6, hudText());
            c.fill(8, 21, ww - 8, 22, D.a(0xFFFFFFFF, 0.10f));
            for (int i = 0; i < rows.length; i++) {
                D.text(c, rows[i][0], 10, 26 + i * 14, D.a(hudText(), 0.65f));
                D.text(c, rows[i][1], ww - 10 - D.w(rows[i][1]), 26 + i * 14, hudText());
            }
            w = ww; h = hh;
        }
    }

    // ================================================================= fps graph
    static class FpsGraph extends HudMod {
        final ArrayDeque<Integer> samples = new ArrayDeque<>();
        FpsGraph() { super("FPS Graph", "Frame-rate graph of the last 3 seconds", Cat.HUD, 0.86f, 0.70f); }
        @Override public void onTick() { samples.addLast(U.mc().getCurrentFps()); while (samples.size() > 60) samples.pollFirst(); }
        @Override protected void render(DrawContext c, float td, boolean pv) {
            int ww = 132, hh = 48, a1 = ClientMods.accent(0), a2 = ClientMods.accent(1);
            panel(c, 0, 0, ww, hh, 10);
            int mx = 1, mn = Integer.MAX_VALUE, cur = U.mc().getCurrentFps();
            for (int s : samples) { mx = Math.max(mx, s); mn = Math.min(mn, s); }
            if (mn == Integer.MAX_VALUE) mn = cur;
            Icons.draw(c, Icons.BOLT, 8, 5, a1);
            D.text(c, cur + " FPS", 22, 4, hudText());
            String mm = "min " + mn + "  max " + mx; D.textS(c, mm, ww - 8 - D.w(mm) * 0.75f, 6, D.a(hudText(), 0.6f), 0.75f);
            int i = 0;
            for (int s : samples) {
                int bh = Math.max(1, (int) (24f * s / mx));
                c.fill(8 + i * 2, hh - 6 - bh, 9 + i * 2, hh - 6, U.lerpColor(a2, a1, s / (float) mx));
                i++;
            }
            w = ww; h = hh;
        }
    }

    // ================================================================= nearby players
    static class Nearby extends HudMod {
        final Setting range = num("Range (blocks)", 64, 16, 160, 4), max = num("Max players", 6, 2, 10, 1);
        Nearby() { super("Nearby", "Players around you with distance and health", Cat.HUD, 0.86f, 0.45f); }
        @Override protected void render(DrawContext c, float td, boolean pv) {
            if (!U.in()) return;
            var p = U.p();
            List<PlayerEntity> l = new ArrayList<>();
            for (PlayerEntity o : U.mc().world.getPlayers()) if (o != p && o.getId() != -4242 && o.distanceTo(p) <= range.d()) l.add(o);
            l.sort(Comparator.comparingDouble(o -> o.distanceTo(p)));
            int n = Math.min(l.size(), max.i());
            if (n == 0 && !pv) return;
            int mw = 120;
            for (int i = 0; i < n; i++) mw = Math.max(mw, D.w(l.get(i).getName().getString()) + 74);
            int hh = 24 + Math.max(1, n) * 16 + 2;
            panel(c, 0, 0, mw, hh, 10);
            Icons.draw(c, Icons.USER, 8, 7, ClientMods.accent(0));
            D.text(c, "Nearby", 24, 6, hudText());
            String cnt = String.valueOf(l.size()); D.text(c, cnt, mw - 8 - D.w(cnt), 6, D.a(hudText(), 0.65f));
            c.fill(8, 21, mw - 8, 22, D.a(0xFFFFFFFF, 0.10f));
            if (n == 0) D.text(c, "Nobody around", 10, 27, D.a(hudText(), 0.5f));
            for (int i = 0; i < n; i++) {
                PlayerEntity o = l.get(i); int y = 26 + i * 16;
                boolean fr = Friends.is(o);
                D.circle(c, 12, y + 5, 2, fr ? 0xFF55FF88 : 0xFFFF6A7A);
                D.text(c, Utils.protect(o.getName().getString()), 22, y, fr ? 0xFF55FF88 : hudText());
                String d = (int) o.distanceTo(p) + "m  " + (int) Math.ceil(o.getHealth()) + "hp";
                D.text(c, d, mw - 8 - D.w(d), y, D.a(hudText(), 0.7f));
            }
            w = mw; h = hh;
        }
    }
}
