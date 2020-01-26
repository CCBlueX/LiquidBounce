/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.aac;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.JumpEvent;
import net.ccbluex.liquidbounce.event.MoveEvent;
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode;
import net.ccbluex.liquidbounce.utils.MovementUtils;
import net.ccbluex.liquidbounce.utils.block.BlockUtils;
import net.minecraft.block.BlockCarpet;
import net.minecraft.util.MathHelper;

public class AACHop3313 extends SpeedMode {

    public AACHop3313() {
        super("AACHop3.3.13");
    }

    @Override
    public void onMotion() {

    }

    @Override
    public void onUpdate() {
        if (!MovementUtils.isMoving() || mc.thePlayer.isInWater() || mc.thePlayer.isInLava() ||
                mc.thePlayer.isOnLadder() || mc.thePlayer.isRiding() || mc.thePlayer.hurtTime > 0)
            return;

        if (mc.thePlayer.onGround && mc.thePlayer.isCollidedVertically) {
            // MotionXYZ
            float yawRad = mc.thePlayer.rotationYaw * 0.017453292F;
            mc.thePlayer.motionX -= MathHelper.sin(yawRad) * 0.202F;
            mc.thePlayer.motionZ += MathHelper.cos(yawRad) * 0.202F;
            mc.thePlayer.motionY = 0.405F;
            LiquidBounce.eventManager.callEvent(new JumpEvent(0.405F));
            MovementUtils.strafe();
        } else if (mc.thePlayer.fallDistance < 0.31F) {
            if (BlockUtils.getBlock(mc.thePlayer.getPosition()) instanceof BlockCarpet) // why?
                return;

            // Motion XZ
            mc.thePlayer.jumpMovementFactor = mc.thePlayer.moveStrafing == 0F ? 0.027F : 0.021F;
            mc.thePlayer.motionX *= 1.001;
            mc.thePlayer.motionZ *= 1.001;

            // Motion Y
            if (!mc.thePlayer.isCollidedHorizontally)
                mc.thePlayer.motionY -= 0.014999993F;
        } else
            mc.thePlayer.jumpMovementFactor = 0.02F;
    }

    @Override
    public void onMove(final MoveEvent event) {
    }

    @Override
    public void onDisable() {
        mc.thePlayer.jumpMovementFactor = 0.02F;
    }
}
