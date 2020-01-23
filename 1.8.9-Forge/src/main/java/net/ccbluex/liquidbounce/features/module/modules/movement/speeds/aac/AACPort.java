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
import net.ccbluex.liquidbounce.utils.block.BlockUtils;
import net.minecraft.block.BlockAir;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;

public class AACPort extends SpeedMode {
    public AACPort() {
        super("AACPort");
    }

    @Override
    public void onMotion() {

    }

    @Override
    public void onUpdate() {
        if(!MovementUtils.isMoving())
            return;

        final float f = mc.thePlayer.rotationYaw * 0.017453292F;
        for (double d = 0.2; d <= ((Speed) LiquidBounce.moduleManager.getModule(Speed.class)).portMax.get(); d += 0.2) {
            final double x = mc.thePlayer.posX - MathHelper.sin(f) * d;
            final double z = mc.thePlayer.posZ + MathHelper.cos(f) * d;

            if(mc.thePlayer.posY < (int) mc.thePlayer.posY + 0.5 && !(BlockUtils.getBlock(new BlockPos(x, mc.thePlayer.posY, z)) instanceof BlockAir))
                break;

            mc.thePlayer.sendQueue.addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(x, mc.thePlayer.posY, z, true));
        }
    }

    @Override
    public void onMove(MoveEvent event) {

    }
}