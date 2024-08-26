/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.fabric.mixins.world;

import net.ccbluex.liquidbounce.features.module.modules.render.ProphuntESP;
import net.ccbluex.liquidbounce.utils.render.MiniMapRegister;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Chunk.class)
public class MixinChunk {
    @Shadow
    @Final
    public int xPosition;

    @Shadow
    @Final
    public int zPosition;

    @Inject(method = "setBlockState", at = @At("HEAD"))
    private void setProphuntBlock(BlockPos pos, BlockState state, final CallbackInfoReturnable callbackInfo) {
        //noinspection ConstantConditions
        MiniMapRegister.INSTANCE.updateChunk((Chunk) ((Object) this));

        final ProphuntESP prophuntESP = ProphuntESP.INSTANCE;

        if (prophuntESP.handleEvents()) {
            synchronized (prophuntESP.getBlocks()) {
                prophuntESP.getBlocks().put(pos, System.currentTimeMillis());
            }
        }
    }

    @Inject(method = "onChunkUnload", at = @At("HEAD"))
    private void injectFillChunk(CallbackInfo ci) {
        MiniMapRegister.INSTANCE.unloadChunk(xPosition, zPosition);
    }

    @Inject(method = "fillChunk", at = @At("RETURN"))
    private void injectFillChunk(byte[] p_177439_1_, int p_177439_2_, boolean p_177439_3_, CallbackInfo ci) {
        //noinspection ConstantConditions
        MiniMapRegister.INSTANCE.updateChunk((Chunk) ((Object) this));
    }
}
