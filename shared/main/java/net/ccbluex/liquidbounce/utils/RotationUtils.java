/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity;
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPlayerSP;
import net.ccbluex.liquidbounce.api.minecraft.network.IPacket;
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketPlayer;
import net.ccbluex.liquidbounce.api.minecraft.util.*;
import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.Listenable;
import net.ccbluex.liquidbounce.event.PacketEvent;
import net.ccbluex.liquidbounce.event.TickEvent;
import net.ccbluex.liquidbounce.features.module.modules.combat.FastBow;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public final class RotationUtils extends MinecraftInstance implements Listenable {

    private static final Random random = new Random();

    private static int keepLength;

    public static Rotation targetRotation;
    public static Rotation serverRotation = new Rotation(0F, 0F);

    public static boolean keepCurrentRotation = false;

    private static double x = random.nextDouble();
    private static double y = random.nextDouble();
    private static double z = random.nextDouble();

    /**
     * Face block
     *
     * @param blockPos target block
     */
    public static VecRotation faceBlock(final WBlockPos blockPos) {
        if (blockPos == null)
            return null;

        VecRotation vecRotation = null;

        for (double xSearch = 0.1D; xSearch < 0.9D; xSearch += 0.1D) {
            for (double ySearch = 0.1D; ySearch < 0.9D; ySearch += 0.1D) {
                for (double zSearch = 0.1D; zSearch < 0.9D; zSearch += 0.1D) {
                    final WVec3 eyesPos = new WVec3(mc.getThePlayer().getPosX(), mc.getThePlayer().getEntityBoundingBox().getMinY() + mc.getThePlayer().getEyeHeight(), mc.getThePlayer().getPosZ());
                    final WVec3 posVec = new WVec3(blockPos).addVector(xSearch, ySearch, zSearch);
                    final double dist = eyesPos.distanceTo(posVec);

                    final double diffX = posVec.getXCoord() - eyesPos.getXCoord();
                    final double diffY = posVec.getYCoord() - eyesPos.getYCoord();
                    final double diffZ = posVec.getZCoord() - eyesPos.getZCoord();

                    final double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

                    final Rotation rotation = new Rotation(
                            WMathHelper.wrapAngleTo180_float((float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90F),
                            WMathHelper.wrapAngleTo180_float((float) -Math.toDegrees(Math.atan2(diffY, diffXZ)))
                    );

                    final WVec3 rotationVector = getVectorForRotation(rotation);
                    final WVec3 vector = eyesPos.addVector(rotationVector.getXCoord() * dist, rotationVector.getYCoord() * dist,
                            rotationVector.getZCoord() * dist);
                    final IMovingObjectPosition obj = mc.getTheWorld().rayTraceBlocks(eyesPos, vector, false,
                            false, true);

                    if (obj != null && obj.getTypeOfHit() == IMovingObjectPosition.WMovingObjectType.BLOCK) {
                        final VecRotation currentVec = new VecRotation(posVec, rotation);

                        if (vecRotation == null || getRotationDifference(currentVec.getRotation()) < getRotationDifference(vecRotation.getRotation()))
                            vecRotation = currentVec;
                    }
                }
            }
        }

        return vecRotation;
    }

    /**
     * Face target with bow
     *
     * @param target      your enemy
     * @param silent      client side rotations
     * @param predict     predict new enemy position
     * @param predictSize predict size of predict
     */
    public static void faceBow(final IEntity target, final boolean silent, final boolean predict, final float predictSize) {
        final IEntityPlayerSP player = mc.getThePlayer();

        final double posX = target.getPosX() + (predict ? (target.getPosX() - target.getPrevPosX()) * predictSize : 0) - (player.getPosX() + (predict ? (player.getPosX() - player.getPrevPosX()) : 0));
        final double posY = target.getEntityBoundingBox().getMinY() + (predict ? (target.getEntityBoundingBox().getMinY() - target.getPrevPosY()) * predictSize : 0) + target.getEyeHeight() - 0.15 - (player.getEntityBoundingBox().getMinY() + (predict ? (player.getPosY() - player.getPrevPosY()) : 0)) - player.getEyeHeight();
        final double posZ = target.getPosZ() + (predict ? (target.getPosZ() - target.getPrevPosZ()) * predictSize : 0) - (player.getPosZ() + (predict ? (player.getPosZ() - player.getPrevPosZ()) : 0));
        final double posSqrt = Math.sqrt(posX * posX + posZ * posZ);

        float velocity = LiquidBounce.moduleManager.getModule(FastBow.class).getState() ? 1F : player.getItemInUseDuration() / 20F;
        velocity = (velocity * velocity + velocity * 2) / 3;

        if (velocity > 1) velocity = 1;

        final Rotation rotation = new Rotation(
                (float) (Math.atan2(posZ, posX) * 180 / Math.PI) - 90,
                (float) -Math.toDegrees(Math.atan((velocity * velocity - Math.sqrt(velocity * velocity * velocity * velocity - 0.006F * (0.006F * (posSqrt * posSqrt) + 2 * posY * (velocity * velocity)))) / (0.006F * posSqrt)))
        );

        if (silent)
            setTargetRotation(rotation);
        else
            limitAngleChange(new Rotation(player.getRotationYaw(), player.getRotationPitch()), rotation, 10 +
                    new Random().nextInt(6)).toPlayer(mc.getThePlayer());
    }

    /**
     * Translate vec to rotation
     *
     * @param vec     target vec
     * @param predict predict new location of your body
     * @return rotation
     */
    public static Rotation toRotation(final WVec3 vec, final boolean predict) {
        final WVec3 eyesPos = new WVec3(mc.getThePlayer().getPosX(), mc.getThePlayer().getEntityBoundingBox().getMinY() +
                mc.getThePlayer().getEyeHeight(), mc.getThePlayer().getPosZ());

        if (predict)
            eyesPos.addVector(mc.getThePlayer().getMotionX(), mc.getThePlayer().getMotionY(), mc.getThePlayer().getMotionZ());

        final double diffX = vec.getXCoord() - eyesPos.getXCoord();
        final double diffY = vec.getYCoord() - eyesPos.getYCoord();
        final double diffZ = vec.getZCoord() - eyesPos.getZCoord();

        return new Rotation(WMathHelper.wrapAngleTo180_float(
                (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90F
        ), WMathHelper.wrapAngleTo180_float(
                (float) (-Math.toDegrees(Math.atan2(diffY, Math.sqrt(diffX * diffX + diffZ * diffZ))))
        ));
    }

    /**
     * Get the center of a box
     *
     * @param bb your box
     * @return center of box
     */
    public static WVec3 getCenter(final IAxisAlignedBB bb) {
        return new WVec3(bb.getMinX() + (bb.getMaxX() - bb.getMinX()) * 0.5, bb.getMinY() + (bb.getMaxY() - bb.getMinY()) * 0.5, bb.getMinZ() + (bb.getMaxZ() - bb.getMinZ()) * 0.5);
    }

    /**
     * Search good center
     *
     * @param bb           enemy box
     * @param outborder    outborder option
     * @param random       random option
     * @param predict      predict option
     * @param throughWalls throughWalls option
     * @return center
     */
    public static VecRotation searchCenter(final IAxisAlignedBB bb, final boolean outborder, final boolean random,
                                           final boolean predict, final boolean throughWalls, final float distance) {
        if (outborder) {
            final WVec3 vec3 = new WVec3(bb.getMinX() + (bb.getMaxX() - bb.getMinX()) * (x * 0.3 + 1.0), bb.getMinY() + (bb.getMaxY() - bb.getMinY()) * (y * 0.3 + 1.0), bb.getMinZ() + (bb.getMaxZ() - bb.getMinZ()) * (z * 0.3 + 1.0));
            return new VecRotation(vec3, toRotation(vec3, predict));
        }

        final WVec3 randomVec = new WVec3(bb.getMinX() + (bb.getMaxX() - bb.getMinX()) * x * 0.8, bb.getMinY() + (bb.getMaxY() - bb.getMinY()) * y * 0.8, bb.getMinZ() + (bb.getMaxZ() - bb.getMinZ()) * z * 0.8);
        final Rotation randomRotation = toRotation(randomVec, predict);

        final WVec3 eyes = mc.getThePlayer().getPositionEyes(1F);

        VecRotation vecRotation = null;

        for(double xSearch = 0.15D; xSearch < 0.85D; xSearch += 0.1D) {
            for (double ySearch = 0.15D; ySearch < 1D; ySearch += 0.1D) {
                for (double zSearch = 0.15D; zSearch < 0.85D; zSearch += 0.1D) {
                    final WVec3 vec3 = new WVec3(bb.getMinX() + (bb.getMaxX() - bb.getMinX()) * xSearch,
                            bb.getMinY() + (bb.getMaxY() - bb.getMinY()) * ySearch, bb.getMinZ() + (bb.getMaxZ() - bb.getMinZ()) * zSearch);
                    final Rotation rotation = toRotation(vec3, predict);
                    final double vecDist = eyes.distanceTo(vec3);

                    if (vecDist > distance)
                        continue;

                    if (throughWalls || isVisible(vec3)) {
                        final VecRotation currentVec = new VecRotation(vec3, rotation);

                        if (vecRotation == null || (random ? getRotationDifference(currentVec.getRotation(), randomRotation) < getRotationDifference(vecRotation.getRotation(), randomRotation) : getRotationDifference(currentVec.getRotation()) < getRotationDifference(vecRotation.getRotation())))
                            vecRotation = currentVec;
                    }
                }
            }
        }

        return vecRotation;
    }

    /**
     * Calculate difference between the client rotation and your entity
     *
     * @param entity your entity
     * @return difference between rotation
     */
    public static double getRotationDifference(final IEntity entity) {
        final Rotation rotation = toRotation(getCenter(entity.getEntityBoundingBox()), true);

        return getRotationDifference(rotation, new Rotation(mc.getThePlayer().getRotationYaw(), mc.getThePlayer().getRotationPitch()));
    }

    /**
     * Calculate difference between the server rotation and your rotation
     *
     * @param rotation your rotation
     * @return difference between rotation
     */
    public static double getRotationDifference(final Rotation rotation) {
        return serverRotation == null ? 0D : getRotationDifference(rotation, serverRotation);
    }

    /**
     * Calculate difference between two rotations
     *
     * @param a rotation
     * @param b rotation
     * @return difference between rotation
     */
    public static double getRotationDifference(final Rotation a, final Rotation b) {
        return Math.hypot(getAngleDifference(a.getYaw(), b.getYaw()), a.getPitch() - b.getPitch());
    }

    /**
     * Limit your rotation using a turn speed
     *
     * @param currentRotation your current rotation
     * @param targetRotation your goal rotation
     * @param turnSpeed your turn speed
     * @return limited rotation
     */
    @NotNull
    public static Rotation limitAngleChange(final Rotation currentRotation, final Rotation targetRotation, final float turnSpeed) {
        final float yawDifference = getAngleDifference(targetRotation.getYaw(), currentRotation.getYaw());
        final float pitchDifference = getAngleDifference(targetRotation.getPitch(), currentRotation.getPitch());

        return new Rotation(
                currentRotation.getYaw() + (yawDifference > turnSpeed ? turnSpeed : Math.max(yawDifference, -turnSpeed)),
                currentRotation.getPitch() + (pitchDifference > turnSpeed ? turnSpeed : Math.max(pitchDifference, -turnSpeed)
        ));
    }

    /**
     * Calculate difference between two angle points
     *
     * @param a angle point
     * @param b angle point
     * @return difference between angle points
     */
    private static float getAngleDifference(final float a, final float b) {
        return ((((a - b) % 360F) + 540F) % 360F) - 180F;
    }

    /**
     * Calculate rotation to vector
     *
     * @param rotation your rotation
     * @return target vector
     */
    public static WVec3 getVectorForRotation(final Rotation rotation) {
        float yawCos = (float) Math.cos(-rotation.getYaw() * 0.017453292F - (float) Math.PI);
        float yawSin = (float) Math.sin(-rotation.getYaw() * 0.017453292F - (float) Math.PI);
        float pitchCos = (float) -Math.cos(-rotation.getPitch() * 0.017453292F);
        float pitchSin = (float) Math.sin(-rotation.getPitch() * 0.017453292F);
        return new WVec3(yawSin * pitchCos, pitchSin, yawCos * pitchCos);
    }

    /**
     * Allows you to check if your crosshair is over your target entity
     *
     * @param targetEntity       your target entity
     * @param blockReachDistance your reach
     * @return if crosshair is over target
     */
    public static boolean isFaced(final IEntity targetEntity, double blockReachDistance) {
        return RaycastUtils.raycastEntity(blockReachDistance, entity -> targetEntity != null && targetEntity.equals(entity)) != null;
    }

    /**
     * Allows you to check if your enemy is behind a wall
     */
    public static boolean isVisible(final WVec3 vec3) {
        final WVec3 eyesPos = new WVec3(mc.getThePlayer().getPosX(), mc.getThePlayer().getEntityBoundingBox().getMinY() + mc.getThePlayer().getEyeHeight(), mc.getThePlayer().getPosZ());

        return mc.getTheWorld().rayTraceBlocks(eyesPos, vec3) == null;
    }

    /**
     * Handle minecraft tick
     *
     * @param event Tick event
     */
    @EventTarget
    public void onTick(final TickEvent event) {
        if(targetRotation != null) {
            keepLength--;

            if (keepLength <= 0)
                reset();
        }

        if(random.nextGaussian() > 0.8D) x = Math.random();
        if(random.nextGaussian() > 0.8D) y = Math.random();
        if(random.nextGaussian() > 0.8D) z = Math.random();
    }

    /**
     * Set your target rotation
     *
     * @param rotation your target rotation
     */
    public static void setTargetRotation(final Rotation rotation, final int keepLength) {
        if (Double.isNaN(rotation.getYaw()) || Double.isNaN(rotation.getPitch())
                || rotation.getPitch() > 90 || rotation.getPitch() < -90)
            return;

        rotation.fixedSensitivity(mc.getGameSettings().getMouseSensitivity());
        targetRotation = rotation;
        RotationUtils.keepLength = keepLength;
    }

    /**
     * Set your target rotation
     *
     * @param rotation your target rotation
     */
    public static void setTargetRotation(final Rotation rotation) {
        setTargetRotation(rotation, 0);
    }

    /**
     * Handle packet
     *
     * @param event Packet Event
     */
    @EventTarget
    public void onPacket(final PacketEvent event) {
        final IPacket packet = event.getPacket();

        if (classProvider.isCPacketPlayer(packet)) {
            final ICPacketPlayer packetPlayer = packet.asCPacketPlayer();

            if (targetRotation != null && !keepCurrentRotation && (targetRotation.getYaw() != serverRotation.getYaw() || targetRotation.getPitch() != serverRotation.getPitch())) {
                packetPlayer.setYaw(targetRotation.getYaw());
                packetPlayer.setPitch(targetRotation.getPitch());
                packetPlayer.setRotating(true);
            }

            if (packetPlayer.isRotating())
                serverRotation = new Rotation(packetPlayer.getYaw(), packetPlayer.getPitch());
        }
    }

    /**
     * Reset your target rotation
     */
    public static void reset() {
        keepLength = 0;
        targetRotation = null;
    }

    /**
     * @return YESSSS!!!
     */
    @Override
    public boolean handleEvents() {
        return true;
    }
}
