package dev.likevisuals.mixin;
import dev.likevisuals.Visuals3;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class StatusBarsMixin {
    @Inject(method = "renderStatusBars", at = @At("HEAD"), cancellable = true)
    private void lv$bars(DrawContext c, CallbackInfo ci) { if (Visuals3.liquidOn() || dev.likevisuals.HudMods2.hotbarBars()) ci.cancel(); }
}
