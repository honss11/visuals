package dev.likevisuals.mixin;
import dev.likevisuals.PlayerMods;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Camera.class)
public class CameraMixin {
    @ModifyVariable(method = "setRotation", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float lv$yaw(float y) { return PlayerMods.flActive ? PlayerMods.flYaw : y; }
    @ModifyVariable(method = "setRotation", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private float lv$pitch(float p) { return PlayerMods.flActive ? PlayerMods.flPitch : p; }
}
