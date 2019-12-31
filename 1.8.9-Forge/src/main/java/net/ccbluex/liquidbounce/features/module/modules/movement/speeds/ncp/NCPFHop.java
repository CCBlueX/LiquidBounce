package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.ncp;

import net.ccbluex.liquidbounce.event.MoveEvent;
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode;
import net.ccbluex.liquidbounce.utils.MovementUtils;

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
public class NCPFHop extends SpeedMode {

    public NCPFHop() {
        super("NCPFHop");
    }

    @Override
    public void onEnable() {
        mc.timer.timerSpeed = 1.0866F;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        mc.thePlayer.speedInAir = 0.02F;
        mc.timer.timerSpeed = 1F;
        super.onDisable();
    }

    @Override
    public void onMotion() {

    }

    @Override
    public void onUpdate() {
        if(MovementUtils.isMoving()) {
            if(mc.thePlayer.onGround) {
                mc.thePlayer.jump();
                mc.thePlayer.motionX *= 1.01D;
                mc.thePlayer.motionZ *= 1.01D;
                mc.thePlayer.speedInAir = 0.0223F;
            }

            mc.thePlayer.motionY -= 0.00099999D;

            MovementUtils.strafe();
        }else{
            mc.thePlayer.motionX = 0D;
            mc.thePlayer.motionZ = 0D;
        }
    }

    @Override
    public void onMove(MoveEvent event) {

    }
}
