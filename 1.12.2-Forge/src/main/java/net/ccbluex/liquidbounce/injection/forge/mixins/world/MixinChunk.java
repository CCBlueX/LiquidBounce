/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.world;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.features.module.modules.render.ProphuntESP;
import net.ccbluex.liquidbounce.injection.backend.ChunkImplKt;
import net.ccbluex.liquidbounce.injection.backend.utils.BackendExtentionsKt;
import net.ccbluex.liquidbounce.utils.render.MiniMapRegister;
import net.minecraft.block.state.IBlockState;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(Chunk.class)
public class MixinChunk {

    @Shadow
    @Final
    public int x;

    @Shadow
    @Final
    public int z;

    @Inject(method = "setBlockState", at = @At("HEAD"))
    private void setProphuntBlock(BlockPos pos, IBlockState state, final CallbackInfoReturnable callbackInfo) {
        //noinspection ConstantConditions
        MiniMapRegister.INSTANCE.updateChunk(ChunkImplKt.wrap((Chunk) ((Object) this)));

        final ProphuntESP prophuntESP = (ProphuntESP) LiquidBounce.moduleManager.getModule(ProphuntESP.class);

        if (Objects.requireNonNull(prophuntESP).getState()) {
            synchronized (prophuntESP.getBlocks()) {
                prophuntESP.getBlocks().put(BackendExtentionsKt.wrap(pos), System.currentTimeMillis());
            }
        }
    }

    @Inject(method = "onUnload", at = @At("HEAD"))
    private void injectFillChunk(CallbackInfo ci) {
        MiniMapRegister.INSTANCE.unloadChunk(this.x, this.z);
    }

    @Inject(method = "read", at = @At("RETURN"))
    private void injectFillChunk(PacketBuffer buf, int availableSections, boolean groundUpContinuous, CallbackInfo ci) {
        //noinspection ConstantConditions
        MiniMapRegister.INSTANCE.updateChunk(ChunkImplKt.wrap((Chunk) ((Object) this)));
    }
}
