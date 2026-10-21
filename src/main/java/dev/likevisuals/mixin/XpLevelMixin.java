package dev.likevisuals.mixin;
import dev.likevisuals.Visuals3;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class XpLevelMixin {
    @Inject(method = "renderExperienceLevel", at = @At("HEAD"), cancellable = true)
    private void lv$lvl(DrawContext c, RenderTickCounter t, CallbackInfo ci) { if (Visuals3.liquidOn() || dev.likevisuals.HudMods2.hotbarBars()) ci.cancel(); }
}
