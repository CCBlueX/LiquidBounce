/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.spectre;

import net.ccbluex.liquidbounce.event.MoveEvent;
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode;
import net.ccbluex.liquidbounce.utils.MovementUtils;
import net.minecraft.util.MathHelper;

public class SpectreOnGround extends SpeedMode {

    private int speedUp;

    public SpectreOnGround() {
        super("SpectreOnGround");
    }

    @Override
    public void onMotion() {
    }

    @Override
    public void onUpdate() {
    }

    @Override
    public void onMove(MoveEvent event) {
        if(!MovementUtils.isMoving() || mc.thePlayer.movementInput.jump)
            return;

        if(speedUp >= 10) {
            if(mc.thePlayer.onGround) {
                mc.thePlayer.motionX = 0D;
                mc.thePlayer.motionZ = 0D;
                speedUp = 0;
            }
            return;
        }

        if(mc.thePlayer.onGround && mc.gameSettings.keyBindForward.isKeyDown()) {
            final float f = mc.thePlayer.rotationYaw * 0.017453292F;
            mc.thePlayer.motionX -= MathHelper.sin(f) * 0.145F;
            mc.thePlayer.motionZ += MathHelper.cos(f) * 0.145F;
            event.setX(mc.thePlayer.motionX);
            event.setY(0.005);
            event.setZ(mc.thePlayer.motionZ);

            speedUp++;
        }
    }
}
