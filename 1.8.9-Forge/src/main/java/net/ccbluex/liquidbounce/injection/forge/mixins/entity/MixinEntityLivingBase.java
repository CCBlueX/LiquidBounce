/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.entity;

import java.util.Objects;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.JumpEvent;
import net.ccbluex.liquidbounce.features.module.modules.movement.AirJump;
import net.ccbluex.liquidbounce.features.module.modules.movement.LiquidWalk;
import net.ccbluex.liquidbounce.features.module.modules.movement.NoJumpDelay;
import net.ccbluex.liquidbounce.features.module.modules.render.AntiBlind;
import net.ccbluex.liquidbounce.utils.MovementUtils;
import net.minecraft.block.Block;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

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
	protected abstract float getJumpUpwardsMotion();

	@Shadow
	public abstract PotionEffect getActivePotionEffect(Potion potionIn);

	@Shadow
	public abstract boolean isPotionActive(Potion potionIn);

	@Shadow
	private int jumpTicks;

	@Shadow
	protected boolean isJumping;

	@Shadow
	public void onLivingUpdate()
	{
	}

	@Shadow
	protected abstract void updateFallState(double y, boolean onGroundIn, Block blockIn, BlockPos pos);

	@Shadow
	public abstract float getHealth();

	@Shadow
	public abstract ItemStack getHeldItem();

	@Shadow
	protected abstract void updateAITick();

	/**
	 * @author CCBlueX
	 * @reason JumpEvent
	 */
	@Overwrite
	protected void jump()
	{
		final JumpEvent jumpEvent = new JumpEvent(getJumpUpwardsMotion());
		LiquidBounce.eventManager.callEvent(jumpEvent);
		if (jumpEvent.isCancelled())
			return;

		motionY = jumpEvent.getMotion();

		if (isPotionActive(Potion.jump))
			motionY += (getActivePotionEffect(Potion.jump).getAmplifier() + 1) * 0.1F;

		if (isSprinting())
		{
			// final float f = rotationYaw * 0.017453292F;
			final float f = MovementUtils.getDirection(); // Compatibility with Sprint AllDirection mode
			motionX -= MathHelper.sin(f) * 0.2F;
			motionZ += MathHelper.cos(f) * 0.2F;
		}

		isAirBorne = true;
	}

	@Inject(method = "onLivingUpdate", at = @At("HEAD"))
	private void headLiving(final CallbackInfo callbackInfo)
	{
		if (Objects.requireNonNull(LiquidBounce.moduleManager.getModule(NoJumpDelay.class)).getState())
			jumpTicks = 0;
	}

	@Inject(method = "onLivingUpdate", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/EntityLivingBase;isJumping:Z", ordinal = 1))
	private void onJumpSection(final CallbackInfo callbackInfo)
	{
		if (Objects.requireNonNull(LiquidBounce.moduleManager.getModule(AirJump.class)).getState() && isJumping && jumpTicks == 0)
		{
			jump();
			jumpTicks = 10;
		}

		final LiquidWalk liquidWalk = (LiquidWalk) LiquidBounce.moduleManager.getModule(LiquidWalk.class);

		if (Objects.requireNonNull(liquidWalk).getState() && !isJumping && !isSneaking() && isInWater() && liquidWalk.getModeValue().get().equalsIgnoreCase("Swim"))
			updateAITick();
	}

	@Inject(method = "getLook", at = @At("HEAD"), cancellable = true)
	private void getLook(final CallbackInfoReturnable<Vec3> callbackInfoReturnable)
	{
		// noinspection ConstantConditions
		if ((EntityLivingBase) (Object) this instanceof EntityPlayerSP)
			callbackInfoReturnable.setReturnValue(getVectorForRotation(rotationPitch, rotationYaw));
	}

	@Inject(method = "isPotionActive(Lnet/minecraft/potion/Potion;)Z", at = @At("HEAD"), cancellable = true)
	private void isPotionActive(final Potion p_isPotionActive_1_, final CallbackInfoReturnable<Boolean> callbackInfoReturnable)
	{
		final AntiBlind antiBlind = (AntiBlind) LiquidBounce.moduleManager.getModule(AntiBlind.class);

		if ((p_isPotionActive_1_ == Potion.confusion || p_isPotionActive_1_ == Potion.blindness) && Objects.requireNonNull(antiBlind).getState() && antiBlind.getConfusionEffect().get())
			callbackInfoReturnable.setReturnValue(false);
	}
}
