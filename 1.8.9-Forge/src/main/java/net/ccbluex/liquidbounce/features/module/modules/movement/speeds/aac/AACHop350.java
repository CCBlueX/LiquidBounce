/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.aac;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.*;
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode;
import net.ccbluex.liquidbounce.utils.MovementUtils;

public class AACHop350 extends SpeedMode implements Listenable {
    public AACHop350() {
        super("AACHop3.5.0");

        LiquidBounce.eventManager.registerListener(this);
    }

    @Override
    public void onMotion() {
    }

    @Override
    public void onUpdate() {
    }

    @Override
    public void onMove(final MoveEvent event) {
    }

    @EventTarget
    public void onMotion(final MotionEvent event) {
        if(event.getEventState() == EventState.POST && MovementUtils.isMoving() && !mc.thePlayer.isInWater() && !mc.thePlayer.isInLava()) {
            mc.thePlayer.jumpMovementFactor += 0.00208F;

            if(mc.thePlayer.fallDistance <= 1F) {
                if(mc.thePlayer.onGround) {
                    mc.thePlayer.jump();
                    mc.thePlayer.motionX *= 1.0118F;
                    mc.thePlayer.motionZ *= 1.0118F;
                }else{
                    mc.thePlayer.motionY -= 0.0147F;

                    mc.thePlayer.motionX *= 1.00138F;
                    mc.thePlayer.motionZ *= 1.00138F;
                }
            }
        }
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