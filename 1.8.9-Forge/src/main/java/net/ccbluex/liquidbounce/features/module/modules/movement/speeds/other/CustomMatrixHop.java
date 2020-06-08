/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.other;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.*;
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed;
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode;

public class CustomMatrixHop extends SpeedMode implements Listenable {
    public CustomMatrixHop() {
        super("CustomMatrixHop");

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
                mc.timer.timerSpeed = ((Speed) LiquidBounce.moduleManager.getModule(Speed.class)).custommatrixHopTimerValue.get();
                mc.thePlayer.motionX *= 1.0635F;
                mc.thePlayer.motionZ *= 1.0635F;
            }else if(mc.thePlayer.fallDistance > 0){
                mc.timer.timerSpeed = ((Speed) LiquidBounce.moduleManager.getModule(Speed.class)).custommatrixHopTimerValue2.get();
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