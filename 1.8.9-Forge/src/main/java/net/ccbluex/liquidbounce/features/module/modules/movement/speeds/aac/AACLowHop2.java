/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.aac;

import net.ccbluex.liquidbounce.event.MoveEvent;
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode;
import net.ccbluex.liquidbounce.utils.MovementUtils;

public class AACLowHop2 extends SpeedMode {
    private boolean legitJump;

    public AACLowHop2() {
        super("AACLowHop2");
    }

    @Override
    public void onEnable() {
        legitJump = true;
        mc.timer.timerSpeed = 1F;
    }

    @Override
    public void onDisable() {
        mc.timer.timerSpeed = 1F;
    }

    @Override
    public void onMotion() {
        mc.timer.timerSpeed = 1F;

        if(mc.thePlayer.isInWater())
            return;

        if(MovementUtils.isMoving()) {
            mc.timer.timerSpeed = 1.09F;

            if(mc.thePlayer.onGround) {
                if(legitJump) {
                    mc.thePlayer.jump();
                    legitJump = false;
                    return;
                }

                mc.thePlayer.motionY = 0.343F;
                MovementUtils.strafe(0.534F);
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
