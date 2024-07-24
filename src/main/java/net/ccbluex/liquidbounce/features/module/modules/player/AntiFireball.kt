/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.GameTickEvent
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.RotationUtils.currentRotation
import net.ccbluex.liquidbounce.utils.RotationUtils.isRotationFaced
import net.ccbluex.liquidbounce.utils.RotationUtils.setTargetRotation
import net.ccbluex.liquidbounce.utils.RotationUtils.toRotation
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.entity.Entity
import net.minecraft.entity.projectile.EntityFireball
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.world.WorldSettings

object AntiFireball : Module("AntiFireball", Category.PLAYER, hideModule = false) {
    private val range by FloatValue("Range", 4.5f, 3f..8f)
    private val swing by ListValue("Swing", arrayOf("Normal", "Packet", "None"), "Normal")

    private val rotations by BoolValue("Rotations", true)
    private val smootherMode by ListValue("SmootherMode", arrayOf("Linear", "Relative"), "Relative") { rotations }
    private val strafe by BoolValue("Strafe", false) { rotations }

    private val simulateShortStop by BoolValue("SimulateShortStop", false) { rotations }
    private val startFirstRotationSlow by BoolValue("StartFirstRotationSlow", false) { rotations }
    private val maxHorizontalSpeedValue = object : FloatValue("MaxHorizontalSpeed", 180f, 1f..180f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtLeast(minHorizontalSpeed)
        override fun isSupported() = rotations

    }
    private val maxHorizontalSpeed by maxHorizontalSpeedValue

    private val minHorizontalSpeed: Float by object : FloatValue("MinHorizontalSpeed", 180f, 1f..180f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtMost(maxHorizontalSpeed)
        override fun isSupported() = !maxHorizontalSpeedValue.isMinimal() && rotations
    }

    private val maxVerticalSpeedValue = object : FloatValue("MaxVerticalSpeed", 180f, 1f..180f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtLeast(minVerticalSpeed)
    }
    private val maxVerticalSpeed by maxVerticalSpeedValue

    private val minVerticalSpeed: Float by object : FloatValue("MinVerticalSpeed", 180f, 1f..180f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtMost(maxVerticalSpeed)
        override fun isSupported() = !maxVerticalSpeedValue.isMinimal() && rotations
    }

    private val angleThresholdUntilReset by FloatValue("AngleThresholdUntilReset", 5f, 0.1f..180f) { rotations }

    private var target: Entity? = null

    @EventTarget
    private fun onMotion(event: MotionEvent) {
        val player = mc.thePlayer ?: return

        if (event.eventState != EventState.POST)
            return

        target = null

        for (entity in mc.theWorld.loadedEntityList.filterIsInstance<EntityFireball>()
            .sortedBy { player.getDistanceToBox(it.hitBox) }) {
            val nearestPoint = getNearestPointBB(player.eyes, entity.hitBox)

            val entityPrediction = entity.currPos - entity.prevPos

            val normalDistance = player.getDistanceToBox(entity.hitBox)

            val predictedDistance = player.getDistanceToBox(
                entity.hitBox.offset(
                    entityPrediction.xCoord,
                    entityPrediction.yCoord,
                    entityPrediction.zCoord
                )
            )

            // Skip if the predicted distance is (further than/same as) the normal distance or the predicted distance is out of reach
            if (predictedDistance >= normalDistance || predictedDistance > range) {
                continue
            }

            if (rotations) {
                setTargetRotation(
                    toRotation(nearestPoint, true),
                    strafe = this.strafe,
                    turnSpeed = minHorizontalSpeed..maxHorizontalSpeed to minVerticalSpeed..maxVerticalSpeed,
                    angleThresholdForReset = angleThresholdUntilReset,
                    smootherMode = smootherMode,
                    simulateShortStop = simulateShortStop,
                    startOffSlow = startFirstRotationSlow
                )
            }

            target = entity
            break
        }
    }

    @EventTarget
    fun onTick(event: GameTickEvent) {
        val player = mc.thePlayer ?: return
        val entity = target ?: return

        val rotation = currentRotation ?: player.rotation

        if (!rotations && player.getDistanceToBox(entity.hitBox) <= range
            || isRotationFaced(entity, range.toDouble(), rotation)
        ) {
            when (swing) {
                "Normal" -> mc.thePlayer.swingItem()
                "Packet" -> sendPacket(C0APacketAnimation())
            }

            sendPacket(C02PacketUseEntity(entity, C02PacketUseEntity.Action.ATTACK))

            if (mc.playerController.currentGameType != WorldSettings.GameType.SPECTATOR) {
                player.attackTargetEntityWithCurrentItem(entity)
            }

            target = null
        }
    }
}