/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.entity;

import java.util.Random;
import java.util.UUID;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.StrafeEvent;
import net.ccbluex.liquidbounce.features.module.modules.combat.HitBox;
import net.ccbluex.liquidbounce.features.module.modules.exploit.NoPitchLimit;
import net.ccbluex.liquidbounce.features.module.modules.render.ItemPhysics;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
@SideOnly(Side.CLIENT)
public abstract class MixinEntity
{
	@Shadow
	public double posX;

	@Shadow
	public double posY;

	@Shadow
	public double posZ;

	@Shadow
	public abstract boolean isSprinting();

	@Shadow
	public float rotationPitch;

	@Shadow
	public float rotationYaw;

	@Shadow
	public abstract AxisAlignedBB getEntityBoundingBox();

	@Shadow
	public Entity ridingEntity;

	@Shadow
	public double motionX;

	@Shadow
	public double motionY;

	@Shadow
	public double motionZ;

	@Shadow
	public boolean onGround;

	@Shadow
	public boolean isAirBorne;

	@Shadow
	public boolean noClip;

	@Shadow
	public World worldObj;

	@SuppressWarnings("NoopMethodInAbstractClass")
	@Shadow
	public void moveEntity(final double x, final double y, final double z)
	{
	}

	@Shadow
	public boolean isInWeb;

	@Shadow
	public float stepHeight;

	@Shadow
	public boolean isCollidedHorizontally;

	@Shadow
	public boolean isCollidedVertically;

	@Shadow
	public boolean isCollided;

	@Shadow
	public float distanceWalkedModified;

	@Shadow
	public float distanceWalkedOnStepModified;

	@Shadow
	public abstract boolean isInWater();

	@Shadow
	protected Random rand;

	@Shadow
	public int fireResistance;

	@Shadow
	protected boolean inPortal;

	@Shadow
	public int timeUntilPortal;

	@Shadow
	public float width;

	@Shadow
	public abstract boolean isRiding();

	@Shadow
	public abstract void setFire(int seconds);

	@Shadow
	protected abstract void dealFireDamage(int amount);

	@Shadow
	public abstract boolean isWet();

	@Shadow
	public abstract void addEntityCrashInfo(CrashReportCategory category);

	@Shadow
	protected abstract void doBlockCollisions();

	@Shadow
	protected abstract void playStepSound(BlockPos pos, Block blockIn);

	@Shadow
	public abstract void setEntityBoundingBox(AxisAlignedBB box);

	@Shadow
	private int nextStepDistance;

	@Shadow
	private int fire;

	@Shadow
	public float prevRotationPitch;

	@Shadow
	public float prevRotationYaw;

	@Shadow
	public int ticksExisted;

	@Shadow
	protected abstract Vec3 getVectorForRotation(float pitch, float yaw);

	@Shadow
	public abstract UUID getUniqueID();

	@Shadow
	public abstract boolean isSneaking();

	@Shadow
	public abstract boolean isInsideOfMaterial(Material materialIn);

	@Shadow
	public abstract void setPosition(double x, double y, double z);

	@Shadow
	protected abstract void setRotation(float yaw, float pitch);

	public int getNextStepDistance()
	{
		return nextStepDistance;
	}

	public void setNextStepDistance(final int nextStepDistance)
	{
		this.nextStepDistance = nextStepDistance;
	}

	public int getFire()
	{
		return fire;
	}

	@Inject(method = "getCollisionBorderSize", at = @At("HEAD"), cancellable = true)
	private void getCollisionBorderSize(final CallbackInfoReturnable<Float> callbackInfoReturnable)
	{
		final HitBox hitBox = (HitBox) LiquidBounce.moduleManager.get(HitBox.class);

		if (hitBox.getState())
			callbackInfoReturnable.setReturnValue(0.1F + hitBox.getSizeValue().get());
	}

	@Inject(method = "setAngles", at = @At("HEAD"), cancellable = true)
	private void setAngles(final float yaw, final float pitch, final CallbackInfo callbackInfo)
	{
		// NoPitchLimit
		if (LiquidBounce.moduleManager.getModule(NoPitchLimit.class).getState())
		{
			callbackInfo.cancel();

			final float rotYaw = rotationYaw;
			final float rotPitch = rotationPitch;

			rotationYaw += yaw * 0.15D;
			rotationPitch -= pitch * 0.15D;
			prevRotationPitch += rotationPitch - rotPitch;
			prevRotationYaw += rotationYaw - rotYaw;
		}
	}

	@Inject(method = "moveFlying", at = @At("HEAD"), cancellable = true)
	private void handleRotations(final float strafe, final float forward, final float friction, final CallbackInfo callbackInfo)
	{
		// Trigger StrafeEvent

		// noinspection ConstantConditions
		if ((Object) this != Minecraft.getMinecraft().thePlayer)
			return;

		final StrafeEvent strafeEvent = new StrafeEvent(strafe, forward, friction);
		LiquidBounce.eventManager.callEvent(strafeEvent);

		if (strafeEvent.isCancelled())
			callbackInfo.cancel();
	}

	@SideOnly(Side.CLIENT)
	@Inject(method = "setPositionAndRotation2", at = @At("HEAD"), cancellable = true)
	public void setPositionAndRotation2(final double x, final double y, final double z, final float yaw, final float pitch, final int posRotationIncrements, final boolean p_180426_10_, final CallbackInfo callbackInfo)
	{
		// ItemPhysics
		final ItemPhysics itemPhysics = (ItemPhysics) LiquidBounce.moduleManager.get(ItemPhysics.class);

		//noinspection ConstantConditions
		if ((Object) this instanceof EntityItem && itemPhysics.getState())
		{
			setPosition(x, y, z);
			callbackInfo.cancel();
		}
	}
}
