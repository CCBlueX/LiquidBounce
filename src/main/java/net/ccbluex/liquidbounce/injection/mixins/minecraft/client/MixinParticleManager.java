package net.ccbluex.liquidbounce.injection.mixins.minecraft.client;

import net.ccbluex.liquidbounce.features.module.modules.render.DoRender;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleAntiBlind;
import net.minecraft.block.BlockState;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleManager.class)
public class MixinParticleManager {
    @Inject(method = "addBlockBreakingParticles", at = @At("HEAD"), cancellable = true)
    private void hookAddBlockBreakingParticles(BlockPos pos, Direction direction, CallbackInfo ci) {
        if (!ModuleAntiBlind.canRender(DoRender.BLOCK_BREAK_PARTICLES)) {
            ci.cancel();
        }
    }

    @Inject(method = "addBlockBreakParticles", at = @At("HEAD"), cancellable = true)
    private void hookAddBlockBreakParticles(BlockPos pos, BlockState state, CallbackInfo ci) {
        if (!ModuleAntiBlind.canRender(DoRender.BLOCK_BREAK_PARTICLES)) {
            ci.cancel();
        }
    }
}
