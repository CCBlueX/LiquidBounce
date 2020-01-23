/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.aac;

import net.ccbluex.liquidbounce.event.MoveEvent;
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode;
import net.ccbluex.liquidbounce.utils.MovementUtils;

public class AAC4BHop extends SpeedMode {
    private boolean legitHop;

    public AAC4BHop() {
        super("AAC4BHop");
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
    public void onEnable() {
        legitHop = true;
    }

    @Override
    public void onDisable() {
        mc.thePlayer.speedInAir = 0.02F;
    }

    @Override
    public void onTick() {
        if(MovementUtils.isMoving()) {
            if(legitHop) {
                if(mc.thePlayer.onGround) {
                    mc.thePlayer.jump();
                    mc.thePlayer.onGround = false;
                    legitHop = false;
                }
                return;
            }

            if(mc.thePlayer.onGround) {
                mc.thePlayer.onGround = false;
                MovementUtils.strafe(0.375F);
                mc.thePlayer.jump();
                mc.thePlayer.motionY = 0.41;
            }else
                mc.thePlayer.speedInAir = 0.0211F;
        }else{
            mc.thePlayer.motionX = 0D;
            mc.thePlayer.motionZ = 0D;
            legitHop = true;
        }
    }
}
