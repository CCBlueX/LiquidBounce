/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.misc;

import net.ccbluex.liquidbounce.utils.MinecraftInstance;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import org.jetbrains.annotations.Nullable;

public class FallingPlayer extends MinecraftInstance {

    private double x;
    private double y;
    private double z;

    private double motionX;
    private double motionY;
    private double motionZ;

    private final float yaw;

    private float strafe;
    private float forward;

    public FallingPlayer(double x, double y, double z, double motionX, double motionY, double motionZ, float yaw, float strafe, float forward) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.motionX = motionX;
        this.motionY = motionY;
        this.motionZ = motionZ;
        this.yaw = yaw;
        this.strafe = strafe;
        this.forward = forward;
    }

    private void calculateForTick() {
        strafe *= 0.98F;
        forward *= 0.98F;

        float v = strafe * strafe + forward * forward;

        if (v >= 0.0001f) {
            v = (float) Math.sqrt(v);

            if (v < 1.0F) {
                v = 1.0F;
            }

            v = mc.thePlayer.jumpMovementFactor / v;
            strafe = strafe * v;
            forward = forward * v;
            float f1 = (float) Math.sin(yaw * (float) Math.PI / 180.0F);
            float f2 = (float) Math.cos(yaw * (float) Math.PI / 180.0F);
            this.motionX += strafe * f2 - forward * f1;
            this.motionZ += forward * f2 + strafe * f1;
        }


        motionY -= 0.08;

        motionX *= 0.91;
        motionY *= 0.9800000190734863D;
        motionY *= 0.91;
        motionZ *= 0.91;

        x += motionX;
        y += motionY;
        z += motionZ;
    }

    public CollisionResult findCollision(int ticks) {
        for (int i = 0; i < ticks; i++) {
            Vec3 start = new Vec3(x, y, z);

            calculateForTick();

            Vec3 end = new Vec3(x, y, z);

            BlockPos raytracedBlock;

            float w = mc.thePlayer.width / 2F;

            if ((raytracedBlock = rayTrace(start, end)) != null) return new CollisionResult(raytracedBlock, i);

            if ((raytracedBlock = rayTrace(start.addVector(w, 0, w), end)) != null)
                return new CollisionResult(raytracedBlock, i);
            if ((raytracedBlock = rayTrace(start.addVector(-w, 0, w), end)) != null)
                return new CollisionResult(raytracedBlock, i);
            if ((raytracedBlock = rayTrace(start.addVector(w, 0, -w), end)) != null)
                return new CollisionResult(raytracedBlock, i);
            if ((raytracedBlock = rayTrace(start.addVector(-w, 0, -w), end)) != null)
                return new CollisionResult(raytracedBlock, i);

            if ((raytracedBlock = rayTrace(start.addVector(w, 0, w / 2f), end)) != null)
                return new CollisionResult(raytracedBlock, i);
            if ((raytracedBlock = rayTrace(start.addVector(-w, 0, w / 2f), end)) != null)
                return new CollisionResult(raytracedBlock, i);
            if ((raytracedBlock = rayTrace(start.addVector(w / 2f, 0, w), end)) != null)
                return new CollisionResult(raytracedBlock, i);
            if ((raytracedBlock = rayTrace(start.addVector(w / 2f, 0, -w), end)) != null)
                return new CollisionResult(raytracedBlock, i);

        }

        return null;
    }


    @Nullable
    private BlockPos rayTrace(Vec3 start, Vec3 end) {
        MovingObjectPosition result = mc.theWorld.rayTraceBlocks(start, end, true);

        if (result != null && result.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && result.sideHit == EnumFacing.UP) {
            return result.getBlockPos();
        }

        return null;
    }

    public static class CollisionResult {
        private final BlockPos pos;
        private final int tick;

        public CollisionResult(BlockPos pos, int tick) {
            this.pos = pos;
            this.tick = tick;
        }

        public BlockPos getPos() {
            return pos;
        }

        public int getTick() {
            return tick;
        }
    }

}