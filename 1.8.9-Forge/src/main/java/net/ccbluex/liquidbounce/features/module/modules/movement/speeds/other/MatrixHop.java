/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.other;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.*;
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode;

public class MatrixHop extends SpeedMode implements Listenable {
    public MatrixHop() {
        super("MatrixHop");

        LiquidBounce.eventManager.registerListener(this);
    }

    @Override
    public void onMotion() {
    }

    @Override
    public void onUpdate() {
        if(mc.thePlayer.moveForward > 0) {
            if(mc.thePlayer.onGround) {
                mc.thePlayer.jump();
                mc.thePlayer.setSprinting(true);
                mc.timer.timerSpeed = 0.775F;
                mc.thePlayer.motionX *= 1.08;
                mc.thePlayer.motionZ *= 1.08;
            }else if(mc.thePlayer.fallDistance > 0){
                mc.timer.timerSpeed = 2.5F;
            }
        }
    }

    @Override
    public void onMove(final MoveEvent event) {
    }

    @EventTarget
    public void onMotion(final MotionEvent event) {
    }

    @Override
    public void onEnable() {
        if(mc.thePlayer.onGround)
            mc.thePlayer.motionX = mc.thePlayer.motionZ = 0;
    }

    @Override
    public void onDisable() {
        mc.thePlayer.jumpMovementFactor = 0.02F;
    }

    @Override
    public boolean handleEvents() {
        return isActive();
    }
}