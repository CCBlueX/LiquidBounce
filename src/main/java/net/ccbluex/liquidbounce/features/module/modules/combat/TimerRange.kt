/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.LiquidBounce.hud
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.player.Reach
import net.ccbluex.liquidbounce.script.api.global.Chat
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification
import net.ccbluex.liquidbounce.utils.BlinkUtils
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.EntityUtils.isLookingOnEntities
import net.ccbluex.liquidbounce.utils.PacketUtils.queuedPackets
import net.ccbluex.liquidbounce.utils.RotationUtils.searchCenter
import net.ccbluex.liquidbounce.utils.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.misc.RandomUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawEntityBox
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawPlatform
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.network.Packet
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket
import net.minecraft.network.packet.c2s.play.C19PacketResourcePackStatus
import net.minecraft.network.packet.s2c.play.S06PacketUpdateHealth
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket
import net.minecraft.network.packet.s2c.play.S12PacketEntityVelocity
import net.minecraft.network.packet.s2c.play.S27PacketExplosion
import java.awt.Color

object TimerRange : Module("TimerRange", Category.COMBAT, hideModule = false) {

    private var playerTicks = 0
    private var smartTick = 0
    private var cooldownTick = 0
    private var randomRange = 0f

    private val packets = mutableListOf<Packet<*>>()
    private val packetsReceived = mutableListOf<Packet<*>>()
    private var blinked = false

    // Condition to confirm
    private var shouldReset = false
    private var confirmTick = false
    private var confirmMove = false
    private var confirmStop = false

    // Condition to prevent getting timer speed stuck
    private var confirmAttack = false

    private val timerBoostMode by ListValue("TimerMode", arrayOf("Normal", "Smart", "Modern"), "Modern")

    private val ticksValue by IntegerValue("Ticks", 10, 1..20)

    // Min & Max Boost Delay Settings
    private val timerBoostValue by FloatValue("TimerBoost", 1.5f, 0.01f..35f)

    private val minBoostDelay: FloatValue = object : FloatValue("MinBoostDelay", 0.5f, 0.1f..1.0f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtMost(maxBoostDelay.get())
    }

    private val maxBoostDelay: FloatValue = object : FloatValue("MaxBoostDelay", 0.55f, 0.1f..1.0f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtLeast(minBoostDelay.get())
    }

    // Min & Max Charged Delay Settings
    private val timerChargedValue by FloatValue("TimerCharged", 0.45f, 0.05f..5f)

    private val minChargedDelay: FloatValue = object : FloatValue("MinChargedDelay", 0.75f, 0.1f..1.0f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtMost(maxChargedDelay.get())
    }

    private val maxChargedDelay: FloatValue = object : FloatValue("MaxChargedDelay", 0.9f, 0.1f..1.0f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtLeast(minChargedDelay.get())
    }

    // Normal Mode Settings
    private val rangeValue by FloatValue("Range", 3.5f, 1f..5f) { timerBoostMode == "Normal" }
    private val cooldownTickValue by IntegerValue("CooldownTick", 10, 1..50) { timerBoostMode == "Normal" }

    // Smart & Modern Mode Range
    private val minRange: FloatValue = object : FloatValue("MinRange", 2.5f, 0f..8f) {
        override fun isSupported() = timerBoostMode != "Normal"
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtMost(maxRange.get())
    }

    private val maxRange: FloatValue = object : FloatValue("MaxRange", 3f, 2f..8f) {
        override fun isSupported() = timerBoostMode != "Normal"
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtLeast(minRange.get())
    }

    private val scanRange: FloatValue = object : FloatValue("ScanRange", 8f, minRange.get()..12f) {
        override fun isSupported() = timerBoostMode != "Normal"
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtLeast(maxRange.get())
    }

    // Min & Max Tick Delay
    private val minTickDelay: IntegerValue = object : IntegerValue("MinTickDelay", 30, 1..200) {
        override fun isSupported() = timerBoostMode != "Normal"
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtMost(maxTickDelay.get())
    }
    private val maxTickDelay: IntegerValue = object : IntegerValue("MaxTickDelay", 60, 1..200) {
        override fun isSupported() = timerBoostMode != "Normal"
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtLeast(minTickDelay.get())
    }

