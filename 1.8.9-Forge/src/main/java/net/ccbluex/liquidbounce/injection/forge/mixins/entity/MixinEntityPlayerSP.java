/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.entity;

import java.util.List;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.*;
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura;
import net.ccbluex.liquidbounce.features.module.modules.exploit.AntiHunger;
import net.ccbluex.liquidbounce.features.module.modules.exploit.PortalMenu;
import net.ccbluex.liquidbounce.features.module.modules.fun.Derp;
import net.ccbluex.liquidbounce.features.module.modules.movement.*;
import net.ccbluex.liquidbounce.features.module.modules.render.Bobbing;
import net.ccbluex.liquidbounce.features.module.modules.render.NoSwing;
import net.ccbluex.liquidbounce.features.module.modules.world.Scaffold;
import net.ccbluex.liquidbounce.utils.MovementUtils;
import net.ccbluex.liquidbounce.utils.Rotation;
import net.ccbluex.liquidbounce.utils.RotationUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockWall;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition;
import net.minecraft.network.play.client.C03PacketPlayer.C05PacketPlayerLook;
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.network.play.client.C0BPacketEntityAction.Action;
import net.minecraft.potion.Potion;
import net.minecraft.util.*;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityPlayerSP.class)
@SideOnly(Side.CLIENT)
public abstract class MixinEntityPlayerSP extends MixinAbstractClientPlayer
{

	@Shadow
	public boolean serverSprintState;

	@Shadow
	public abstract void playSound(String name, float volume, float pitch);

	@Shadow
	public int sprintingTicksLeft;

	@Shadow
	protected int sprintToggleTimer;

	@Shadow
	public float timeInPortal;

	@Shadow
	public float prevTimeInPortal;

	@Shadow
	protected Minecraft mc;

	@Shadow
	public MovementInput movementInput;

	@Shadow
	public abstract void setSprinting(boolean sprinting);

	@Shadow
	protected abstract boolean pushOutOfBlocks(double x, double y, double z);

	@Shadow
	public abstract void sendPlayerAbilities();

	@Shadow
	public float horseJumpPower;

	@Shadow
	public int horseJumpPowerCounter;

	@Shadow
	protected abstract void sendHorseJump();

	@Shadow
	public abstract boolean isRidingHorse();

	@Shadow
	@Final
	public NetHandlerPlayClient sendQueue;

	@Shadow
	private boolean serverSneakState;

	@SuppressWarnings("AbstractMethodOverridesAbstractMethod")
	@Shadow
	public abstract boolean isSneaking();

	@Shadow
	protected abstract boolean isCurrentViewEntity();

	@Shadow
	private double lastReportedPosX;

	@Shadow
	private int positionUpdateTicks;

	@Shadow
	private double lastReportedPosY;

	@Shadow
	private double lastReportedPosZ;

	@Shadow
	private float lastReportedYaw;

	@Shadow
	private float lastReportedPitch;

