/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.entity;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPlayerSP;
import net.ccbluex.liquidbounce.event.JumpEvent;
import net.ccbluex.liquidbounce.features.module.modules.movement.AirJump;
import net.ccbluex.liquidbounce.features.module.modules.movement.NoJumpDelay;
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed;
import net.ccbluex.liquidbounce.features.module.modules.render.AntiBlind;
import net.ccbluex.liquidbounce.features.module.modules.render.SwingAnimation;
import net.ccbluex.liquidbounce.utils.MovementUtils;
import net.minecraft.block.Block;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S0BPacketAnimation;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.WorldServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.ccbluex.liquidbounce.LiquidBounce.wrapper;

@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBase extends MixinEntity
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

	/**
	 * @author CCBlueX
	 * @reason JumpEvent
	 */
	@Overwrite
	protected void jump()
	{
		final IEntityPlayerSP thePlayer = wrapper.getMinecraft().getThePlayer();
		if (thePlayer == null)
			return;

		final JumpEvent jumpEvent = new JumpEvent(getJumpUpwardsMotion());
		LiquidBounce.eventManager.callEvent(jumpEvent);
		if (jumpEvent.isCancelled())
			return;

		motionY = jumpEvent.getMotion();

		if (isPotionActive(Potion.jump))
			motionY += (getActivePotionEffect(Potion.jump).getAmplifier() + 1) * 0.1F;

		// Sprint-jump with Speed module enabled will boost you too fast as get caught by anticheats.
		final Speed speed = (Speed) LiquidBounce.moduleManager.get(Speed.class);

		if (isSprinting() && (!speed.getState() || speed.allowSprintBoost()))
		{
			// Sprint-Jump Boost
			final float dir = MovementUtils.getDirection(thePlayer); // Compatibility with Sprint AllDirection mode
			motionX -= MathHelper.sin(dir) * 0.2F;
			motionZ += MathHelper.cos(dir) * 0.2F;
		}

		isAirBorne = true;
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
	}

	@Inject(method = "getLook", at = @At("HEAD"), cancellable = true)
	private void getLook(final CallbackInfoReturnable<Vec3> callbackInfoReturnable)
	{
		// noinspection ConstantConditions
		if ((EntityLivingBase) (Object) this instanceof EntityPlayerSP)
			callbackInfoReturnable.setReturnValue(getVectorForRotation(rotationPitch, rotationYaw));
	}

	@Inject(method = "isPotionActive(Lnet/minecraft/potion/Potion;)Z", at = @At("HEAD"), cancellable = true)
	private void isPotionActive(final Potion potion, final CallbackInfoReturnable<Boolean> callbackInfoReturnable)
	{
		final AntiBlind antiBlind = (AntiBlind) LiquidBounce.moduleManager.get(AntiBlind.class);

		if ((potion == Potion.confusion || potion == Potion.blindness) && antiBlind.getState() && antiBlind.getConfusionEffect().get())
			callbackInfoReturnable.setReturnValue(false);
	}

	/**
	 * @author eric0210
	 * @reason SwingAnimation customSwingSpeed
	 * @see    SwingAnimation
	 */
	@Overwrite
	private int getArmSwingAnimationEnd()
	{
		final SwingAnimation sa = (SwingAnimation) LiquidBounce.moduleManager.getModule(SwingAnimation.class);
		int swingAnimationEnd = isPotionActive(Potion.digSpeed) ? 6 - (1 + getActivePotionEffect(Potion.digSpeed).getAmplifier()) : isPotionActive(Potion.digSlowdown) ? 6 + (1 + getActivePotionEffect(Potion.digSlowdown).getAmplifier() << 1) : 6;

		if (sa.getState() && sa.getCustomSwingSpeed().get())
			swingAnimationEnd += sa.getSwingSpeed().get();

		return swingAnimationEnd;
	}

	/**
	 * @author eric0210
	 * @reason SwingAnimation swingProgressLimit
	 * @see    SwingAnimation
	 */
	@Overwrite
	public void swingItem()
	{
		final ItemStack stack = getHeldItem();
		if (stack == null || stack.getItem() == null || !stack.getItem().onEntitySwing((EntityLivingBase) (Object) this, stack))
		{
			final SwingAnimation sa = (SwingAnimation) LiquidBounce.moduleManager.getModule(SwingAnimation.class);
			if (!isSwingInProgress || swingProgressInt >= (sa.getState() ? sa.getSwingProgressLimit().get() : getArmSwingAnimationEnd() >> 1) || swingProgressInt < 0)
			{
				swingProgressInt = -1;
				isSwingInProgress = true;

				final Entity entity = (Entity) (Object) this;
				final S0BPacketAnimation packetAnimation = new S0BPacketAnimation(entity, 0);

				if (worldObj instanceof WorldServer)
					((WorldServer) worldObj).getEntityTracker().sendToAllTrackingEntity(entity, packetAnimation);
			}
		}
	}
}
