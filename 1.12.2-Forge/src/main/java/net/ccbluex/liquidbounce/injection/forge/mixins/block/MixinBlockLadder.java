/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.block;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.features.module.modules.movement.FastClimb;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Objects;

@Mixin(BlockLadder.class)
@SideOnly(Side.CLIENT)
public abstract class MixinBlockLadder extends MixinBlock {

    @Shadow
    @Final
    public static PropertyDirection FACING;

    @Shadow
    @Final
    protected static AxisAlignedBB LADDER_NORTH_AABB;

    @Shadow
    @Final
    protected static AxisAlignedBB LADDER_SOUTH_AABB;

    @Shadow
    @Final
    protected static AxisAlignedBB LADDER_WEST_AABB;

    @Shadow
    @Final
    protected static AxisAlignedBB LADDER_EAST_AABB;

    /**
     * @author CCBlueX
     */
    @Overwrite
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        if (state.getBlock() instanceof BlockLadder) {
            final FastClimb fastClimb = (FastClimb) LiquidBounce.moduleManager.getModule(FastClimb.class);
            boolean fastLadder = (Objects.requireNonNull(fastClimb).getState() && fastClimb.getModeValue().get().equalsIgnoreCase("AAC3.0.0"));
            final float f = 0.99f;

            if (fastLadder) {
                switch (state.getValue(FACING)) {
                    case NORTH:
                        return new AxisAlignedBB(0.0F, 0.0F, 1.0F - f, 1.0F, 1.0F, 1.0F);
                    case SOUTH:
                        return new AxisAlignedBB(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, f);
                    case WEST:
                        return new AxisAlignedBB(1.0F - f, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
                    case EAST:
                    default:
                        return new AxisAlignedBB(0.0F, 0.0F, 0.0F, f, 1.0F, 1.0F);
                }
            }
        }

        // Default to the default implementation
        switch (state.getValue(FACING)) {
            case NORTH:
                return LADDER_NORTH_AABB;
            case SOUTH:
                return LADDER_SOUTH_AABB;
            case WEST:
                return LADDER_WEST_AABB;
            case EAST:
            default:
                return LADDER_EAST_AABB;
        }
    }
}
