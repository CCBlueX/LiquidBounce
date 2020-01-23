/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.aac;

import net.ccbluex.liquidbounce.event.MoveEvent;
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode;
import net.ccbluex.liquidbounce.utils.MovementUtils;

public class AACYPort extends SpeedMode {
    public AACYPort() {
        super("AACYPort");
    }

    @Override
    public void onMotion() {
        if(MovementUtils.isMoving() && !mc.thePlayer.isSneaking()) {
            mc.thePlayer.cameraPitch = 0F;

            if(mc.thePlayer.onGround) {
                mc.thePlayer.motionY = 0.3425F;
                mc.thePlayer.motionX *= 1.5893F;
                mc.thePlayer.motionZ *= 1.5893F;
            }else
                mc.thePlayer.motionY = -0.19D;
        }
    }

    @Override
    public void onUpdate() {
    }

    @Override
    public void onMove(MoveEvent event) {
    }
}
