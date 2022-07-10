/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.block;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.BlockBBEvent;
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
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
@SideOnly(Side.CLIENT)
public abstract class MixinBlock
{
    @Shadow
    @Final
    protected BlockState blockState;

    @Shadow
    public abstract AxisAlignedBB getCollisionBoundingBox(World worldIn, BlockPos pos, IBlockState state);

    @Shadow
    public abstract void setBlockBounds(float minX, float minY, float minZ, float maxX, float maxY, float maxZ);

    // Has to be implemented since a non-virtual call on an abstract method is illegal
    @Shadow
    public IBlockState onBlockPlaced(final World worldIn, final BlockPos pos, final EnumFacing facing, final float hitX, final float hitY, final float hitZ, final int meta, final EntityLivingBase placer)
    {
        return null;
    }

    /**
     * @author CCBlueX
     * @reason BlockBBEvent
     */
    @Redirect(method = "addCollisionBoxesToList", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;getCollisionBoundingBox(Lnet/minecraft/world/World;Lnet/minecraft/util/BlockPos;Lnet/minecraft/block/state/IBlockState;)Lnet/minecraft/util/AxisAlignedBB;"))
    public AxisAlignedBB handleBlockBBEvent(final Block instance, final World worldIn, final BlockPos pos, final IBlockState state)
    {
        final BlockBBEvent blockBBEvent = new BlockBBEvent(pos, blockState.getBlock(), getCollisionBoundingBox(worldIn, pos, state));
        LiquidBounce.eventManager.callEvent(blockBBEvent);

        return blockBBEvent.getBoundingBox();
    }

    @Inject(method = "shouldSideBeRendered", at = @At("HEAD"), cancellable = true)
    private void injectXRay(final IBlockAccess worldIn, final BlockPos pos, final EnumFacing side, final CallbackInfoReturnable<? super Boolean> callbackInfoReturnable)
    {
        final XRay xray = (XRay) LiquidBounce.moduleManager.get(XRay.class);

        if (xray.getState())
            // noinspection ConstantConditions
            callbackInfoReturnable.setReturnValue(xray.canBeRendered((Block) (Object) this, pos)); // #298 Bugfix
    }

    @Inject(method = "isCollidable", at = @At("HEAD"), cancellable = true)
    private void injectGhostHand(final CallbackInfoReturnable<? super Boolean> callbackInfoReturnable)
    {
        final GhostHand ghostHand = (GhostHand) LiquidBounce.moduleManager.get(GhostHand.class);

        // noinspection ConstantConditions
        if (ghostHand.getState() && ghostHand.getBlockValue().get() != Block.getIdFromBlock((Block) (Object) this))
            callbackInfoReturnable.setReturnValue(false);
    }

    @Inject(method = "getAmbientOcclusionLightValue", at = @At("HEAD"), cancellable = true)
    private void injectXRay(final CallbackInfoReturnable<? super Float> floatCallbackInfoReturnable)
    {
        if (LiquidBounce.moduleManager.get(XRay.class).getState())
            floatCallbackInfoReturnable.setReturnValue(1.0F);
    }

    @Inject(method = "getPlayerRelativeBlockHardness", at = @At("RETURN"), cancellable = true)
    public void modifyBreakSpeed(final EntityPlayer playerIn, final World worldIn, final BlockPos pos, final CallbackInfoReturnable<? super Float> callbackInfo)
    {
        float returnValue = callbackInfo.getReturnValueF();

        // NoSlowBreak
        final NoSlowBreak noSlowBreak = (NoSlowBreak) LiquidBounce.moduleManager.get(NoSlowBreak.class);
        if (noSlowBreak.getState())
        {
            // Water
            if (noSlowBreak.getWaterValue().get() && playerIn.isInsideOfMaterial(Material.water) && !EnchantmentHelper.getAquaAffinityModifier(playerIn))
                returnValue *= 5.0F;

            // Air
            if (noSlowBreak.getAirValue().get() && !playerIn.onGround)
                returnValue *= 5.0F;
        } else if (playerIn.onGround)
        {
            // NoFall, Criticals NoGround mode NoSlowBreak
            final NoFall noFall = (NoFall) LiquidBounce.moduleManager.get(NoFall.class);
            final Criticals criticals = (Criticals) LiquidBounce.moduleManager.get(Criticals.class);

            if (noFall.getState() && "NoGround".equalsIgnoreCase(noFall.modeValue.get()) || criticals.getState() && "NoGround".equalsIgnoreCase(criticals.getModeValue().get()))
                returnValue /= 5.0F;
        }

        callbackInfo.setReturnValue(returnValue);
    }
}
