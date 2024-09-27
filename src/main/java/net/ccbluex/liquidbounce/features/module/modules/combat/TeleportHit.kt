/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.utils.EntityUtils.isSelected
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.PathUtils.findPath
import net.ccbluex.liquidbounce.utils.RaycastUtils
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.util.Vec3

object TeleportHit : Module("TeleportHit", Category.COMBAT, hideModule = false) {

    private var targetEntity: EntityLivingBase? = null
    private var shouldHit = false

    @EventTarget
    fun onMotion(event: MotionEvent) {
        if (event.eventState != EventState.PRE)
            return

        val facedEntity = RaycastUtils.raycastEntity(100.0) { raycastedEntity -> raycastedEntity is EntityLivingBase }

        val player: EntityPlayerSP = mc.thePlayer ?: return

        if (mc.gameSettings.keyBindAttack.isKeyDown && isSelected(facedEntity, true)) {
            if (facedEntity?.getDistanceSqToEntity(mc.thePlayer)!! >= 1) targetEntity = facedEntity as EntityLivingBase
        }

        targetEntity?.let {
            if (!shouldHit) {
                shouldHit = true
                return
            }

            if (player.fallDistance > 0F) {
                val rotationVector: Vec3 = RotationUtils.getVectorForRotation(mc.thePlayer.rotationYaw, 0f)
                val x = mc.thePlayer.posX + rotationVector.xCoord * (mc.thePlayer.getDistanceToEntity(it) - 1f)
                val z = mc.thePlayer.posZ + rotationVector.zCoord * (mc.thePlayer.getDistanceToEntity(it) - 1f)
                val y = it.posY + 0.25

                findPath(x, y + 1, z, 4.0).forEach { pos -> sendPacket(C04PacketPlayerPosition(pos.x, pos.y, pos.z, false)) }

                player.swingItem()
                sendPacket(C02PacketUseEntity(it, C02PacketUseEntity.Action.ATTACK))
                player.onCriticalHit(it)
                shouldHit = false
                targetEntity = null
            } else if (player.onGround) {
                player.jump()
            }
        } ?: run { shouldHit = false }
    }
}
