package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.other;

import net.ccbluex.liquidbounce.event.MoveEvent;
import net.ccbluex.liquidbounce.features.module.ModuleManager;
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed;
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode;
import net.ccbluex.liquidbounce.utils.MovementUtils;

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
public class CustomSpeed extends SpeedMode {

    public CustomSpeed() {
        super("Custom");
    }

    @Override
    public void onMotion() {
        if(MovementUtils.isMoving()) {
            final Speed speed = (Speed) ModuleManager.getModule(Speed.class);

            if(speed == null)
                return;

            mc.timer.timerSpeed = speed.customTimerValue.get();

            if(mc.thePlayer.onGround) {
                MovementUtils.strafe(speed.customSpeedValue.get());
                mc.thePlayer.motionY = speed.customYValue.get();
            }else if(speed.customStrafeValue.get())
                MovementUtils.strafe(speed.customSpeedValue.get());
            else
                MovementUtils.strafe();
        }else
            mc.thePlayer.motionX = mc.thePlayer.motionZ = 0D;
    }

    @Override
    public void onEnable() {
        final Speed speed = (Speed) ModuleManager.getModule(Speed.class);

        if(speed == null)
            return;

        if(speed.resetXZValue.get()) mc.thePlayer.motionX = mc.thePlayer.motionZ = 0D;
        if(speed.resetYValue.get()) mc.thePlayer.motionY = 0D;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        mc.timer.timerSpeed = 1F;
        super.onDisable();
    }

    @Override
    public void onUpdate() {

    }

    @Override
    public void onMove(MoveEvent event) {
    }
}
