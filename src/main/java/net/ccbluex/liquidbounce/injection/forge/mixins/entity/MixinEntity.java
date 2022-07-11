/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.entity;

import java.util.List;
import java.util.Random;
import java.util.UUID;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.MoveEvent;
import net.ccbluex.liquidbounce.event.StepConfirmEvent;
import net.ccbluex.liquidbounce.event.StepEvent;
import net.ccbluex.liquidbounce.event.StrafeEvent;
import net.ccbluex.liquidbounce.features.module.modules.combat.HitBox;
import net.ccbluex.liquidbounce.features.module.modules.exploit.NoPitchLimit;
import net.ccbluex.liquidbounce.features.module.modules.movement.Step;
import net.ccbluex.liquidbounce.features.module.modules.render.ItemPhysics;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
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
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(Entity.class)
@SideOnly(Side.CLIENT)
public abstract class MixinEntity
{
    @Shadow
    public Entity ridingEntity;

    @Shadow
    public World worldObj;

    @Shadow
    public double posX;

    @Shadow
    public double posY;

    @Shadow
    public double posZ;

    @Shadow
    public double motionX;

    @Shadow
    public double motionY;

    @Shadow
    public double motionZ;

    @Shadow
    public float rotationPitch;

    @Shadow
    public float rotationYaw;

    @Shadow
    public boolean onGround;

    @Shadow
    public boolean noClip;

    @Shadow
    public boolean isAirBorne;

    @Shadow
    public abstract boolean isSprinting();

    @Shadow
    public abstract AxisAlignedBB getEntityBoundingBox();

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
    public boolean isDead;

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
    private void injectHitbox(final CallbackInfoReturnable<? super Float> callbackInfoReturnable)
    {
        final HitBox hitBox = (HitBox) LiquidBounce.moduleManager.get(HitBox.class);

        if (hitBox.getState())
            callbackInfoReturnable.setReturnValue(0.1F + hitBox.getSizeValue().get());
    }

    @Inject(method = "setAngles", at = @At("HEAD"), cancellable = true)
    private void injectNoPitchLimit(final float yaw, final float pitch, final CallbackInfo callbackInfo)
    {
        // NoPitchLimit
        if (LiquidBounce.moduleManager.get(NoPitchLimit.class).getState())
        {
            final float rotYaw = rotationYaw;
            final float rotPitch = rotationPitch;

            rotationYaw += yaw * 0.15D;
            rotationPitch -= pitch * 0.15D;
            prevRotationPitch += rotationPitch - rotPitch;
            prevRotationYaw += rotationYaw - rotYaw;

            callbackInfo.cancel();
        }
    }

    @Inject(method = "moveFlying", at = @At("HEAD"), cancellable = true)
    private void handleStrafeEvent(final float strafe, final float forward, final float friction, final CallbackInfo callbackInfo)
    {
        // Trigger StrafeEvent

        // noinspection ConstantConditions
        if ((Object) this != Minecraft.getMinecraft().thePlayer)
            return;

        final StrafeEvent strafeEvent = new StrafeEvent(strafe, forward, friction);
        LiquidBounce.eventManager.callEvent(strafeEvent, true);

        if (strafeEvent.isCancelled())
            callbackInfo.cancel();
    }

    @SideOnly(Side.CLIENT)
    @Inject(method = "setPositionAndRotation2", at = @At("HEAD"), cancellable = true)
    public void injectItemPhysics(final double x, final double y, final double z, final float yaw, final float pitch, final int posRotationIncrements, final boolean p_180426_10_, final CallbackInfo callbackInfo)
    {
        // ItemPhysics
        final ItemPhysics itemPhysics = (ItemPhysics) LiquidBounce.moduleManager.get(ItemPhysics.class);

        // noinspection ConstantConditions
        if ((Object) this instanceof EntityItem && itemPhysics.getState())
        {
            setPosition(x, y, z);
            callbackInfo.cancel();
        }
    }

    private Step step;
    private boolean isThePlayer;
    private double newMoveX;
    private double newMoveY;
    private double newMoveZ;
    private boolean moveSafeWalk;
    private boolean wasAirStep;

    @Inject(method = "moveEntity", at = @At("HEAD"), cancellable = true)
    private void injectMoveEvent(final double moveX, final double moveY, final double moveZ, final CallbackInfo ci)
    {
        isThePlayer = (Entity) (Object) this instanceof EntityPlayerSP;
        if (!isThePlayer)
            return;

        final MoveEvent moveEvent = new MoveEvent(moveX, moveY, moveZ);
        LiquidBounce.eventManager.callEvent(moveEvent);

        if (moveEvent.isCancelled())
            ci.cancel();

        newMoveX = moveEvent.getX();
        newMoveY = moveEvent.getY();
        newMoveZ = moveEvent.getZ();
        moveSafeWalk = moveEvent.isSafeWalk();

        step = (Step) LiquidBounce.moduleManager.get(Step.class);
    }

