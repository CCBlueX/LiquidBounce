/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.entity;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.*;
import net.ccbluex.liquidbounce.features.module.modules.exploit.AntiHunger;
import net.ccbluex.liquidbounce.features.module.modules.exploit.PortalMenu;
import net.ccbluex.liquidbounce.features.module.modules.fun.Derp;
import net.ccbluex.liquidbounce.features.module.modules.movement.*;
import net.ccbluex.liquidbounce.features.module.modules.render.NoSwing;
import net.ccbluex.liquidbounce.features.module.modules.world.Scaffold;
import net.ccbluex.liquidbounce.utils.RotationUtils;
import net.ccbluex.liquidbounce.utils.extensions.MovementExtensionKt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.potion.Potion;
import net.minecraft.util.*;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.At.Shift;
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

    @Override
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

    private Sneak sneak;
    public float backup_rotationYaw = rotationYaw;
    public float backup_rotationPitch;
    private float backup_lastReportedYaw;
    private float backup_lastReportedPitch;

    @Inject(method = "onUpdateWalkingPlayer", at = @At("HEAD"))
    private void injectMotionEventPre(final CallbackInfo ci)
    {
        mc.mcProfiler.startSection("LiquidBounce-MotionEvent-PRE");
        LiquidBounce.eventManager.callEvent(new MotionEvent(EventState.PRE), true);
        mc.mcProfiler.endStartSection("onUpdateWalkingPlayer");
    }

    @SuppressWarnings("InvalidInjectorMethodSignature")
    @ModifyVariable(method = "onUpdateWalkingPlayer", at = @At(value = "STORE", ordinal = 0), ordinal = 0)
    private boolean applySprint(final boolean flag)
    {
        final InventoryMove inventoryMove = (InventoryMove) LiquidBounce.moduleManager.get(InventoryMove.class);
        sneak = (Sneak) LiquidBounce.moduleManager.get(Sneak.class);
        return flag && !(inventoryMove.getState() && inventoryMove.getAacAdditionProValue().get() || LiquidBounce.moduleManager.get(AntiHunger.class).getState() || sneak.getState() && (!MovementExtensionKt.isMoving(mc.thePlayer) || !sneak.stopMoveValue.get()) && "MineSecure".equalsIgnoreCase(sneak.modeValue.get()));
    }

    @ModifyVariable(method = "onUpdateWalkingPlayer", at = @At(value = "STORE", ordinal = 1), ordinal = 1)
    private boolean applySneak(final boolean flag1)
    {
        return sneak.getState() && !"Legit".equalsIgnoreCase(sneak.modeValue.get()) ? serverSneakState : flag1;
    }

    @Inject(method = "onUpdateWalkingPlayer", at = @At(value = "FIELD", target = "Lnet/minecraft/client/entity/EntityPlayerSP;posX:D", ordinal = 0))
    private void applyRotations(final CallbackInfo ci)
    {
        backup_rotationYaw = rotationYaw;
        backup_rotationPitch = rotationPitch;
        backup_lastReportedYaw = lastReportedYaw;
        backup_lastReportedPitch = lastReportedPitch;

        lastReportedYaw = RotationUtils.serverRotation.getYaw();
        lastReportedPitch = RotationUtils.serverRotation.getPitch();

        final Derp derp = (Derp) LiquidBounce.moduleManager.get(Derp.class);
        if (derp.getState())
        {
            final float[] rot = derp.getRotation();
            rotationYaw = rot[0];
            rotationPitch = rot[1];
        }

        if (RotationUtils.targetRotation != null)
        {
            rotationYaw = RotationUtils.targetRotation.getYaw();
            rotationPitch = RotationUtils.targetRotation.getPitch();
        }
    }

    @Inject(method = "onUpdateWalkingPlayer", at = @At(value = "FIELD", target = "Lnet/minecraft/client/entity/EntityPlayerSP;positionUpdateTicks:I", ordinal = 1))
    private void restoreRotations(final CallbackInfo ci)
    {
        rotationYaw = backup_rotationYaw;
        rotationPitch = backup_rotationPitch;
        lastReportedYaw = backup_lastReportedYaw;
        lastReportedPitch = backup_lastReportedPitch;
    }

    @Inject(method = "onUpdateWalkingPlayer", at = @At("TAIL"))
    private void injectMotionEventPost(final CallbackInfo ci)
    {
        mc.mcProfiler.endStartSection("LiquidBounce-MotionEvent-POST");
        LiquidBounce.eventManager.callEvent(new MotionEvent(EventState.POST), true);
        mc.mcProfiler.endSection();
    }

    @Inject(method = "swingItem", at = @At("HEAD"), cancellable = true)
    private void injectNoSwing(final CallbackInfo callbackInfo)
    {
        final NoSwing noSwing = (NoSwing) LiquidBounce.moduleManager.get(NoSwing.class);

        if (noSwing.getState())
        {
            if (!noSwing.getServerSideValue().get())
                sendQueue.addToSendQueue(new C0APacketAnimation());

            callbackInfo.cancel();
        }
    }

    @Inject(method = "pushOutOfBlocks", at = @At("HEAD"), cancellable = true)
    private void handlePushOutEvent(final CallbackInfoReturnable<? super Boolean> callbackInfoReturnable)
    {
        final PushOutEvent event = new PushOutEvent();
        if (noClip)
            event.cancelEvent();
        LiquidBounce.eventManager.callEvent(event);

        if (event.isCancelled())
            callbackInfoReturnable.setReturnValue(false);
    }

    private Sprint sprint;

    @Inject(method = "onLivingUpdate", at = @At("HEAD"))
    private void injectUpdateEvent(CallbackInfo ci)
    {
        worldObj.theProfiler.startSection("LiquidBounce-UpdateEvent");
        LiquidBounce.eventManager.callEvent(new UpdateEvent(), true);
        worldObj.theProfiler.endSection();
        sprint = (Sprint) LiquidBounce.moduleManager.get(Sprint.class);
    }

    @Redirect(method = "onLivingUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiScreen;doesGuiPauseGame()Z", ordinal = 0))
    private boolean injectPortalMenu(final GuiScreen instance)
    {
        return instance.doesGuiPauseGame() || LiquidBounce.moduleManager.get(PortalMenu.class).getState();
    }

    @ModifyVariable(method = "onLivingUpdate", at = @At(value = "FIELD", target = "Lnet/minecraft/client/entity/EntityPlayerSP;onGround:Z", shift = Shift.BEFORE, ordinal = 0), ordinal = 3)
    private boolean sprintFood(final boolean flag3)
    {
        return flag3 || !sprint.getFoodValue().get();
    }

    @Inject(method = "onLivingUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;isSprinting()Z", shift = Shift.BEFORE, ordinal = 2))
    private void injectSprintDisable(final CallbackInfo ci)
    {
        final Scaffold scaffold = (Scaffold) LiquidBounce.moduleManager.get(Scaffold.class);
        final boolean scaffoldSprintCheck = scaffold.getState() && !scaffold.getMovementSprintValue().get();
        final boolean sprintDirectionCheck = sprint.getState()
                && sprint.getCheckServerSide().get()
                && (onGround || !sprint.getCheckServerSideGround().get())
                && !sprint.getAllDirectionsValue().get()
                && RotationUtils.Companion.getRotationDifference(RotationUtils.Companion.getClientRotation()) > 30;
        if (scaffoldSprintCheck || sprintDirectionCheck)
            setSprinting(false);
    }

    @Redirect(method = "onLivingUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;isPotionActive(Lnet/minecraft/potion/Potion;)Z", ordinal = 1))
    private boolean injectAntiBlindCompatibility(final EntityPlayerSP instance, final Potion potion)
    {
        // isPotionActive(Potion) is tampered by AntiBlind injection.
        // Thus, we should us e isPotionActive(int) here instead to bypass AntiCheat "Blind sprint" detection
        return instance.isPotionActive(potion.id);
    }

    @Redirect(method = "onLivingUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;isUsingItem()Z", ordinal = 2))
    private boolean injectNoSlowSprint(final EntityPlayerSP instance)
    {
        return instance.isUsingItem() && !LiquidBounce.moduleManager.get(NoSlow.class).getState();
    }

    @Redirect(method = "onLivingUpdate", at = @At(value = "FIELD", target = "Lnet/minecraft/util/MovementInput;moveForward:F", ordinal = 4))
    private float injectSprintAllDirection(final MovementInput instance)
    {
        return sprint.getState() && sprint.getAllDirectionsValue().get() ? Float.MAX_VALUE : instance.moveForward;
    }
}
