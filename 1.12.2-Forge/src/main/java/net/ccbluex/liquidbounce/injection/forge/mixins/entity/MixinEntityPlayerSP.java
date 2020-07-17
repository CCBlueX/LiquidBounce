/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.entity;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.*;
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
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.util.*;
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

import java.util.Arrays;
import java.util.List;

@Mixin(EntityPlayerSP.class)
@SideOnly(Side.CLIENT)
public abstract class MixinEntityPlayerSP extends MixinAbstractClientPlayer {

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

    @Shadow
    public abstract boolean isSneaking();

    @Shadow
    protected abstract boolean isCurrentViewEntity();

    @Shadow
    public abstract void closeScreen();

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
    public void onUpdateWalkingPlayer() {
        LiquidBounce.eventManager.callEvent(new MotionEvent(EventState.PRE));

        final InventoryMove inventoryMove = (InventoryMove) LiquidBounce.moduleManager.getModule(InventoryMove.class);
        final Sneak sneak = (Sneak) LiquidBounce.moduleManager.getModule(Sneak.class);
        final boolean fakeSprint = (inventoryMove.getState() && inventoryMove.getAacAdditionProValue().get()) || LiquidBounce.moduleManager.getModule(AntiHunger.class).getState() || (sneak.getState() && (!MovementUtils.isMoving() || !sneak.stopMoveValue.get()) && sneak.modeValue.get().equalsIgnoreCase("MineSecure"));

        boolean clientSprintState = this.isSprinting() && !fakeSprint;

        if (clientSprintState != this.serverSprintState) {
            if (clientSprintState) {
                this.connection.sendPacket(new CPacketEntityAction((EntityPlayerSP) (Object) this, CPacketEntityAction.Action.START_SPRINTING));
            } else {
                this.connection.sendPacket(new CPacketEntityAction((EntityPlayerSP) (Object) this, CPacketEntityAction.Action.STOP_SPRINTING));
            }

            this.serverSprintState = clientSprintState;
        }

        boolean flag1 = this.isSneaking();

        if (flag1 != this.serverSneakState && (!sneak.getState() || sneak.modeValue.get().equalsIgnoreCase("Legit"))) {
            if (flag1) {
                this.connection.sendPacket(new CPacketEntityAction((EntityPlayerSP) (Object) this, CPacketEntityAction.Action.START_SNEAKING));
            } else {
                this.connection.sendPacket(new CPacketEntityAction((EntityPlayerSP) (Object) this, CPacketEntityAction.Action.STOP_SNEAKING));
            }

            this.serverSneakState = flag1;
        }

        if (this.isCurrentViewEntity()) {
            float yaw = rotationYaw;
            float pitch = rotationPitch;
            float lastReportedYaw = RotationUtils.serverRotation.getYaw();
            float lastReportedPitch = RotationUtils.serverRotation.getPitch();

            final Derp derp = (Derp) LiquidBounce.moduleManager.getModule(Derp.class);
            if (derp.getState()) {
                float[] rot = derp.getRotation();
                yaw = rot[0];
                pitch = rot[1];
            }

            if (RotationUtils.targetRotation != null) {
                yaw = RotationUtils.targetRotation.getYaw();
                pitch = RotationUtils.targetRotation.getPitch();
            }

            AxisAlignedBB axisalignedbb = this.getEntityBoundingBox();
            double xDiff = this.posX - this.lastReportedPosX;
            double yDiff = axisalignedbb.minY - this.lastReportedPosY;
            double zDiff = this.posZ - this.lastReportedPosZ;
            double yawDiff = yaw - lastReportedYaw;
            double pitchDiff = pitch - lastReportedPitch;

            ++this.positionUpdateTicks;

            boolean flag2 = xDiff * xDiff + yDiff * yDiff + zDiff * zDiff > 9.0E-4D || this.positionUpdateTicks >= 20;
            boolean flag3 = yawDiff != 0.0D || pitchDiff != 0.0D;

            if (this.isRiding()) {
                this.connection.sendPacket(new CPacketPlayer.PositionRotation(this.motionX, -999.0D, this.motionZ, this.rotationYaw, this.rotationPitch, this.onGround));
                flag2 = false;
            } else if (flag2 && flag3) {
                this.connection.sendPacket(new CPacketPlayer.PositionRotation(this.posX, axisalignedbb.minY, this.posZ, this.rotationYaw, this.rotationPitch, this.onGround));
            } else if (flag2) {
                this.connection.sendPacket(new CPacketPlayer.Position(this.posX, axisalignedbb.minY, this.posZ, this.onGround));
            } else if (flag3) {
                this.connection.sendPacket(new CPacketPlayer.Rotation(this.rotationYaw, this.rotationPitch, this.onGround));
            } else if (this.prevOnGround != this.onGround) {
                this.connection.sendPacket(new CPacketPlayer(this.onGround));
            }

            if (flag2) {
                this.lastReportedPosX = this.posX;
                this.lastReportedPosY = axisalignedbb.minY;
                this.lastReportedPosZ = this.posZ;
                this.positionUpdateTicks = 0;
            }

            if (flag3) {
                this.lastReportedYaw = this.rotationYaw;
                this.lastReportedPitch = this.rotationPitch;
            }

            this.prevOnGround = this.onGround;
            this.autoJumpEnabled = this.mc.gameSettings.autoJump;
        }

        LiquidBounce.eventManager.callEvent(new MotionEvent(EventState.POST));
    }

