package dev.likevisuals.mixin;
import dev.likevisuals.Visuals;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ClientWorld.Properties.class)
public class ClientWorldPropertiesMixin {
    @ModifyVariable(method = "setTimeOfDay", at = @At("HEAD"), argsOnly = true)
    private long lv$time(long t) { long f = Visuals.forcedTime(); return f >= 0 ? f : t; }
}