	/**
	 * @author CCBlueX
	 * @reason InventoryMove, Sneak, MotionEvent
	 */
	@Overwrite
	public void onUpdateWalkingPlayer()
	{
		try
		{
			LiquidBounce.eventManager.callEvent(new MotionEvent(EventState.PRE));

			final InventoryMove inventoryMove = (InventoryMove) LiquidBounce.moduleManager.get(InventoryMove.class);
			final Sneak sneak = (Sneak) LiquidBounce.moduleManager.get(Sneak.class);
			final boolean fakeSprint = inventoryMove.getState() && inventoryMove.getAacAdditionProValue().get() || LiquidBounce.moduleManager.get(AntiHunger.class).getState() || sneak.getState() && (!MovementUtils.isMoving() || !sneak.stopMoveValue.get()) && "MineSecure".equalsIgnoreCase(sneak.modeValue.get());

			final boolean sprinting = isSprinting() && !fakeSprint;

			if (sprinting != serverSprintState)
			{
				if (sprinting)
					sendQueue.addToSendQueue(new C0BPacketEntityAction((EntityPlayerSP) (Object) this, Action.START_SPRINTING));
				else
					sendQueue.addToSendQueue(new C0BPacketEntityAction((EntityPlayerSP) (Object) this, Action.STOP_SPRINTING));

				serverSprintState = sprinting;
			}

			final boolean sneaking = isSneaking();

			if (sneaking != serverSneakState && (!sneak.getState() || "Legit".equalsIgnoreCase(sneak.modeValue.get())))
			{
				if (sneaking)
					sendQueue.addToSendQueue(new C0BPacketEntityAction((EntityPlayerSP) (Object) this, Action.START_SNEAKING));
				else
					sendQueue.addToSendQueue(new C0BPacketEntityAction((EntityPlayerSP) (Object) this, Action.STOP_SNEAKING));

				serverSneakState = sneaking;
			}

			if (isCurrentViewEntity())
			{
				float yaw = rotationYaw;
				float pitch = rotationPitch;
				final float lastReportedYaw = RotationUtils.serverRotation.getYaw();
				final float lastReportedPitch = RotationUtils.serverRotation.getPitch();

				final Derp derp = (Derp) LiquidBounce.moduleManager.getModule(Derp.class);
				if (derp.getState())
				{
					final float[] rot = derp.getRotation();
					yaw = rot[0];
					pitch = rot[1];
				}

				if (RotationUtils.targetRotation != null)
				{
					yaw = RotationUtils.targetRotation.getYaw();
					pitch = RotationUtils.targetRotation.getPitch();
				}

				final double xDiff = posX - lastReportedPosX;
				final double yDiff = getEntityBoundingBox().minY - lastReportedPosY;
				final double zDiff = posZ - lastReportedPosZ;
				final double yawDiff = yaw - lastReportedYaw;
				final double pitchDiff = pitch - lastReportedPitch;
				boolean moved = xDiff * xDiff + yDiff * yDiff + zDiff * zDiff > 9.0E-4D || positionUpdateTicks >= 20;
				final boolean rotated = yawDiff != 0.0D || pitchDiff != 0.0D;

				if (ridingEntity == null)
					if (moved && rotated)
						sendQueue.addToSendQueue(new C06PacketPlayerPosLook(posX, getEntityBoundingBox().minY, posZ, yaw, pitch, onGround));
					else if (moved)
						sendQueue.addToSendQueue(new C04PacketPlayerPosition(posX, getEntityBoundingBox().minY, posZ, onGround));
					else
						sendQueue.addToSendQueue(rotated ? new C05PacketPlayerLook(yaw, pitch, onGround) : new C03PacketPlayer(onGround));
				else
				{
					sendQueue.addToSendQueue(new C06PacketPlayerPosLook(motionX, -999.0D, motionZ, yaw, pitch, onGround));
					moved = false;
				}

				++positionUpdateTicks;

				if (moved)
				{
					lastReportedPosX = posX;
					lastReportedPosY = getEntityBoundingBox().minY;
					lastReportedPosZ = posZ;
					positionUpdateTicks = 0;
				}

				if (rotated)
				{
					this.lastReportedYaw = rotationYaw;
					this.lastReportedPitch = rotationPitch;
				}
			}

			LiquidBounce.eventManager.callEvent(new MotionEvent(EventState.POST));
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
	}

	@Inject(method = "swingItem", at = @At("HEAD"), cancellable = true)
	private void swingItem(final CallbackInfo callbackInfo)
	{
		final NoSwing noSwing = (NoSwing) LiquidBounce.moduleManager.getModule(NoSwing.class);

		if (noSwing.getState())
		{
			callbackInfo.cancel();

			if (!noSwing.getServerSideValue().get())
				sendQueue.addToSendQueue(new C0APacketAnimation());
		}
	}

	@Inject(method = "pushOutOfBlocks", at = @At("HEAD"), cancellable = true)
	private void onPushOutOfBlocks(final CallbackInfoReturnable<Boolean> callbackInfoReturnable)
	{
		final PushOutEvent event = new PushOutEvent();
		if (noClip)
			event.cancelEvent();
		LiquidBounce.eventManager.callEvent(event);

		if (event.isCancelled())
			callbackInfoReturnable.setReturnValue(false);
	}

	/**
	 * @author CCBlueX
	 * @reason Sprint
	 */
	@Overwrite
	public void onLivingUpdate()
	{
		LiquidBounce.eventManager.callEvent(new UpdateEvent());

		if (sprintingTicksLeft > 0)
		{
			--sprintingTicksLeft;

			if (sprintingTicksLeft == 0)
				setSprinting(false);
		}

		if (sprintToggleTimer > 0)
			--sprintToggleTimer;

		prevTimeInPortal = timeInPortal;

		if (inPortal)
		{
			if (mc.currentScreen != null && !mc.currentScreen.doesGuiPauseGame() && !LiquidBounce.moduleManager.getModule(PortalMenu.class).getState())
				mc.displayGuiScreen(null);

			if (timeInPortal == 0.0F)
				mc.getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("portal.trigger"), rand.nextFloat() * 0.4F + 0.8F));

			timeInPortal += 0.0125F;

			if (timeInPortal >= 1.0F)
				timeInPortal = 1.0F;

			inPortal = false;
		}
		else if (isPotionActive(Potion.confusion) && getActivePotionEffect(Potion.confusion).getDuration() > 60)
		{
			timeInPortal += 0.006666667F;

			if (timeInPortal > 1.0F)
				timeInPortal = 1.0F;
		}
		else
		{
			if (timeInPortal > 0.0F)
				timeInPortal -= 0.05F;

			if (timeInPortal < 0.0F)
				timeInPortal = 0.0F;
		}

