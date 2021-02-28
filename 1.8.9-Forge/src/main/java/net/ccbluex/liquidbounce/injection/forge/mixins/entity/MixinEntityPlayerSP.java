/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.entity;

import java.util.List;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPlayerSP;
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
			final IEntityPlayerSP thePlayer = LiquidBounce.wrapper.getMinecraft().getThePlayer();
			if (thePlayer == null)
				return;

			LiquidBounce.eventManager.callEvent(new MotionEvent(EventState.PRE));

			final InventoryMove inventoryMove = (InventoryMove) LiquidBounce.moduleManager.get(InventoryMove.class);
			final Sneak sneak = (Sneak) LiquidBounce.moduleManager.get(Sneak.class);
			final boolean fakeSprint = inventoryMove.getState() && inventoryMove.getAacAdditionProValue().get() || LiquidBounce.moduleManager.get(AntiHunger.class).getState() || sneak.getState() && (!MovementUtils.isMoving(thePlayer) || !sneak.stopMoveValue.get()) && "MineSecure".equalsIgnoreCase(sneak.modeValue.get());

			final boolean sprinting = isSprinting() && !fakeSprint;

			if (sprinting != serverSprintState)
			{
				final Action action = sprinting ? Action.START_SPRINTING : Action.STOP_SPRINTING;

				//noinspection ConstantConditions
				sendQueue.addToSendQueue(new C0BPacketEntityAction((Entity) (Object) this, action));

				serverSprintState = sprinting;
			}

			final boolean sneaking = isSneaking();

			if (sneaking != serverSneakState && (!sneak.getState() || "Legit".equalsIgnoreCase(sneak.modeValue.get())))
			{
				final Action action = sneaking ? Action.START_SNEAKING : Action.STOP_SNEAKING;

				//noinspection ConstantConditions
				sendQueue.addToSendQueue(new C0BPacketEntityAction((Entity) (Object) this, action));

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
		final float sprintForwardThreshold = 0.8F;
		final boolean forward = movementInput.moveForward >= sprintForwardThreshold;
		final boolean sneak = movementInput.sneak;

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

		final boolean foodCheck = !sprint.getFoodValue().get() || getFoodStats().getFoodLevel() > 6.0F || capabilities.allowFlying;

		final boolean blindCheck = !isPotionActive(Potion.blindness);
		if (onGround && !sneak && !forward && movementInput.moveForward >= sprintForwardThreshold && !isSprinting() && foodCheck && !isUsingItem() && blindCheck)
			if (sprintToggleTimer <= 0 && !mc.gameSettings.keyBindSprint.isKeyDown())
				sprintToggleTimer = 7;
			else
				setSprinting(true);

		final boolean sprintCheck = noSlow.getState() || !isUsingItem();
		if (!isSprinting() && movementInput.moveForward >= sprintForwardThreshold && foodCheck && sprintCheck && blindCheck && mc.gameSettings.keyBindSprint.isKeyDown())
			setSprinting(true);

		final Scaffold scaffold = (Scaffold) LiquidBounce.moduleManager.get(Scaffold.class);

		final boolean groundCheck = onGround || !sprint.getCheckServerSideGround().get();
		if (scaffold.getState() && !scaffold.getSprintValue().get() || sprint.getState() && sprint.getCheckServerSide().get() && groundCheck && !sprint.getAllDirectionsValue().get() && RotationUtils.targetRotation != null && RotationUtils.Companion.getRotationDifference(new Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch)) > 30)
			setSprinting(false);

		final boolean allDirection = sprint.getState() && sprint.getAllDirectionsValue().get();
		if (isSprinting() && (!allDirection && movementInput.moveForward < sprintForwardThreshold || isCollidedHorizontally || !foodCheck))
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
	public void moveEntity(double moveX, double moveY, double moveZ)
	{
		// Call MoveEvent
		final MoveEvent moveEvent = new MoveEvent(moveX, moveY, moveZ);
		LiquidBounce.eventManager.callEvent(moveEvent);

		if (moveEvent.isCancelled())
			return;

		// Fix Position
		moveX = moveEvent.getX();
		moveY = moveEvent.getY();
		moveZ = moveEvent.getZ();

		if (noClip)
		{
			setEntityBoundingBox(getEntityBoundingBox().offset(moveX, moveY, moveZ));

			posX = (getEntityBoundingBox().minX + getEntityBoundingBox().maxX) * 0.5;
			posY = getEntityBoundingBox().minY;
			posZ = (getEntityBoundingBox().minZ + getEntityBoundingBox().maxZ) * 0.5;
		}
		else
		{
			/* Move */
			worldObj.theProfiler.startSection("move");
			final double lastPosX = posX;
			final double lastPosY = posY;
			final double lastPosZ = posZ;

			// Motion correction for web
			if (isInWeb)
			{
				isInWeb = false;

				moveX *= 0.25D;
				moveY *= 0.05000000074505806D;
				moveZ *= 0.25D;

				motionX = 0.0D;
				motionY = 0.0D;
				motionZ = 0.0D;
			}

			double safewalkAppliedX = moveX;
			final double safewalkAppliedY = moveY;
			double safewalkAppliedZ = moveZ;

			final boolean sneaking = onGround && isSneaking();

			// SafeWalk
			if (sneaking || moveEvent.isSafeWalk())
			{
				final double collisionCheckRange = 0.05D;

				// TODO: Please fold these triple-iterations into one if can

				// X
				// noinspection ConstantConditions
				while (moveX != 0.0D && worldObj.getCollidingBoundingBoxes((Entity) (Object) this, getEntityBoundingBox().offset(moveX, -1.0D, 0.0D)).isEmpty())
				{
					if (moveX < collisionCheckRange && moveX >= -collisionCheckRange)
						moveX = 0.0D;
					else
						moveX -= moveX > 0.0D ? collisionCheckRange : -collisionCheckRange;

					safewalkAppliedX = moveX;
				}

				// Z
				// noinspection ConstantConditions
				while (moveZ != 0.0D && worldObj.getCollidingBoundingBoxes((Entity) (Object) this, getEntityBoundingBox().offset(0.0D, -1.0D, moveZ)).isEmpty())
				{
					if (moveZ < collisionCheckRange && moveZ >= -collisionCheckRange)
						moveZ = 0.0D;
					else
						moveZ -= moveZ > 0.0D ? collisionCheckRange : -collisionCheckRange;

					safewalkAppliedZ = moveZ;
				}

				// X, Z
				// noinspection ConstantConditions
				while (moveX != 0.0D && moveZ != 0.0D && worldObj.getCollidingBoundingBoxes((Entity) (Object) this, getEntityBoundingBox().offset(moveX, -1.0D, moveZ)).isEmpty())
				{
					if (moveX < collisionCheckRange && moveX >= -collisionCheckRange)
						moveX = 0.0D;
					else
						moveX -= moveX > 0.0D ? collisionCheckRange : -collisionCheckRange;

					safewalkAppliedX = moveX;

					if (moveZ < collisionCheckRange && moveZ >= -collisionCheckRange)
						moveZ = 0.0D;
					else
						moveZ -= moveZ > 0.0D ? collisionCheckRange : -collisionCheckRange;

					safewalkAppliedZ = moveZ;
				}
			}

			// noinspection ConstantConditions
			final List<AxisAlignedBB> collidedBoxList = worldObj.getCollidingBoundingBoxes((Entity) (Object) this, getEntityBoundingBox().addCoord(moveX, moveY, moveZ));
			final AxisAlignedBB entityBox = getEntityBoundingBox();

			// TODO: Please fold these triple-iterations into one if can

			// Calculate Y Offset
			for (final AxisAlignedBB collidedBox : collidedBoxList)
				moveY = collidedBox.calculateYOffset(getEntityBoundingBox(), moveY);

			// Apply Y Offset
			setEntityBoundingBox(getEntityBoundingBox().offset(0.0D, moveY, 0.0D));

			// Calculate X Offset
			for (final AxisAlignedBB collidedBox : collidedBoxList)
				moveX = collidedBox.calculateXOffset(getEntityBoundingBox(), moveX);

			// Apply X Offset
			setEntityBoundingBox(getEntityBoundingBox().offset(moveX, 0.0D, 0.0D));

			// Calculate Z Offset
			for (final AxisAlignedBB collidedBox : collidedBoxList)
				moveZ = collidedBox.calculateZOffset(getEntityBoundingBox(), moveZ);

			// Apply Z Offset
			setEntityBoundingBox(getEntityBoundingBox().offset(0.0D, 0.0D, moveZ));

			final Step step = (Step) LiquidBounce.moduleManager.getModule(Step.class);
			final boolean airStep = step.getState() && step.getAirStepValue().get() && step.canAirStep();
			final boolean steppable = onGround || airStep || safewalkAppliedY != moveY && safewalkAppliedY < 0.0D;

			// Stepping
			if (stepHeight > 0.0F && steppable && (safewalkAppliedX != moveX || safewalkAppliedZ != moveZ))
			{
				// Call StepEvent
				final StepEvent stepEvent = new StepEvent(stepHeight);
				LiquidBounce.eventManager.callEvent(stepEvent);

				final double __x = moveX;
				final double __y = moveY;
				final double __z = moveZ;

				final AxisAlignedBB cachedBB = getEntityBoundingBox();

				setEntityBoundingBox(entityBox);

				moveY = stepEvent.getStepHeight();

				// noinspection ConstantConditions
				final List<AxisAlignedBB> collisionList = worldObj.getCollidingBoundingBoxes((Entity) (Object) this, getEntityBoundingBox().addCoord(safewalkAppliedX, moveY, safewalkAppliedZ));

				AxisAlignedBB offsetAppliedBB = getEntityBoundingBox();

				final AxisAlignedBB safewalkAppliedBB = offsetAppliedBB.addCoord(safewalkAppliedX, 0.0D, safewalkAppliedZ);

				// TODO: Please fold these triple-iterations into one if can

				double offsetY = moveY;

				// Calculate Y Offset
				for (final AxisAlignedBB collision : collisionList)
					offsetY = collision.calculateYOffset(safewalkAppliedBB, offsetY);

				// Apply Y Offset
				offsetAppliedBB = offsetAppliedBB.offset(0.0D, offsetY, 0.0D);

				double offsetX = safewalkAppliedX;

				// Calculate X Offset
				for (final AxisAlignedBB collision : collisionList)
					offsetX = collision.calculateXOffset(offsetAppliedBB, offsetX);

				// Apply X Offset
				offsetAppliedBB = offsetAppliedBB.offset(offsetX, 0.0D, 0.0D);

				double offsetZ = safewalkAppliedZ;

				// Calculate Z Offset
				for (final AxisAlignedBB collision : collisionList)
					offsetZ = collision.calculateZOffset(offsetAppliedBB, offsetZ);

				// Apply Z Offset
				offsetAppliedBB = offsetAppliedBB.offset(0.0D, 0.0D, offsetZ);

				AxisAlignedBB offset2AppliedBB = getEntityBoundingBox();

				double offsetY2 = moveY;

				// Calculate Y Offset 2
				for (final AxisAlignedBB collision : collisionList)
					offsetY2 = collision.calculateYOffset(offset2AppliedBB, offsetY2);

				// Apply Y Offset 2
				offset2AppliedBB = offset2AppliedBB.offset(0.0D, offsetY2, 0.0D);
				double offsetX2 = safewalkAppliedX;

				// Calculate X Offset 2
				for (final AxisAlignedBB collision : collisionList)
					offsetX2 = collision.calculateXOffset(offset2AppliedBB, offsetX2);

				// Apply X Offset 2
				offset2AppliedBB = offset2AppliedBB.offset(offsetX2, 0.0D, 0.0D);

				double offsetZ2 = safewalkAppliedZ;

				// Calculate Z Offset 2
				for (final AxisAlignedBB collision : collisionList)
					offsetZ2 = collision.calculateZOffset(offset2AppliedBB, offsetZ2);

				// Apply Z Offset 2
				offset2AppliedBB = offset2AppliedBB.offset(0.0D, 0.0D, offsetZ2);

				final double offset = offsetX * offsetX + offsetZ * offsetZ;
				final double offset2 = offsetX2 * offsetX2 + offsetZ2 * offsetZ2;

				// Apply bigger one
				if (offset > offset2)
				{
					moveX = offsetX;
					moveZ = offsetZ;
					moveY = -offsetY;
					setEntityBoundingBox(offsetAppliedBB);
				}
				else
				{
					moveX = offsetX2;
					moveZ = offsetZ2;
					moveY = -offsetY2;
					setEntityBoundingBox(offset2AppliedBB);
				}

				// Calculate Y Offset 3
				for (final AxisAlignedBB collision : collisionList)
					moveY = collision.calculateYOffset(getEntityBoundingBox(), moveY);

				// Apply Y Offset 3
				setEntityBoundingBox(getEntityBoundingBox().offset(0.0D, moveY, 0.0D));

				if (__x * __x + __z * __z >= moveX * moveX + moveZ * moveZ)
				{
					moveX = __x;
					moveY = __y;
					moveZ = __z;
					setEntityBoundingBox(cachedBB);
				}
				else
					LiquidBounce.eventManager.callEvent(new StepConfirmEvent());
			}

			worldObj.theProfiler.endSection();

			/* Rest */
			worldObj.theProfiler.startSection("rest");
			posX = (getEntityBoundingBox().minX + getEntityBoundingBox().maxX) * 0.5;
			posY = getEntityBoundingBox().minY;
			posZ = (getEntityBoundingBox().minZ + getEntityBoundingBox().maxZ) * 0.5;
			isCollidedHorizontally = safewalkAppliedX != moveX || safewalkAppliedZ != moveZ;
			isCollidedVertically = safewalkAppliedY != moveY;
			onGround = isCollidedVertically && safewalkAppliedY < 0.0D;
			isCollided = isCollidedHorizontally || isCollidedVertically;

			final double groundCheckDepth = 0.20000000298023224D;

			final int groundCheckX = MathHelper.floor_double(posX);
			final int groundCheckY = MathHelper.floor_double(posY - groundCheckDepth);
			final int groundCheckZ = MathHelper.floor_double(posZ);
			BlockPos groundCheckPos = new BlockPos(groundCheckX, groundCheckY, groundCheckZ);
			Block groundBlock = worldObj.getBlockState(groundCheckPos).getBlock();

			if (groundBlock.getMaterial() == Material.air)
			{
				final Block groundDownBlock = worldObj.getBlockState(groundCheckPos.down()).getBlock();

				if (groundDownBlock instanceof BlockFence || groundDownBlock instanceof BlockWall || groundDownBlock instanceof BlockFenceGate)
				{
					groundBlock = groundDownBlock;
					groundCheckPos = groundCheckPos.down();
				}
			}

			updateFallState(moveY, onGround, groundBlock, groundCheckPos);

			if (safewalkAppliedX != moveX)
				motionX = 0.0D;

			if (safewalkAppliedY != moveY)
				// noinspection ConstantConditions
				groundBlock.onLanded(worldObj, (Entity) (Object) this);

			if (safewalkAppliedZ != moveZ)
				motionZ = 0.0D;

			if (canTriggerWalking() && !sneaking && ridingEntity == null)
			{
				final double xDelta = posX - lastPosX;
				double yDelta = posY - lastPosY;
				final double zDelta = posZ - lastPosZ;

				if (groundBlock != Blocks.ladder)
					yDelta = 0.0D;

				if (onGround)
					// noinspection ConstantConditions
					groundBlock.onEntityCollidedWithBlock(worldObj, groundCheckPos, (Entity) (Object) this);

				final Bobbing bobbing = (Bobbing) LiquidBounce.moduleManager.get(Bobbing.class);
				final boolean bobbingState = bobbing.getState();
				final double bobbingMultiplier = bobbingState ? bobbing.getMultiplierValue().get() : 0.6D;

				distanceWalkedModified += StrictMath.hypot(xDelta, zDelta) * bobbingMultiplier;
				distanceWalkedOnStepModified += MathHelper.sqrt_double(xDelta * xDelta + yDelta * yDelta + zDelta * zDelta) * bobbingMultiplier;

				if (distanceWalkedOnStepModified > getNextStepDistance() && groundBlock.getMaterial() != Material.air)
				{
					setNextStepDistance((int) distanceWalkedOnStepModified + 1);

					// Swimming check
					if (isInWater())
					{
						float volume = MathHelper.sqrt_double(motionX * motionX * groundCheckDepth + motionY * motionY + motionZ * motionZ * groundCheckDepth) * 0.35F;

						if (volume > 1.0F)
							volume = 1.0F;

						playSound(getSwimSound(), volume, 1.0F + (rand.nextFloat() - rand.nextFloat()) * 0.4F);
					}

					playStepSound(groundCheckPos, groundBlock);
				}
			}

			// Block collision check
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

			final boolean wet = isWet();

			// Take fire damage
			if (worldObj.isFlammableWithin(getEntityBoundingBox().contract(0.001D, 0.001D, 0.001D)))
			{
				dealFireDamage(1);

				if (!wet)
				{
					setFire(getFire() + 1);

					if (getFire() == 0)
						setFire(8);
				}
			}
			else if (getFire() <= 0)
				setFire(-fireResistance);

			// Extinguish fire
			if (wet && getFire() > 0)
			{
				playSound("random.fizz", 0.7F, 1.6F + (rand.nextFloat() - rand.nextFloat()) * 0.4F);
				setFire(-fireResistance);
			}

			worldObj.theProfiler.endSection();
		}
	}
}