    // Blink Option
    private val blink by BoolValue("Blink", false)

    // Prediction Settings
    private val predictClientMovement by IntegerValue("PredictClientMovement", 2, 0..5)
    private val predictEnemyPosition by FloatValue("PredictEnemyPosition", 1.5f, -1f..2f)

    private val maxAngleDifference by FloatValue("MaxAngleDifference", 5f, 5f..90f) { timerBoostMode == "Modern" }

    // Mark Option
    private val markMode by ListValue("Mark", arrayOf("Off", "Box", "Platform"), "Off") { timerBoostMode == "Modern" }
    private val outline by BoolValue("Outline", false) { timerBoostMode == "Modern" && markMode == "Box" }

    // Optional
    private val onWeb by BoolValue("OnWeb", false)
    private val onWater by BoolValue("OnWater", false)
    private val resetOnlagBack by BoolValue("ResetOnLagback", false)
    private val resetOnKnockback by BoolValue("ResetOnKnockback", false)
    private val chatDebug by BoolValue("ChatDebug", true) { resetOnlagBack || resetOnKnockback }
    private val notificationDebug by BoolValue("NotificationDebug", false) { resetOnlagBack || resetOnKnockback }

    override fun onDisable() {
        shouldResetTimer()
        BlinkUtils.unblink()

        smartTick = 0
        cooldownTick = 0
        playerTicks = 0

        shouldReset = false
        blinked = false

        confirmTick = false
        confirmMove = false
        confirmStop = false
        confirmAttack = false
    }

    /**
     * Attack event (Normal & Smart Mode)
     */
    @EventTarget
    fun onAttack(event: AttackEvent) {
        if (event.targetEntity !is LivingEntity && playerTicks >= 1) {
            shouldResetTimer()
            return
        } else {
            confirmAttack = true
        }

        val targetEntity = event.targetEntity ?: return
        val entityDistance = targetEntity.let { mc.player.getDistanceToEntityBox(it) }
        val randomTickDelay = RandomUtils.nextInt(minTickDelay.get(), maxTickDelay.get() + 1)
        var shouldReturn = false

        Backtrack.runWithNearestTrackedDistance(targetEntity) {
            shouldReturn = !updateDistance(targetEntity)
        }

        if (shouldReturn || (mc.player.isInWeb() && !onWeb) || (mc.player.isTouchingWater && !onWater)) {
            return
        }

        smartTick++
        cooldownTick++

        val shouldSlowed = when (timerBoostMode) {
            "Normal" -> cooldownTick >= cooldownTickValue && entityDistance <= rangeValue
            "Smart" -> smartTick >= randomTickDelay && entityDistance <= randomRange
            else -> false
        }

        if (shouldSlowed && confirmAttack) {
            if (updateDistance(targetEntity)) {
                confirmAttack = false
                playerTicks = ticksValue
                cooldownTick = 0
                smartTick = 0
            }
        } else {
            shouldResetTimer()
        }
    }

    /**
     * Move event (Modern Mode)
     */
    @EventTarget
    fun onMove(event: MoveEvent) {
        if (timerBoostMode != "Modern") {
            return
        }

        val nearbyEntity = getNearestEntityInRange() ?: return

        val randomTickDelay = RandomUtils.nextInt(minTickDelay.get(), maxTickDelay.get())

        var shouldReturn = false

        Backtrack.runWithNearestTrackedDistance(nearbyEntity) {
            shouldReturn = !updateDistance(nearbyEntity)
        }

        if (shouldReturn || (mc.player.isInWeb() && !onWeb) || (mc.player.isTouchingWater && !onWater)) {
            return
        }

        if (isPlayerMoving()) {
            smartTick++

            if (smartTick >= randomTickDelay) {
                confirmTick = true
                smartTick = 0
            }
        } else {
            smartTick = 0
            confirmMove = false
        }

        if (isPlayerMoving() && !confirmStop) {
            if (isLookingOnEntities(nearbyEntity, maxAngleDifference.toDouble())) {
                val entityDistance = mc.player.getDistanceToEntityBox(nearbyEntity)
                if (confirmTick && entityDistance <= scanRange.get() && entityDistance >= randomRange) {
                    if (updateDistance(nearbyEntity)) {
                        playerTicks = ticksValue
                        confirmTick = false
                        confirmMove = true
                    }
                }
            } else {
                shouldResetTimer()
            }
        } else {
            shouldResetTimer()
        }
    }

