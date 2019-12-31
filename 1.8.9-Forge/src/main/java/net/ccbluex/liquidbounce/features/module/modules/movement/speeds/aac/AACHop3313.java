package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.aac;

import net.ccbluex.liquidbounce.event.MoveEvent;
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode;
import net.ccbluex.liquidbounce.utils.MovementUtils;
import net.ccbluex.liquidbounce.utils.misc.RandomUtils;

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
public class AACHop3313 extends SpeedMode {

    private boolean damageToGround;

    public AACHop3313() {
        super("AACHop3.3.13");
    }

    @Override
    public void onMotion() {
        if(!MovementUtils.isMoving() || mc.thePlayer.isInWater() || mc.thePlayer.isInLava() ||
                mc.thePlayer.isOnLadder() || mc.thePlayer.isRiding())
            return;

        if(mc.thePlayer.hurtTime > 0) {
            damageToGround = true;
            return;
        }

        if(mc.thePlayer.onGround && mc.thePlayer.isCollidedVertically) {
            // MotionXYZ
            mc.thePlayer.jump();
            if(!damageToGround) mc.thePlayer.motionY = 0.41;
            damageToGround = false;
        }else if(!mc.thePlayer.isCollidedHorizontally && !damageToGround) {
            // Motion XZ
            mc.thePlayer.jumpMovementFactor = 0.027F;

            final float boostUp = mc.thePlayer.motionY <= 0F ? RandomUtils.nextFloat(1.002F, 1.0023F) : RandomUtils.nextFloat(1.0059F, 1.0061F);
            mc.thePlayer.motionX *= boostUp;
            mc.thePlayer.motionZ *= boostUp;

            MovementUtils.forward(0.0019);

            // Motion Y
            mc.thePlayer.motionY -= 0.0149F;
        }
    }

    @Override
    public void onUpdate() {
    }

    @Override
    public void onMove(final MoveEvent event) {
    }

    @Override
    public void onDisable() {
        mc.thePlayer.jumpMovementFactor = 0.02F;
    }
}
