/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.aac;

import net.ccbluex.liquidbounce.event.MoveEvent;
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode;
import net.ccbluex.liquidbounce.utils.MovementUtils;

        public class AAC4Hop extends SpeedMode {
        private boolean legitJump;

        public AAC4Hop() {
        super("AACv4Hop");
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
        legitJump = true;
        return;
        }

        mc.thePlayer.motionY = 0.42F;
        MovementUtils.strafe(0.0F);
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
