/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.entity;

import com.mojang.authlib.GameProfile;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.features.module.modules.render.Bobbing;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.entity.player.PlayerCapabilities;
import net.minecraft.item.ItemStack;
import net.minecraft.util.FoodStats;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumDifficulty;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(EntityPlayer.class)
public abstract class MixinEntityPlayer extends MixinEntityLivingBase
{
	@Shadow
	public float prevCameraYaw;
	@Shadow
	public float cameraYaw;

	@Shadow
	protected int flyToggleTimer;

	@Shadow
	public PlayerCapabilities capabilities;

	@Shadow
	protected FoodStats foodStats;

	@Shadow
	public float speedInAir;

	@Shadow
	public InventoryPlayer inventory;

	@Shadow
	public abstract ItemStack getHeldItem();

	@Shadow
	public abstract GameProfile getGameProfile();

	@Shadow
	protected abstract boolean canTriggerWalking();

	@Shadow
	protected abstract String getSwimSound();

	@Shadow
	public abstract FoodStats getFoodStats();

	@Shadow
	public abstract int getItemInUseDuration();

	@Shadow
	public abstract ItemStack getItemInUse();

	@Shadow
	public abstract boolean isUsingItem();

	@Shadow
	public abstract boolean isSpectator();

	@Shadow
	private void collideWithPlayer(final Entity p_71044_1_)
	{
	}

	/**
	 * @reason Bobbing Camera Yaw/Pitch multiplier & Ignore ground check
	 * @author eric0210 Called frequently so the entity can update its state every tick as required. For example, zombies and skeletons use this to react to sunlight and start to burn.
	 */
	@Overwrite
	public void onLivingUpdate()
	{
		if (flyToggleTimer > 0)
			--flyToggleTimer;

		if (worldObj.getDifficulty() == EnumDifficulty.PEACEFUL && worldObj.getGameRules().getBoolean("naturalRegeneration"))
		{
			if (getHealth() < getMaxHealth() && ticksExisted % 20 == 0)
				heal(1.0F);

			if (foodStats.needFood() && ticksExisted % 10 == 0)
				foodStats.setFoodLevel(foodStats.getFoodLevel() + 1);
		}

		inventory.decrementAnimations();
		prevCameraYaw = cameraYaw;

		super.onLivingUpdate();

		final IAttributeInstance iattributeinstance = getEntityAttribute(SharedMonsterAttributes.movementSpeed);

		if (!worldObj.isRemote)
			iattributeinstance.setBaseValue(capabilities.getWalkSpeed());

		jumpMovementFactor = speedInAir;

		if (isSprinting())
			jumpMovementFactor += speedInAir * 0.3D;

		setAIMoveSpeed((float) iattributeinstance.getAttributeValue());

		final Bobbing bobbing = (Bobbing) LiquidBounce.moduleManager.get(Bobbing.class);
		final boolean bobbingState = bobbing.getState();

		final float yawIncMultiplier = bobbing.getCameraIncrementMultiplierYawValue().get();
		final float pitchIncMultiplier = bobbing.getCameraIncrementMultiplierPitchValue().get();

		float cameraYawInc = Math.min(MathHelper.sqrt_double(motionX * motionX + motionZ * motionZ), 0.1f) * yawIncMultiplier;
		float cameraPitchInc = (float) (StrictMath.atan(-motionY * 0.20000000298023224D) * 15.0D) * pitchIncMultiplier;

		final boolean groundCheck = onGround || bobbingState && !bobbing.getCheckGroundValue().get();

		if (!groundCheck || getHealth() <= 0.0F)
			cameraYawInc = 0.0F;

		if (groundCheck || getHealth() <= 0.0F)
			cameraPitchInc = 0.0F;

		final float yawMultiplier = bobbingState ? bobbing.getCameraMultiplierYawValue().get() : 0.4F;
		cameraYaw += (cameraYawInc - cameraYaw) * yawMultiplier;

		final float pitchMultiplier = bobbingState ? bobbing.getCameraMultiplierPitchValue().get() : 0.8F;
		cameraPitch += (cameraPitchInc - cameraPitch) * pitchMultiplier;

		if (getHealth() > 0.0F && !isSpectator())
			// noinspection ConstantConditions
			worldObj.getEntitiesWithinAABBExcludingEntity((Entity) (Object) this, ridingEntity != null && !ridingEntity.isDead ? getEntityBoundingBox().union(ridingEntity.getEntityBoundingBox()).expand(1.0D, 0.0D, 1.0D) : getEntityBoundingBox().expand(1.0D, 0.5D, 1.0D)).stream().filter(entity -> !entity.isDead).forEach(this::collideWithPlayer);
	}
}
