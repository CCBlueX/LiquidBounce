/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.aac;

import net.ccbluex.liquidbounce.event.MoveEvent;
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode;
import net.ccbluex.liquidbounce.utils.MovementUtils;

public class AACLowHop5 extends SpeedMode {
    private boolean legitJump;

    public AACLowHop5() {
        super("AACLowHop5");
    }

    @Override
    public void onEnable() {
        legitJump = true;
        super.onEnable();
    }

    @Override
    public void onMotion() {
        if(MovementUtils.isMoving()) {
            if(mc.thePlayer.onGround) {
                if(legitJump) {
                    mc.thePlayer.jump();
                    legitJump = false;
                    return;
                }

                mc.thePlayer.motionY = 0.4032F;
                MovementUtils.strafe(0.2F);
            }
        }else{
            legitJump = true;
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
