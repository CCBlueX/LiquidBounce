/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.entity;

import java.util.List;

import com.mojang.authlib.GameProfile;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.features.module.modules.render.Bobbing;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.entity.player.PlayerCapabilities;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.util.FoodStats;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.EnumDifficulty;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(EntityPlayer.class)
public abstract class MixinEntityPlayer extends MixinEntityLivingBase
{

	@Shadow
	@Final
	protected static DataParameter<Byte> MAIN_HAND;
	@Shadow
	public PlayerCapabilities capabilities;
	@Shadow
	protected int flyToggleTimer;

	@Shadow
	public abstract ItemStack getItemStackFromSlot(EntityEquipmentSlot p_getItemStackFromSlot_1_);

	@Shadow
	public abstract GameProfile getGameProfile();

	@Override
	@Shadow
	protected abstract boolean canTriggerWalking();

	@Override
	@Shadow
	protected abstract SoundEvent getSwimSound();

	@Shadow
	public abstract FoodStats getFoodStats();

	@Shadow
	public float prevCameraYaw;
	@Shadow
	public float cameraYaw;

	@Shadow
	protected FoodStats foodStats;

	@Shadow
	public float speedInAir;

	@Shadow
	public InventoryPlayer inventory;

	@Shadow
	public abstract boolean isSpectator();

	@Shadow
	private void collideWithPlayer(final Entity p_71044_1_)
	{
	}

	/**
	 * Called frequently so the entity can update its state every tick as required. For example, zombies and skeletons use this to react to sunlight and start to burn.
	 *
	 * @reason Bobbing Camera Yaw/Pitch multiplier & Ignore ground check
	 * @author eric0210
	 */
	@Override
	@Overwrite
	public void onLivingUpdate()
	{
		if (flyToggleTimer > 0)
			--flyToggleTimer;

		if (world.getDifficulty() == EnumDifficulty.PEACEFUL && world.getGameRules().getBoolean("naturalRegeneration"))
		{
			if (getHealth() < getMaxHealth() && this.ticksExisted % 20 == 0)
				heal(1.0F);

			if (foodStats.needFood() && this.ticksExisted % 10 == 0)
				foodStats.setFoodLevel(foodStats.getFoodLevel() + 1);
		}

		inventory.decrementAnimations();
		prevCameraYaw = cameraYaw;
		super.onLivingUpdate();
		final IAttributeInstance iattributeinstance = getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
		if (!world.isRemote)
			iattributeinstance.setBaseValue(capabilities.getWalkSpeed());

		this.jumpMovementFactor = speedInAir;
		if (isSprinting())
			this.jumpMovementFactor = (float) ((double) this.jumpMovementFactor + (double) speedInAir * 0.3D);

		setAIMoveSpeed((float) iattributeinstance.getAttributeValue());

		final Bobbing bobbing = (Bobbing) LiquidBounce.moduleManager.get(Bobbing.class);
		final boolean bobbingState = bobbing.getState();

		final float yawIncMultiplier = bobbing.getCameraIncrementMultiplierYawValue().get();
		final float pitchIncMultiplier = bobbing.getCameraIncrementMultiplierPitchValue().get();

		float cameraYawInc = Math.min(MathHelper.sqrt(motionX * motionX + motionZ * motionZ), 0.1f) * yawIncMultiplier;
		float cameraPitchInc = (float) (Math.atan(-motionY * 0.20000000298023224D) * 15.0D) * pitchIncMultiplier;
		if (cameraYawInc > 0.1F)
			cameraYawInc = 0.1F;

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
		{
			final AxisAlignedBB axisalignedbb;
			if (isRiding() && !getRidingEntity().isDead)
				axisalignedbb = getEntityBoundingBox().union(getRidingEntity().getEntityBoundingBox()).grow(1.0D, 0.0D, 1.0D);
			else
				axisalignedbb = getEntityBoundingBox().grow(1.0D, 0.5D, 1.0D);

			final List<Entity> list = world.getEntitiesWithinAABBExcludingEntity(this, axisalignedbb);

			for (int i = 0; i < list.size(); ++i)
			{
				final Entity entity = list.get(i);
				if (!entity.isDead)
					collideWithPlayer(entity);
			}
		}

		playShoulderEntityAmbientSound(getLeftShoulderEntity());
		playShoulderEntityAmbientSound(getRightShoulderEntity());
		if (!world.isRemote && (this.fallDistance > 0.5F || isInWater() || isRiding()) || capabilities.isFlying)
			spawnShoulderEntities();
	}
}