    // FIXME Mixin: argsOnly doesn't works well with double captures
    // /*****************************************************************************************************************/
    // /*         Target Class : net.minecraft.entity.Entity                                                            */
    // /*        Target Method : func_70091_d                                                                           */
    // /*        Callback Name : localvar$injectMoveEventNewMoveX$zzl000                                                */
    // /*         Capture Type : double                                                                                 */
    // /*          Instruction : LabelNode UNKNOWN                                                                      */
    // /*****************************************************************************************************************/
    // /*           Match mode : EXPLICIT (match by criteria)                                                           */
    // /*        Match ordinal : 0                                                                                      */
    // /*          Match index : any                                                                                    */
    // /*        Match name(s) : any                                                                                    */
    // /*            Args only : true                                                                                   */
    //
    // Reality:
    // /*****************************************************************************************************************/
    // /* INDEX  ORDINAL                            TYPE  NAME                                                CANDIDATE */
    // /* [  1]    [  0]                          double  arg1                                                YES       */
    // /* [  2]    [  1]                          double  arg2                                                YES       */
    // /* [  3]    [  2]                          double  arg3                                                YES       */
    // /*****************************************************************************************************************/
    //
    // Imagine:
    // /*****************************************************************************************************************/
    // /* INDEX  ORDINAL                            TYPE  NAME                                                CANDIDATE */
    // /* [  1]    [  0]                          double  x                                                   YES       */
    // /* [  2]                                    <top>                                                                */
    // /* [  3]    [  1]                          double  y                                                   YES       */
    // /* [  4]                                    <top>                                                                */
    // /* [  5]    [  2]                          double  z                                                   YES       */
    // /* [  6]                                    <top>                                                                */
    // ... empty locals
    // /*****************************************************************************************************************/
    //
    //

    @ModifyVariable(method = "moveEntity", at = @At("HEAD"), ordinal = 0 /* , argsOnly = true */)
    private double injectMoveEventNewMoveX(final double moveX)
    {
        return isThePlayer ? newMoveX : moveX;
    }

    @ModifyVariable(method = "moveEntity", at = @At("HEAD"), ordinal = 1 /* , argsOnly = true */)
    private double injectMoveEventNewMoveY(final double moveY)
    {
        return isThePlayer ? newMoveY : moveY;
    }

    @ModifyVariable(method = "moveEntity", at = @At("HEAD"), ordinal = 2 /* , argsOnly = true */)
    private double injectMoveEventNewMoveZ(final double moveZ)
    {
        return isThePlayer ? newMoveZ : moveZ;
    }

    @ModifyVariable(method = "moveEntity", at = @At(value = "LOAD", ordinal = 0), ordinal = 0)
    private boolean injectSafeWalk(final boolean flag)
    {
        return flag || isThePlayer && moveSafeWalk;
    }

    @SuppressWarnings("InvalidInjectorMethodSignature")
    @ModifyVariable(method = "moveEntity", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;stepHeight:F", shift = Shift.BEFORE, ordinal = 0), ordinal = 1)
    private boolean injectAirStep(final boolean flag1)
    {
        if (flag1 || !isThePlayer)
            return flag1;

        wasAirStep = step.getState() && step.getAirStepValue().get() && step.canAirStep();
        return wasAirStep;
    }

    @Inject(method = "moveEntity", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;stepHeight:F", shift = Shift.BEFORE, ordinal = 1))
    private void injectStepEvent(final CallbackInfo ci)
    {
        if (!isThePlayer)
            return;

        final StepEvent stepEvent = new StepEvent(!onGround && wasAirStep ? Math.min(stepHeight, step.getAirStepHeightValue().get()) : stepHeight);
        LiquidBounce.eventManager.callEvent(stepEvent);
    }

    @SuppressWarnings("InvalidInjectorMethodSignature")
    @Inject(method = "moveEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;setEntityBoundingBox(Lnet/minecraft/util/AxisAlignedBB;)V", shift = Shift.AFTER, ordinal = 7), locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    private void injectStepConfirmEvent(final double x, final double y, final double z, final CallbackInfo ci, final double d0, final double d1, final double d2, final double d3, final double d4, final double d5, final boolean flag, final List list1, final AxisAlignedBB axisalignedbb, final boolean flag1, final double d11, final double d7, final double d8, final AxisAlignedBB axisalignedbb3, final List list, final AxisAlignedBB axisalignedbb4, final AxisAlignedBB axisalignedbb5, final double d9, final double d15, final double d16, final AxisAlignedBB axisalignedbb14, final double d17, final double d18, final double d19, final double d20, final double d10)
    {
        if (isThePlayer && d11 * d11 + d8 * d8 < x * x + z * z)
            LiquidBounce.eventManager.callEvent(new StepConfirmEvent());
    }
}
