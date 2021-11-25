/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.entity;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.JumpEvent;
import net.ccbluex.liquidbounce.features.module.modules.movement.AirJump;
import net.ccbluex.liquidbounce.features.module.modules.movement.NoJumpDelay;
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed;
import net.ccbluex.liquidbounce.features.module.modules.render.AntiBlind;
import net.ccbluex.liquidbounce.features.module.modules.render.SwingAnimation;
import net.ccbluex.liquidbounce.injection.implementations.IMixinEntityLivingBase;
import net.ccbluex.liquidbounce.utils.MovementUtils;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBase extends MixinEntity implements IMixinEntityLivingBase
{
	@Shadow
	public float prevCameraPitch;
	@Shadow
	public float cameraPitch;
	@Shadow
	private int jumpTicks;

	@Shadow
	protected boolean isJumping;

	@Shadow
	public boolean isSwingInProgress;

	@Shadow
	public int swingProgressInt;

	@Shadow
	public float jumpMovementFactor;

	@Shadow
	protected abstract float getJumpUpwardsMotion();

	@Shadow
	public abstract PotionEffect getActivePotionEffect(Potion potionIn);

	@Shadow
	public abstract boolean isPotionActive(Potion potionIn);

	@Shadow
	public abstract boolean isPotionActive(int potionId);

	@SuppressWarnings("NoopMethodInAbstractClass")
	@Shadow
	public void onLivingUpdate()
	{
	}

	@Shadow
	protected abstract void updateArmSwingProgress();

	@Shadow
	protected abstract void updateFallState(double y, boolean onGroundIn, Block blockIn, BlockPos pos);

	@Shadow
	public abstract float getHealth();

	@Shadow
	public abstract ItemStack getHeldItem();

	@Shadow
	public abstract void heal(float healAmount);

	@Shadow
	public abstract float getMaxHealth();

	@Shadow
	public abstract void setAIMoveSpeed(float speedIn);

	@Shadow
	public abstract IAttributeInstance getEntityAttribute(IAttribute attribute);

	@Shadow
	public float moveForward;

	@Shadow
	public float moveStrafing;

	@Shadow
	protected abstract int getArmSwingAnimationEnd();

	@Shadow
	protected abstract void jump();

	private boolean isCanBeCollidedWith = true;

	private double prevMotionY;

	@Override
	public void setCanBeCollidedWith(final boolean value)
	{
		isCanBeCollidedWith = value;
	}

	@Inject(method = "canBeCollidedWith", at = @At("HEAD"), cancellable = true)
	public void canBeCollidedWith(final CallbackInfoReturnable<? super Boolean> ci)
	{
		if (!isCanBeCollidedWith)
			ci.setReturnValue(false);
	}

	@Inject(method = "jump", at = @At("HEAD"))
	protected void handleJumpEventPre(final CallbackInfo ci)
	{
		prevMotionY = motionY;
	}

	@Inject(method = "jump", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/EntityLivingBase;motionY:D", shift = Shift.AFTER, ordinal = 0), cancellable = true)
	protected void handleJumpEvent(final CallbackInfo ci)
	{
		final EntityPlayerSP thePlayer = Minecraft.getMinecraft().thePlayer;
		if (thePlayer != null && thePlayer.isEntityEqual((Entity) (Object) this))
		{
			final JumpEvent jumpEvent = new JumpEvent(getJumpUpwardsMotion());
			LiquidBounce.eventManager.callEvent(jumpEvent);

			if (jumpEvent.isCancelled())
			{
				motionY = prevMotionY;
				ci.cancel();
				return;
			}

			motionY = jumpEvent.getMotion();
		}
	}

	@Redirect(method = "jump", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;isSprinting()Z"))
	protected boolean injectSprintJumpAvailability(final EntityLivingBase instance)
	{
		final Speed speed = (Speed) LiquidBounce.moduleManager.get(Speed.class);
		return isSprinting() && (!speed.getState() || speed.allowSprintBoost());
	}

	@Redirect(method = "jump", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/EntityLivingBase;rotationYaw:F"))
	protected float injectSprintJumpDirection(final EntityLivingBase instance)
	{
		return MovementUtils.getDirectionDegrees(rotationYaw, moveForward, moveStrafing);
	}

	@Inject(method = "onLivingUpdate", at = @At("HEAD"))
	private void injectNoJumpDelay(final CallbackInfo callbackInfo)
	{
		if (LiquidBounce.moduleManager.get(NoJumpDelay.class).getState())
			jumpTicks = 0;
	}

	@Inject(method = "onLivingUpdate", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/EntityLivingBase;isJumping:Z", ordinal = 1))
	private void injectAirJump(final CallbackInfo callbackInfo)
	{
		if (LiquidBounce.moduleManager.get(AirJump.class).getState() && isJumping && jumpTicks == 0)
		{
			jump();
			jumpTicks = 10;
		}
	}

	@Inject(method = "getLook", at = @At("HEAD"), cancellable = true)
	private void injectMouseDelayFix(final CallbackInfoReturnable<? super Vec3> callbackInfoReturnable)
	{
		// MouseDelayFix
		// noinspection ConstantConditions
		if ((EntityLivingBase) (Object) this instanceof EntityPlayerSP)
			callbackInfoReturnable.setReturnValue(getVectorForRotation(rotationPitch, rotationYaw));
	}

	@Inject(method = "isPotionActive(Lnet/minecraft/potion/Potion;)Z", at = @At("HEAD"), cancellable = true)
	private void injectAntiBlind(final Potion potion, final CallbackInfoReturnable<? super Boolean> callbackInfoReturnable)
	{
		final AntiBlind antiBlind = (AntiBlind) LiquidBounce.moduleManager.get(AntiBlind.class);

		if (antiBlind.getState() && (potion == Potion.confusion && antiBlind.getConfusionEffect().get() || potion == Potion.blindness && antiBlind.getBlindnessEffect().get()))
			callbackInfoReturnable.setReturnValue(false);
	}

	/**
	 * @author eric0210
	 * @reason SwingAnimation customSwingSpeed
	 * @see    SwingAnimation
	 */
	@Inject(method = "getArmSwingAnimationEnd", at = @At("RETURN"), cancellable = true)
	private void injectSwingAnimationEnd(final CallbackInfoReturnable<? super Integer> ci)
	{
		final SwingAnimation sa = (SwingAnimation) LiquidBounce.moduleManager.get(SwingAnimation.class);
		int swingAnimationEnd = ci.getReturnValueI();

		if (sa.getState())
			swingAnimationEnd += sa.getSwingSpeedSwingSpeed().get();

		swingAnimationEnd += sa.swingSpeedBoost;

		ci.setReturnValue(swingAnimationEnd);
	}

	@Redirect(method = "swingItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;getArmSwingAnimationEnd()I"))
	public int injectSwingAnimationEnd2(final EntityLivingBase instance)
	{
		final SwingAnimation sa = (SwingAnimation) LiquidBounce.moduleManager.get(SwingAnimation.class);
		return sa.getState() ? sa.getSwingSpeedSwingProgressLimit().get() + sa.swingProgressEndBoost << 1 : getArmSwingAnimationEnd();
	}
}
