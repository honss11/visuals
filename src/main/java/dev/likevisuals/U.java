package dev.likevisuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;

import java.util.Random;
import java.util.function.Predicate;

public final class U {
    public static final Random RNG = new Random();
    public static MinecraftClient mc() { return MinecraftClient.getInstance(); }
    public static ClientPlayerEntity p() { return mc().player; }
    public static boolean in() { MinecraftClient m = mc(); return m.player != null && m.world != null; }

    public static void chat(String s) { if (mc().player != null) mc().player.sendMessage(Text.literal("§d[LV] §f" + s), false); }
    public static void bar(String s) { if (mc().inGameHud != null) mc().inGameHud.setOverlayMessage(Text.literal(s), false); }
    public static void cmd(String c) { if (mc().player != null) mc().player.networkHandler.sendChatCommand(c); }
    public static void say(String s) { if (mc().player != null) mc().player.networkHandler.sendChatMessage(s); }

    public static int slotId(int idx) { return idx < 9 ? 36 + idx : idx; }
    public static int find(Predicate<ItemStack> pr) {
        var inv = mc().player.getInventory();
        for (int i = 0; i < 36; i++) if (pr.test(inv.getStack(i))) return i;
        return -1;
    }
    public static boolean invOk() { var s = mc().currentScreen; return s == null || s instanceof InventoryScreen; }
    public static void click(int slot, int btn, SlotActionType t) {
        mc().interactionManager.clickSlot(mc().player.playerScreenHandler.syncId, slot, btn, t, mc().player);
    }
    public static void particle(ParticleEffect e, double x, double y, double z, double vx, double vy, double vz) {
        mc().particleManager.addParticle(e, x, y, z, vx, vy, vz);
    }
    public static void sound(SoundEvent s, float pitch) { mc().getSoundManager().play(PositionedSoundInstance.master(s, pitch)); }
    public static double rnd(double range) { return (RNG.nextDouble() * 2 - 1) * range; }
    public static int argb(int rgb, float a) { return ((int) (Math.max(0, Math.min(1, a)) * 255) << 24) | (rgb & 0xFFFFFF); }
    public static int lerpColor(int c1, int c2, float t) {
        t = Math.max(0, Math.min(1, t));
        int a = (int) (((c1 >>> 24) & 255) * (1 - t) + ((c2 >>> 24) & 255) * t);
        int r = (int) (((c1 >> 16) & 255) * (1 - t) + ((c2 >> 16) & 255) * t);
        int g = (int) (((c1 >> 8) & 255) * (1 - t) + ((c2 >> 8) & 255) * t);
        int b = (int) ((c1 & 255) * (1 - t) + (c2 & 255) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
