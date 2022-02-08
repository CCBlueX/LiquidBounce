package net.ccbluex.liquidbounce.injection.mixins.minecraft.render;

import me.jellysquid.mods.sodium.client.render.occlusion.BlockOcclusionCache;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleXRay;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(BlockOcclusionCache.class)
public class MixinSodiumBlockOcclusionCache {

    @Inject(method = "shouldDrawSide", at = @At("RETURN"), cancellable = true)
    private void injectXRay(BlockState selfState, BlockView view, BlockPos pos, Direction facing, CallbackInfoReturnable<Boolean> cir) {
        ModuleXRay module = ModuleXRay.INSTANCE;
        if (!module.getEnabled()) {
            return;
        }

        Set<Block> blocks = module.getBlocks();
        cir.setReturnValue(blocks.contains(selfState.getBlock()));
        cir.cancel();
    }
}
