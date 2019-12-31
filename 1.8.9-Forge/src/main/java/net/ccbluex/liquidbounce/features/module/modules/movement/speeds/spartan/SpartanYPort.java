package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.spartan;

import net.ccbluex.liquidbounce.event.MoveEvent;
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode;

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
public class SpartanYPort extends SpeedMode {

    private int airMoves;

    public SpartanYPort() {
        super("SpartanYPort");
    }

    @Override
    public void onMotion() {
        if(mc.gameSettings.keyBindForward.isKeyDown() && !mc.gameSettings.keyBindJump.isKeyDown()) {
            if(mc.thePlayer.onGround) {
                mc.thePlayer.jump();
                airMoves = 0;
            }else{
                mc.timer.timerSpeed = 1.08F;

                if(airMoves >= 3)
                    mc.thePlayer.jumpMovementFactor = 0.0275F;

                if(airMoves >= 4 && airMoves % 2 == 0.0) {
                    mc.thePlayer.motionY = -0.32F - 0.009 * Math.random();
                    mc.thePlayer.jumpMovementFactor = 0.0238F;
                }

                airMoves++;
            }
        }
    }

    @Override
    public void onUpdate() {
    }

    @Override
    public void onMove(MoveEvent event) {
    }
}