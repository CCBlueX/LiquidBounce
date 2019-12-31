package net.ccbluex.liquidbounce.utils.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.util.*;
import org.jetbrains.annotations.Nullable;

public class FallingPlayer {
    private double x;
    private double y;
    private double z;

    private double motionX;
    private double motionY;
    private double motionZ;

    private float yaw;

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

    public void calculateForTick() {
        strafe *= 0.98F;
        forward *= 0.98F;


        float f = strafe * strafe + forward * forward;

        if (f >= 0.0001f) {
            f = MathHelper.sqrt_float(f);

            if (f < 1.0F) {
                f = 1.0F;
            }

            f = Minecraft.getMinecraft().thePlayer.jumpMovementFactor / f;
            strafe = strafe * f;
            forward = forward * f;
            float f1 = MathHelper.sin(yaw * (float) Math.PI / 180.0F);
            float f2 = MathHelper.cos(yaw * (float) Math.PI / 180.0F);
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

    public BlockPos findCollision(int ticks) {
        for (int i = 0; i < ticks; i++) {
            Vec3 start = new Vec3(x, y, z);

            calculateForTick();

            Vec3 end = new Vec3(x, y, z);

            BlockPos shit;

            float w = Minecraft.getMinecraft().thePlayer.width / 2.0f;

            if ((shit = rayTrace(start, end)) != null) return shit;

            if ((shit = rayTrace(start.addVector(w, 0, w), end)) != null) return shit;
            if ((shit = rayTrace(start.addVector(-w, 0, w), end)) != null) return shit;
            if ((shit = rayTrace(start.addVector(w, 0, -w), end)) != null) return shit;
            if ((shit = rayTrace(start.addVector(-w, 0, -w), end)) != null) return shit;

            if ((shit = rayTrace(start.addVector(w, 0, w / 2f), end)) != null) return shit;
            if ((shit = rayTrace(start.addVector(-w, 0, w / 2f), end)) != null) return shit;
            if ((shit = rayTrace(start.addVector(w / 2f, 0, w), end)) != null) return shit;
            if ((shit = rayTrace(start.addVector(w / 2f, 0, -w), end)) != null) return shit;

        }

        return null;
    }


    @Nullable
    private BlockPos rayTrace(Vec3 start, Vec3 end) {
        MovingObjectPosition movingObjectPosition = Minecraft.getMinecraft().theWorld.rayTraceBlocks(start, end, true);

        if (movingObjectPosition != null && movingObjectPosition.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && movingObjectPosition.sideHit == EnumFacing.UP) {
            return movingObjectPosition.getBlockPos();
        }
        return null;
    }

}