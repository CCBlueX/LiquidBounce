/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.aac;

import net.ccbluex.liquidbounce.event.MoveEvent;
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode;
import net.ccbluex.liquidbounce.utils.MovementUtils;

public class AACLowHop3 extends SpeedMode {
    private boolean firstJump;
    private boolean waitForGround;

    public AACLowHop3() {
        super("AACLowHop3");
    }

    @Override
    public void onEnable() {
        firstJump = true;
    }

    @Override
    public void onMotion() {
        if(MovementUtils.isMoving()) {
            if(mc.thePlayer.hurtTime <= 0) {
                if(mc.thePlayer.onGround) {
                    waitForGround = false;

                    if(!firstJump)
                        firstJump = true;

                    mc.thePlayer.jump();
                    mc.thePlayer.motionY = 0.41;
                }else{
                    if(waitForGround)
                        return;

                    if(mc.thePlayer.isCollidedHorizontally)
                        return;

                    firstJump = false;
                    mc.thePlayer.motionY -= 0.0149;
                }

                if(!mc.thePlayer.isCollidedHorizontally)
                    MovementUtils.forward(firstJump ? 0.0016 : 0.001799);
            }else{
                firstJump = true;
                waitForGround = true;
            }
        }else{
            mc.thePlayer.motionZ = 0;
            mc.thePlayer.motionX = 0;
        }

        final double speed = MovementUtils.getSpeed();
        mc.thePlayer.motionX = -(Math.sin(MovementUtils.getDirection()) * speed);
        mc.thePlayer.motionZ = Math.cos(MovementUtils.getDirection()) * speed;
    }

    @Override
    public void onUpdate() {
    }

    @Override
    public void onMove(MoveEvent event) {
    }
}
