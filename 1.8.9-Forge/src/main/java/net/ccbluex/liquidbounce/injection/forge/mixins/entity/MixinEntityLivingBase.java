/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.entity;

import static net.ccbluex.liquidbounce.LiquidBounce.wrapper;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPlayerSP;
import net.ccbluex.liquidbounce.event.JumpEvent;
import net.ccbluex.liquidbounce.features.module.modules.movement.AirJump;
import net.ccbluex.liquidbounce.features.module.modules.movement.NoJumpDelay;
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed;
import net.ccbluex.liquidbounce.features.module.modules.render.AntiBlind;
import net.ccbluex.liquidbounce.features.module.modules.render.SwingAnimation;
import net.ccbluex.liquidbounce.injection.implementations.IMixinEntityLivingBase;
import net.ccbluex.liquidbounce.utils.ClientUtils;
import net.ccbluex.liquidbounce.utils.MovementUtils;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.server.S0BPacketAnimation;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeHooks;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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
	private boolean isCanBeCollidedWith = true;

	@Override
	public void setCanBeCollidedWith(final boolean value)
	{
		isCanBeCollidedWith = value;
	}

	/**
	 * @author eric0210
	 * @reason
	 */
	@Overwrite
	public boolean canBeCollidedWith()
	{
		return isCanBeCollidedWith && !isDead;
	}

	/**
	 * @author CCBlueX
	 * @reason JumpEvent
	 */
	@Overwrite
	protected void jump()
	{
		final EntityPlayerSP thePlayer = Minecraft.getMinecraft().thePlayer;

		if (thePlayer != null && thePlayer.isEntityEqual((Entity) (Object) this))
		{
			final JumpEvent jumpEvent = new JumpEvent(getJumpUpwardsMotion());
			LiquidBounce.eventManager.callEvent(jumpEvent);
			if (jumpEvent.isCancelled())
				return;

			motionY = jumpEvent.getMotion();
		}
		else
			motionY = getJumpUpwardsMotion();

		if (isPotionActive(Potion.jump))
			motionY += (getActivePotionEffect(Potion.jump).getAmplifier() + 1) * 0.1F;

		// Sprint-jump with Speed module enabled will boost you too fast as get caught by anticheats.
		final Speed speed = (Speed) LiquidBounce.moduleManager.get(Speed.class);

		// Sprint-Jump Boost
		if (isSprinting() && (!speed.getState() || speed.allowSprintBoost()))
		{
			// Sprint-Jump Boost
			final float dir = MovementUtils.getDirection(rotationYaw, moveForward, moveStrafing); // Compatibility with Sprint AllDirection mode
			motionX -= MathHelper.sin(dir) * 0.2F;
			motionZ += MathHelper.cos(dir) * 0.2F;
		}

		isAirBorne = true;
		ForgeHooks.onLivingJump((EntityLivingBase) (Object) this);
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
		final SwingAnimation sa = (SwingAnimation) LiquidBounce.moduleManager.get(SwingAnimation.class);
		int swingAnimationEnd = isPotionActive(Potion.digSpeed) ? 6 - (1 + getActivePotionEffect(Potion.digSpeed).getAmplifier()) : isPotionActive(Potion.digSlowdown) ? 6 + (1 + getActivePotionEffect(Potion.digSlowdown).getAmplifier() << 1) : 6;

		if (sa.getState())
			swingAnimationEnd += sa.getSwingSpeedSwingSpeed().get();

		swingAnimationEnd += sa.swingSpeedBoost;

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
			final SwingAnimation sa = (SwingAnimation) LiquidBounce.moduleManager.get(SwingAnimation.class);
			if (!isSwingInProgress || swingProgressInt >= (sa.getState() ? sa.getSwingSpeedSwingProgressLimit().get() : getArmSwingAnimationEnd() >> 1) || swingProgressInt < 0)
			{
				swingProgressInt = -1;
				isSwingInProgress = true;

				final Entity entity = (Entity) (Object) this;
				final Packet<INetHandlerPlayClient> packetAnimation = new S0BPacketAnimation(entity, 0);

				if (worldObj instanceof WorldServer)
					((WorldServer) worldObj).getEntityTracker().sendToAllTrackingEntity(entity, packetAnimation);
			}
		}
	}
}
