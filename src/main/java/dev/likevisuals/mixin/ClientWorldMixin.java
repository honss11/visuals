package dev.likevisuals.mixin;
import dev.likevisuals.Visuals;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientWorld.class)
public class ClientWorldMixin {
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
    private void lv$sky(CallbackInfoReturnable cir) {
        Integer rgb = Visuals.skyRgb();
        if (rgb == null) return;
        Object v = cir.getReturnValue();
        if (v instanceof Integer) cir.setReturnValue(0xFF000000 | rgb);
        else if (v instanceof Vec3d) cir.setReturnValue(new Vec3d(((rgb >> 16) & 255) / 255.0, ((rgb >> 8) & 255) / 255.0, (rgb & 255) / 255.0));
    }
}
