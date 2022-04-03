/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat;

import net.ccbluex.liquidbounce.event.EventState;
import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.MotionEvent;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.utils.*;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.Vec3;

@ModuleInfo(name = "TeleportHit", description = "Allows to hit entities from far away.", category = ModuleCategory.COMBAT)
public class TeleportHit extends Module {
    private EntityLivingBase targetEntity;
    private boolean shouldHit;

    @EventTarget
    public void onMotion(MotionEvent event) {
        if (event.getEventState() != EventState.PRE)
            return;

        final Entity facedEntity = RaycastUtils.raycastEntity(100D, raycastedEntity -> raycastedEntity instanceof EntityLivingBase);

        EntityPlayerSP thePlayer = mc.thePlayer;

        if (thePlayer == null)
            return;

        if(mc.gameSettings.keyBindAttack.isKeyDown() && EntityUtils.isSelected(facedEntity, true)) {
            if (facedEntity.getDistanceSqToEntity(mc.thePlayer) >= 1D) targetEntity = (EntityLivingBase) facedEntity;
        }

        if (targetEntity != null) {
            if (!shouldHit) {
                shouldHit = true;
                return;
            }

            if (thePlayer.fallDistance > 0F) {
                final Vec3 rotationVector = RotationUtils.getVectorForRotation(new Rotation(mc.thePlayer.rotationYaw, 0F));
                final double x = mc.thePlayer.posX + rotationVector.xCoord * (mc.thePlayer.getDistanceToEntity(targetEntity) - 1.0F);
                final double z = mc.thePlayer.posZ + rotationVector.zCoord * (mc.thePlayer.getDistanceToEntity(targetEntity) - 1.0F);
                final double y = targetEntity.getPosition().getY() + 0.25D;

                PathUtils.findPath(x, y + 1.0D, z, 4D).forEach(pos -> mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(pos.getX(), pos.getY(), pos.getZ(), false)));

                thePlayer.swingItem();
                mc.thePlayer.sendQueue.addToSendQueue(new C02PacketUseEntity(targetEntity, C02PacketUseEntity.Action.ATTACK));
                thePlayer.onCriticalHit(targetEntity);
                shouldHit = false;
                targetEntity = null;
            } else if (thePlayer.onGround)
                thePlayer.jump();
        } else
            shouldHit = false;
    }
}
