/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.RotationUtils.limitAngleChange
import net.ccbluex.liquidbounce.utils.RotationUtils.serverRotation
import net.ccbluex.liquidbounce.utils.RotationUtils.setTargetRotation
import net.ccbluex.liquidbounce.utils.RotationUtils.toRotation
import net.ccbluex.liquidbounce.utils.extensions.eyes
import net.ccbluex.liquidbounce.utils.extensions.getNearestPointBB
import net.ccbluex.liquidbounce.utils.extensions.hitBox
import net.ccbluex.liquidbounce.utils.misc.RandomUtils.nextFloat
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.entity.projectile.EntityFireball
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C02PacketUseEntity.Action.ATTACK
import net.minecraft.network.play.client.C0APacketAnimation

object AntiFireball : Module("AntiFireball", ModuleCategory.PLAYER) {
    private val timer = MSTimer()

    private val range by FloatValue("Range", 4.5f, 3f..8f)
    private val delay by IntegerValue("Delay", 300, 50..1000)
    private val swing by ListValue("Swing", arrayOf("Normal", "Packet", "None"), "Normal")
    private val rotations by BoolValue("Rotations", true)
    private val strafe by BoolValue("Strafe", false) { rotations }

    private val maxTurnSpeedValue: FloatValue = object : FloatValue("MaxTurnSpeed", 120f, 0f..180f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtLeast(minTurnSpeed)
    }
    private val maxTurnSpeed by maxTurnSpeedValue

    private val minTurnSpeed by object : FloatValue("MinTurnSpeed", 80f, 0f..180f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtMost(maxTurnSpeed)

        override fun isSupported() = !maxTurnSpeedValue.isMinimal()
    }

    private val angleThresholdUntilReset by FloatValue("AngleThresholdUntilReset", 5f, 0.1f..180f)

    @EventTarget
    private fun onUpdate(event: UpdateEvent) {
        if (!timer.hasTimePassed(delay)) return

        val eyesVec = mc.thePlayer.eyes

        mc.theWorld.loadedEntityList.filterIsInstance<EntityFireball>().forEach { entity ->
            val nearestPoint = getNearestPointBB(eyesVec, entity.hitBox)

            if (eyesVec.distanceTo(nearestPoint) > range) return@forEach

            if (rotations) setTargetRotation(
                limitAngleChange(
                    serverRotation, toRotation(nearestPoint, true), nextFloat(minTurnSpeed, maxTurnSpeed)
                ),
                strafe = this.strafe,
                resetSpeed = minTurnSpeed to maxTurnSpeed,
                angleThresholdForReset = angleThresholdUntilReset
            )

            sendPacket(C02PacketUseEntity(entity, ATTACK))

            when (swing) {
                "Normal" -> mc.thePlayer.swingItem()
                "Packet" -> sendPacket(C0APacketAnimation())
            }

            timer.reset()
            return
        }
    }
}