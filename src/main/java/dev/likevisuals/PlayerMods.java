package dev.likevisuals;

import dev.likevisuals.mixin.HandledScreenAccessor;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Arm;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public final class PlayerMods {
    public static boolean flActive;
    public static float flYaw, flPitch;

    public static void register() {
        Modules.add(new AutoSprint()); Modules.add(new FreeLook()); Modules.add(new AutoRespawn()); Modules.add(new DeathCoords());
        Modules.add(new ItemSaving()); Modules.add(new ItemScroller()); Modules.add(new ChangeHand()); Modules.add(new ElytraSwap());
        Modules.add(new ItemPickupLogger());
    }

    static class AutoSprint extends Mod {
        final Setting style = mode("Режим", 0, "Только вперёд", "Всегда при движении");
        final Setting hunger = bool("Не бежать при голоде", true);
        boolean set;
        AutoSprint() { super("Auto Sprint", "Автоматически удерживает кнопку спринта", Cat.PLAYER); }
        @Override public void onTick() {
            var p = U.p(); var o = U.mc().options;
            boolean move = style.mode == 0 ? p.input.movementForward > 0 : (p.input.movementForward != 0 || p.input.movementSideways != 0);
            boolean can = move && !p.isSneaking() && !p.isUsingItem() && !(hunger.b && p.getHungerManager().getFoodLevel() <= 6);
            if (can) { o.sprintKey.setPressed(true); set = true; } else if (set) { o.sprintKey.setPressed(false); set = false; }
        }
        @Override public void onDisable() { if (set) U.mc().options.sprintKey.setPressed(false); set = false; }
    }

    static class FreeLook extends Mod {
        final Setting tp = mode("Камера", 0, "Третье лицо", "Не менять");
        Perspective prev;
        FreeLook() { super("Free Look", "Осмотр в третьем лице без поворота персонажа", Cat.PLAYER); key = GLFW.GLFW_KEY_V; bindMode.mode = 1; }
        @Override public void onEnable() {
            var p = U.p(); if (p == null) return;
            flYaw = p.getYaw(); flPitch = p.getPitch(); flActive = true;
            prev = U.mc().options.getPerspective();
            if (tp.mode == 0) U.mc().options.setPerspective(Perspective.THIRD_PERSON_BACK);
        }
        @Override public void onDisable() {
            flActive = false;
            if (prev != null) U.mc().options.setPerspective(prev);
        }
    }

    static class AutoRespawn extends Mod {
        final Setting delay = num("Задержка (тики)", 10, 0, 60, 1);
        int w;
        AutoRespawn() { super("Auto Respawn", "Автоматически возрождает игрока после смерти", Cat.PLAYER); }
        @Override public void onTick() {
            if (U.mc().currentScreen instanceof DeathScreen && U.p().isDead()) { if (++w >= delay.i()) { U.p().requestRespawn(); w = 0; } } else w = 0;
        }
    }

    static class DeathCoords extends Mod {
        final Setting copy = bool("Копировать в буфер", false), mark = bool("Создать метку смерти", true);
        boolean reported;
        DeathCoords() { super("Death Coordinates", "Показывает твои координаты в момент смерти", Cat.PLAYER); }
        @Override public void onTick() {
            var p = U.p();
            if (p.isDead() && !reported) {
                reported = true;
                String s = (int) p.getX() + " " + (int) p.getY() + " " + (int) p.getZ();
                U.chat("Ты умер на: " + s);
                if (mark.b) Tools.addMark("Death", p.getX(), p.getY(), p.getZ());
                if (copy.b) U.mc().keyboard.setClipboard(s);
            } else if (!p.isDead()) reported = false;
        }
    }

    static class ItemSaving extends Mod {
        final Setting thr = num("Порог прочности (%)", 5, 1, 30, 1);
        final Setting act = mode("Действие", 0, "Убрать из руки", "Только предупредить");
        long warn;
        ItemSaving() { super("Item Saving", "Убирает инструмент при низкой прочности и предупреждает", Cat.PLAYER); }
        @Override public void onTick() {
            var p = U.p(); ItemStack s = p.getMainHandStack();
            if (!s.isDamageable()) return;
            int left = s.getMaxDamage() - s.getDamage();
            if (left * 100f / s.getMaxDamage() > thr.f()) return;
            if (System.currentTimeMillis() - warn > 3000) { U.bar("\u00a7cПредмет скоро сломается: " + left + " ед."); warn = System.currentTimeMillis(); }
            if (act.mode == 1) return;
            for (int i = 0; i < 9; i++) { ItemStack o = p.getInventory().getStack(i); if (!o.isDamageable() || o.isEmpty()) { p.getInventory().selectedSlot = i; return; } }
        }
    }

    static class ItemScroller extends Mod {
        final Setting style = mode("Режим", 2, "Shift: перенос", "Ctrl: выброс", "Оба");
        Slot last;
        ItemScroller() { super("Item Scroller", "Быстрый перенос и выброс предметов зажатием мыши", Cat.PLAYER); }
        @Override public void onTick() {
            var mc = U.mc();
            if (!(mc.currentScreen instanceof HandledScreen<?> hs)) { last = null; return; }
            long h = mc.getWindow().getHandle();
            if (GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS) { last = null; return; }
            Slot s = ((HandledScreenAccessor) hs).getFocusedSlot();
            if (s == null || s == last || !s.hasStack()) return;
            boolean shift = InputUtil.isKeyPressed(h, GLFW.GLFW_KEY_LEFT_SHIFT) || InputUtil.isKeyPressed(h, GLFW.GLFW_KEY_RIGHT_SHIFT);
            boolean ctrl = InputUtil.isKeyPressed(h, GLFW.GLFW_KEY_LEFT_CONTROL) || InputUtil.isKeyPressed(h, GLFW.GLFW_KEY_RIGHT_CONTROL);
            if (shift && style.mode != 1) { last = s; mc.interactionManager.clickSlot(hs.getScreenHandler().syncId, s.id, 0, SlotActionType.QUICK_MOVE, mc.player); }
            else if (ctrl && style.mode != 0) { last = s; mc.interactionManager.clickSlot(hs.getScreenHandler().syncId, s.id, 1, SlotActionType.THROW, mc.player); }
        }
    }

    static class ChangeHand extends Mod {
        ChangeHand() { super("Change Hand", "Меняет ведущую руку (правая/левая)", Cat.PLAYER); oneShot = true; }
        @Override public void onEnable() {
            var o = U.mc().options.getMainArm();
            o.setValue(o.getValue() == Arm.RIGHT ? Arm.LEFT : Arm.RIGHT);
            U.mc().options.sendClientSettings();
        }
    }

    static class ElytraSwap extends Mod {
        static final Set<Item> CHEST = Set.of(Items.NETHERITE_CHESTPLATE, Items.DIAMOND_CHESTPLATE, Items.IRON_CHESTPLATE, Items.GOLDEN_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE, Items.LEATHER_CHESTPLATE);
        ElytraSwap() { super("Elytra Swap", "Меняет нагрудник на элитры и обратно по бинду", Cat.PLAYER); oneShot = true; }
        @Override public void onEnable() {
            if (!U.in() || !U.invOk()) return;
            boolean wearing = U.p().getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);
            int idx = wearing ? U.find(s -> CHEST.contains(s.getItem())) : U.find(s -> s.isOf(Items.ELYTRA));
            if (idx < 0) { U.bar("Elytra Swap: предмет не найден"); return; }
            int id = U.slotId(idx);
            U.click(id, 0, SlotActionType.PICKUP); U.click(6, 0, SlotActionType.PICKUP); U.click(id, 0, SlotActionType.PICKUP);
        }
    }

    static class ItemPickupLogger extends Mod {
        final Setting where = mode("Куда выводить", 1, "Чат", "Над хотбаром", "Оба");
        final Map<Item, Integer> prev = new HashMap<>();
        int t;
        ItemPickupLogger() { super("Item Pickup Logger", "Показывает подобранные предметы в чате и над хотбаром", Cat.PLAYER); }
        @Override public void onEnable() { prev.clear(); t = 0; }
        @Override public void onTick() {
            if (++t % 4 != 0) return;
            var inv = U.p().getInventory(); Map<Item, Integer> now = new HashMap<>();
            for (int i = 0; i < inv.size(); i++) { ItemStack s = inv.getStack(i); if (!s.isEmpty()) now.merge(s.getItem(), s.getCount(), Integer::sum); }
            if (!prev.isEmpty()) for (var e : now.entrySet()) {
                int d = e.getValue() - prev.getOrDefault(e.getKey(), 0);
                if (d > 0) { String m = "+" + d + " " + e.getKey().getName().getString(); if (where.mode != 1) U.chat(m); if (where.mode != 0) U.bar("\u00a7a" + m); }
            }
            prev.clear(); prev.putAll(now);
        }
    }
}
