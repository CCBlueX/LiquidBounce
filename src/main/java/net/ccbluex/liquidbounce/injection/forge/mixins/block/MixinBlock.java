/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.block;

import net.ccbluex.liquidbounce.event.BlockBBEvent;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.features.module.modules.combat.Criticals;
import net.ccbluex.liquidbounce.features.module.modules.exploit.GhostHand;
import net.ccbluex.liquidbounce.features.module.modules.player.NoFall;
import net.ccbluex.liquidbounce.features.module.modules.render.XRay;
import net.ccbluex.liquidbounce.features.module.modules.world.NoSlowBreak;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(Block.class)
@SideOnly(Side.CLIENT)
public abstract class MixinBlock {

    @Shadow
    @Final
    protected BlockState blockState;

    @Shadow
    public abstract AxisAlignedBB getCollisionBoundingBox(World worldIn, BlockPos pos, IBlockState state);

    @Shadow
    public abstract void setBlockBounds(float minX, float minY, float minZ, float maxX, float maxY, float maxZ);

    // Has to be implemented since a non-virtual call on an abstract method is illegal
    @Shadow
    public IBlockState onBlockPlaced(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
        return null;
    }

    /**
     * @author CCBlueX
     */
    @Overwrite
    public void addCollisionBoxesToList(World worldIn, BlockPos pos, IBlockState state, AxisAlignedBB mask, List<AxisAlignedBB> list, Entity collidingEntity) {
        AxisAlignedBB axisalignedbb = getCollisionBoundingBox(worldIn, pos, state);
        BlockBBEvent blockBBEvent = new BlockBBEvent(pos, blockState.getBlock(), axisalignedbb);
        EventManager.INSTANCE.callEvent(blockBBEvent);

        axisalignedbb = blockBBEvent.getBoundingBox();

        if (axisalignedbb != null && mask.intersectsWith(axisalignedbb)) list.add(axisalignedbb);
    }

    @Inject(method = "shouldSideBeRendered", at = @At("HEAD"), cancellable = true)
    private void shouldSideBeRendered(IBlockAccess p_shouldSideBeRendered_1_, BlockPos p_shouldSideBeRendered_2_, EnumFacing p_shouldSideBeRendered_3_, CallbackInfoReturnable<Boolean> cir) {
        if (XRay.INSTANCE.handleEvents()) {
            cir.setReturnValue(XRay.INSTANCE.getXrayBlocks().contains((Block) (Object) this));
        }
    }

    @Inject(method = "isCollidable", at = @At("HEAD"), cancellable = true)
    private void isCollidable(CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        final GhostHand ghostHand = GhostHand.INSTANCE;

        if (ghostHand.handleEvents() && !(ghostHand.getBlock() == Block.getIdFromBlock((Block) (Object) this))) {
            callbackInfoReturnable.setReturnValue(false);
        }
    }

    @Inject(method = "getAmbientOcclusionLightValue", at = @At("HEAD"), cancellable = true)
    private void getAmbientOcclusionLightValue(CallbackInfoReturnable<Float> cir) {
        if (XRay.INSTANCE.handleEvents()) {
            cir.setReturnValue(1F);
        }
    }

    @Inject(method = "getPlayerRelativeBlockHardness", at = @At("RETURN"), cancellable = true)
    public void modifyBreakSpeed(EntityPlayer playerIn, World worldIn, BlockPos pos, final CallbackInfoReturnable<Float> callbackInfo) {
        float f = callbackInfo.getReturnValue();

        // NoSlowBreak
        final NoSlowBreak noSlowBreak = NoSlowBreak.INSTANCE;
        if (noSlowBreak.handleEvents()) {
            if (noSlowBreak.getWater() && playerIn.isInsideOfMaterial(Material.water) && !EnchantmentHelper.getAquaAffinityModifier(playerIn)) {
                f *= 5f;
            }

            if (noSlowBreak.getAir() && !playerIn.onGround) {
                f *= 5f;
            }
        } else if (playerIn.onGround) { // NoGround
            final NoFall noFall = NoFall.INSTANCE;
            final Criticals criticals = Criticals.INSTANCE;

            if (noFall.handleEvents() && noFall.getMode().equals("NoGround") || criticals.handleEvents() && criticals.getMode().equals("NoGround")) {
                f /= 5F;
            }
        }

        callbackInfo.setReturnValue(f);
    }
}