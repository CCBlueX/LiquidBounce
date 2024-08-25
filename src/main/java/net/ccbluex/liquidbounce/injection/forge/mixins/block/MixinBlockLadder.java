/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.block;

import net.ccbluex.liquidbounce.features.module.modules.movement.FastClimb;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BlockLadder.class)
@SideOnly(Side.CLIENT)
public abstract class MixinBlockLadder extends MixinBlock {

    @Shadow
    @Final
    public static PropertyDirection FACING;

    /**
     * @author CCBlueX
     */
    @Overwrite
    public void setBlockBoundsBasedOnState(IBlockAccess worldIn, BlockPos pos) {
        final IBlockState blockState = worldIn.getBlockState(pos);

        if (blockState.getBlock() instanceof BlockLadder) {
            final FastClimb fastClimb = FastClimb.INSTANCE;
            final float f = fastClimb.handleEvents() && fastClimb.getMode().equals("AAC3.0.0") ? 0.99f : 0.125f;

            switch (blockState.getValue(FACING)) {
                case NORTH:
                    setBlockBounds(0f, 0f, 1f - f, 1f, 1f, 1f);
                    break;
                case SOUTH:
                    setBlockBounds(0f, 0f, 0f, 1f, 1f, f);
                    break;
                case WEST:
                    setBlockBounds(1f - f, 0f, 0f, 1f, 1f, 1f);
                    break;
                case EAST:
                default:
                    setBlockBounds(0f, 0f, 0f, f, 1f, 1f);
            }
        }
    }
}
