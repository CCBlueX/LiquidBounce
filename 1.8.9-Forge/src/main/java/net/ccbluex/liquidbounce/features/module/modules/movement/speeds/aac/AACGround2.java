/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.aac;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.MoveEvent;
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed;
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode;
import net.ccbluex.liquidbounce.utils.MovementUtils;

public class AACGround2 extends SpeedMode {
    public AACGround2() {
        super("AACGround2");
    }

    @Override
    public void onMotion() {

    }

    @Override
    public void onUpdate() {
        if(!MovementUtils.isMoving())
            return;

        mc.timer.timerSpeed = ((Speed) LiquidBounce.moduleManager.getModule(Speed.class)).aacGroundTimerValue.get();
        MovementUtils.strafe(0.02F);
    }

    @Override
    public void onMove(MoveEvent event) {

    }

    @Override
    public void onDisable() {
        mc.timer.timerSpeed = 1F;
    }
}
