/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.entity;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.JumpEvent;
import net.ccbluex.liquidbounce.event.StrafeEvent;
import net.ccbluex.liquidbounce.features.module.modules.movement.AirJump;
import net.ccbluex.liquidbounce.features.module.modules.movement.LiquidWalk;
import net.ccbluex.liquidbounce.features.module.modules.movement.NoJumpDelay;
import net.ccbluex.liquidbounce.features.module.modules.render.AntiBlind;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.ForgeHooks;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBase extends MixinEntity
{
	@Shadow
	public int activeItemStackUseCount;
	@Shadow
	protected boolean isJumping;
	@Shadow
	private int jumpTicks;

	@Shadow
	public float cameraPitch;

	@Shadow
	public abstract boolean isHandActive();

	@Shadow
	public abstract ItemStack getActiveItemStack();

	@Shadow
	protected abstract float getJumpUpwardsMotion();

	@Shadow
	public abstract PotionEffect getActivePotionEffect(Potion potionIn);

	@Shadow
	public abstract boolean isPotionActive(Potion potionIn);

	@Shadow
	public void onLivingUpdate()
	{
	}

	@Override
	@Shadow
	protected abstract void updateFallState(double y, boolean onGroundIn, IBlockState state, BlockPos pos);

	@Shadow
	public abstract float getHealth();

	@Shadow
	public abstract ItemStack getHeldItem(EnumHand hand);

	@Shadow
	protected abstract void updateEntityActionState();

	@Shadow
	protected abstract void handleJumpWater();

	@Shadow
	public abstract boolean isElytraFlying();

	@Shadow
	public abstract int getItemInUseCount();

	/**
	 * @author CCBlueX
	 */
	@Overwrite
	protected void jump()
	{
		final JumpEvent jumpEvent = new JumpEvent(getJumpUpwardsMotion());
		LiquidBounce.eventManager.callEvent(jumpEvent);
		if (jumpEvent.isCancelled())
			return;

		motionY = jumpEvent.getMotion();

		if (isPotionActive(MobEffects.JUMP_BOOST))
			motionY += (float) (getActivePotionEffect(MobEffects.JUMP_BOOST).getAmplifier() + 1) * 0.1F;

		if (isSprinting())
		{
			final float f = rotationYaw * 0.017453292F;
			motionX -= MathHelper.sin(f) * 0.2F;
			motionZ += MathHelper.cos(f) * 0.2F;
		}

		isAirBorne = true;
		ForgeHooks.onLivingJump((EntityLivingBase) (Object) this);
	}

	@Inject(method = "onLivingUpdate", at = @At("HEAD"))
	private void headLiving(final CallbackInfo callbackInfo)
	{
		if (LiquidBounce.moduleManager.get(NoJumpDelay.class).getState())
			jumpTicks = 0;
	}

	@Inject(method = "onLivingUpdate", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/EntityLivingBase;isJumping:Z", ordinal = 1))
	private void onJumpSection(final CallbackInfo callbackInfo)
	{
		if (LiquidBounce.moduleManager.get(AirJump.class).getState() && isJumping && jumpTicks == 0)
		{
			jump();
			jumpTicks = 10;
		}

		final LiquidWalk liquidWalk = (LiquidWalk) LiquidBounce.moduleManager.get(LiquidWalk.class);

		if (liquidWalk.getState() && !isJumping && !isSneaking() && isInWater() && liquidWalk.getModeValue().get().equalsIgnoreCase("Swim"))
		{
			handleJumpWater();
		}
	}

	@Inject(method = "getLook", at = @At("HEAD"), cancellable = true)
	private void getLook(final CallbackInfoReturnable<Vec3d> callbackInfoReturnable)
	{
		// noinspection ConstantConditions
		if ((EntityLivingBase) (Object) this instanceof EntityPlayerSP)
			callbackInfoReturnable.setReturnValue(getVectorForRotation(rotationPitch, rotationYaw));
	}

	@Inject(method = "isPotionActive(Lnet/minecraft/potion/Potion;)Z", at = @At("HEAD"), cancellable = true)
	private void isPotionActive(final Potion p_isPotionActive_1_, final CallbackInfoReturnable<? super Boolean> callbackInfoReturnable)
	{
		final AntiBlind antiBlind = (AntiBlind) LiquidBounce.moduleManager.get(AntiBlind.class);

		if ((p_isPotionActive_1_ == MobEffects.NAUSEA || p_isPotionActive_1_ == MobEffects.BLINDNESS) && antiBlind.getState() && antiBlind.getConfusionEffect().get())
			callbackInfoReturnable.setReturnValue(false);
	}

	@Inject(method = "moveRelative", at = @At("HEAD"), cancellable = true)
	private void handleRotations(final float strafe, final float up, final float forward, final float friction, final CallbackInfo callbackInfo)
	{
		// noinspection ConstantConditions
		if ((Object) this != Minecraft.getMinecraft().player)
			return;

		final StrafeEvent strafeEvent = new StrafeEvent(strafe, forward, friction);
		LiquidBounce.eventManager.callEvent(strafeEvent);

		if (strafeEvent.isCancelled())
			callbackInfo.cancel();
	}
}
