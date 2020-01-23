/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.aac;

import net.ccbluex.liquidbounce.event.MoveEvent;
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode;
import net.ccbluex.liquidbounce.utils.MovementUtils;
import net.minecraft.util.MathHelper;

public class AACBHop extends SpeedMode {
    public AACBHop() {
        super("AACBHop");
    }

    @Override
    public void onMotion() {
        if(mc.thePlayer.isInWater())
            return;

        if(MovementUtils.isMoving()) {
            mc.timer.timerSpeed = 1.08F;

            if(mc.thePlayer.onGround) {
                mc.thePlayer.motionY = 0.399D;
                float f = mc.thePlayer.rotationYaw * 0.017453292F;
                mc.thePlayer.motionX -= MathHelper.sin(f) * 0.2F;
                mc.thePlayer.motionZ += MathHelper.cos(f) * 0.2F;
                mc.timer.timerSpeed = 2F;
            }else{
                mc.thePlayer.motionY *= 0.97D;
                mc.thePlayer.motionX *= 1.008D;
                mc.thePlayer.motionZ *= 1.008D;
            }
        }else{
            mc.thePlayer.motionX = 0D;
            mc.thePlayer.motionZ = 0D;
            mc.timer.timerSpeed = 1F;
        }
    }

    @Override
    public void onUpdate() {

    }

    @Override
    public void onMove(MoveEvent event) {
    }

    @Override
    public void onDisable() {
        mc.timer.timerSpeed = 1F;
    }
}