    private fun updateDistance(entity: Entity): Boolean {
        val player = mc.player ?: return false

        val (predictX, predictY, predictZ) = entity.currPos.subtract(entity.prevPos)
            .times(2 + predictEnemyPosition.toDouble())

        val boundingBox = entity.hitBox.offset(predictX, predictY, predictZ)
        val (currPos, oldPos) = player.currPos to player.prevPos

        val simPlayer = SimulatedPlayer.fromClientPlayer(player.movementInput)

        repeat(predictClientMovement + 1) {
            simPlayer.tick()
        }

        player.setPosAndPrevPos(simPlayer.pos)

        val distance = searchCenter(
            boundingBox,
            outborder = false,
            random = false,
            predict = true,
            lookRange = if (timerBoostMode == "Normal") rangeValue else randomRange,
            attackRange = if (Reach.handleEvents()) Reach.combatReach else 3f,
        )

        if (distance == null) {
            player.setPosAndPrevPos(currPos, oldPos)
            return false
        }

        player.setPosAndPrevPos(currPos, oldPos)

        return true
    }

    /**
     * Motion event
     * (Resets player speed when less/more than target distance)
     */
    @EventTarget
    fun onMotion(event: MotionEvent) {
        if (blink && event.eventState == EventState.POST) {
            synchronized(packetsReceived) {
                queuedPackets.addAll(packetsReceived)
            }
            packetsReceived.clear()
        }
    }

    /**
     * World Event
     * (Clear packets on disconnect)
     */
    @EventTarget
    fun onWorld(event: WorldEvent) {
        if (blink && event.worldClient == null) {
            packets.clear()
            packetsReceived.clear()
        }
    }

    /**
     * Update event
     */
    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        // Randomize the timer & charged delay a bit, to bypass some AntiCheat
        val timerboost = RandomUtils.nextFloat(minBoostDelay.get(), maxBoostDelay.get())
        val charged = RandomUtils.nextFloat(minChargedDelay.get(), maxChargedDelay.get())

        if (mc.player != null && mc.world != null) {
            randomRange = RandomUtils.nextFloat(minRange.get(), maxRange.get())
        }

        if (playerTicks <= 0 || confirmStop) {
            shouldResetTimer()

            if (blink && blinked) {
                BlinkUtils.unblink()
                blinked = false
            }

            return
        }

        val tickProgress = playerTicks.toDouble() / ticksValue.toDouble()
        val playerSpeed = when {
            tickProgress < timerboost -> timerBoostValue
            tickProgress < charged -> timerChargedValue
            else -> 1f
        }

        val speedAdjustment = if (playerSpeed >= 0) playerSpeed else 1f + ticksValue - playerTicks
        val adjustedTimerSpeed = maxOf(speedAdjustment, 0f)

        mc.ticker.timerSpeed = adjustedTimerSpeed