    @Inject(method = "swingArm", at = @At("HEAD"), cancellable = true)
    private void swingItem(EnumHand hand, CallbackInfo callbackInfo) {
        final NoSwing noSwing = (NoSwing) LiquidBounce.moduleManager.getModule(NoSwing.class);

        if (noSwing.getState()) {
            callbackInfo.cancel();

            if (!noSwing.getServerSideValue().get())
                this.connection.sendPacket(new CPacketAnimation(hand));
        }
    }

    @Inject(method = "pushOutOfBlocks", at = @At("HEAD"), cancellable = true)
    private void onPushOutOfBlocks(CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        PushOutEvent event = new PushOutEvent();
        if (this.noClip) event.cancelEvent();
        LiquidBounce.eventManager.callEvent(event);

        if (event.isCancelled())
            callbackInfoReturnable.setReturnValue(false);
    }

    /**
     * @author CCBlueX (superblaubeere27)
     */
    @Overwrite
    public void onLivingUpdate() {
        try {
            LiquidBounce.eventManager.callEvent(new UpdateEvent());
        } catch (Throwable e) {
            e.printStackTrace();
        }

        ++this.sprintingTicksLeft;

        if (this.sprintToggleTimer > 0) {
            --this.sprintToggleTimer;
        }

        this.prevTimeInPortal = this.timeInPortal;

        if (this.inPortal) {
            if (this.mc.currentScreen != null && !this.mc.currentScreen.doesGuiPauseGame() && !LiquidBounce.moduleManager.getModule(PortalMenu.class).getState()) {
                if (this.mc.currentScreen instanceof GuiContainer) {
                    this.closeScreen();
                }

                this.mc.displayGuiScreen(null);
            }

            if (this.timeInPortal == 0.0F) {
                this.mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.BLOCK_PORTAL_TRIGGER, this.rand.nextFloat() * 0.4F + 0.8F));
            }

            this.timeInPortal += 0.0125F;

            if (this.timeInPortal >= 1.0F) {
                this.timeInPortal = 1.0F;
            }

