/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat;

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity;
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityLivingBase;
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPlayerSP;
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketUseEntity;
import net.ccbluex.liquidbounce.api.minecraft.util.WVec3;
import net.ccbluex.liquidbounce.event.EventState;
import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.MotionEvent;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.utils.*;

@ModuleInfo(name = "TeleportHit", description = "Allows to hit entities from far away.", category = ModuleCategory.COMBAT)
public class TeleportHit extends Module {
    private IEntityLivingBase targetEntity;
    private boolean shouldHit;

    @EventTarget
    public void onMotion(MotionEvent event) {
        if (event.getEventState() != EventState.PRE)
            return;

        final IEntity facedEntity = RaycastUtils.raycastEntity(100D, classProvider::isEntityLivingBase);

        IEntityPlayerSP thePlayer = mc.getThePlayer();

        if (thePlayer == null)
            return;

        if (mc.getGameSettings().getKeyBindAttack().isKeyDown() && EntityUtils.isSelected(facedEntity, true) && facedEntity.getDistanceSqToEntity(thePlayer) >= 1D)
            targetEntity = facedEntity.asEntityLivingBase();

        if (targetEntity != null) {
            if (!shouldHit) {
                shouldHit = true;
                return;
            }

            if (thePlayer.getFallDistance() > 0F) {
                final WVec3 rotationVector = RotationUtils.getVectorForRotation(new Rotation(thePlayer.getRotationYaw(), 0F));
                final double x = thePlayer.getPosX() + rotationVector.getXCoord() * (thePlayer.getDistanceToEntity(targetEntity) - 1.0F);
                final double z = thePlayer.getPosZ() + rotationVector.getZCoord() * (thePlayer.getDistanceToEntity(targetEntity) - 1.0F);
                final double y = targetEntity.getPosition().getY() + 0.25D;

                PathUtils.findPath(x, y + 1.0D, z, 4D).forEach(pos -> mc.getNetHandler().addToSendQueue(classProvider.createCPacketPlayerPosition(pos.getX(), pos.getY(), pos.getZ(), false)));

                thePlayer.swingItem();
                mc.getNetHandler().addToSendQueue(classProvider.createCPacketUseEntity(targetEntity, ICPacketUseEntity.WAction.ATTACK));
                thePlayer.onCriticalHit(targetEntity);
                shouldHit = false;
                targetEntity = null;
            } else if (thePlayer.getOnGround())
                thePlayer.jump();
        } else
            shouldHit = false;
    }
}
