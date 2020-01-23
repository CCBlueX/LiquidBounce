/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.aac;

import net.ccbluex.liquidbounce.event.MoveEvent;
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode;
import net.ccbluex.liquidbounce.utils.MovementUtils;

public class AAC2BHop extends SpeedMode {
    public AAC2BHop() {
        super("AAC2BHop");
    }

    @Override
    public void onMotion() {
        if(mc.thePlayer.isInWater())
            return;

        if(MovementUtils.isMoving()) {
            if(mc.thePlayer.onGround) {
                mc.thePlayer.jump();
                mc.thePlayer.motionX *= 1.02D;
                mc.thePlayer.motionZ *= 1.02D;
            }else if(mc.thePlayer.motionY > -0.2D) {
                mc.thePlayer.jumpMovementFactor = 0.08F;
                mc.thePlayer.motionY += 0.0143099999999999999999999999999D;
                mc.thePlayer.jumpMovementFactor = 0.07F;
            }
        }else{
            mc.thePlayer.motionX = 0D;
            mc.thePlayer.motionZ = 0D;
        }
    }

    @Override
    public void onUpdate() {

    }

    @Override
    public void onMove(MoveEvent event) {
    }
}
