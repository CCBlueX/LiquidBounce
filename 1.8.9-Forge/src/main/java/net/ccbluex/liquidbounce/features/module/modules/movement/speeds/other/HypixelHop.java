/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.other;

import net.ccbluex.liquidbounce.event.MoveEvent;
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode;
import net.ccbluex.liquidbounce.utils.MovementUtils;
import net.minecraft.potion.Potion;

public class HypixelHop extends SpeedMode {

    public HypixelHop() {
        super("HypixelHop");
    }

    @Override
    public void onMotion() {
        if(MovementUtils.isMoving()) {

            if(mc.thePlayer.onGround) {
                mc.thePlayer.jump();

                float speed = MovementUtils.getSpeed() < 0.56F ? MovementUtils.getSpeed() * 1.045F : 0.56F;

                if(mc.thePlayer.onGround && mc.thePlayer.isPotionActive(Potion.moveSpeed))
                    speed *= 1F + 0.13F * (1 + mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).getAmplifier());

                MovementUtils.strafe(speed);
                return;
            }else if(mc.thePlayer.motionY < 0.2D)
                mc.thePlayer.motionY -= 0.02D;

            MovementUtils.strafe(MovementUtils.getSpeed() * 1.01889F);
        }else{
            mc.thePlayer.motionX = mc.thePlayer.motionZ = 0D;
        }
    }

    @Override
    public void onUpdate() {
    }

    @Override
    public void onMove(final MoveEvent event) {
    }
}