package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.*
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import java.util.function.Consumer
import javax.vecmath.Vector3d

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
@ModuleInfo(name = "TeleportHit", description = "Allows to hit entities from far away.", category = ModuleCategory.COMBAT)
class TeleportHit : Module() {
    private var targetEntity: EntityLivingBase? = null
    private var shouldHit = false

    @EventTarget
    fun onMotion(event: MotionEvent) {
        if (event.eventState !== EventState.PRE)
            return

        val facedEntity = RaycastUtils.raycastEntity(100.0) { raycastedEntity: Entity? -> raycastedEntity is EntityLivingBase }

        if (mc.gameSettings.keyBindAttack.isKeyDown && EntityUtils.isSelected(facedEntity, true) && facedEntity.getDistanceSqToEntity(mc.thePlayer) >= 1.0)
            targetEntity = facedEntity as EntityLivingBase

        if (targetEntity != null) {
            if (!shouldHit) {
                shouldHit = true
                return
            }

            if (mc.thePlayer.fallDistance > 0f) {
                val rotationVector = RotationUtils.getVectorForRotation(Rotation(mc.thePlayer.rotationYaw, 0f))
                val x = mc.thePlayer.posX + rotationVector.xCoord * (mc.thePlayer.getDistanceToEntity(targetEntity) - 1.0f)
                val z = mc.thePlayer.posZ + rotationVector.zCoord * (mc.thePlayer.getDistanceToEntity(targetEntity) - 1.0f)
                val y = targetEntity!!.position.y + 0.25

                PathUtils.findPath(x, y + 1.0, z, 4.0).forEach(Consumer { pos: Vector3d -> mc.netHandler.addToSendQueue(C04PacketPlayerPosition(pos.getX(), pos.getY(), pos.getZ(), false)) })

                mc.thePlayer.swingItem()
                mc.thePlayer.sendQueue.addToSendQueue(C02PacketUseEntity(targetEntity, C02PacketUseEntity.Action.ATTACK))
                mc.thePlayer.onCriticalHit(targetEntity)

                shouldHit = false
                targetEntity = null
            } else if (mc.thePlayer.onGround)
                mc.thePlayer.jump()

        } else shouldHit = false
    }
}