            this.inPortal = false;
        } else if (this.isPotionActive(MobEffects.NAUSEA) && this.getActivePotionEffect(MobEffects.NAUSEA).getDuration() > 60) {
            this.timeInPortal += 0.006666667F;

            if (this.timeInPortal > 1.0F) {
                this.timeInPortal = 1.0F;
            }
        } else {
            if (this.timeInPortal > 0.0F) {
                this.timeInPortal -= 0.05F;
            }

            if (this.timeInPortal < 0.0F) {
                this.timeInPortal = 0.0F;
            }
        }

        if (this.timeUntilPortal > 0) {
            --this.timeUntilPortal;
        }

        boolean flag = this.movementInput.jump;
        boolean flag1 = this.movementInput.sneak;
        float f = 0.8F;
        boolean flag2 = this.movementInput.moveForward >= 0.8F;
        this.movementInput.updatePlayerMoveState();
        final NoSlow noSlow = (NoSlow) LiquidBounce.moduleManager.getModule(NoSlow.class);
        final KillAura killAura = (KillAura) LiquidBounce.moduleManager.getModule(KillAura.class);

        ForgeHooksClient.onInputUpdate((EntityPlayerSP) (Object) this, this.movementInput);
        this.mc.getTutorial().handleMovement(this.movementInput);

        if (this.isHandActive() || (getHeldItem(EnumHand.MAIN_HAND).getItem() instanceof ItemSword && killAura.getBlockingStatus()) && !this.isRiding()) {
            final SlowDownEvent slowDownEvent = new SlowDownEvent(0.2F, 0.2F);
            LiquidBounce.eventManager.callEvent(slowDownEvent);
            this.movementInput.moveStrafe *= slowDownEvent.getStrafe();
            this.movementInput.moveForward *= slowDownEvent.getForward();
            this.sprintToggleTimer = 0;
        }

        boolean flag3 = false;

        if (this.autoJumpTime > 0) {
            --this.autoJumpTime;
            flag3 = true;
            this.movementInput.jump = true;
        }

        AxisAlignedBB axisalignedbb = this.getEntityBoundingBox();
        PlayerSPPushOutOfBlocksEvent event = new PlayerSPPushOutOfBlocksEvent((EntityPlayerSP) (Object) this, axisalignedbb);
        if (!MinecraftForge.EVENT_BUS.post(event)) {
            axisalignedbb = event.getEntityBoundingBox();
            this.pushOutOfBlocks(this.posX - (double) this.width * 0.35D, axisalignedbb.minY + 0.5D, this.posZ + (double) this.width * 0.35D);
            this.pushOutOfBlocks(this.posX - (double) this.width * 0.35D, axisalignedbb.minY + 0.5D, this.posZ - (double) this.width * 0.35D);
            this.pushOutOfBlocks(this.posX + (double) this.width * 0.35D, axisalignedbb.minY + 0.5D, this.posZ - (double) this.width * 0.35D);
            this.pushOutOfBlocks(this.posX + (double) this.width * 0.35D, axisalignedbb.minY + 0.5D, this.posZ + (double) this.width * 0.35D);
        }
        final Sprint sprint = (Sprint) LiquidBounce.moduleManager.getModule(Sprint.class);

        boolean flag4 = !sprint.foodValue.get() || (float) this.getFoodStats().getFoodLevel() > 6.0F || this.capabilities.allowFlying;

        if (this.onGround && !flag1 && !flag2 && this.movementInput.moveForward >= 0.8F && !this.isSprinting() && flag4 && !this.isHandActive() && !this.isPotionActive(MobEffects.BLINDNESS)) {
            if (this.sprintToggleTimer <= 0 && !this.mc.gameSettings.keyBindSprint.isKeyDown()) {
                this.sprintToggleTimer = 7;
            } else {
                this.setSprinting(true);
            }
        }

        if (!this.isSprinting() && this.movementInput.moveForward >= 0.8F && flag4 && (noSlow.getState() || !this.isHandActive()) && !this.isPotionActive(MobEffects.BLINDNESS) && this.mc.gameSettings.keyBindSprint.isKeyDown()) {
            this.setSprinting(true);
        }

        final Scaffold scaffold = (Scaffold) LiquidBounce.moduleManager.getModule(Scaffold.class);

        if ((scaffold.getState() && !scaffold.sprintValue.get()) || (sprint.getState() && sprint.checkServerSide.get() && (onGround || !sprint.checkServerSideGround.get()) && !sprint.allDirectionsValue.get() && RotationUtils.targetRotation != null && RotationUtils.getRotationDifference(new Rotation(mc.player.rotationYaw, mc.player.rotationPitch)) > 30))
            this.setSprinting(false);

        if (this.isSprinting() && (this.movementInput.moveForward < 0.8F || this.collidedHorizontally || !flag4)) {
            this.setSprinting(false);
        }

        if (this.capabilities.allowFlying) {
            if (this.mc.playerController.isSpectatorMode()) {
                if (!this.capabilities.isFlying) {
                    this.capabilities.isFlying = true;
                    this.sendPlayerAbilities();
                }
            } else if (!flag && this.movementInput.jump && !flag3) {
                if (this.flyToggleTimer == 0) {
                    this.flyToggleTimer = 7;
                } else {
                    this.capabilities.isFlying = !this.capabilities.isFlying;
                    this.sendPlayerAbilities();
                    this.flyToggleTimer = 0;
                }
            }
        }

        if (this.movementInput.jump && !flag && !this.onGround && this.motionY < 0.0D && !this.isElytraFlying() && !this.capabilities.isFlying) {
            ItemStack itemstack = this.getItemStackFromSlot(EntityEquipmentSlot.CHEST);

            if (itemstack.getItem() == Items.ELYTRA && ItemElytra.isUsable(itemstack)) {
                this.connection.sendPacket(new CPacketEntityAction((EntityPlayerSP) (Object) this, CPacketEntityAction.Action.START_FALL_FLYING));
            }
        }

        this.wasFallFlying = this.isElytraFlying();

        if (this.capabilities.isFlying && this.isCurrentViewEntity()) {
            if (this.movementInput.sneak) {
                this.movementInput.moveStrafe = (float) ((double) this.movementInput.moveStrafe / 0.3D);
                this.movementInput.moveForward = (float) ((double) this.movementInput.moveForward / 0.3D);
                this.motionY -= this.capabilities.getFlySpeed() * 3.0F;
            }

            if (this.movementInput.jump) {
                this.motionY += this.capabilities.getFlySpeed() * 3.0F;
            }
        }

        if (this.isRidingHorse()) {
            IJumpingMount ijumpingmount = (IJumpingMount) this.getRidingEntity();

            if (this.horseJumpPowerCounter < 0) {
                ++this.horseJumpPowerCounter;

                if (this.horseJumpPowerCounter == 0) {
                    this.horseJumpPower = 0.0F;
                }
            }

            if (flag && !this.movementInput.jump) {
                this.horseJumpPowerCounter = -10;
                ijumpingmount.setJumpPower(MathHelper.floor(this.getHorseJumpPower() * 100.0F));
                this.sendHorseJump();
            } else if (!flag && this.movementInput.jump) {
                this.horseJumpPowerCounter = 0;
                this.horseJumpPower = 0.0F;
            } else if (flag) {
                ++this.horseJumpPowerCounter;

                if (this.horseJumpPowerCounter < 10) {
                    this.horseJumpPower = (float) this.horseJumpPowerCounter * 0.1F;
                } else {
                    this.horseJumpPower = 0.8F + 2.0F / (float) (this.horseJumpPowerCounter - 9) * 0.1F;
                }
            }
        } else {
            this.horseJumpPower = 0.0F;
        }

        super.onLivingUpdate();

        if (this.onGround && this.capabilities.isFlying && !this.mc.playerController.isSpectatorMode()) {
            this.capabilities.isFlying = false;
            this.sendPlayerAbilities();
        }
    }

    /**
     * @author CCBlueX (superblaubeere27)
     */
    @Overwrite
    public void move(MoverType type, double x, double y, double z) {
        MoveEvent moveEvent = new MoveEvent(x, y, z);
        LiquidBounce.eventManager.callEvent(moveEvent);

        if (moveEvent.isCancelled())
            return;

        x = moveEvent.getX();
        y = moveEvent.getY();
        z = moveEvent.getZ();

        if (this.noClip) {
            this.setEntityBoundingBox(this.getEntityBoundingBox().offset(x, y, z));
            this.resetPositionToBB();
        } else {
            if (type == MoverType.PISTON) {
                long i = this.world.getTotalWorldTime();

                if (i != this.pistonDeltasGameTime) {
                    Arrays.fill(this.pistonDeltas, 0.0D);
                    this.pistonDeltasGameTime = i;
                }

                if (x != 0.0D) {
                    int j = EnumFacing.Axis.X.ordinal();
                    double d0 = MathHelper.clamp(x + this.pistonDeltas[j], -0.51D, 0.51D);
                    x = d0 - this.pistonDeltas[j];
                    this.pistonDeltas[j] = d0;

                    if (Math.abs(x) <= 9.999999747378752E-6D) {
                        return;
                    }
                } else if (y != 0.0D) {
                    int l4 = EnumFacing.Axis.Y.ordinal();
                    double d12 = MathHelper.clamp(y + this.pistonDeltas[l4], -0.51D, 0.51D);
                    y = d12 - this.pistonDeltas[l4];
                    this.pistonDeltas[l4] = d12;

                    if (Math.abs(y) <= 9.999999747378752E-6D) {
                        return;
                    }
                } else {
                    if (z == 0.0D) {
                        return;
                    }

                    int i5 = EnumFacing.Axis.Z.ordinal();
                    double d13 = MathHelper.clamp(z + this.pistonDeltas[i5], -0.51D, 0.51D);
                    z = d13 - this.pistonDeltas[i5];
                    this.pistonDeltas[i5] = d13;

                    if (Math.abs(z) <= 9.999999747378752E-6D) {
                        return;
                    }
                }
            }

            this.world.profiler.startSection("move");
            double d10 = this.posX;
            double d11 = this.posY;
            double d1 = this.posZ;

            if (this.isInWeb) {
                this.isInWeb = false;
                x *= 0.25D;
                y *= 0.05000000074505806D;
                z *= 0.25D;
                this.motionX = 0.0D;
                this.motionY = 0.0D;
                this.motionZ = 0.0D;
            }

            double d2 = x;
            double d3 = y;
            double d4 = z;

            //noinspection ConstantConditions
            if ((type == MoverType.SELF || type == MoverType.PLAYER) && (this.onGround && this.isSneaking() || moveEvent.isSafeWalk()) && ((Object) this) instanceof EntityPlayer) {
                for (double d5 = 0.05D; x != 0.0D && this.world.getCollisionBoxes((EntityPlayerSP) (Object) this, this.getEntityBoundingBox().offset(x, -this.stepHeight, 0.0D)).isEmpty(); d2 = x) {
                    if (x < 0.05D && x >= -0.05D) {
                        x = 0.0D;
                    } else if (x > 0.0D) {
                        x -= 0.05D;
                    } else {
                        x += 0.05D;
                    }
                }

                for (; z != 0.0D && this.world.getCollisionBoxes((EntityPlayerSP) (Object) this, this.getEntityBoundingBox().offset(0.0D, -this.stepHeight, z)).isEmpty(); d4 = z) {
                    if (z < 0.05D && z >= -0.05D) {
                        z = 0.0D;
                    } else if (z > 0.0D) {
                        z -= 0.05D;
                    } else {
                        z += 0.05D;
                    }
                }

                for (; x != 0.0D && z != 0.0D && this.world.getCollisionBoxes((EntityPlayerSP) (Object) this, this.getEntityBoundingBox().offset(x, -this.stepHeight, z)).isEmpty(); d4 = z) {
                    if (x < 0.05D && x >= -0.05D) {
                        x = 0.0D;
                    } else if (x > 0.0D) {
                        x -= 0.05D;
                    } else {
                        x += 0.05D;
                    }

                    d2 = x;

                    if (z < 0.05D && z >= -0.05D) {
                        z = 0.0D;
                    } else if (z > 0.0D) {
                        z -= 0.05D;
                    } else {
                        z += 0.05D;
                    }
                }
            }

            List<AxisAlignedBB> list1 = this.world.getCollisionBoxes((EntityPlayerSP) (Object) this, this.getEntityBoundingBox().expand(x, y, z));
            AxisAlignedBB axisalignedbb = this.getEntityBoundingBox();

            if (y != 0.0D) {
                int k = 0;

                for (int l = list1.size(); k < l; ++k) {
                    y = list1.get(k).calculateYOffset(this.getEntityBoundingBox(), y);
                }

                this.setEntityBoundingBox(this.getEntityBoundingBox().offset(0.0D, y, 0.0D));
            }

            if (x != 0.0D) {
                int j5 = 0;

                for (int l5 = list1.size(); j5 < l5; ++j5) {
                    x = list1.get(j5).calculateXOffset(this.getEntityBoundingBox(), x);
                }

                if (x != 0.0D) {
                    this.setEntityBoundingBox(this.getEntityBoundingBox().offset(x, 0.0D, 0.0D));
                }
            }

            if (z != 0.0D) {
                int k5 = 0;

                for (int i6 = list1.size(); k5 < i6; ++k5) {
                    z = list1.get(k5).calculateZOffset(this.getEntityBoundingBox(), z);
                }

                if (z != 0.0D) {
                    this.setEntityBoundingBox(this.getEntityBoundingBox().offset(0.0D, 0.0D, z));
                }
            }

            boolean flag = this.onGround || d3 != y && d3 < 0.0D;

            if (this.stepHeight > 0.0F && flag && (d2 != x || d4 != z)) {
                StepEvent stepEvent = new StepEvent(this.stepHeight);
                LiquidBounce.eventManager.callEvent(stepEvent);

                double d14 = x;
                double d6 = y;
                double d7 = z;
                AxisAlignedBB axisalignedbb1 = this.getEntityBoundingBox();
                this.setEntityBoundingBox(axisalignedbb);
                y = stepEvent.getStepHeight();
                List<AxisAlignedBB> list = this.world.getCollisionBoxes((EntityPlayerSP) (Object) this, this.getEntityBoundingBox().expand(d2, y, d4));
                AxisAlignedBB axisalignedbb2 = this.getEntityBoundingBox();
                AxisAlignedBB axisalignedbb3 = axisalignedbb2.expand(d2, 0.0D, d4);
                double d8 = y;
                int j1 = 0;

                for (int k1 = list.size(); j1 < k1; ++j1) {
                    d8 = list.get(j1).calculateYOffset(axisalignedbb3, d8);
                }

                axisalignedbb2 = axisalignedbb2.offset(0.0D, d8, 0.0D);
                double d18 = d2;
                int l1 = 0;

                for (int i2 = list.size(); l1 < i2; ++l1) {
                    d18 = list.get(l1).calculateXOffset(axisalignedbb2, d18);
                }

                axisalignedbb2 = axisalignedbb2.offset(d18, 0.0D, 0.0D);
                double d19 = d4;
                int j2 = 0;

                for (int k2 = list.size(); j2 < k2; ++j2) {
                    d19 = list.get(j2).calculateZOffset(axisalignedbb2, d19);
                }

                axisalignedbb2 = axisalignedbb2.offset(0.0D, 0.0D, d19);
                AxisAlignedBB axisalignedbb4 = this.getEntityBoundingBox();
                double d20 = y;
                int l2 = 0;

                for (int i3 = list.size(); l2 < i3; ++l2) {
                    d20 = list.get(l2).calculateYOffset(axisalignedbb4, d20);
                }

                axisalignedbb4 = axisalignedbb4.offset(0.0D, d20, 0.0D);
                double d21 = d2;
                int j3 = 0;

                for (int k3 = list.size(); j3 < k3; ++j3) {
                    d21 = list.get(j3).calculateXOffset(axisalignedbb4, d21);
                }

                axisalignedbb4 = axisalignedbb4.offset(d21, 0.0D, 0.0D);
                double d22 = d4;
                int l3 = 0;

                for (int i4 = list.size(); l3 < i4; ++l3) {
                    d22 = list.get(l3).calculateZOffset(axisalignedbb4, d22);
                }

                axisalignedbb4 = axisalignedbb4.offset(0.0D, 0.0D, d22);
                double d23 = d18 * d18 + d19 * d19;
                double d9 = d21 * d21 + d22 * d22;

                if (d23 > d9) {
                    x = d18;
                    z = d19;
                    y = -d8;
                    this.setEntityBoundingBox(axisalignedbb2);
                } else {
                    x = d21;
                    z = d22;
                    y = -d20;
                    this.setEntityBoundingBox(axisalignedbb4);
                }

                int j4 = 0;

                for (int k4 = list.size(); j4 < k4; ++j4) {
                    y = list.get(j4).calculateYOffset(this.getEntityBoundingBox(), y);
                }

                this.setEntityBoundingBox(this.getEntityBoundingBox().offset(0.0D, y, 0.0D));

                if (d14 * d14 + d7 * d7 >= x * x + z * z) {
                    x = d14;
                    y = d6;
                    z = d7;
                    this.setEntityBoundingBox(axisalignedbb1);
                } else {
                    LiquidBounce.eventManager.callEvent(new StepConfirmEvent());
                }
            }

            this.world.profiler.endSection();
            this.world.profiler.startSection("rest");
            this.resetPositionToBB();
            this.collidedHorizontally = d2 != x || d4 != z;
            this.collidedVertically = d3 != y;
            this.onGround = this.collidedVertically && d3 < 0.0D;
            this.collided = this.collidedHorizontally || this.collidedVertically;
            int j6 = MathHelper.floor(this.posX);
            int i1 = MathHelper.floor(this.posY - 0.20000000298023224D);
            int k6 = MathHelper.floor(this.posZ);
            BlockPos blockpos = new BlockPos(j6, i1, k6);
            IBlockState iblockstate = this.world.getBlockState(blockpos);

            if (iblockstate.getMaterial() == Material.AIR) {
                BlockPos blockpos1 = blockpos.down();
                IBlockState iblockstate1 = this.world.getBlockState(blockpos1);
                Block block1 = iblockstate1.getBlock();

                if (block1 instanceof BlockFence || block1 instanceof BlockWall || block1 instanceof BlockFenceGate) {
                    iblockstate = iblockstate1;
                    blockpos = blockpos1;
                }
            }

            this.updateFallState(y, this.onGround, iblockstate, blockpos);

            if (d2 != x) {
                this.motionX = 0.0D;
            }

            if (d4 != z) {
                this.motionZ = 0.0D;
            }

            Block block = iblockstate.getBlock();

            if (d3 != y) {
                block.onLanded(this.world, (EntityPlayerSP) (Object) this);
            }

            if (this.canTriggerWalking() && (!this.onGround || !this.isSneaking() || !(((Object) this) instanceof EntityPlayer)) && !this.isRiding()) {
                double d15 = this.posX - d10;
                double d16 = this.posY - d11;
                double d17 = this.posZ - d1;

                if (block != Blocks.LADDER) {
                    d16 = 0.0D;
                }

                if (block != null && this.onGround) {
                    block.onEntityWalk(this.world, blockpos, (EntityPlayerSP) (Object) this);
                }

                this.distanceWalkedModified = (float) ((double) this.distanceWalkedModified + (double) MathHelper.sqrt(d15 * d15 + d17 * d17) * 0.6D);
                this.distanceWalkedOnStepModified = (float) ((double) this.distanceWalkedOnStepModified + (double) MathHelper.sqrt(d15 * d15 + d16 * d16 + d17 * d17) * 0.6D);

                if (this.distanceWalkedOnStepModified > (float) this.nextStepDistance && iblockstate.getMaterial() != Material.AIR) {
                    this.nextStepDistance = (int) this.distanceWalkedOnStepModified + 1;

                    if (this.isInWater()) {
                        Entity entity = this.isBeingRidden() && this.getControllingPassenger() != null ? this.getControllingPassenger() : (EntityPlayerSP) (Object) this;
                        float f = entity == (Object) this ? 0.35F : 0.4F;
                        float f1 = MathHelper.sqrt(entity.motionX * entity.motionX * 0.20000000298023224D + entity.motionY * entity.motionY + entity.motionZ * entity.motionZ * 0.20000000298023224D) * f;

                        if (f1 > 1.0F) {
                            f1 = 1.0F;
                        }

                        this.playSound(this.getSwimSound(), f1, 1.0F + (this.rand.nextFloat() - this.rand.nextFloat()) * 0.4F);
                    } else {
                        this.playStepSound(blockpos, block);
                    }
                } else if (this.distanceWalkedOnStepModified > this.nextFlap && this.makeFlySound() && iblockstate.getMaterial() == Material.AIR) {
                    this.nextFlap = this.playFlySound(this.distanceWalkedOnStepModified);
                }
            }

            try {
                this.doBlockCollisions();
            } catch (Throwable throwable) {
                CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Checking entity block collision");
                CrashReportCategory crashreportcategory = crashreport.makeCategory("Entity being checked for collision");
                this.addEntityCrashInfo(crashreportcategory);
                throw new ReportedException(crashreport);
            }

            boolean flag1 = this.isWet();

            if (this.world.isFlammableWithin(this.getEntityBoundingBox().shrink(0.001D))) {
                this.dealFireDamage(1);

                if (!flag1) {
                    ++this.fire;

                    if (this.fire == 0) {
                        this.setFire(8);
                    }
                }
            } else if (this.fire <= 0) {
                this.fire = -this.getFireImmuneTicks();
            }

            if (flag1 && this.isBurning()) {
                this.playSound(SoundEvents.ENTITY_GENERIC_EXTINGUISH_FIRE, 0.7F, 1.6F + (this.rand.nextFloat() - this.rand.nextFloat()) * 0.4F);
                this.fire = -this.getFireImmuneTicks();
            }

            this.world.profiler.endSection();
        }

        this.updateAutoJump((float) (this.posX - this.posX), (float) (this.posZ - this.posZ));
    }
}

