/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.other;

import net.ccbluex.liquidbounce.event.MoveEvent;
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode;
import net.ccbluex.liquidbounce.utils.MovementUtils;

public class HiveHop extends SpeedMode {

    public HiveHop() {
        super("HiveHop");
    }

    @Override
    public void onEnable() {
        mc.thePlayer.speedInAir = 0.0425F;
        mc.timer.timerSpeed = 1.04F;
    }

    @Override
    public void onDisable() {
        mc.thePlayer.speedInAir = 0.02F;
        mc.timer.timerSpeed = 1F;
    }

    @Override
    public void onMotion() {
    }

    @Override
    public void onUpdate() {
        if(MovementUtils.isMoving()) {
            if(mc.thePlayer.onGround)
                mc.thePlayer.motionY = 0.3;

            mc.thePlayer.speedInAir = 0.0425F;
            mc.timer.timerSpeed = 1.04F;
            MovementUtils.strafe();
        }else{
            mc.thePlayer.motionX = mc.thePlayer.motionZ = 0D;
            mc.thePlayer.speedInAir = 0.02F;
            mc.timer.timerSpeed = 1F;
        }
    }

    @Override
    public void onMove(MoveEvent event) {
    }
}