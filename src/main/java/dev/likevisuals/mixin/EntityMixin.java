package dev.likevisuals.mixin;
import dev.likevisuals.PlayerMods;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    private void lv$look(double dx, double dy, CallbackInfo ci) {
        if (PlayerMods.flActive && (Object) this == MinecraftClient.getInstance().player) {
            PlayerMods.flYaw += (float) dx * 0.15f;
            PlayerMods.flPitch = Math.max(-90f, Math.min(90f, PlayerMods.flPitch + (float) dy * 0.15f));
            ci.cancel();
        }
    }
}
