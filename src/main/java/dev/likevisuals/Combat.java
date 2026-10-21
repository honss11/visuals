package dev.likevisuals;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public final class Combat {
    public static void register() {
        Modules.add(new AutoSwap());
        Modules.add(new TapMouse());
        Modules.add(new AutoGG());
        Modules.add(new PvPSafe());
        Modules.add(new HealingHelper());
        Modules.add(new HitSounds());
        Modules.add(new TotemTracker());
        Modules.add(new FakePlayer());
    }

    public static boolean allowCommand(String cmd) {
        PvPSafe m = Modules.get(PvPSafe.class);
        if (m == null || !m.enabled) return true;
        if (System.currentTimeMillis() - m.last > m.time.i() * 1000L) return true;
        String c = cmd.toLowerCase().split(" ")[0];
        if (PvPSafe.BLOCK.contains(c)) { U.chat("Команда /" + c + " заблокирована в бою (PvP Safe)"); return false; }
        return true;
    }

    // ---------------------------------------------------------------- AutoSwap
    static class AutoSwap extends Mod {
        final Setting pair = mode("Пара предметов", 0, "Тотем / Щит", "Тотем / Золотое яблоко", "Щит / Золотое яблоко", "Тотем / Жемчуг", "Тотем / Меч");
        AutoSwap() { super("AutoSwap", "Меняет предмет в левой руке по бинду (пара выбирается)", Cat.COMBAT); oneShot = true; }
        static boolean is(ItemStack s, int k) {
            switch (k) {
                case 0: return s.isOf(Items.TOTEM_OF_UNDYING);
                case 1: return s.isOf(Items.SHIELD);
                case 2: return s.isOf(Items.GOLDEN_APPLE) || s.isOf(Items.ENCHANTED_GOLDEN_APPLE);
                case 3: return s.isOf(Items.ENDER_PEARL);
                default: return s.isOf(Items.NETHERITE_SWORD) || s.isOf(Items.DIAMOND_SWORD) || s.isOf(Items.IRON_SWORD);
            }
        }
        @Override public void onEnable() {
            if (!U.in() || !U.invOk()) return;
            int[][] pairs = {{0, 1}, {0, 2}, {1, 2}, {0, 3}, {0, 4}};
            int a = pairs[pair.mode][0], b = pairs[pair.mode][1];
            ItemStack off = U.p().getOffHandStack();
            int want = is(off, a) ? b : a;
            int idx = U.find(s -> is(s, want));
            if (idx < 0) { U.bar("AutoSwap: предмет не найден"); return; }
            String nm = U.p().getInventory().getStack(idx).getName().getString();
            U.click(U.slotId(idx), 40, SlotActionType.SWAP);
            HudMods2.info("Auto Swap", "Swapped to " + nm);
        }
    }

    // ---------------------------------------------------------------- TapMouse
    static class TapMouse extends Mod {
        final Setting targets = mode("Цели", 0, "Игроки", "Мобы", "Все");
        final Setting cd = num("Готовность удара", 0.95, 0.6, 1.0, 0.01);
        final Setting crit = bool("Только критические", false);
        TapMouse() { super("Tape Mouse", "Автоматически бьёт цель в прицеле, когда удар готов", Cat.COMBAT); key = org.lwjgl.glfw.GLFW.GLFW_KEY_Q; }
        @Override public void onTick() {
            var mc = U.mc(); var p = U.p();
            if (mc.currentScreen != null || !(mc.crosshairTarget instanceof EntityHitResult er)) return;
            Entity t = er.getEntity();
            if (!(t instanceof LivingEntity) || !t.isAlive() || t == p || Friends.blocks(t)) return;
            boolean pl = t instanceof PlayerEntity;
            if ((targets.mode == 0 && !pl) || (targets.mode == 1 && pl)) return;
            if (p.getAttackCooldownProgress(0.5f) < cd.f()) return;
            if (crit.b && (p.isOnGround() || p.fallDistance <= 0f || p.isClimbing() || p.isTouchingWater())) return;
            mc.interactionManager.attackEntity(p, t);
            p.swingHand(Hand.MAIN_HAND);
        }
    }

    // ---------------------------------------------------------------- AutoGG
    static class AutoGG extends Mod {
        final Setting text = mode("Сообщение", 0, "gg", "!gg", "ez", "gg wp", "GG");
        final Setting delay = num("Кулдаун (сек)", 5, 1, 30, 1);
        Entity tgt; long t0, lastSay;
        AutoGG() { super("Auto GG", "Пишет \"gg\" в чат после убийства игрока", Cat.COMBAT); }
        @Override public void onAttack(Entity e) { if (e instanceof PlayerEntity) { tgt = e; t0 = System.currentTimeMillis(); } }
        @Override public void onTick() {
            if (tgt == null) return;
            long now = System.currentTimeMillis();
            if (now - t0 > 6000) { tgt = null; return; }
            if (tgt instanceof LivingEntity le && (le.isDead() || le.getHealth() <= 0)) {
                if (now - lastSay > delay.i() * 1000L) { U.say(text.modes[text.mode]); lastSay = now; }
                tgt = null;
            }
        }
    }

    // ---------------------------------------------------------------- PvPSafe
    static class PvPSafe extends Mod {
        static final Set<String> BLOCK = new HashSet<>(Arrays.asList("spawn", "home", "hub", "lobby", "tpa", "tpahere", "tp", "back", "warp", "rtp", "sethome", "tpaccept", "tpyes", "wild"));
        final Setting time = num("Блокировка после боя (сек)", 15, 3, 60, 1);
        long last;
        PvPSafe() { super("PvP Safe", "Блокирует рискованные команды (/spawn, /home, /tpa) в бою", Cat.COMBAT); }
        @Override public void onTick() { if (U.p().hurtTime > 0) last = System.currentTimeMillis(); }
        @Override public void onAttack(Entity e) { if (e instanceof PlayerEntity) last = System.currentTimeMillis(); }
    }

    // ---------------------------------------------------------------- HealingHelper
    static class HealingHelper extends Mod {
        final Setting what = mode("Что использовать", 0, "Золотые яблоки", "Любая еда");
        final Setting hp = num("Здоровье ниже", 12, 2, 20, 1);
        final Setting food = num("Голод ниже", 10, 1, 20, 1);
        boolean eating; int prev, ticks, cooldown;
        HealingHelper() { super("Healing Helper", "Сам ест яблоки/еду, когда мало здоровья или голод", Cat.COMBAT); }
        boolean match(ItemStack s) {
            if (s.isEmpty()) return false;
            if (what.mode == 0) return s.isOf(Items.GOLDEN_APPLE) || s.isOf(Items.ENCHANTED_GOLDEN_APPLE);
            return s.get(DataComponentTypes.FOOD) != null;
        }
        @Override public void onDisable() { U.mc().options.useKey.setPressed(false); eating = false; }
        @Override public void onTick() {
            var mc = U.mc(); var p = U.p();
            if (mc.currentScreen != null) return;
            if (eating) {
                ticks++;
                if (!p.isUsingItem() && ticks > 3 || ticks > 50) {
                    mc.options.useKey.setPressed(false); p.getInventory().selectedSlot = prev; eating = false; cooldown = 15;
                } else mc.options.useKey.setPressed(true);
                return;
            }
            if (cooldown > 0) { cooldown--; return; }
            boolean low = p.getHealth() <= hp.f(), hungry = p.getHungerManager().getFoodLevel() <= food.i();
            if (!low && !(hungry && what.mode == 1)) return;
            for (int i = 0; i < 9; i++) {
                if (!match(p.getInventory().getStack(i))) continue;
                prev = p.getInventory().selectedSlot; p.getInventory().selectedSlot = i;
                eating = true; ticks = 0; mc.options.useKey.setPressed(true);
                return;
            }
        }
    }

    // ---------------------------------------------------------------- HitSounds
    static class HitSounds extends Mod {
        static final SoundEvent[] S = {SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundEvents.ENTITY_ARROW_HIT_PLAYER,
            SoundEvents.ENTITY_PLAYER_LEVELUP, SoundEvents.BLOCK_ANVIL_LAND, SoundEvents.UI_BUTTON_CLICK.value()};
        final Setting snd = mode("Звук", 1, "Орб", "Колокольчик", "Стрела", "Левел-ап", "Наковальня", "Клик");
        final Setting pitch = num("Тон", 1.2, 0.5, 2.0, 0.05);
        HitSounds() { super("Hit Sounds", "Играет звук при ударе по цели", Cat.COMBAT); }
        @Override public void onAttack(Entity e) { U.sound(S[snd.mode], pitch.f()); }
    }

    // ---------------------------------------------------------------- TotemTracker
    static class TotemTracker extends Mod {
        final Setting chat = bool("Писать в чат", true);
        final Setting hud = bool("Список на экране", true);
        final Map<String, Integer> pops = new LinkedHashMap<>();
        TotemTracker() { super("Totem Tracker", "Считает сколько тотемов потратил каждый игрок", Cat.COMBAT); }
        @Override public void onTotemPop(Entity e) {
            if (!(e instanceof PlayerEntity pl)) return;
            String n = pl.getName().getString();
            int c = pops.merge(n, 1, Integer::sum);
            if (chat.b) U.chat(n + " потерял тотем (" + c + ")");
        }
        @Override public void onDisable() { pops.clear(); }
        @Override public void onHud(DrawContext ctx, float td) {
            if (!hud.b || pops.isEmpty()) return;
            int x = ctx.getScaledWindowWidth() - 120, y = ctx.getScaledWindowHeight() / 2 + 50;
            D.text(ctx, "Тотемы", x, y, ClientMods.accent(0));
            y += 11;
            for (var en : pops.entrySet()) { D.text(ctx, en.getKey() + ": " + en.getValue(), x, y, 0xFFFFFFFF); y += 10; }
        }
    }

    // ---------------------------------------------------------------- FakePlayer
    static class FakePlayer extends Mod {
        final Setting move = mode("Поведение", 0, "Стоит", "Бегает кругами");
        final Setting armor = bool("Копировать броню", true);
        OtherClientPlayerEntity fake; double ang; Vec3d centre;
        FakePlayer() { super("Fake Player", "Создаёт тренировочного фейкового игрока рядом с тобой", Cat.COMBAT); }
        @Override public void onEnable() {
            var p = U.p(); var w = U.mc().world;
            if (p == null || w == null) return;
            fake = new OtherClientPlayerEntity(w, new GameProfile(UUID.randomUUID(), "Dummy"));
            centre = p.getPos().add(p.getRotationVec(1f).multiply(3, 0, 3));
            fake.refreshPositionAndAngles(centre.x, centre.y, centre.z, p.getYaw() + 180, 0);
            fake.setId(-4242);
            fake.setHealth(20f);
            if (armor.b) for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND})
                fake.equipStack(s, p.getEquippedStack(s).copy());
            w.addEntity(fake);
        }
        boolean dead; long reviveAt;
        @Override public void onAttack(Entity e) {
            if (fake == null || e != fake) return;
            var p = U.p();
            String id = p.getMainHandStack().getItem().toString();
            float dmg = 1f;
            if (id.contains("netherite_sword")) dmg = 8; else if (id.contains("diamond_sword")) dmg = 7; else if (id.contains("iron_sword")) dmg = 6;
            else if (id.contains("stone_sword")) dmg = 5; else if (id.contains("sword")) dmg = 4; else if (id.contains("netherite_axe")) dmg = 10;
            else if (id.contains("axe")) dmg = 9; else if (id.contains("trident")) dmg = 9; else if (id.contains("mace")) dmg = 6;
            float cd = p.getAttackCooldownProgress(0.5f);
            dmg *= 0.2f + cd * cd * 0.8f;
            if (p.fallDistance > 0f && !p.isOnGround()) dmg *= 1.5f;
            fake.hurtTime = 10; fake.maxHurtTime = 10;
            U.sound(SoundEvents.ENTITY_ARROW_HIT_PLAYER, 0.8f);
            float nh = fake.getHealth() - dmg;
            if (nh <= 0.1f) { fake.setHealth(0.1f); dead = true; reviveAt = System.currentTimeMillis() + 1500; U.chat("Fake Player убит! Воскреснет через 1.5 сек"); }
            else fake.setHealth(nh);
        }
        @Override public void onDisable() {
            var w = U.mc().world;
            if (fake != null && w != null) w.removeEntity(fake.getId(), Entity.RemovalReason.DISCARDED);
            fake = null;
        }
        @Override public void onTick() {
            if (dead && System.currentTimeMillis() >= reviveAt && fake != null) { fake.setHealth(20f); dead = false; }
            if (fake == null || move.mode == 0) return;
            ang += 0.08;
            fake.refreshPositionAndAngles(centre.x + Math.cos(ang) * 2.5, centre.y, centre.z + Math.sin(ang) * 2.5, (float) Math.toDegrees(ang) + 180, 0);
        }
    }

}
