package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.aac;

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
public class AACLowHop extends SpeedMode {

    private boolean legitJump;

    public AACLowHop() {
        super("AACLowHop");
    }

    @Override
    public void onEnable() {
        legitJump = true;
        super.onEnable();
    }

    @Override
    public void onMotion() {
        if(MovementUtils.isMoving()) {
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
