package dev.likevisuals.mixin;
import dev.likevisuals.Visuals;
import net.minecraft.client.render.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "getBasicProjectionMatrix", at = @At("RETURN"))
    private void lv$aspect(float fov, CallbackInfoReturnable<Matrix4f> cir) { Visuals.applyAspect(cir.getReturnValue()); }
}
