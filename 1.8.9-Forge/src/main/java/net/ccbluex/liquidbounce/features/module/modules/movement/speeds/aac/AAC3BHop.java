/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.aac;

import net.ccbluex.liquidbounce.event.MoveEvent;
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode;
import net.ccbluex.liquidbounce.utils.MovementUtils;

public class AAC3BHop extends SpeedMode {
    private boolean legitJump;

    public AAC3BHop() {
        super("AAC3BHop");
    }

    @Override
    public void onMotion() {

    }

    @Override
    public void onUpdate() {

    }

    @Override
    public void onMove(MoveEvent event) {

    }

    @Override
    public void onTick() {
        mc.timer.timerSpeed = 1F;

        if(mc.thePlayer.isInWater())
            return;

        if(MovementUtils.isMoving()) {
            if(mc.thePlayer.onGround) {
                if(legitJump) {
                    mc.thePlayer.jump();
                    legitJump = false;
                    return;
                }

                mc.thePlayer.motionY = 0.3852;
                mc.thePlayer.onGround = false;
                MovementUtils.strafe(0.374F);
            }else if(mc.thePlayer.motionY < 0D) {
                mc.thePlayer.speedInAir = 0.0201F;
                mc.timer.timerSpeed = 1.02F;
            }else
                mc.timer.timerSpeed = 1.01F;
        }else{
            legitJump = true;
            mc.thePlayer.motionX = 0D;
            mc.thePlayer.motionZ = 0D;
        }
    }
}