        playerTicks--
    }

    /**
     * Render event (Mark)
     */
    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        if (timerBoostMode.lowercase() != "modern") return

        getNearestEntityInRange()?.let { nearbyEntity ->
            val entityDistance = mc.player.getDistanceToEntityBox(nearbyEntity)

            if (entityDistance > scanRange.get()) return@let

            val color = if (isLookingOnEntities(nearbyEntity, maxAngleDifference.toDouble())) {
                Color(37, 126, 255, 70)
            } else {
                Color(210, 60, 60, 70)
            }

            if (markMode != "Off") {
                when (markMode) {
                    "Box" -> drawEntityBox(nearbyEntity, color, outline)
                    "Platform" -> drawPlatform(nearbyEntity, color)
                }
            }
        }
    }

    /**
     * Check if player is moving
     */
    private fun isPlayerMoving(): Boolean {
        return !mc.options.backKey.isPressed && (mc.player?.moveForward != 0f || mc.player?.moveStrafing != 0f)
    }

    /**
     * Get all entities in the world.
     */
    private fun getAllEntities(): List<Entity?>? {
        return mc.world?.entities?.toList()
            ?.filter { EntityUtils.isSelected(it, true) }
    }

    /**
     * Find the nearest entity in range.
     */
    private fun getNearestEntityInRange(): Entity? {
        val player = mc.player ?: return null

        val entitiesInRange = getAllEntities()?.filter { entity ->
            var isInRange = false

            Backtrack.runWithNearestTrackedDistance(entity!!) {
                val distance = player.getDistanceToEntityBox(entity)
                isInRange = when (timerBoostMode.lowercase()) {
                    "normal" -> distance <= rangeValue
                    "smart", "modern" -> distance <= scanRange.get() + randomRange
                    else -> false
                }
            }

            isInRange
        }

        // Find the nearest entity
        return entitiesInRange?.minByOrNull { player.getDistanceToEntityBox(it!!) }
    }

    /**
     * Separate condition to make it cleaner
     */
    private fun shouldResetTimer() {
        val nearestEntity = getNearestEntityInRange()

        if (nearestEntity == null || !nearestEntity.isAlive) {
            if (!shouldReset) {
                mc.ticker.timerSpeed = 1f
                shouldReset = true
            }
        } else {
            if (mc.ticker.timerSpeed != 1f) {
                mc.ticker.timerSpeed = 1f
                shouldReset = true
            } else {
                shouldReset = false
            }
        }
    }

    /**
     * Lagback Reset is Inspired from Nextgen TimerRange
     * Reset Timer on Lagback & Knockback.
     */
    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet

        if (mc.player == null || mc.player.isDead)
            return

        if (blink) {
            if (playerTicks > 0 && !blinked) {
                BlinkUtils.blink(packet, event, sent = false, receive = true)
                blinked = true
            }

            if (blink && blinked) {
                when (packet) {
                    // Flush on doing/getting action.
                    is PlayerPositionLookS2CPacket, is PlayerActionC2SPacket, is UpdateSignC2SPacket, is C19PacketResourcePackStatus -> {
                        BlinkUtils.unblink()
                        return
                    }

                    // Flush on explosion
                    is S27PacketExplosion -> {
                        if (packet.field_149153_g != 0f || packet.field_149152_f != 0f || packet.field_149159_h != 0f) {
                            BlinkUtils.unblink()
                            return
                        }
                    }

                    // Flush on damage
                    is S06PacketUpdateHealth -> {
                        if (packet.health < mc.player.health) {
                            BlinkUtils.unblink()
                            return
                        }
                    }
                }
            }
        }

        // Check for lagback
        if (resetOnlagBack && packet is PlayerPositionLookS2CPacket) {
            shouldResetTimer()

            if (shouldReset) {
                if (chatDebug) {
                    Chat.print("Lagback Received | Timer Reset")
                }
                if (notificationDebug) {
                    hud.addNotification(Notification("Lagback Received | Timer Reset", 1000F))
                }

                shouldReset = false
            }
        }

        // Check for knockback
        if (resetOnKnockback && packet is S12PacketEntityVelocity && mc.player.entityId == packet.entityID) {
            shouldResetTimer()

            if (shouldReset) {
                if (chatDebug) {
                    Chat.print("Knockback Received | Timer Reset")
                }
                if (notificationDebug) {
                    hud.addNotification(Notification("Knockback Received | Timer Reset", 1000F))
                }

                shouldReset = false
            }
        }
    }

    /**
     * HUD Tag
     */
    override val tag
        get() = timerBoostMode
}
