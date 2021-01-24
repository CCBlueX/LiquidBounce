/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.world;

import java.util.Objects;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.features.module.modules.render.ProphuntESP;
import net.ccbluex.liquidbounce.injection.backend.ChunkImplKt;
import net.ccbluex.liquidbounce.injection.backend.utils.BackendExtentionsKt;
import net.ccbluex.liquidbounce.utils.render.MiniMapRegister;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockPos;
import net.minecraft.world.chunk.Chunk;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Chunk.class)
public class MixinChunk
{
	@Shadow
	@Final
	public int xPosition;

	@Shadow
	@Final
	public int zPosition;

	@Inject(method = "setBlockState", at = @At("HEAD"))
	private void setProphuntBlock(final BlockPos pos, final IBlockState state, final CallbackInfoReturnable callbackInfo)
	{
		// noinspection ConstantConditions
		MiniMapRegister.INSTANCE.updateChunk(ChunkImplKt.wrap((Chunk) (Object) this));

		final ProphuntESP prophuntESP = (ProphuntESP) LiquidBounce.moduleManager.get(ProphuntESP.class);

		if (Objects.requireNonNull(prophuntESP).getState())
			synchronized (prophuntESP.getBlocks()) {
				prophuntESP.getBlocks().put(BackendExtentionsKt.wrap(pos), System.currentTimeMillis());
			}
	}

	@Inject(method = "onChunkUnload", at = @At("HEAD"))
	private void injectFillChunk(final CallbackInfo ci)
	{
		MiniMapRegister.INSTANCE.unloadChunk(xPosition, zPosition);
	}

	@Inject(method = "fillChunk", at = @At("RETURN"))
	private void injectFillChunk(final byte[] p_177439_1_, final int p_177439_2_, final boolean p_177439_3_, final CallbackInfo ci)
	{
		// noinspection ConstantConditions
		MiniMapRegister.INSTANCE.updateChunk(ChunkImplKt.wrap((Chunk) (Object) this));
	}
}
