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
public abstract class MixinBlockAnvil extends MixinBlock {

    @Inject(method = "onBlockPlaced", cancellable = true, at = @At("HEAD"))
    private void injectAnvilCrashFix(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, CallbackInfoReturnable<IBlockState> cir) {
        if (((meta >> 2) & ~0x3) != 0) {
            cir.setReturnValue(super.onBlockPlaced(worldIn, pos, facing, hitX, hitY, hitZ, meta, placer).withProperty(BlockAnvil.FACING, placer.getHorizontalFacing().rotateY()).withProperty(BlockAnvil.DAMAGE, 2));
            cir.cancel();
        }
    }
}
