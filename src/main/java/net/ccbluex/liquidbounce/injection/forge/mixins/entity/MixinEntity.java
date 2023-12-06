/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.entity;

import net.ccbluex.liquidbounce.injection.implementations.IMixinEntity;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.StrafeEvent;
import net.ccbluex.liquidbounce.features.module.modules.combat.HitBox;
import net.ccbluex.liquidbounce.features.module.modules.exploit.NoPitchLimit;
import net.ccbluex.liquidbounce.features.module.modules.movement.NoFluid;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
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

import java.util.Random;
import java.util.UUID;

import static net.ccbluex.liquidbounce.utils.MinecraftInstance.mc;

@Mixin(Entity.class)
@SideOnly(Side.CLIENT)
public abstract class MixinEntity implements IMixinEntity {

    @Shadow
    public double posX;

    @Shadow
    public double posY;

    @Shadow
    public double posZ;

    private double trueX;

    public double getTrueX() {
        return trueX;
    }

    public void setTrueX(double x) {
        trueX = x;
    }

    private double trueY;

    public double getTrueY() {
        return trueY;
    }

    public void setTrueY(double y) {
        trueY = y;
    }

    private double trueZ;

    public double getTrueZ() {
        return trueZ;
    }

    public void setTrueZ(double z) {
        trueZ = z;
    }

    private boolean truePos;

    public boolean getTruePos() {
        return truePos;
    }

    public void setTruePos(boolean set) {
        truePos = set;
    }

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

    @Shadow
    public void moveEntity(double x, double y, double z) {
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
    public abstract void setEntityBoundingBox(AxisAlignedBB bb);

    @Shadow
    private int nextStepDistance;

    @Shadow
    private int fire;

    @Shadow
    public float prevRotationPitch;

    @Shadow
    public float prevRotationYaw;

    @Shadow
    protected abstract Vec3 getVectorForRotation(float pitch, float yaw);

    @Shadow
    public abstract UUID getUniqueID();

    @Shadow
    public abstract boolean isSneaking();

    @Shadow
    public abstract boolean isInsideOfMaterial(Material materialIn);

    public int getNextStepDistance() {
        return nextStepDistance;
    }

    public void setNextStepDistance(int nextStepDistance) {
        this.nextStepDistance = nextStepDistance;
    }

    public int getFire() {
        return fire;
    }

    @Inject(method = "getCollisionBorderSize", at = @At("HEAD"), cancellable = true)
    private void getCollisionBorderSize(final CallbackInfoReturnable<Float> callbackInfoReturnable) {
        final HitBox hitBox = HitBox.INSTANCE;

        if (hitBox.handleEvents())
            callbackInfoReturnable.setReturnValue(0.1F + hitBox.determineSize((Entity) (Object) this));
    }

    @Inject(method = "setAngles", at = @At("HEAD"), cancellable = true)
    private void setAngles(final float yaw, final float pitch, final CallbackInfo callbackInfo) {
        if (NoPitchLimit.INSTANCE.handleEvents()) {
            callbackInfo.cancel();

            prevRotationYaw = rotationYaw;
            prevRotationPitch = rotationPitch;

            rotationYaw += yaw * 0.15f;
            rotationPitch -= pitch * 0.15f;
        }
    }

    @Inject(method = "moveFlying", at = @At("HEAD"), cancellable = true)
    private void handleRotations(float strafe, float forward, float friction, final CallbackInfo callbackInfo) {
        //noinspection ConstantConditions
        if ((Object) this != mc.thePlayer) return;

        final StrafeEvent strafeEvent = new StrafeEvent(strafe, forward, friction);
        EventManager.INSTANCE.callEvent(strafeEvent);

        if (strafeEvent.isCancelled()) callbackInfo.cancel();
    }

    @Inject(method = "isInWater", at = @At("HEAD"), cancellable = true)
    private void isInWater(final CallbackInfoReturnable<Boolean> cir) {
        if (NoFluid.INSTANCE.handleEvents() && NoFluid.INSTANCE.getWater()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isInLava", at = @At("HEAD"), cancellable = true)
    private void isInLava(final CallbackInfoReturnable<Boolean> cir) {
        if (NoFluid.INSTANCE.handleEvents() && NoFluid.INSTANCE.getLava()) {
            cir.setReturnValue(false);
        }
    }
}