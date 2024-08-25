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
import net.minecraft.entity.LivingEntity
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionOnly
import net.minecraft.util.Vec3d

object TeleportHit : Module("TeleportHit", Category.COMBAT, hideModule = false) {

    private var targetEntity: LivingEntity? = null
    private var shouldHit = false

    @EventTarget
    fun onMotion(event: MotionEvent) {
        if (event.eventState != EventState.PRE)
            return

        val facedEntity = RaycastUtils.raycastEntity(100.0) { raycastedEntity -> raycastedEntity is LivingEntity }

        val thePlayer: EntityPlayerSP = mc.player ?: return

        if (mc.gameSettings.keyBindAttack.isKeyDown && isSelected(facedEntity, true)) {
            if (facedEntity?.squaredDistanceToToEntity(mc.player)!! >= 1) targetEntity = facedEntity as LivingEntity
        }

        targetEntity?.let {
            if (!shouldHit) {
                shouldHit = true
                return
            }

            if (thePlayer.fallDistance > 0F) {
                val rotationVector: Vec3d = RotationUtils.getVectorForRotation(mc.player.yaw, 0f)
                val x = mc.player.x + rotationVector.xCoord * (mc.player.distanceTo(it) - 1f)
                val z = mc.player.z + rotationVector.zCoord * (mc.player.distanceTo(it) - 1f)
                val y = it.y + 0.25

                findPath(x, y + 1, z, 4.0).forEach { pos -> sendPacket(PositionOnly(pos.x, pos.y, pos.z, false)) }

                thePlayer.swingItem()
                sendPacket(PlayerInteractEntityC2SPacket(it, PlayerInteractEntityC2SPacket.Action.ATTACK))
                thePlayer.onCriticalHit(it)
                shouldHit = false
                targetEntity = null
            } else if (thePlayer.onGround) {
                thePlayer.jump()
            }
        } ?: run { shouldHit = false }
    }
}
