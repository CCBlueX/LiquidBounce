/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.entity;

import java.util.Arrays;
import java.util.List;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura;
import net.ccbluex.liquidbounce.features.module.modules.exploit.AntiHunger;
import net.ccbluex.liquidbounce.features.module.modules.exploit.PortalMenu;
import net.ccbluex.liquidbounce.features.module.modules.fun.Derp;
import net.ccbluex.liquidbounce.features.module.modules.movement.InventoryMove;
import net.ccbluex.liquidbounce.features.module.modules.movement.NoSlow;
import net.ccbluex.liquidbounce.features.module.modules.movement.Sneak;
import net.ccbluex.liquidbounce.features.module.modules.movement.Sprint;
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
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.IJumpingMount;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemElytra;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.CPacketAnimation;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketEntityAction.Action;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.client.CPacketPlayer.Position;
import net.minecraft.network.play.client.CPacketPlayer.PositionRotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.PlayerSPPushOutOfBlocksEvent;
import net.minecraftforge.common.MinecraftForge;
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
	public int sprintingTicksLeft;
	@Shadow
	public float timeInPortal;
	@Shadow
	public float prevTimeInPortal;
	@Shadow
	public MovementInput movementInput;
	@Shadow
	public float horseJumpPower;
	@Shadow
	public int horseJumpPowerCounter;
	@Shadow
	@Final
	public NetHandlerPlayClient connection;
	@Shadow
	protected int sprintToggleTimer;
	@Shadow
	protected Minecraft mc;
	@Shadow
	private boolean serverSneakState;
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
	@Shadow
	private int autoJumpTime;
	@Shadow
	private boolean wasFallFlying;
	@Shadow
	private boolean prevOnGround;
	@Shadow
	private boolean autoJumpEnabled;

	@Shadow
	public abstract void playSound(SoundEvent soundIn, float volume, float pitch);

	@Shadow
	public abstract void setSprinting(boolean sprinting);

	@Shadow
	protected abstract boolean pushOutOfBlocks(double x, double y, double z);

	@Shadow
	public abstract void sendPlayerAbilities();

	@Shadow
	protected abstract void sendHorseJump();

	@Shadow
	public abstract boolean isRidingHorse();

	@Override
	@Shadow
	public abstract boolean isSneaking();

	@Shadow
	protected abstract boolean isCurrentViewEntity();

	@Shadow
	public abstract void closeScreen();

	@Override
	@Shadow
	public abstract boolean isHandActive();

	@Shadow
	public abstract float getHorseJumpPower();

	@Shadow
	protected abstract void updateAutoJump(float p_189810_1_, float p_189810_2_);

	/**
	 * @author CCBlueX
	 */
	@Overwrite
	public void onUpdateWalkingPlayer()
	{
		LiquidBounce.eventManager.callEvent(new MotionEvent(EventState.PRE));

		final InventoryMove inventoryMove = (InventoryMove) LiquidBounce.moduleManager.get(InventoryMove.class);
		final Sneak sneak = (Sneak) LiquidBounce.moduleManager.get(Sneak.class);
		final boolean fakeSprint = inventoryMove.getState() && inventoryMove.getAacAdditionProValue().get() || LiquidBounce.moduleManager.get(AntiHunger.class).getState() || sneak.getState() && (!MovementUtils.isMoving() || !sneak.stopMoveValue.get()) && sneak.modeValue.get().equalsIgnoreCase("MineSecure");

		final boolean clientSprintState = isSprinting() && !fakeSprint;

		if (clientSprintState != serverSprintState)
		{
			if (clientSprintState)
			{
				connection.sendPacket(new CPacketEntityAction((EntityPlayerSP) (Object) this, Action.START_SPRINTING));
			}
			else
			{
				connection.sendPacket(new CPacketEntityAction((EntityPlayerSP) (Object) this, Action.STOP_SPRINTING));
			}

			serverSprintState = clientSprintState;
		}

		final boolean flag1 = isSneaking();

		if (flag1 != serverSneakState && (!sneak.getState() || sneak.modeValue.get().equalsIgnoreCase("Legit")))
		{
			if (flag1)
			{
				connection.sendPacket(new CPacketEntityAction((EntityPlayerSP) (Object) this, Action.START_SNEAKING));
			}
			else
			{
				connection.sendPacket(new CPacketEntityAction((EntityPlayerSP) (Object) this, Action.STOP_SNEAKING));
			}

			serverSneakState = flag1;
		}

		if (isCurrentViewEntity())
		{
			float yaw = rotationYaw;
			float pitch = rotationPitch;
			final float lastReportedYaw = RotationUtils.serverRotation.getYaw();
			final float lastReportedPitch = RotationUtils.serverRotation.getPitch();

			final Derp derp = (Derp) LiquidBounce.moduleManager.get(Derp.class);
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

			final AxisAlignedBB axisalignedbb = getEntityBoundingBox();
			final double xDiff = posX - lastReportedPosX;
			final double yDiff = axisalignedbb.minY - lastReportedPosY;
			final double zDiff = posZ - lastReportedPosZ;
			final double yawDiff = yaw - lastReportedYaw;
			final double pitchDiff = pitch - lastReportedPitch;

			++positionUpdateTicks;

			boolean flag2 = xDiff * xDiff + yDiff * yDiff + zDiff * zDiff > 9.0E-4D || positionUpdateTicks >= 20;
			final boolean flag3 = yawDiff != 0.0D || pitchDiff != 0.0D;

			if (isRiding())
			{
				connection.sendPacket(new PositionRotation(motionX, -999.0D, motionZ, rotationYaw, rotationPitch, onGround));
				flag2 = false;
			}
			else if (flag2 && flag3)
			{
				connection.sendPacket(new PositionRotation(posX, axisalignedbb.minY, posZ, rotationYaw, rotationPitch, onGround));
			}
			else if (flag2)
			{
				connection.sendPacket(new Position(posX, axisalignedbb.minY, posZ, onGround));
			}
			else if (flag3)
			{
				connection.sendPacket(new CPacketPlayer.Rotation(rotationYaw, rotationPitch, onGround));
			}
			else if (prevOnGround != onGround)
			{
				connection.sendPacket(new CPacketPlayer(onGround));
			}

			if (flag2)
			{
				lastReportedPosX = posX;
				lastReportedPosY = axisalignedbb.minY;
				lastReportedPosZ = posZ;
				positionUpdateTicks = 0;
			}

			if (flag3)
			{
				this.lastReportedYaw = rotationYaw;
				this.lastReportedPitch = rotationPitch;
			}

			prevOnGround = onGround;
			autoJumpEnabled = mc.gameSettings.autoJump;
		}

		LiquidBounce.eventManager.callEvent(new MotionEvent(EventState.POST));
	}

	@Inject(method = "swingArm", at = @At("HEAD"), cancellable = true)
	private void swingItem(final EnumHand hand, final CallbackInfo callbackInfo)
	{
		final NoSwing noSwing = (NoSwing) LiquidBounce.moduleManager.get(NoSwing.class);

		if (noSwing.getState())
		{
			callbackInfo.cancel();

			if (!noSwing.getServerSideValue().get())
				connection.sendPacket(new CPacketAnimation(hand));
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
	 * @author CCBlueX (superblaubeere27)
	 */
	@Override
	@Overwrite
	public void onLivingUpdate()
	{
		try
		{
			LiquidBounce.eventManager.callEvent(new UpdateEvent());
		}
		catch (final Throwable e)
		{
			e.printStackTrace();
		}

		++sprintingTicksLeft;

		if (sprintToggleTimer > 0)
		{
			--sprintToggleTimer;
		}

		prevTimeInPortal = timeInPortal;

		if (inPortal)
		{
			if (mc.currentScreen != null && !mc.currentScreen.doesGuiPauseGame() && !LiquidBounce.moduleManager.get(PortalMenu.class).getState())
			{
				if (mc.currentScreen instanceof GuiContainer)
				{
					closeScreen();
				}

				mc.displayGuiScreen(null);
			}

			if (timeInPortal == 0.0F)
			{
				mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.BLOCK_PORTAL_TRIGGER, rand.nextFloat() * 0.4F + 0.8F));
			}

			timeInPortal += 0.0125F;

			if (timeInPortal >= 1.0F)
			{
				timeInPortal = 1.0F;
			}

			inPortal = false;
		}
		else if (isPotionActive(MobEffects.NAUSEA) && getActivePotionEffect(MobEffects.NAUSEA).getDuration() > 60)
		{
			timeInPortal += 0.006666667F;

			if (timeInPortal > 1.0F)
			{
				timeInPortal = 1.0F;
			}
		}
		else
		{
			if (timeInPortal > 0.0F)
			{
				timeInPortal -= 0.05F;
			}

			if (timeInPortal < 0.0F)
			{
				timeInPortal = 0.0F;
			}
		}

		if (timeUntilPortal > 0)
		{
			--timeUntilPortal;
		}

		final boolean flag = movementInput.jump;
		final boolean flag1 = movementInput.sneak;
		final float f = 0.8F;
		final boolean flag2 = movementInput.moveForward >= 0.8F;
		movementInput.updatePlayerMoveState();
		final NoSlow noSlow = (NoSlow) LiquidBounce.moduleManager.get(NoSlow.class);
		final KillAura killAura = (KillAura) LiquidBounce.moduleManager.get(KillAura.class);

		ForgeHooksClient.onInputUpdate((EntityPlayerSP) (Object) this, movementInput);
		mc.getTutorial().handleMovement(movementInput);

		if (isHandActive() || getHeldItem(EnumHand.MAIN_HAND).getItem() instanceof ItemSword && killAura.getBlockingStatus() && !isRiding())
		{
			final SlowDownEvent slowDownEvent = new SlowDownEvent(0.2F, 0.2F);
			LiquidBounce.eventManager.callEvent(slowDownEvent);
			movementInput.moveStrafe *= slowDownEvent.getStrafe();
			movementInput.moveForward *= slowDownEvent.getForward();
			sprintToggleTimer = 0;
		}

		boolean flag3 = false;

		if (autoJumpTime > 0)
		{
			--autoJumpTime;
			flag3 = true;
			movementInput.jump = true;
		}

		AxisAlignedBB axisalignedbb = getEntityBoundingBox();
		final PlayerSPPushOutOfBlocksEvent event = new PlayerSPPushOutOfBlocksEvent((EntityPlayerSP) (Object) this, axisalignedbb);
		if (!MinecraftForge.EVENT_BUS.post(event))
		{
			axisalignedbb = event.getEntityBoundingBox();
			pushOutOfBlocks(posX - (double) width * 0.35D, axisalignedbb.minY + 0.5D, posZ + (double) width * 0.35D);
			pushOutOfBlocks(posX - (double) width * 0.35D, axisalignedbb.minY + 0.5D, posZ - (double) width * 0.35D);
			pushOutOfBlocks(posX + (double) width * 0.35D, axisalignedbb.minY + 0.5D, posZ - (double) width * 0.35D);
			pushOutOfBlocks(posX + (double) width * 0.35D, axisalignedbb.minY + 0.5D, posZ + (double) width * 0.35D);
		}
		final Sprint sprint = (Sprint) LiquidBounce.moduleManager.get(Sprint.class);

		final boolean flag4 = !sprint.foodValue.get() || (float) getFoodStats().getFoodLevel() > 6.0F || capabilities.allowFlying;

		if (onGround && !flag1 && !flag2 && movementInput.moveForward >= 0.8F && !isSprinting() && flag4 && !isHandActive() && !isPotionActive(MobEffects.BLINDNESS))
		{
			if (sprintToggleTimer <= 0 && !mc.gameSettings.keyBindSprint.isKeyDown())
			{
				sprintToggleTimer = 7;
			}
			else
			{
				setSprinting(true);
			}
		}

		if (!isSprinting() && movementInput.moveForward >= 0.8F && flag4 && (noSlow.getState() || !isHandActive()) && !isPotionActive(MobEffects.BLINDNESS) && mc.gameSettings.keyBindSprint.isKeyDown())
		{
			setSprinting(true);
		}

		final Scaffold scaffold = (Scaffold) LiquidBounce.moduleManager.get(Scaffold.class);

		if (scaffold.getState() && !scaffold.sprintValue.get() || sprint.getState() && sprint.checkServerSide.get() && (onGround || !sprint.checkServerSideGround.get()) && !sprint.allDirectionsValue.get() && RotationUtils.targetRotation != null && RotationUtils.getRotationDifference(new Rotation(mc.player.rotationYaw, mc.player.rotationPitch)) > 30)
			setSprinting(false);

		if (isSprinting() && (movementInput.moveForward < 0.8F || collidedHorizontally || !flag4))
		{
			setSprinting(false);
		}

		if (capabilities.allowFlying)
		{
			if (mc.playerController.isSpectatorMode())
			{
				if (!capabilities.isFlying)
				{
					capabilities.isFlying = true;
					sendPlayerAbilities();
				}
			}
			else if (!flag && movementInput.jump && !flag3)
			{
				if (flyToggleTimer == 0)
				{
					flyToggleTimer = 7;
				}
				else
				{
					capabilities.isFlying = !capabilities.isFlying;
					sendPlayerAbilities();
					flyToggleTimer = 0;
				}
			}
		}

		if (movementInput.jump && !flag && !onGround && motionY < 0.0D && !isElytraFlying() && !capabilities.isFlying)
		{
			final ItemStack itemstack = getItemStackFromSlot(EntityEquipmentSlot.CHEST);

			if (itemstack.getItem() == Items.ELYTRA && ItemElytra.isUsable(itemstack))
			{
				connection.sendPacket(new CPacketEntityAction((EntityPlayerSP) (Object) this, Action.START_FALL_FLYING));
			}
		}

		wasFallFlying = isElytraFlying();

		if (capabilities.isFlying && isCurrentViewEntity())
		{
			if (movementInput.sneak)
			{
				movementInput.moveStrafe = (float) ((double) movementInput.moveStrafe / 0.3D);
				movementInput.moveForward = (float) ((double) movementInput.moveForward / 0.3D);
				motionY -= capabilities.getFlySpeed() * 3.0F;
			}

			if (movementInput.jump)
			{
				motionY += capabilities.getFlySpeed() * 3.0F;
			}
		}

		if (isRidingHorse())
		{
			final IJumpingMount ijumpingmount = (IJumpingMount) getRidingEntity();

			if (horseJumpPowerCounter < 0)
			{
				++horseJumpPowerCounter;

				if (horseJumpPowerCounter == 0)
				{
					horseJumpPower = 0.0F;
				}
			}

			if (flag && !movementInput.jump)
			{
				horseJumpPowerCounter = -10;
				ijumpingmount.setJumpPower(MathHelper.floor(getHorseJumpPower() * 100.0F));
				sendHorseJump();
			}
			else if (!flag && movementInput.jump)
			{
				horseJumpPowerCounter = 0;
				horseJumpPower = 0.0F;
			}
			else if (flag)
			{
				++horseJumpPowerCounter;

				if (horseJumpPowerCounter < 10)
				{
					horseJumpPower = (float) horseJumpPowerCounter * 0.1F;
				}
				else
				{
					horseJumpPower = 0.8F + 2.0F / (float) (horseJumpPowerCounter - 9) * 0.1F;
				}
			}
		}
		else
		{
			horseJumpPower = 0.0F;
		}

		super.onLivingUpdate();

		if (onGround && capabilities.isFlying && !mc.playerController.isSpectatorMode())
		{
			capabilities.isFlying = false;
			sendPlayerAbilities();
		}
	}

	/**
	 * @author CCBlueX (superblaubeere27)
	 */
	@Override
	@Overwrite
	public void move(final MoverType type, double x, double y, double z)
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
			resetPositionToBB();
		}
		else
		{
			if (type == MoverType.PISTON)
			{
				final long i = world.getTotalWorldTime();

				if (i != pistonDeltasGameTime)
				{
					Arrays.fill(pistonDeltas, 0.0D);
					pistonDeltasGameTime = i;
				}

				if (x != 0.0D)
				{
					final int j = EnumFacing.Axis.X.ordinal();
					final double d0 = MathHelper.clamp(x + pistonDeltas[j], -0.51D, 0.51D);
					x = d0 - pistonDeltas[j];
					pistonDeltas[j] = d0;

					if (Math.abs(x) <= 9.999999747378752E-6D)
					{
						return;
					}
				}
				else if (y != 0.0D)
				{
					final int l4 = EnumFacing.Axis.Y.ordinal();
					final double d12 = MathHelper.clamp(y + pistonDeltas[l4], -0.51D, 0.51D);
					y = d12 - pistonDeltas[l4];
					pistonDeltas[l4] = d12;

					if (Math.abs(y) <= 9.999999747378752E-6D)
					{
						return;
					}
				}
				else
				{
					if (z == 0.0D)
					{
						return;
					}

					final int i5 = EnumFacing.Axis.Z.ordinal();
					final double d13 = MathHelper.clamp(z + pistonDeltas[i5], -0.51D, 0.51D);
					z = d13 - pistonDeltas[i5];
					pistonDeltas[i5] = d13;

					if (Math.abs(z) <= 9.999999747378752E-6D)
					{
						return;
					}
				}
			}

			world.profiler.startSection("move");
			final double d10 = posX;
			final double d11 = posY;
			final double d1 = posZ;

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

			double d2 = x;
			final double d3 = y;
			double d4 = z;

			// noinspection ConstantConditions
			if ((type == MoverType.SELF || type == MoverType.PLAYER) && (onGround && isSneaking() || moveEvent.isSafeWalk()) && (Object) this instanceof EntityPlayer)
			{
				for (final double d5 = 0.05D; x != 0.0D && world.getCollisionBoxes((EntityPlayerSP) (Object) this, getEntityBoundingBox().offset(x, -stepHeight, 0.0D)).isEmpty(); d2 = x)
				{
					if (x < 0.05D && x >= -0.05D)
					{
						x = 0.0D;
					}
					else if (x > 0.0D)
					{
						x -= 0.05D;
					}
					else
					{
						x += 0.05D;
					}
				}

				for (; z != 0.0D && world.getCollisionBoxes((EntityPlayerSP) (Object) this, getEntityBoundingBox().offset(0.0D, -stepHeight, z)).isEmpty(); d4 = z)
				{
					if (z < 0.05D && z >= -0.05D)
					{
						z = 0.0D;
					}
					else if (z > 0.0D)
					{
						z -= 0.05D;
					}
					else
					{
						z += 0.05D;
					}
				}

				for (; x != 0.0D && z != 0.0D && world.getCollisionBoxes((EntityPlayerSP) (Object) this, getEntityBoundingBox().offset(x, -stepHeight, z)).isEmpty(); d4 = z)
				{
					if (x < 0.05D && x >= -0.05D)
					{
						x = 0.0D;
					}
					else if (x > 0.0D)
					{
						x -= 0.05D;
					}
					else
					{
						x += 0.05D;
					}

					d2 = x;

					if (z < 0.05D && z >= -0.05D)
					{
						z = 0.0D;
					}
					else if (z > 0.0D)
					{
						z -= 0.05D;
					}
					else
					{
						z += 0.05D;
					}
				}
			}

			final List<AxisAlignedBB> list1 = world.getCollisionBoxes((EntityPlayerSP) (Object) this, getEntityBoundingBox().expand(x, y, z));
			final AxisAlignedBB axisalignedbb = getEntityBoundingBox();

			if (y != 0.0D)
			{
				int k = 0;

				for (final int l = list1.size(); k < l; ++k)
				{
					y = list1.get(k).calculateYOffset(getEntityBoundingBox(), y);
				}

				setEntityBoundingBox(getEntityBoundingBox().offset(0.0D, y, 0.0D));
			}

			if (x != 0.0D)
			{
				int j5 = 0;

				for (final int l5 = list1.size(); j5 < l5; ++j5)
				{
					x = list1.get(j5).calculateXOffset(getEntityBoundingBox(), x);
				}

				if (x != 0.0D)
				{
					setEntityBoundingBox(getEntityBoundingBox().offset(x, 0.0D, 0.0D));
				}
			}

			if (z != 0.0D)
			{
				int k5 = 0;

				for (final int i6 = list1.size(); k5 < i6; ++k5)
				{
					z = list1.get(k5).calculateZOffset(getEntityBoundingBox(), z);
				}

				if (z != 0.0D)
				{
					setEntityBoundingBox(getEntityBoundingBox().offset(0.0D, 0.0D, z));
				}
			}

			final boolean flag = onGround || d3 != y && d3 < 0.0D;

			if (stepHeight > 0.0F && flag && (d2 != x || d4 != z))
			{
				final StepEvent stepEvent = new StepEvent(stepHeight);
				LiquidBounce.eventManager.callEvent(stepEvent);

				final double d14 = x;
				final double d6 = y;
				final double d7 = z;
				final AxisAlignedBB axisalignedbb1 = getEntityBoundingBox();
				setEntityBoundingBox(axisalignedbb);
				y = stepEvent.getStepHeight();
				final List<AxisAlignedBB> list = world.getCollisionBoxes((EntityPlayerSP) (Object) this, getEntityBoundingBox().expand(d2, y, d4));
				AxisAlignedBB axisalignedbb2 = getEntityBoundingBox();
				final AxisAlignedBB axisalignedbb3 = axisalignedbb2.expand(d2, 0.0D, d4);
				double d8 = y;
				int j1 = 0;

				for (final int k1 = list.size(); j1 < k1; ++j1)
				{
					d8 = list.get(j1).calculateYOffset(axisalignedbb3, d8);
				}

				axisalignedbb2 = axisalignedbb2.offset(0.0D, d8, 0.0D);
				double d18 = d2;
				int l1 = 0;

				for (final int i2 = list.size(); l1 < i2; ++l1)
				{
					d18 = list.get(l1).calculateXOffset(axisalignedbb2, d18);
				}

				axisalignedbb2 = axisalignedbb2.offset(d18, 0.0D, 0.0D);
				double d19 = d4;
				int j2 = 0;

				for (final int k2 = list.size(); j2 < k2; ++j2)
				{
					d19 = list.get(j2).calculateZOffset(axisalignedbb2, d19);
				}

				axisalignedbb2 = axisalignedbb2.offset(0.0D, 0.0D, d19);
				AxisAlignedBB axisalignedbb4 = getEntityBoundingBox();
				double d20 = y;
				int l2 = 0;

				for (final int i3 = list.size(); l2 < i3; ++l2)
				{
					d20 = list.get(l2).calculateYOffset(axisalignedbb4, d20);
				}

				axisalignedbb4 = axisalignedbb4.offset(0.0D, d20, 0.0D);
				double d21 = d2;
				int j3 = 0;

				for (final int k3 = list.size(); j3 < k3; ++j3)
				{
					d21 = list.get(j3).calculateXOffset(axisalignedbb4, d21);
				}

				axisalignedbb4 = axisalignedbb4.offset(d21, 0.0D, 0.0D);
				double d22 = d4;
				int l3 = 0;

				for (final int i4 = list.size(); l3 < i4; ++l3)
				{
					d22 = list.get(l3).calculateZOffset(axisalignedbb4, d22);
				}

				axisalignedbb4 = axisalignedbb4.offset(0.0D, 0.0D, d22);
				final double d23 = d18 * d18 + d19 * d19;
				final double d9 = d21 * d21 + d22 * d22;

				if (d23 > d9)
				{
					x = d18;
					z = d19;
					y = -d8;
					setEntityBoundingBox(axisalignedbb2);
				}
				else
				{
					x = d21;
					z = d22;
					y = -d20;
					setEntityBoundingBox(axisalignedbb4);
				}

				int j4 = 0;

				for (final int k4 = list.size(); j4 < k4; ++j4)
				{
					y = list.get(j4).calculateYOffset(getEntityBoundingBox(), y);
				}

				setEntityBoundingBox(getEntityBoundingBox().offset(0.0D, y, 0.0D));

				if (d14 * d14 + d7 * d7 >= x * x + z * z)
				{
					x = d14;
					y = d6;
					z = d7;
					setEntityBoundingBox(axisalignedbb1);
				}
				else
				{
					LiquidBounce.eventManager.callEvent(new StepConfirmEvent());
				}
			}

			world.profiler.endSection();
			world.profiler.startSection("rest");
			resetPositionToBB();
			collidedHorizontally = d2 != x || d4 != z;
			collidedVertically = d3 != y;
			onGround = collidedVertically && d3 < 0.0D;
			collided = collidedHorizontally || collidedVertically;
			final int j6 = MathHelper.floor(posX);
			final int i1 = MathHelper.floor(posY - 0.20000000298023224D);
			final int k6 = MathHelper.floor(posZ);
			BlockPos blockpos = new BlockPos(j6, i1, k6);
			IBlockState iblockstate = world.getBlockState(blockpos);

			if (iblockstate.getMaterial() == Material.AIR)
			{
				final BlockPos blockpos1 = blockpos.down();
				final IBlockState iblockstate1 = world.getBlockState(blockpos1);
				final Block block1 = iblockstate1.getBlock();

				if (block1 instanceof BlockFence || block1 instanceof BlockWall || block1 instanceof BlockFenceGate)
				{
					iblockstate = iblockstate1;
					blockpos = blockpos1;
				}
			}

			updateFallState(y, onGround, iblockstate, blockpos);

			if (d2 != x)
			{
				motionX = 0.0D;
			}

			if (d4 != z)
			{
				motionZ = 0.0D;
			}

			final Block block = iblockstate.getBlock();

			if (d3 != y)
			{
				block.onLanded(world, (EntityPlayerSP) (Object) this);
			}

			if (canTriggerWalking() && (!onGround || !isSneaking() || !((Object) this instanceof EntityPlayer)) && !isRiding())
			{
				final double d15 = posX - d10;
				double d16 = posY - d11;
				final double d17 = posZ - d1;

				if (block != Blocks.LADDER)
				{
					d16 = 0.0D;
				}

				if (block != null && onGround)
				{
					block.onEntityWalk(world, blockpos, (EntityPlayerSP) (Object) this);
				}

				distanceWalkedModified = (float) ((double) distanceWalkedModified + (double) MathHelper.sqrt(d15 * d15 + d17 * d17) * 0.6D);
				distanceWalkedOnStepModified = (float) ((double) distanceWalkedOnStepModified + (double) MathHelper.sqrt(d15 * d15 + d16 * d16 + d17 * d17) * 0.6D);

				if (distanceWalkedOnStepModified > (float) nextStepDistance && iblockstate.getMaterial() != Material.AIR)
				{
					nextStepDistance = (int) distanceWalkedOnStepModified + 1;

					if (isInWater())
					{
						final Entity entity = isBeingRidden() && getControllingPassenger() != null ? getControllingPassenger() : (EntityPlayerSP) (Object) this;
						final float f = entity == (Object) this ? 0.35F : 0.4F;
						float f1 = MathHelper.sqrt(entity.motionX * entity.motionX * 0.20000000298023224D + entity.motionY * entity.motionY + entity.motionZ * entity.motionZ * 0.20000000298023224D) * f;

						if (f1 > 1.0F)
						{
							f1 = 1.0F;
						}

						playSound(getSwimSound(), f1, 1.0F + (rand.nextFloat() - rand.nextFloat()) * 0.4F);
					}
					else
					{
						playStepSound(blockpos, block);
					}
				}
				else if (distanceWalkedOnStepModified > nextFlap && makeFlySound() && iblockstate.getMaterial() == Material.AIR)
				{
					nextFlap = playFlySound(distanceWalkedOnStepModified);
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

			final boolean flag1 = isWet();

			if (world.isFlammableWithin(getEntityBoundingBox().shrink(0.001D)))
			{
				dealFireDamage(1);

				if (!flag1)
				{
					++fire;

					if (fire == 0)
					{
						setFire(8);
					}
				}
			}
			else if (fire <= 0)
			{
				fire = -getFireImmuneTicks();
			}

			if (flag1 && isBurning())
			{
				playSound(SoundEvents.ENTITY_GENERIC_EXTINGUISH_FIRE, 0.7F, 1.6F + (rand.nextFloat() - rand.nextFloat()) * 0.4F);
				fire = -getFireImmuneTicks();
			}

			world.profiler.endSection();
		}

		updateAutoJump((float) (posX - posX), (float) (posZ - posZ));
	}
}
