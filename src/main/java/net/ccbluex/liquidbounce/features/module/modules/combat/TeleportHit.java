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
import net.ccbluex.liquidbounce.utils.*;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.util.Vec3;

import static net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket;
import static net.minecraft.network.play.client.C02PacketUseEntity.Action.ATTACK;
import static net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition;

public class TeleportHit extends Module {

    public TeleportHit() {
        super("TeleportHit", ModuleCategory.COMBAT);
    }
    private EntityLivingBase targetEntity;
    private boolean shouldHit;

    @EventTarget
    public void onMotion(MotionEvent event) {
        if (event.getEventState() != EventState.PRE)
            return;

        final Entity facedEntity = RaycastUtils.INSTANCE.raycastEntity(100, raycastedEntity -> raycastedEntity instanceof EntityLivingBase);

        EntityPlayerSP thePlayer = mc.thePlayer;

        if (thePlayer == null)
            return;

        if (mc.gameSettings.keyBindAttack.isKeyDown() && EntityUtils.INSTANCE.isSelected(facedEntity, true)) {
            if (facedEntity.getDistanceSqToEntity(mc.thePlayer) >= 1) targetEntity = (EntityLivingBase) facedEntity;
        }

        if (targetEntity != null) {
            if (!shouldHit) {
                shouldHit = true;
                return;
            }

            if (thePlayer.fallDistance > 0F) {
                final Vec3 rotationVector = RotationUtils.INSTANCE.getVectorForRotation(new Rotation(mc.thePlayer.rotationYaw, 0F));
                final double x = mc.thePlayer.posX + rotationVector.xCoord * (mc.thePlayer.getDistanceToEntity(targetEntity) - 1f);
                final double z = mc.thePlayer.posZ + rotationVector.zCoord * (mc.thePlayer.getDistanceToEntity(targetEntity) - 1f);
                final double y = targetEntity.getPosition().getY() + 0.25;

                PathUtils.INSTANCE.findPath(x, y + 1, z, 4).forEach(pos -> sendPacket(new C04PacketPlayerPosition(pos.getX(), pos.getY(), pos.getZ(), false)));

                thePlayer.swingItem();
                sendPacket(new C02PacketUseEntity(targetEntity, ATTACK));
                thePlayer.onCriticalHit(targetEntity);
                shouldHit = false;
                targetEntity = null;
            } else if (thePlayer.onGround)
                thePlayer.jump();
        } else
            shouldHit = false;
    }
}
