/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.aac;

import net.ccbluex.liquidbounce.event.MoveEvent;
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode;
import net.ccbluex.liquidbounce.utils.MovementUtils;

public class AAC7BHop extends SpeedMode {
    public AAC7BHop() {
        super("AAC7BHop");
    }

    @Override
    public void onMotion() {
    }

    @Override
    public void onUpdate() {
        if(!MovementUtils.isMoving() || mc.thePlayer.ridingEntity != null || mc.thePlayer.hurtTime > 0)
            return;

        if(mc.thePlayer.onGround) {
            mc.thePlayer.jump();
            mc.thePlayer.motionY = 0.405;
            mc.thePlayer.motionX *= 1.004;
            mc.thePlayer.motionZ *= 1.004;
            return;
        }

        final double speed = MovementUtils.getSpeed() * 1.0072D;
        final double yaw = Math.toRadians(mc.thePlayer.rotationYaw);
        mc.thePlayer.motionX = -Math.sin(yaw) * speed;
        mc.thePlayer.motionZ = Math.cos(yaw) * speed;
    }

    @Override
    public void onMove(MoveEvent event) {
    }
}