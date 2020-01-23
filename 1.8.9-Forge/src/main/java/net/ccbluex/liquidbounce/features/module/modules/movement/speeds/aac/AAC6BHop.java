/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.aac;

import net.ccbluex.liquidbounce.event.MoveEvent;
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode;
import net.ccbluex.liquidbounce.utils.MovementUtils;

public class AAC6BHop extends SpeedMode {
    private boolean legitJump;

    public AAC6BHop() {
        super("AAC6BHop");
    }

    @Override
    public void onMotion() {

    }

    @Override
    public void onUpdate() {
        mc.timer.timerSpeed = 1F;

        if(mc.thePlayer.isInWater())
            return;

        if(MovementUtils.isMoving()) {
            if(mc.thePlayer.onGround) {
                if(legitJump) {
                    mc.thePlayer.motionY = 0.4;
                    MovementUtils.strafe(0.15F);
                    mc.thePlayer.onGround = false;
                    legitJump = false;
                    return;
                }

                mc.thePlayer.motionY = 0.41;
                MovementUtils.strafe(0.47458485F);
            }

            if(mc.thePlayer.motionY < 0 && mc.thePlayer.motionY > -0.2)
                mc.timer.timerSpeed = ((float) (1.2 + mc.thePlayer.motionY));

            mc.thePlayer.speedInAir = 0.022151F;
        }else{
            legitJump = true;
            mc.thePlayer.motionX = 0D;
            mc.thePlayer.motionZ = 0D;
        }
    }

    @Override
    public void onMove(MoveEvent event) {

    }

    @Override
    public void onEnable() {
        legitJump = true;
    }

    @Override
    public void onDisable() {
        mc.timer.timerSpeed = 1F;
        mc.thePlayer.speedInAir = 0.02F;
    }

}