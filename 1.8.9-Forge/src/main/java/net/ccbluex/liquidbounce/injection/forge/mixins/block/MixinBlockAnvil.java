package net.ccbluex.liquidbounce.injection.forge.mixins.block;

import net.minecraft.block.BlockAnvil;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockAnvil.class)
public abstract class MixinBlockAnvil extends MixinBlock
{
	@Inject(method = "onBlockPlaced", cancellable = true, at = @At("HEAD"))
	private void injectCrashAnvilFix(final World worldIn, final BlockPos pos, final EnumFacing facing, final float hitX, final float hitY, final float hitZ, final int meta, final EntityLivingBase placer, final CallbackInfoReturnable<? super IBlockState> cir)
	{
		// Make anvil crash exploit not work for me
		if ((meta >> 2 & ~0x3) != 0)
		{
			// noinspection UnnecessarySuperQualifier - super qualifier is very important on this mixin context
			cir.setReturnValue(super.onBlockPlaced(worldIn, pos, facing, hitX, hitY, hitZ, meta, placer).withProperty(BlockAnvil.FACING, placer.getHorizontalFacing().rotateY()).withProperty(BlockAnvil.DAMAGE, 2));
			cir.cancel();
		}
	}
}
