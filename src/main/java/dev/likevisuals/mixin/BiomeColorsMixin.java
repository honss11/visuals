package dev.likevisuals.mixin;
import dev.likevisuals.Visuals3;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BiomeColors.class)
public class BiomeColorsMixin {
    @Inject(method = "getGrassColor", at = @At("RETURN"), cancellable = true)
    private static void lv$grass(BlockRenderView w, BlockPos p, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(Visuals3.grass(cir.getReturnValue(), p.getX(), p.getZ()));
    }
    @Inject(method = "getFoliageColor", at = @At("RETURN"), cancellable = true)
    private static void lv$foliage(BlockRenderView w, BlockPos p, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(Visuals3.foliage(cir.getReturnValue(), p.getX(), p.getZ()));
    }
}
