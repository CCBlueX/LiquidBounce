/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.block;

import java.util.List;
import java.util.Optional;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.BlockBBEvent;
import net.ccbluex.liquidbounce.features.module.modules.combat.Criticals;
import net.ccbluex.liquidbounce.features.module.modules.exploit.GhostHand;
import net.ccbluex.liquidbounce.features.module.modules.player.NoFall;
import net.ccbluex.liquidbounce.features.module.modules.render.XRay;
import net.ccbluex.liquidbounce.features.module.modules.world.NoSlowBreak;
import net.ccbluex.liquidbounce.injection.backend.AxisAlignedBBImplKt;
import net.ccbluex.liquidbounce.injection.backend.BlockImplKt;
import net.ccbluex.liquidbounce.injection.backend.EnumFacingImplKt;
import net.ccbluex.liquidbounce.injection.backend.utils.BackendExtentionsKt;
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

@Mixin(Block.class)
@SideOnly(Side.CLIENT)
public abstract class MixinBlock
{

	@Shadow
	public abstract AxisAlignedBB getCollisionBoundingBox(World worldIn, BlockPos pos, IBlockState state);

	@Shadow
	@Final
	protected BlockState blockState;

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
	@Overwrite
	public void addCollisionBoxesToList(final World worldIn, final BlockPos pos, final IBlockState state, final AxisAlignedBB mask, final List<? super AxisAlignedBB> list, final Entity collidingEntity)
	{
		AxisAlignedBB axisalignedbb = getCollisionBoundingBox(worldIn, pos, state);
		final BlockBBEvent blockBBEvent = new BlockBBEvent(BackendExtentionsKt.wrap(pos), BlockImplKt.wrap(blockState.getBlock()), Optional.ofNullable(axisalignedbb).map(AxisAlignedBBImplKt::wrap).orElse(null));
		LiquidBounce.eventManager.callEvent(blockBBEvent);

		axisalignedbb = blockBBEvent.getBoundingBox() == null ? null : AxisAlignedBBImplKt.unwrap(blockBBEvent.getBoundingBox());

		if (axisalignedbb != null && mask.intersectsWith(axisalignedbb))
			list.add(axisalignedbb);
	}

	@Inject(method = "shouldSideBeRendered", at = @At("HEAD"), cancellable = true)
	private void shouldSideBeRendered(final IBlockAccess worldIn, final BlockPos pos, final EnumFacing side, final CallbackInfoReturnable<? super Boolean> callbackInfoReturnable)
	{
		final XRay xray = (XRay) LiquidBounce.moduleManager.get(XRay.class);

		if (xray.getState())
			// noinspection ConstantConditions
			callbackInfoReturnable.setReturnValue(xray.canBeRendered(BackendExtentionsKt.wrap(pos), BlockImplKt.wrap((Block) (Object) this))); // #298 Bugfix
	}

	@Inject(method = "isCollidable", at = @At("HEAD"), cancellable = true)
	private void isCollidable(final CallbackInfoReturnable<? super Boolean> callbackInfoReturnable)
	{
		final GhostHand ghostHand = (GhostHand) LiquidBounce.moduleManager.get(GhostHand.class);

		// noinspection ConstantConditions
		if (ghostHand.getState() && ghostHand.getBlockValue().get() != Block.getIdFromBlock((Block) (Object) this))
			callbackInfoReturnable.setReturnValue(false);
	}

	@Inject(method = "getAmbientOcclusionLightValue", at = @At("HEAD"), cancellable = true)
	private void getAmbientOcclusionLightValue(final CallbackInfoReturnable<? super Float> floatCallbackInfoReturnable)
	{
		if (LiquidBounce.moduleManager.get(XRay.class).getState())
			floatCallbackInfoReturnable.setReturnValue(1.0F);
	}

	@Inject(method = "getPlayerRelativeBlockHardness", at = @At("RETURN"), cancellable = true)
	public void modifyBreakSpeed(final EntityPlayer playerIn, final World worldIn, final BlockPos pos, final CallbackInfoReturnable<Float> callbackInfo)
	{
		float returnValue = callbackInfo.getReturnValue();

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
		}
		else if (playerIn.onGround)
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
