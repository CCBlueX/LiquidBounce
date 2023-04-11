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
import net.minecraft.network.play.client.C0APacketAnimation

object AntiFireBall : Module("AntiFireBall", ModuleCategory.PLAYER) {
    private val timer = MSTimer()

    private val rangeValue = FloatValue("Range", 4.5f, 3f, 8f)
    private val delayValue = IntegerValue("Delay", 300, 50, 1000)
    private val swingValue = ListValue("Swing", arrayOf("Normal", "Packet", "None"), "Normal")
    private val rotationsValue = BoolValue("Rotations", true)

    private val maxTurnSpeedValue: FloatValue =
        object : FloatValue("MaxTurnSpeed", 120f, 0f, 180f) {
            override fun onChanged(oldValue: Float, newValue: Float) {
                val i = minTurnSpeedValue.get()
                if (i > newValue) set(i)
            }
        }
    private val minTurnSpeedValue: FloatValue =
        object : FloatValue("MinTurnSpeed", 80f, 0f, 180f) {
            override fun onChanged(oldValue: Float, newValue: Float) {
                val i = maxTurnSpeedValue.get()
                if (i < newValue) set(i)
            }

            override fun isSupported() = !maxTurnSpeedValue.isMinimal()
        }

    @EventTarget
    private fun onUpdate(event: UpdateEvent) {
        if (!timer.hasTimePassed(delayValue.get())) return

        val eyesVec = mc.thePlayer.eyes

        mc.theWorld.loadedEntityList.filterIsInstance<EntityFireball>().forEach { entity ->
            val nearestPoint = getNearestPointBB(eyesVec, entity.hitBox)

            if (eyesVec.distanceTo(nearestPoint) > rangeValue.get()) return@forEach

            if (rotationsValue.get())
                setTargetRotation(
                    limitAngleChange(serverRotation, toRotation(nearestPoint, true),
                        nextFloat(minTurnSpeedValue.get(), maxTurnSpeedValue.get())
                    )
                )

            mc.thePlayer.sendQueue.addToSendQueue(C02PacketUseEntity(entity, C02PacketUseEntity.Action.ATTACK))

            when (swingValue.get()) {
                "Normal" -> mc.thePlayer.swingItem()
                "Packet" -> mc.netHandler.addToSendQueue(C0APacketAnimation())
            }

            timer.reset()
            return
        }
    }
}