		if (timeUntilPortal > 0)
			--timeUntilPortal;

		final boolean jump = movementInput.jump;
		final boolean sneak = movementInput.sneak;
		final float f = 0.8F;
		final boolean forward = movementInput.moveForward >= f;
		movementInput.updatePlayerMoveState();

		final NoSlow noSlow = (NoSlow) LiquidBounce.moduleManager.getModule(NoSlow.class);
		final KillAura killAura = (KillAura) LiquidBounce.moduleManager.getModule(KillAura.class);

		if (getHeldItem() != null && (isUsingItem() || getHeldItem().getItem() instanceof ItemSword && killAura.getServerSideBlockingStatus()) && !isRiding())
		{
			final SlowDownEvent slowDownEvent = new SlowDownEvent(0.2F, 0.2F);
			LiquidBounce.eventManager.callEvent(slowDownEvent);
			movementInput.moveStrafe *= slowDownEvent.getStrafe();
			movementInput.moveForward *= slowDownEvent.getForward();
			sprintToggleTimer = 0;
		}

		pushOutOfBlocks(posX - width * 0.35D, getEntityBoundingBox().minY + 0.5D, posZ + width * 0.35D);
		pushOutOfBlocks(posX - width * 0.35D, getEntityBoundingBox().minY + 0.5D, posZ - width * 0.35D);
		pushOutOfBlocks(posX + width * 0.35D, getEntityBoundingBox().minY + 0.5D, posZ - width * 0.35D);
		pushOutOfBlocks(posX + width * 0.35D, getEntityBoundingBox().minY + 0.5D, posZ + width * 0.35D);

		final Sprint sprint = (Sprint) LiquidBounce.moduleManager.getModule(Sprint.class);

		final boolean foodCheck = !sprint.foodValue.get() || getFoodStats().getFoodLevel() > 6.0F || capabilities.allowFlying;

		if (onGround && !sneak && !forward && movementInput.moveForward >= f && !isSprinting() && foodCheck && !isUsingItem() && !isPotionActive(Potion.blindness))
			if (sprintToggleTimer <= 0 && !mc.gameSettings.keyBindSprint.isKeyDown())
				sprintToggleTimer = 7;
			else
				setSprinting(true);

		if (!isSprinting() && movementInput.moveForward >= f && foodCheck && (noSlow.getState() || !isUsingItem()) && !isPotionActive(Potion.blindness) && mc.gameSettings.keyBindSprint.isKeyDown())
			setSprinting(true);

		final Scaffold scaffold = (Scaffold) LiquidBounce.moduleManager.get(Scaffold.class);

