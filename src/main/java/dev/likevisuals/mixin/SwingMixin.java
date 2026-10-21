package dev.likevisuals.mixin;
import dev.likevisuals.Visuals;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public class SwingMixin {
    @Inject(method = "applySwingOffset", at = @At("HEAD"), cancellable = true)
    private void lv$swing(MatrixStack m, Arm arm, float f, CallbackInfo ci) { if (Visuals.applySwing(m, arm, f)) ci.cancel(); }
}
