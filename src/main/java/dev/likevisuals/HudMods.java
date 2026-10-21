package dev.likevisuals;

import net.minecraft.block.BlockState;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.scoreboard.number.NumberFormat;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class HudMods {
    public static void register() {
        Modules.add(new Watermark()); Modules.add(new InfoHud()); Modules.add(new ModuleList()); Modules.add(new PotionsHud());
        Modules.add(new ScoreboardHud()); Modules.add(new TargetHud()); Modules.add(new MiniMap()); Modules.add(new InventoryHud());
        Modules.add(new Keystrokes()); Modules.add(new ArmorHud()); Modules.add(new Compass()); Modules.add(new Silhouette());
    }

    public static boolean scoreboardOn() { return Modules.on(ScoreboardHud.class); }

    /** Target shared by Target HUD / Silhouette. */
    static LivingEntity target(int holdSec) {
        var mc = U.mc();
        Entity t = Modules.lastTarget;
        if (t instanceof LivingEntity le && le.isAlive() && System.currentTimeMillis() - Modules.lastTargetTime < holdSec * 1000L) return le;
        if (mc.crosshairTarget instanceof EntityHitResult er && er.getEntity() instanceof LivingEntity le2) return le2;
        return null;
    }

    // ================================================================= Watermark
    static class Watermark extends HudMod {
        final Setting style = mode("Style", 0, "Bar", "Compact", "Segments");
        final Setting logo = bool("Logo", true), name = bool("Name", true), fps = bool("FPS", true), ping = bool("Ping", true), bps = bool("BPS", true),
            time = bool("Time", false), coords = bool("Coordinates", false), server = bool("Server", false), ver = bool("Version", false), line = bool("Animated line", false);

        Watermark() { super("Watermark", "Modern top bar: logo, name, FPS, ping and speed", Cat.HUD, 0.008f, 0.012f); enabled = true; }

        List<Object[]> parts() {
            List<Object[]> l = new ArrayList<>();
            var mc = U.mc(); var p = mc.player;
            if (p == null) return l;
            if (name.b) l.add(new Object[]{Icons.USER, Utils.protect(p.getName().getString()), ""});
            if (fps.b) l.add(new Object[]{Icons.BOLT, String.valueOf(mc.getCurrentFps()), "FPS"});
            if (ping.b && mc.getNetworkHandler() != null) { PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(p.getUuid()); l.add(new Object[]{Icons.WIFI, String.valueOf(e == null ? 0 : e.getLatency()), "MS"}); }
            if (bps.b) l.add(new Object[]{Icons.SPEED, String.format(Locale.ROOT, "%.1f", Math.hypot(p.getX() - p.prevX, p.getZ() - p.prevZ) * 20), "BPS"});
            if (time.b) l.add(new Object[]{Icons.CLOCK, LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")), ""});
            if (coords.b && !Utils.streamerHides(false)) l.add(new Object[]{Icons.PIN, String.format(Locale.ROOT, "%.0f %.0f %.0f", p.getX(), p.getY(), p.getZ()), ""});
            if (server.b && !Utils.streamerHides(true)) { var s = mc.getCurrentServerEntry(); l.add(new Object[]{Icons.SHIELD, s == null ? "Singleplayer" : s.address, ""}); }
            if (ver.b) l.add(new Object[]{Icons.DIAMOND, "1.21.4", ""});
            return l;
        }

        @Override protected void render(DrawContext c, float td, boolean pv) {
            if (!U.in()) return;
            int a1 = ClientMods.accent(0), a2 = ClientMods.accent(1), tc = hudText();
            List<Object[]> ps = parts();
            String title = "LikeVisuals";
            if (style.mode == 1) {
                int x = 0;
                if (logo.b) { Logo.draw(c, 1, 1, 22); x = 28; }
                D.gradTextS(c, title, x, 0, a1, a2, 1.4f);
                StringBuilder sb = new StringBuilder();
                for (Object[] o : ps) { if (sb.length() > 0) sb.append("   "); sb.append(o[1]).append(o[2]); }
                D.text(c, sb.toString(), x + 1, 16, D.a(tc, 0.9f));
                w = Math.max(x + (int) (D.w(title) * 1.4f), x + D.w(sb.toString())); h = 28;
                return;
            }
            if (style.mode == 2) {
                int x = 0, bw = D.w(title) + (logo.b ? 34 : 18);
                panel(c, 0, 0, bw, 22, 11);
                int tx = 9;
                if (logo.b) { Logo.draw(c, 3, 3, 16); tx = 26; }
                D.gradText(c, title, tx, 6, a1, a2);
                x = bw + 4;
                for (Object[] o : ps) {
                    String s = o[1] + (String) o[2];
                    int sw = D.w(s) + 28;
                    panel(c, x, 0, sw, 22, 11);
                    Icons.draw(c, (Integer) o[0], x + 8, 6, a1);
                    D.text(c, s, x + 22, 6, tc);
                    x += sw + 4;
                }
                w = x - 4; h = 22;
                return;
            }
            // Bar: one panel, icon + value segments separated by chevrons
            int wd = 10;
            if (logo.b) wd += 24;
            wd += D.w(title) + 8;
            for (Object[] o : ps) wd += 12 + 16 + D.w(o[1] + (String) o[2]) + 4;
            panel(c, 0, 0, wd, 24, 12);
            int x = 6;
            if (logo.b) { Logo.draw(c, 4, 3, 18); x = 28; }
            D.gradText(c, title, x, 7, a1, a2);
            x += D.w(title) + 8;
            for (Object[] o : ps) {
                Icons.draw(c, Icons.CHEV, x, 7, D.a(tc, 0.35f));
                x += 12;
                Icons.draw(c, (Integer) o[0], x, 7, a1);
                x += 14;
                String v = (String) o[1], u = (String) o[2];
                D.text(c, v, x, 7, tc);
                x += D.w(v);
                if (!u.isEmpty()) { D.text(c, u, x, 7, a1); x += D.w(u); }
                x += 4;
            }
            if (line.b) for (int i = 0; i < wd - 20; i++) c.fill(10 + i, 21, 11 + i, 23, U.lerpColor(a1, a2, (float) (Math.sin(Sakura.time() * 2 + i * 0.03) * 0.5 + 0.5)));
            w = wd; h = 24;
        }
    }

    // ================================================================= InfoHud
    static class InfoHud extends HudMod {
        final Setting coords = bool("Coordinates", true), speed = bool("Speed", true), time = bool("Time", false), ip = bool("Server IP", false),
            biome = bool("Biome", false), dir = bool("Direction", false), light = bool("Light level", false), nether = bool("Nether coords", false), combined = bool("Combine coords + speed", true);

        InfoHud() { super("Hud", "Info pills: coordinates, speed, time, biome", Cat.HUD, 0.008f, 0.075f); enabled = true; }

        @Override protected void render(DrawContext c, float td, boolean pv) {
            if (!U.in()) return;
            var mc = U.mc(); var p = mc.player;
            int a1 = ClientMods.accent(0), a2 = ClientMods.accent(1), tc = hudText();
            int y = 0, mw = 40;
            String bps = String.format(Locale.ROOT, "%.0fBPS", Math.hypot(p.getX() - p.prevX, p.getZ() - p.prevZ) * 20);
            if (coords.b) {
                String s = Utils.streamerHides(false) ? "x? y? z?" : String.format(Locale.ROOT, "x%.0f y%.0f z%.0f", p.getX(), p.getY(), p.getZ());
                int pw = D.w(s) + 28 + (combined.b && speed.b ? D.w(bps) + 26 : 0);
                panel(c, 0, y, pw, 20, 10);
                Icons.draw(c, Icons.PIN, 7, y + 5, a1);
                D.text(c, s, 21, y + 6, tc);
                if (combined.b && speed.b) { Icons.draw(c, Icons.CHEV, 24 + D.w(s), y + 5, D.a(tc, 0.35f)); Icons.draw(c, Icons.SPEED, 38 + D.w(s), y + 5, a2); D.text(c, bps, 51 + D.w(s), y + 6, tc); }
                mw = Math.max(mw, pw); y += 24;
            }
            if (speed.b && !(combined.b && coords.b)) { mw = Math.max(mw, iconPill(c, 0, y, Icons.SPEED, bps, a2)); y += 22; }
            if (nether.b && !Utils.streamerHides(false)) {
                boolean n = mc.world.getRegistryKey() == net.minecraft.world.World.NETHER;
                mw = Math.max(mw, iconPill(c, 0, y, Icons.PIN, n ? String.format(Locale.ROOT, "Overworld %d %d", (int) (p.getX() * 8), (int) (p.getZ() * 8)) : String.format(Locale.ROOT, "Nether %d %d", (int) (p.getX() / 8), (int) (p.getZ() / 8)), 0xFFFF8F8F)); y += 22;
            }
            if (dir.b) { mw = Math.max(mw, iconPill(c, 0, y, Icons.DIAMOND, p.getHorizontalFacing().name() + String.format(Locale.ROOT, " %.0f", MathHelper.wrapDegrees(p.getYaw())), 0xFFB8C8FF)); y += 22; }
            if (biome.b) { mw = Math.max(mw, iconPill(c, 0, y, Icons.DIAMOND, mc.world.getBiome(p.getBlockPos()).getKey().map(k -> k.getValue().getPath()).orElse("?"), 0xFF9FE6A0)); y += 22; }
            if (light.b) { mw = Math.max(mw, iconPill(c, 0, y, Icons.BOLT, "Light " + mc.world.getLightLevel(p.getBlockPos()), 0xFFFFF0A0)); y += 22; }
            if (time.b) { mw = Math.max(mw, iconPill(c, 0, y, Icons.CLOCK, LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")), a1)); y += 22; }
            if (ip.b && !Utils.streamerHides(true)) { var sv = mc.getCurrentServerEntry(); mw = Math.max(mw, iconPill(c, 0, y, Icons.SHIELD, sv == null ? "Singleplayer" : sv.address, a1)); y += 22; }
            w = mw; h = Math.max(18, y);
        }
    }

    // ================================================================= ModuleList
    static class ModuleList extends HudMod {
        final Setting sort = mode("Сортировка", 0, "По длине", "По алфавиту", "По категории");
        final Setting cats = bool("Показывать цвет категории", true);
        ModuleList() { super("Module List", "Список включённых модулей", Cat.HUD, 0.86f, 0.012f); enabled = true; }
        @Override protected void render(DrawContext c, float td, boolean pv) {
            List<Mod> en = new ArrayList<>();
            for (Mod m : Modules.ALL) if (m.enabled && m != this && m.cat != Cat.CLIENT && !(m instanceof HudMod)) en.add(m);
            if (en.isEmpty() && pv) return;
            if (sort.mode == 0) en.sort((x, z) -> D.w(z.name) - D.w(x.name));
            else if (sort.mode == 1) en.sort(Comparator.comparing(x -> x.name));
            else en.sort(Comparator.comparingInt((Mod x) -> x.cat.ordinal()).thenComparing(x -> x.name));
            int mw = 60;
            for (Mod m : en) mw = Math.max(mw, D.w(m.name) + 22);
            int y = 0, i = 0, a1 = ClientMods.accent(0), a2 = ClientMods.accent(1);
            for (Mod m : en) {
                int rw = D.w(m.name) + 22, x = mw - rw;
                panel(c, x, y, rw, 16, 8);
                int col = cats.b ? m.cat.color : U.lerpColor(a1, a2, (i % 8) / 7f);
                D.circle(c, x + 9, y + 8, 2, col);
                D.text(c, m.name, x + 15, y + 4, 0xFFFFFFFF);
                y += 18; i++;
            }
            w = mw; h = Math.max(16, y);
        }
    }

    // ================================================================= Potions + cooldowns
    static class PotionsHud extends HudMod {
        final Setting effects = bool("Эффекты", true), cds = bool("Кулдауны предметов", true), bars = bool("Полоски времени", true), names = bool("Названия", true);
        final Map<String, Integer> maxDur = new HashMap<>();
        PotionsHud() { super("Potions & Cooldowns", "Эффекты зелий и кулдауны предметов с иконками", Cat.HUD, 0.008f, 0.45f); enabled = true; }

        @Override protected void render(DrawContext c, float td, boolean pv) {
            if (!U.in()) return;
            var p = U.p(); var tr = U.mc().textRenderer;
            int y = 0, mw = 110, a1 = ClientMods.accent(0), a2 = ClientMods.accent(1);
            if (effects.b) {
                for (StatusEffectInstance e : p.getStatusEffects()) {
                    String key = e.getEffectType().getKey().map(k -> k.getValue().getPath()).orElse("unknown");
                    int dur = e.getDuration();
                    int mx = maxDur.merge(key, dur, Math::max);
                    if (dur > mx) { maxDur.put(key, dur); mx = dur; }
                    String nm = names.b ? e.getEffectType().value().getName().getString() + (e.getAmplifier() > 0 ? " " + (e.getAmplifier() + 1) : "") : "";
                    int sec = dur / 20;
                    String t = e.isInfinite() ? "inf" : String.format(Locale.ROOT, "%d:%02d", sec / 60, sec % 60);
                    int rw = 30 + D.w(nm) + D.w(t) + 24; mw = Math.max(mw, rw);
                    row(c, y, rw0(mw), key, null, nm, t, e.isInfinite() ? 1f : Math.min(1f, dur / (float) Math.max(1, mx)), e.getEffectType().value().isBeneficial() ? 0xFF7CFFB0 : 0xFFFF7C8F, a1, a2);
                    y += 24;
                }
            }
            if (cds.b) {
                Set<Object> seen = new HashSet<>();
                List<ItemStack> list = new ArrayList<>();
                for (int i = 0; i < 9; i++) list.add(p.getInventory().getStack(i));
                list.add(p.getOffHandStack());
                for (ItemStack s : list) {
                    if (s.isEmpty() || !seen.add(s.getItem())) continue;
                    float cd = p.getItemCooldownManager().getCooldownProgress(s, td);
                    if (cd <= 0f) continue;
                    String nm = names.b ? s.getName().getString() : "";
                    String t = (int) (cd * 100) + "%";
                    mw = Math.max(mw, 30 + D.w(nm) + D.w(t) + 24);
                    row(c, y, rw0(mw), null, s, nm, t, 1f - cd, 0xFFFFB84D, a1, a2);
                    y += 24;
                }
            }
            if (y == 0 && pv) { // preview rows so the element can be positioned
                row(c, 0, 130, "speed", null, "Speed II", "1:24", 0.6f, 0xFF7CFFB0, a1, a2);
                row(c, 24, 130, null, new ItemStack(net.minecraft.item.Items.ENDER_PEARL), "Ender Pearl", "40%", 0.4f, 0xFFFFB84D, a1, a2);
                y = 48; mw = 130;
            }
            w = mw; h = Math.max(20, y);
        }
        int rw0(int mw) { return mw; }

        void row(DrawContext c, int y, int rw, String effect, ItemStack item, String name, String time, float frac, int barCol, int a1, int a2) {
            panel(c, 0, y, rw, 22, 9);
            boolean drawn = false;
            if (effect != null) {
                try { c.drawGuiTexture(RenderLayer::getGuiTextured, Identifier.ofVanilla("mob_effect/" + effect), 4, y + 3, 16, 16); drawn = true; } catch (Throwable ignored) {}
                if (!drawn) D.circle(c, 12, y + 11, 5, barCol);
            } else if (item != null) c.drawItem(item, 4, y + 3);
            D.text(c, name, 26, y + 4, 0xFFFFFFFF);
            D.text(c, time, rw - 6 - D.w(time), y + 4, D.a(0xFFFFFFFF, 0.8f));
            if (bars.b) {
                D.rrect(c, 26, y + 16, rw - 34, 3, 1, 0x55000000);
                D.rrectV(c, 26, y + 16, Math.max(2, (int) ((rw - 34) * frac)), 3, 1, barCol, U.lerpColor(barCol, 0xFFFFFFFF, 0.3f));
            }
        }
    }

    // ================================================================= Custom scoreboard
    static class ScoreboardHud extends HudMod {
        final Setting header = bool("Заголовок", true), logo = bool("Логотип", true), nums = bool("Показывать числа", true),
            rows = num("Макс. строк", 15, 5, 15, 1), accentLine = bool("Акцентная линия", true), center = bool("Заголовок по центру", true);
        ScoreboardHud() { super("Custom Scoreboard", "Красивый скорборд вместо ванильного: двигается и масштабируется", Cat.HUD, 0.86f, 0.30f); }

        @Override protected void render(DrawContext c, float td, boolean pv) {
            if (!U.in()) return;
            var mc = U.mc(); var tr = mc.textRenderer;
            Scoreboard sb = mc.world.getScoreboard();
            ScoreboardObjective obj = sb.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
            List<Text> names = new ArrayList<>(), vals = new ArrayList<>();
            Text title;
            if (obj == null) {
                if (!pv) return;
                title = Text.literal("LikeVisuals");
                names.add(Text.literal("Name: Player")); vals.add(Text.literal(""));
                names.add(Text.literal("Balance: 1000")); vals.add(Text.literal(""));
                names.add(Text.literal("Online: 42")); vals.add(Text.literal(""));
            } else {
                title = obj.getDisplayName();
                NumberFormat nf = obj.getNumberFormat();
                List<ScoreboardEntry> es = new ArrayList<>();
                for (ScoreboardEntry e : sb.getScoreboardEntries(obj)) if (!e.hidden()) es.add(e);
                es.sort(Comparator.comparingInt(ScoreboardEntry::value).reversed());
                int n = 0;
                for (ScoreboardEntry e : es) {
                    if (n++ >= rows.i()) break;
                    Team t = sb.getScoreHolderTeam(e.owner());
                    Text nm = Team.decorateName(t, e.name());
                    String str = nm.getString();
                    if (Utils.censor(str)) nm = Text.literal("\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588");
                    names.add(nm);
                    if (!nums.b) vals.add(Text.literal(""));
                    else if (nf != null) vals.add(nf.format(e.value()));
                    else vals.add(Text.literal(String.valueOf(e.value())).formatted(Formatting.RED));
                }
            }
            Style f = Style.EMPTY.withFont(D.FONT);
            int mw = tr.getWidth(Text.empty().setStyle(f).append(title)) + (logo.b ? 30 : 0) + 20;
            for (int i = 0; i < names.size(); i++) {
                int nw = tr.getWidth(Text.empty().setStyle(f).append(names.get(i)));
                int vw = tr.getWidth(Text.empty().setStyle(f).append(vals.get(i)));
                mw = Math.max(mw, nw + vw + (vw > 0 ? 14 : 0) + 18);
            }
            int hh = header.b ? 26 : 6;
            int total = hh + names.size() * 11 + 8;
            panel(c, 0, 0, mw, total, 10);
            int a1 = ClientMods.accent(0), a2 = ClientMods.accent(1);
            if (header.b) {
                int tw = tr.getWidth(Text.empty().setStyle(f).append(title));
                int tx = center.b ? (mw - tw) / 2 + (logo.b ? 10 : 0) : (logo.b ? 28 : 8);
                if (logo.b) Logo.draw(c, 6, 4, 18);
                c.drawText(tr, Text.empty().setStyle(f).append(title), tx, 9, 0xFFFFFFFF, false);
                if (accentLine.b) for (int x = 8; x < mw - 8; x++) c.fill(x, 24, x + 1, 25, U.lerpColor(a1, a2, (x - 8) / (float) Math.max(1, mw - 16)));
            }
            int y = hh + 3;
            for (int i = 0; i < names.size(); i++) {
                c.drawText(tr, Text.empty().setStyle(f).append(names.get(i)), 8, y, 0xFFFFFFFF, false);
                int vw = tr.getWidth(Text.empty().setStyle(f).append(vals.get(i)));
                if (vw > 0) c.drawText(tr, Text.empty().setStyle(f).append(vals.get(i)), mw - 8 - vw, y, 0xFFFFFFFF, false);
                y += 11;
            }
            w = mw; h = total;
        }
    }

    // ================================================================= Target HUD
    static class TargetHud extends HudMod {
        final Setting hold = num("Время показа (сек)", 5, 1, 15, 1);
        final Setting face = bool("Голова / иконка", true), armor = bool("Броня", true), dist = bool("Дистанция", true), chipBar = bool("След урона на полоске", true);
        LivingEntity cur; float shown, chip, last, flash;
        TargetHud() { super("Target HUD", "Ник и здоровье цели; полоска уменьшается при каждом ударе", Cat.HUD, 0.40f, 0.62f); }

        @Override protected void render(DrawContext c, float td, boolean pv) {
            if (!U.in()) return;
            var mc = U.mc();
            LivingEntity t = target(hold.i());
            if (t == null) { if (!pv) return; t = mc.player; }
            float hp = t.getHealth() + t.getAbsorptionAmount(), max = Math.max(1f, t.getMaxHealth());
            if (cur != t) { cur = t; shown = hp; chip = hp; last = hp; flash = 0; }
            if (hp < last - 0.01f) flash = 1f;
            last = hp;
            shown += (hp - shown) * 0.22f;
            if (hp > chip) chip = hp; else chip += (hp - chip) * 0.035f;
            flash = Math.max(0, flash - 0.05f);
            int a1 = ClientMods.accent(0), a2 = ClientMods.accent(1);
            int ww = 166, hh = 46;
            panel(c, 0, 0, ww, hh, 11);
            if (flash > 0.01f) D.rrect(c, 0, 0, ww, hh, 11, D.a(0xFFFF4D6A, 0.25f * flash));
            int tx = 8;
            if (face.b) {
                tx = 48;
                boolean done = false;
                if (t instanceof PlayerEntity pe && mc.getNetworkHandler() != null) {
                    PlayerListEntry ple = mc.getNetworkHandler().getPlayerListEntry(pe.getUuid());
                    if (ple != null) { try { PlayerSkinDrawer.draw(c, ple.getSkinTextures(), 7, 7, 32); done = true; } catch (Throwable ignored) {} }
                }
                if (!done) {
                    D.rrectV(c, 7, 7, 32, 32, 8, D.a(a1, 0.5f), D.a(a2, 0.5f));
                    String l = t.getName().getString().substring(0, 1).toUpperCase();
                    D.textS(c, l, 18, 12, 0xFFFFFFFF, 1.6f);
                }
            }
            String nm = Utils.protect(t.getName().getString());
            D.text(c, D.clip(nm, ww - tx - 10), tx, 7, 0xFFFFFFFF);
            int bx = tx, by = 21, bw = ww - tx - 8;
            D.rrect(c, bx, by, bw, 9, 4, 0x77000000);
            if (chipBar.b) D.rrect(c, bx, by, Math.max(2, (int) (bw * Math.min(1f, chip / max))), 9, 4, D.a(0xFFFFFFFF, 0.55f));
            float pct = Math.max(0f, Math.min(1f, shown / max));
            int col1 = U.lerpColor(0xFFFF4D6A, 0xFF7CFFB0, pct), col2 = U.lerpColor(0xFFFF8F9F, 0xFFB8FFD6, pct);
            D.rrectV(c, bx, by, Math.max(4, (int) (bw * pct)), 9, 4, col1, col2);
            String hs = String.format(Locale.ROOT, "%.1f / %.0f", hp, max);
            D.text(c, hs, bx + 4, by + 1, 0xFFFFFFFF);
            String info = (armor.b ? "Armor " + t.getArmor() : "") + (armor.b && dist.b ? "   " : "") + (dist.b ? String.format(Locale.ROOT, "%.1f m", mc.player.distanceTo(t)) : "");
            if (!info.isEmpty()) D.text(c, info, tx, 33, D.a(0xFFFFFFFF, 0.75f));
            w = ww; h = hh;
        }
    }

    // ================================================================= MiniMap
    static class MiniMap extends HudMod {
        final Setting size = num("Размер", 110, 70, 200, 5), radius = num("Радиус (блоки)", 40, 16, 120, 4);
        final Setting shape = mode("Форма", 0, "Круг", "Квадрат");
        final Setting ents = bool("Сущности", true), marks = bool("Метки", true), coordsS = bool("Координаты", true), north = bool("Буква N", true);
        final Setting refresh = num("Обновление (мс)", 300, 100, 2000, 50);
        static final int N = 64;
        final int[] cache = new int[N * N];
        long lastUpdate; boolean filled;

        MiniMap() { super("Mini Map", "Мини-карта с метками, игроками и мобами", Cat.HUD, 0.008f, 0.63f); }

        int sample(ClientWorld wd, int x, int z, int py) {
            if (!wd.getChunkManager().isChunkLoaded(x >> 4, z >> 4)) return 0xFF0B0B12;
            int y;
            if (wd.getDimension().hasCeiling()) {
                y = py + 3;
                BlockPos.Mutable m = new BlockPos.Mutable(x, y, z);
                while (y > wd.getBottomY() && wd.getBlockState(m.set(x, y, z)).isAir()) y--;
            } else y = wd.getTopY(Heightmap.Type.WORLD_SURFACE, x, z) - 1;
            BlockPos pos = new BlockPos(x, y, z);
            BlockState st = wd.getBlockState(pos);
            int col = st.getMapColor(wd, pos).color;
            if (col == 0) col = 0x2A2A38;
            int yn = wd.getDimension().hasCeiling() ? y : wd.getTopY(Heightmap.Type.WORLD_SURFACE, x, z - 1) - 1;
            float k = 1f + Math.max(-2, Math.min(2, y - yn)) * 0.08f;
            int r = Math.min(255, (int) (((col >> 16) & 255) * k)), g = Math.min(255, (int) (((col >> 8) & 255) * k)), b = Math.min(255, (int) ((col & 255) * k));
            return 0xFF000000 | (r << 16) | (g << 8) | b;
        }

        @Override protected void render(DrawContext c, float td, boolean pv) {
            if (!U.in()) return;
            var mc = U.mc(); var p = mc.player; var wd = mc.world;
            int S = size.i(); boolean circ = shape.mode == 0;
            int a1 = ClientMods.accent(0), a2 = ClientMods.accent(1);
            double px = MathHelper.lerp(td, p.prevX, p.getX()), pz = MathHelper.lerp(td, p.prevZ, p.getZ());
            long now = System.currentTimeMillis();
            if (!filled || now - lastUpdate > refresh.i()) {
                lastUpdate = now; filled = true;
                double span = radius.d() * 2;
                for (int j = 0; j < N; j++) for (int i = 0; i < N; i++) {
                    int wx = (int) Math.floor(px - radius.d() + (i + 0.5) * span / N), wz = (int) Math.floor(pz - radius.d() + (j + 0.5) * span / N);
                    cache[i + j * N] = sample(wd, wx, wz, (int) p.getY());
                }
            }
            D.rrectV(c, -2, -2, S + 4, S + 4, circ ? (S + 4) / 2 : 9, D.a(a1, 0.95f), D.a(a2, 0.95f));
            D.rrect(c, 0, 0, S, S, circ ? S / 2 : 7, 0xFF0B0B12);
            var ms = c.getMatrices();
            float cell = S / (float) N;
            ms.push();
            ms.scale(cell, cell, 1f);
            int half = N / 2;
            for (int j = 0; j < N; j++) for (int i = 0; i < N; i++) {
                if (circ) { int dx = i - half, dy = j - half; if (dx * dx + dy * dy > (half - 1) * (half - 1)) continue; }
                c.fill(i, j, i + 1, j + 1, cache[i + j * N]);
            }
            ms.pop();
            double scale = S / 2.0 / radius.d();
            if (marks.b) for (Tools.Mark m : Tools.visibleMarks()) {
                double dx = (m.x - px) * scale, dz = (m.z - pz) * scale;
                double d = Math.hypot(dx, dz), lim = S / 2.0 - 5;
                if (d > lim) { dx = dx / d * lim; dz = dz / d * lim; }
                int mx = (int) (S / 2 + dx), my = (int) (S / 2 + dz);
                ms.push(); ms.translate(mx, my, 0); ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(45f));
                c.fill(-3, -3, 3, 3, 0xFFFFFFFF); c.fill(-2, -2, 2, 2, m.color); ms.pop();
            }
            if (ents.b) for (Entity e : wd.getEntities()) {
                if (e == p || !(e instanceof LivingEntity) || !e.isAlive()) continue;
                double dx = (e.getX() - px) * scale, dz = (e.getZ() - pz) * scale;
                if (circ ? Math.hypot(dx, dz) > S / 2.0 - 3 : (Math.abs(dx) > S / 2.0 - 3 || Math.abs(dz) > S / 2.0 - 3)) continue;
                int col;
                if (e instanceof PlayerEntity) col = Friends.is(e) ? 0xFF55FF88 : 0xFFFF4D6A;
                else if (e instanceof Monster) col = 0xFFFFA040; else if (e instanceof AnimalEntity) col = 0xFFE8E8F0; else col = 0xFFB0B0C0;
                int ex = (int) (S / 2 + dx), ey = (int) (S / 2 + dz);
                c.fill(ex - 1, ey - 1, ex + 2, ey + 2, 0xFF000000); c.fill(ex, ey, ex + 1, ey + 1, col);
                if (e instanceof PlayerEntity) c.fill(ex - 1, ey - 1, ex + 2, ey + 2, col);
            }
            ms.push();
            ms.translate(S / 2f, S / 2f, 0);
            ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(MathHelper.wrapDegrees(p.getYaw()) + 180f));
            c.fill(-1, -4, 1, 3, 0xFFFFFFFF); c.fill(-3, 0, 3, 2, 0xFFFFFFFF); c.fill(-2, -2, 2, 0, 0xFFFFFFFF);
            ms.pop();
            if (north.b) D.text(c, "N", S / 2 - D.w("N") / 2, 2, 0xFFFFFFFF);
            int hh = S;
            if (coordsS.b) {
                String s = Utils.streamerHides(false) ? "hidden" : (int) p.getX() + "  " + (int) p.getY() + "  " + (int) p.getZ();
                D.text(c, s, S / 2 - D.w(s) / 2, S + 4, 0xFFFFFFFF);
                hh += 14;
            }
            w = S; h = hh;
        }
    }

    // ================================================================= Inventory HUD
    static class InventoryHud extends HudMod {
        final Setting hotbar = bool("Показывать хотбар", false), title = bool("Заголовок", true);
        InventoryHud() { super("Inventory HUD", "Твой инвентарь прямо на экране", Cat.HUD, 0.66f, 0.74f); }
        @Override protected void render(DrawContext c, float td, boolean pv) {
            if (!U.in()) return;
            var p = U.p(); var tr = U.mc().textRenderer;
            int rows = hotbar.b ? 4 : 3, top = title.b ? 16 : 4;
            int ww = 9 * 18 + 8, hh = top + rows * 18 + 6;
            panel(c, 0, 0, ww, hh, 9);
            if (title.b) { Logo.draw(c, 5, 3, 11); D.text(c, "Inventory", 21, 4, 0xFFFFFFFF); }
            for (int r = 0; r < 3; r++) for (int col = 0; col < 9; col++) {
                int x = 4 + col * 18, y = top + r * 18;
                D.rrect(c, x, y, 17, 17, 3, 0x33FFFFFF);
                ItemStack s = p.getInventory().getStack(9 + r * 9 + col);
                if (!s.isEmpty()) { c.drawItem(s, x + 1, y + 1); c.drawStackOverlay(tr, s, x + 1, y + 1); }
            }
            if (hotbar.b) for (int col = 0; col < 9; col++) {
                int x = 4 + col * 18, y = top + 3 * 18;
                D.rrect(c, x, y, 17, 17, 3, D.a(ClientMods.accent(0), 0.25f));
                ItemStack s = p.getInventory().getStack(col);
                if (!s.isEmpty()) { c.drawItem(s, x + 1, y + 1); c.drawStackOverlay(tr, s, x + 1, y + 1); }
            }
            w = ww; h = hh;
        }
    }

    // ================================================================= Keystrokes
    static class Keystrokes extends HudMod {
        final Setting cps = bool("CPS", true), space = bool("Пробел", true), mouse = bool("Мышь", true);
        final float[] anim = new float[7];
        final ArrayDeque<Long> lc = new ArrayDeque<>(), rc = new ArrayDeque<>();
        boolean wl, wr;
        Keystrokes() { super("Keystrokes", "Клавиши WASD, мышь и CPS", Cat.HUD, 0.008f, 0.30f); }

        @Override public void onTick() {
            var o = U.mc().options; long now = System.currentTimeMillis();
            boolean l = o.attackKey.isPressed(), r = o.useKey.isPressed();
            if (l && !wl) lc.add(now); if (r && !wr) rc.add(now);
            wl = l; wr = r;
            while (!lc.isEmpty() && now - lc.peekFirst() > 1000) lc.pollFirst();
            while (!rc.isEmpty() && now - rc.peekFirst() > 1000) rc.pollFirst();
        }

        void key(DrawContext c, int i, int x, int y, int kw, int kh, String label, boolean down, String sub) {
            anim[i] += ((down ? 1 : 0) - anim[i]) * 0.3f;
            int a1 = ClientMods.accent(0), a2 = ClientMods.accent(1);
            panel(c, x, y, kw, kh, 6);
            if (anim[i] > 0.02f) D.rrectV(c, x, y, kw, kh, 6, D.a(a1, 0.85f * anim[i]), D.a(a2, 0.85f * anim[i]));
            int ty = sub == null ? y + (kh - 9) / 2 : y + 3;
            D.text(c, label, x + (kw - D.w(label)) / 2, ty, 0xFFFFFFFF);
            if (sub != null) D.text(c, sub, x + (kw - D.w(sub)) / 2, y + kh - 11, D.a(0xFFFFFFFF, 0.75f));
        }

        @Override protected void render(DrawContext c, float td, boolean pv) {
            var o = U.mc().options;
            key(c, 0, 25, 0, 22, 22, "W", o.forwardKey.isPressed(), null);
            key(c, 1, 0, 25, 22, 22, "A", o.leftKey.isPressed(), null);
            key(c, 2, 25, 25, 22, 22, "S", o.backKey.isPressed(), null);
            key(c, 3, 50, 25, 22, 22, "D", o.rightKey.isPressed(), null);
            int y = 50;
            if (mouse.b) {
                key(c, 4, 0, y, 35, 24, "LMB", o.attackKey.isPressed(), cps.b ? lc.size() + " cps" : null);
                key(c, 5, 37, y, 35, 24, "RMB", o.useKey.isPressed(), cps.b ? rc.size() + " cps" : null);
                y += 27;
            }
            if (space.b) { key(c, 6, 0, y, 72, 14, "SPACE", o.jumpKey.isPressed(), null); y += 17; }
            w = 72; h = y;
        }
    }

    // ================================================================= Armor HUD
    static class ArmorHud extends HudMod {
        final Setting style = mode("Показ", 0, "Проценты", "Остаток");
        final Setting hands = bool("Предметы в руках", true);
        ArmorHud() { super("Armor HUD", "Броня и предметы в руках с прочностью", Cat.HUD, 0.40f, 0.82f); }
        @Override protected void render(DrawContext c, float td, boolean pv) {
            if (!U.in()) return;
            var p = U.p();
            List<ItemStack> l = new ArrayList<>();
            for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) l.add(p.getEquippedStack(s));
            if (hands.b) { l.add(p.getMainHandStack()); l.add(p.getOffHandStack()); }
            int ww = l.size() * 26 + 6;
            panel(c, 0, 0, ww, 38, 9);
            for (int i = 0; i < l.size(); i++) {
                ItemStack s = l.get(i); int x = 5 + i * 26;
                D.rrect(c, x, 4, 20, 20, 4, 0x33FFFFFF);
                if (s.isEmpty()) continue;
                c.drawItem(s, x + 2, 6);
                if (s.getCount() > 1) D.text(c, String.valueOf(s.getCount()), x + 14, 16, 0xFFFFFFFF);
                if (!s.isDamageable()) continue;
                int left = s.getMaxDamage() - s.getDamage(), pct = left * 100 / s.getMaxDamage();
                int col = pct > 50 ? 0xFF7CFFB0 : pct > 20 ? 0xFFFFD86B : 0xFFFF6A7A;
                D.rrect(c, x, 26, 20, 2, 1, 0x55000000);
                D.rrect(c, x, 26, Math.max(1, 20 * pct / 100), 2, 1, col);
                String t = style.mode == 0 ? pct + "%" : String.valueOf(left);
                D.text(c, t, x + 10 - D.w(t) / 2, 28, col);
            }
            w = ww; h = 38;
        }
    }

    // ================================================================= Compass
    static class Compass extends HudMod {
        final Setting marks = bool("Метки", true), deg = bool("Градусы", true);
        Compass() { super("Compass", "Компас-лента с направлениями и метками", Cat.HUD, 0.38f, 0.012f); }
        @Override protected void render(DrawContext c, float td, boolean pv) {
            if (!U.in()) return;
            var p = U.p();
            int ww = 240, hh = 24;
            panel(c, 0, 0, ww, hh, 10);
            float yaw = MathHelper.wrapDegrees(MathHelper.lerp(td, p.prevYaw, p.getYaw()));
            String[] dirs = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
            float ppd = 1.6f; // pixels per degree
            for (int a = -180; a < 180; a += 15) {
                float rel = MathHelper.wrapDegrees(a - yaw);
                if (Math.abs(rel) > ww / 2f / ppd - 4) continue;
                int x = (int) (ww / 2f + rel * ppd);
                float fade = 1f - Math.abs(rel) / (ww / 2f / ppd);
                boolean card = a % 45 == 0;
                c.fill(x, card ? 4 : 7, x + 1, card ? 12 : 11, D.a(0xFFFFFFFF, 0.7f * fade));
                if (card) {
                    int idx = ((a + 180) / 45) % 8;
                    String d = dirs[idx];
                    D.text(c, d, x - D.w(d) / 2, 12, d.equals("N") ? D.a(0xFFFF8FA8, fade) : D.a(0xFFFFFFFF, fade));
                }
            }
            if (marks.b) for (Tools.Mark m : Tools.visibleMarks()) {
                double brg = Math.toDegrees(Math.atan2(-(m.x - p.getX()), m.z - p.getZ()));
                float rel = MathHelper.wrapDegrees((float) brg - yaw);
                if (Math.abs(rel) > ww / 2f / ppd - 6) continue;
                int x = (int) (ww / 2f + rel * ppd);
                c.fill(x - 2, 2, x + 3, 7, 0xFFFFFFFF); c.fill(x - 1, 3, x + 2, 6, m.color);
            }
            c.fill(ww / 2 - 1, hh - 5, ww / 2 + 2, hh - 1, ClientMods.accent(0));
            if (deg.b) { String d = (int) ((yaw + 180) % 360) + "\u00b0"; D.text(c, d, ww / 2 - D.w(d) / 2, -11, 0xFFFFFFFF); }
            w = ww; h = hh;
        }
    }

    // ================================================================= Damage silhouette
    static class Silhouette extends HudMod {
        final Setting hold = num("Время показа (сек)", 6, 2, 20, 1);
        final Setting hp = bool("Здоровье", true), armorCol = bool("Прочность брони", true), sway = bool("Покачивание (3D)", true);
        final float[] heat = new float[6]; // head, torso, armL, armR, legL, legR
        float lastDmg; long lastDmgTime;
        Silhouette() { super("Damage Silhouette", "Силуэт цели: подсвечиваются части тела, куда попал удар", Cat.HUD, 0.90f, 0.62f); }

        @Override public void onAttack(Entity e) {
            var mc = U.mc(); var p = mc.player;
            int part = 1;
            if (mc.crosshairTarget instanceof EntityHitResult er && er.getEntity() == e) {
                Vec3d hit = er.getPos();
                double rel = (hit.y - e.getY()) / Math.max(0.1, e.getHeight());
                Vec3d look = p.getRotationVec(1f);
                double lat = (hit.x - e.getX()) * (-look.z) + (hit.z - e.getZ()) * look.x;
                if (rel > 0.78) part = 0;
                else if (rel < 0.40) part = lat < 0 ? 4 : 5;
                else if (Math.abs(lat) > 0.22) part = lat < 0 ? 2 : 3;
                else part = 1;
            }
            heat[part] = 1f;
            if (e instanceof LivingEntity le) lastDmg = le.getHealth();
            lastDmgTime = System.currentTimeMillis();
        }

        int armorColor(LivingEntity t, EquipmentSlot s) {
            ItemStack st = t.getEquippedStack(s);
            if (st.isEmpty()) return 0x00000000;
            if (!st.isDamageable()) return 0xFF7CFFB0;
            int pct = (st.getMaxDamage() - st.getDamage()) * 100 / st.getMaxDamage();
            return pct > 50 ? 0xFF7CFFB0 : pct > 20 ? 0xFFFFD86B : 0xFFFF6A7A;
        }

        @Override protected void render(DrawContext c, float td, boolean pv) {
            if (!U.in()) return;
            LivingEntity t = target(hold.i());
            if (t == null) { if (!pv) { decay(); return; } t = U.p(); }
            decay();
            float time = Sakura.time();
            int off = sway.b ? (int) (Math.sin(time * 1.6) * 2) : 0;
            int ww = 74, hh = 104;
            panel(c, 0, 0, ww, hh, 11);
            int bx = 17 + off, by = 10;
            int[][] parts = {{14, 0, 14, 14}, {10, 15, 22, 28}, {1, 15, 8, 27}, {33, 15, 8, 27}, {10, 44, 10, 32}, {22, 44, 10, 32}};
            for (int i = 0; i < 6; i++) {
                int[] r = parts[i];
                int x = bx + r[0] + (sway.b ? (int) Math.round(Math.sin(time * 1.6 + i) * 0.6) : 0), y = by + r[1];
                // shadow for pseudo-3D depth
                D.rrect(c, x + 2, y + 2, r[2], r[3], 4, D.a(0xFF000000, 0.35f));
                D.rrectV(c, x, y, r[2], r[3], 4, D.a(0xFFFFFFFF, 0.30f), D.a(0xFFB8A8D0, 0.18f));
                if (heat[i] > 0.02f) D.rrectV(c, x, y, r[2], r[3], 4, D.a(0xFFFF4D6A, 0.9f * heat[i]), D.a(0xFFFF8F6A, 0.7f * heat[i]));
                c.fill(x + 2, y + 1, x + r[2] - 2, y + 2, D.a(0xFFFFFFFF, 0.5f)); // highlight edge
            }
            if (armorCol.b) {
                int hc = armorColor(t, EquipmentSlot.HEAD), cc = armorColor(t, EquipmentSlot.CHEST), lc = armorColor(t, EquipmentSlot.LEGS), fc = armorColor(t, EquipmentSlot.FEET);
                if ((hc >>> 24) != 0) c.drawBorder(bx + 13, by - 1, 16, 16, hc);
                if ((cc >>> 24) != 0) c.drawBorder(bx + 9, by + 14, 24, 30, cc);
                if ((lc >>> 24) != 0) c.drawBorder(bx + 9, by + 43, 24, 20, lc);
                if ((fc >>> 24) != 0) c.drawBorder(bx + 9, by + 64, 24, 14, fc);
            }
            if (hp.b) {
                String s = String.format(Locale.ROOT, "%.1f HP", t.getHealth());
                D.text(c, s, ww / 2 - D.w(s) / 2, hh - 16, 0xFFFFFFFF);
            }
            w = ww; h = hh;
        }
        void decay() { for (int i = 0; i < 6; i++) heat[i] = Math.max(0f, heat[i] - 0.012f); }
    }
}