		if (scaffold.getState() && !scaffold.sprintValue.get() || sprint.getState() && sprint.checkServerSide.get() && (onGround || !sprint.checkServerSideGround.get()) && !sprint.allDirectionsValue.get() && RotationUtils.targetRotation != null && RotationUtils.getRotationDifference(new Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch)) > 30)
			setSprinting(false);

		final boolean allDirection = sprint.getState() && sprint.allDirectionsValue.get();
		if (isSprinting() && (!allDirection && movementInput.moveForward < f || isCollidedHorizontally || !foodCheck))
			setSprinting(false);

		if (capabilities.allowFlying)
			if (mc.playerController.isSpectatorMode())
			{
				if (!capabilities.isFlying)
				{
					capabilities.isFlying = true;
					sendPlayerAbilities();
				}
			}
			else if (!jump && movementInput.jump)
				if (flyToggleTimer == 0)
					flyToggleTimer = 7;
				else
				{
					capabilities.isFlying = !capabilities.isFlying;
					sendPlayerAbilities();
					flyToggleTimer = 0;
				}

		if (capabilities.isFlying && isCurrentViewEntity())
		{
			if (movementInput.sneak)
				motionY -= capabilities.getFlySpeed() * 3.0F;

			if (movementInput.jump)
				motionY += capabilities.getFlySpeed() * 3.0F;
		}

		if (isRidingHorse())
		{
			if (horseJumpPowerCounter < 0)
			{
				++horseJumpPowerCounter;

				if (horseJumpPowerCounter == 0)
					horseJumpPower = 0.0F;
			}

			if (jump && !movementInput.jump)
			{
				horseJumpPowerCounter = -10;
				sendHorseJump();
			}
			else if (!jump && movementInput.jump)
			{
				horseJumpPowerCounter = 0;
				horseJumpPower = 0.0F;
			}
			else if (jump)
			{
				++horseJumpPowerCounter;

				horseJumpPower = horseJumpPowerCounter < 10 ? horseJumpPowerCounter * 0.1F : 0.8F + 2.0F / (horseJumpPowerCounter - 9) * 0.1F;
			}
		}
		else
			horseJumpPower = 0.0F;

		super.onLivingUpdate();

		if (onGround && capabilities.isFlying && !mc.playerController.isSpectatorMode())
		{
			capabilities.isFlying = false;
			sendPlayerAbilities();
		}
	}

	@Override
	public void moveEntity(double x, double y, double z)
	{
		final MoveEvent moveEvent = new MoveEvent(x, y, z);
		LiquidBounce.eventManager.callEvent(moveEvent);

		if (moveEvent.isCancelled())
			return;

		x = moveEvent.getX();
		y = moveEvent.getY();
		z = moveEvent.getZ();

		if (noClip)
		{
			setEntityBoundingBox(getEntityBoundingBox().offset(x, y, z));
			posX = (getEntityBoundingBox().minX + getEntityBoundingBox().maxX) / 2.0D;
			posY = getEntityBoundingBox().minY;
			posZ = (getEntityBoundingBox().minZ + getEntityBoundingBox().maxZ) / 2.0D;
		}
		else
		{
			worldObj.theProfiler.startSection("move");
			final double d0 = posX;
			final double d1 = posY;
			final double d2 = posZ;

			if (isInWeb)
			{
				isInWeb = false;
				x *= 0.25D;
				y *= 0.05000000074505806D;
				z *= 0.25D;
				motionX = 0.0D;
				motionY = 0.0D;
				motionZ = 0.0D;
			}

			double d3 = x;
			final double d4 = y;
			double d5 = z;
			final boolean sneaking = onGround && isSneaking();

			if (sneaking || moveEvent.isSafeWalk())
			{
				final double d6;

				// noinspection ConstantConditions
				d6 = 0.05D;
				while (x != 0.0D && worldObj.getCollidingBoundingBoxes((Entity) (Object) this, getEntityBoundingBox().offset(x, -1.0D, 0.0D)).isEmpty()) {
					if (x < d6 && x >= -d6)
						x = 0.0D;
					else
						x -= x > 0.0D ? d6 : -d6;

					d3 = x;
				}

				// noinspection ConstantConditions
				while (z != 0.0D && worldObj.getCollidingBoundingBoxes((Entity) (Object) this, getEntityBoundingBox().offset(0.0D, -1.0D, z)).isEmpty()) {
					if (z < d6 && z >= -d6)
						z = 0.0D;
					else
						z -= z > 0.0D ? d6 : -d6;

					d5 = z;
				}

				// noinspection ConstantConditions
				while (x != 0.0D && z != 0.0D && worldObj.getCollidingBoundingBoxes((Entity) (Object) this, getEntityBoundingBox().offset(x, -1.0D, z)).isEmpty()) {
					if (x < d6 && x >= -d6)
						x = 0.0D;
					else x -= x > 0.0D ? d6 : -d6;

					d3 = x;

					if (z < d6 && z >= -d6)
						z = 0.0D;
					else
						z -= z > 0.0D ? d6 : -d6;

					d5 = z;
				}
			}

			// noinspection ConstantConditions
			final List<AxisAlignedBB> list1 = worldObj.getCollidingBoundingBoxes((Entity) (Object) this, getEntityBoundingBox().addCoord(x, y, z));
			final AxisAlignedBB axisalignedbb = getEntityBoundingBox();

			for (final AxisAlignedBB axisalignedbb1 : list1)
				y = axisalignedbb1.calculateYOffset(getEntityBoundingBox(), y);

			setEntityBoundingBox(getEntityBoundingBox().offset(0.0D, y, 0.0D));
			final Step step = (Step) LiquidBounce.moduleManager.getModule(Step.class);
			final boolean airStep = step.getState() && step.getAirStepValue().get() && step.canAirStep();
			final boolean steppable = onGround || airStep || d4 != y && d4 < 0.0D;

			for (final AxisAlignedBB axisalignedbb2 : list1)
				x = axisalignedbb2.calculateXOffset(getEntityBoundingBox(), x);

			setEntityBoundingBox(getEntityBoundingBox().offset(x, 0.0D, 0.0D));

			for (final AxisAlignedBB axisalignedbb13 : list1)
				z = axisalignedbb13.calculateZOffset(getEntityBoundingBox(), z);

			setEntityBoundingBox(getEntityBoundingBox().offset(0.0D, 0.0D, z));

			if (stepHeight > 0.0F && steppable && (d3 != x || d5 != z))
			{
				final StepEvent stepEvent = new StepEvent(stepHeight);
				LiquidBounce.eventManager.callEvent(stepEvent);
				final double d11 = x;
				final double d7 = y;
				final double d8 = z;
				final AxisAlignedBB axisalignedbb3 = getEntityBoundingBox();
				setEntityBoundingBox(axisalignedbb);
				y = stepEvent.getStepHeight();
				// noinspection ConstantConditions
				final List<AxisAlignedBB> list = worldObj.getCollidingBoundingBoxes((Entity) (Object) this, getEntityBoundingBox().addCoord(d3, y, d5));
				AxisAlignedBB axisalignedbb4 = getEntityBoundingBox();
				final AxisAlignedBB axisalignedbb5 = axisalignedbb4.addCoord(d3, 0.0D, d5);
				double d9 = y;

				for (final AxisAlignedBB axisalignedbb6 : list)
					d9 = axisalignedbb6.calculateYOffset(axisalignedbb5, d9);

				axisalignedbb4 = axisalignedbb4.offset(0.0D, d9, 0.0D);
				double d15 = d3;

				for (final AxisAlignedBB axisalignedbb7 : list)
					d15 = axisalignedbb7.calculateXOffset(axisalignedbb4, d15);

				axisalignedbb4 = axisalignedbb4.offset(d15, 0.0D, 0.0D);
				double d16 = d5;

				for (final AxisAlignedBB axisalignedbb8 : list)
					d16 = axisalignedbb8.calculateZOffset(axisalignedbb4, d16);

				axisalignedbb4 = axisalignedbb4.offset(0.0D, 0.0D, d16);
				AxisAlignedBB axisalignedbb14 = getEntityBoundingBox();
				double d17 = y;

				for (final AxisAlignedBB axisalignedbb9 : list)
					d17 = axisalignedbb9.calculateYOffset(axisalignedbb14, d17);

				axisalignedbb14 = axisalignedbb14.offset(0.0D, d17, 0.0D);
				double d18 = d3;

				for (final AxisAlignedBB axisalignedbb10 : list)
					d18 = axisalignedbb10.calculateXOffset(axisalignedbb14, d18);

				axisalignedbb14 = axisalignedbb14.offset(d18, 0.0D, 0.0D);
				double d19 = d5;

				for (final AxisAlignedBB axisalignedbb11 : list)
					d19 = axisalignedbb11.calculateZOffset(axisalignedbb14, d19);

				axisalignedbb14 = axisalignedbb14.offset(0.0D, 0.0D, d19);
				final double d20 = d15 * d15 + d16 * d16;
				final double d10 = d18 * d18 + d19 * d19;

				if (d20 > d10)
				{
					x = d15;
					z = d16;
					y = -d9;
					setEntityBoundingBox(axisalignedbb4);
				}
				else
				{
					x = d18;
					z = d19;
					y = -d17;
					setEntityBoundingBox(axisalignedbb14);
				}

				for (final AxisAlignedBB axisalignedbb12 : list)
					y = axisalignedbb12.calculateYOffset(getEntityBoundingBox(), y);

				setEntityBoundingBox(getEntityBoundingBox().offset(0.0D, y, 0.0D));

				if (d11 * d11 + d8 * d8 >= x * x + z * z)
				{
					x = d11;
					y = d7;
					z = d8;
					setEntityBoundingBox(axisalignedbb3);
				}
				else
					LiquidBounce.eventManager.callEvent(new StepConfirmEvent());
			}

			worldObj.theProfiler.endSection();
			worldObj.theProfiler.startSection("rest");
			posX = (getEntityBoundingBox().minX + getEntityBoundingBox().maxX) / 2.0D;
			posY = getEntityBoundingBox().minY;
			posZ = (getEntityBoundingBox().minZ + getEntityBoundingBox().maxZ) / 2.0D;
			isCollidedHorizontally = d3 != x || d5 != z;
			isCollidedVertically = d4 != y;
			onGround = isCollidedVertically && d4 < 0.0D;
			isCollided = isCollidedHorizontally || isCollidedVertically;
			final int i = MathHelper.floor_double(posX);
			final int j = MathHelper.floor_double(posY - 0.20000000298023224D);
			final int k = MathHelper.floor_double(posZ);
			BlockPos blockpos = new BlockPos(i, j, k);
			Block block1 = worldObj.getBlockState(blockpos).getBlock();

			if (block1.getMaterial() == Material.air)
			{
				final Block block = worldObj.getBlockState(blockpos.down()).getBlock();

				if (block instanceof BlockFence || block instanceof BlockWall || block instanceof BlockFenceGate)
				{
					block1 = block;
					blockpos = blockpos.down();
				}
			}

			updateFallState(y, onGround, block1, blockpos);

			if (d3 != x)
				motionX = 0.0D;

			if (d5 != z)
				motionZ = 0.0D;

			if (d4 != y)
				block1.onLanded(worldObj, (Entity) (Object) this);

			if (canTriggerWalking() && !sneaking && ridingEntity == null)
			{
				final double d12 = posX - d0;
				double d13 = posY - d1;
				final double d14 = posZ - d2;

				if (block1 != Blocks.ladder)
					d13 = 0.0D;

				if (onGround)
					block1.onEntityCollidedWithBlock(worldObj, blockpos, (Entity) (Object) this);

				final Bobbing bobbing = (Bobbing) LiquidBounce.moduleManager.get(Bobbing.class);
				distanceWalkedModified += MathHelper.sqrt_double(d12 * d12 + d14 * d14) * (bobbing.getState() ? bobbing.getMultiplierValue().get() : 0.6D);
				distanceWalkedOnStepModified += MathHelper.sqrt_double(d12 * d12 + d13 * d13 + d14 * d14) * (bobbing.getState() ? bobbing.getMultiplierValue().get() : 0.6D);

				if (distanceWalkedOnStepModified > getNextStepDistance() && block1.getMaterial() != Material.air)
				{
					setNextStepDistance((int) distanceWalkedOnStepModified + 1);

					if (isInWater())
					{
						float f = MathHelper.sqrt_double(motionX * motionX * 0.20000000298023224D + motionY * motionY + motionZ * motionZ * 0.20000000298023224D) * 0.35F;

						if (f > 1.0F)
							f = 1.0F;

						playSound(getSwimSound(), f, 1.0F + (rand.nextFloat() - rand.nextFloat()) * 0.4F);
					}

					playStepSound(blockpos, block1);
				}
			}

			try
			{
				doBlockCollisions();
			}
			catch (final Throwable throwable)
			{
				final CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Checking entity block collision");
				final CrashReportCategory crashreportcategory = crashreport.makeCategory("Entity being checked for collision");
				addEntityCrashInfo(crashreportcategory);
				throw new ReportedException(crashreport);
			}

			final boolean flag2 = isWet();

			if (worldObj.isFlammableWithin(getEntityBoundingBox().contract(0.001D, 0.001D, 0.001D)))
			{
				dealFireDamage(1);

				if (!flag2)
				{
					setFire(getFire() + 1);

					if (getFire() == 0)
						setFire(8);
				}
			}
			else if (getFire() <= 0)
				setFire(-fireResistance);

			if (flag2 && getFire() > 0)
			{
				playSound("random.fizz", 0.7F, 1.6F + (rand.nextFloat() - rand.nextFloat()) * 0.4F);
				setFire(-fireResistance);
			}

			worldObj.theProfiler.endSection();
		}
	}
}
