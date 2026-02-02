/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.features.module.modules.combat.backtrack

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.events.BlinkPacketEvent
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.TickPacketProcessEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.blink.BlinkManager
import net.ccbluex.liquidbounce.features.blink.esp.BlinkEspBox
import net.ccbluex.liquidbounce.features.blink.esp.BlinkEspModel
import net.ccbluex.liquidbounce.features.blink.esp.BlinkEspWireframe
import net.ccbluex.liquidbounce.features.blink.esp.BlinkEspNone
import net.ccbluex.liquidbounce.features.blink.esp.BlinkEspData
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.combat.findEnemy
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.ccbluex.liquidbounce.utils.entity.boxedDistanceTo
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.entity.squareBoxedDistanceTo
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket
import net.minecraft.network.protocol.game.ServerboundChatPacket
import net.minecraft.network.protocol.game.VecDeltaCodec
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.Vec3

object ModuleBacktrack : ClientModule("Backtrack", ModuleCategories.COMBAT) {

    private val range by floatRange("Range", 1f..3f, 0f..10f)
    val delay by intRange("Delay", 100..150, 0..1000, "ms")
    private val nextBacktrackDelay by intRange("NextBacktrackDelay", 0..10, 0..2000, "ms")
    private val trackingBuffer by int("TrackingBuffer", 500, 0..2000, "ms")
    private val chance by float("Chance", 50f, 0f..100f, "%")
    private var currentChance = (0..100).random()

    private object PauseOnHurtTime : ToggleableValueGroup(this, "PauseOnHurtTime", false) {
        val hurtTime by int("HurtTime", 3, 0..10)
    }

    private val pauseOnHurtTime = tree(PauseOnHurtTime)

    private val targetMode by enumChoice("TargetMode", Mode.ATTACK)
    private val lastAttackTimeToWork by int("LastAttackTimeToWork", 1000, 0..5000)

    enum class Mode(override val tag: String) : Tagged {
        ATTACK("Attack"),
        RANGE("Range")
    }

    private val espMode = choices("Esp", 2) {
        arrayOf(
            BlinkEspBox(it, ::getEspData),
            BlinkEspModel(it, ::getEspData),
            BlinkEspWireframe(it, ::getEspData),
            BlinkEspNone(it),
        )
    }.apply {
        doNotIncludeAlways()
    }

    private val chronometer = Chronometer()
    private val trackingBufferChronometer = Chronometer()
    private val attackChronometer = Chronometer()

    private var shouldPause = false

    private var target: Entity? = null
    private val position = VecDeltaCodec()

    var currentDelay = delay.random()

    @Suppress("unused")
    private val queuePacketHandler = handler<BlinkPacketEvent> { event ->
        if (event.origin != TransferOrigin.INCOMING) {
            return@handler
        }

        val packet = event.packet
        val shouldCancel = shouldCancelPackets()
        val hasQueuedIncoming = hasQueuedIncoming()

        if (packet == null) {
            if (shouldCancel || hasQueuedIncoming) {
                event.action = BlinkManager.Action.PASS
            }
            return@handler
        }

        if (!hasQueuedIncoming && !shouldCancel) {
            return@handler
        }

        when (packet) {
            // Ignore message-related packets
            is ServerboundChatPacket, is ClientboundSystemChatPacket, is ServerboundChatCommandPacket -> {
                event.action = BlinkManager.Action.PASS
                return@handler
            }

            // Flush on teleport or disconnect
            is ClientboundPlayerPositionPacket, is ClientboundDisconnectPacket -> {
                clear(true)
                return@handler
            }

            // Ignore own hurt sounds
            is ClientboundSoundPacket -> {
                if (packet.sound.value() == SoundEvents.PLAYER_HURT) {
                    event.action = BlinkManager.Action.PASS
                    return@handler
                }
            }

            // Flush on own death
            is ClientboundSetHealthPacket -> {
                if (packet.health <= 0) {
                    clear(true)
                    return@handler
                }
            }
        }

        // Update box position with these packets
        val target = target ?: return@handler
        val entityPacket = packet is ClientboundMoveEntityPacket && packet.getEntity(world) == target
        val positionPacket = packet is ClientboundTeleportEntityPacket && packet.id == target.id
        val syncPacket = packet is ClientboundEntityPositionSyncPacket && packet.id == target.id
        if (entityPacket || positionPacket || syncPacket) {
            val pos = when (packet) {
                is ClientboundMoveEntityPacket ->
                    position.decode(packet.xa.toLong(), packet.ya.toLong(), packet.za.toLong())
                is ClientboundTeleportEntityPacket ->
                    packet.change.position
                else -> (packet as ClientboundEntityPositionSyncPacket).values.position
            } ?: return@handler
            position.setBase(pos)

            // Is the target's actual position closer than its tracked position?
            if (target.squareBoxedDistanceTo(player, pos) < target.squaredBoxedDistanceTo(player)) {
                // Process all packets. We want to be able to hit the enemy, not the opposite.
                event.action = BlinkManager.Action.FLUSH
                // And stop right here. No need to cancel further packets.
                return@handler
            }
        }

        event.action = BlinkManager.Action.QUEUE
    }

    private fun getEspData(): BlinkEspData? {
        val entity = target ?: return null
        val pos = position.base
        val rotation = entity.rotation

        return BlinkEspData(entity, pos, rotation)
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> { event ->
        // Clear packets on disconnect only
        if (event.world == null) {
            clear(clearOnly = true)
        }
    }

    @Suppress("unused")
    private val tickPacketProcessHandler = handler<TickPacketProcessEvent> {
        if (!inGame) {
            clear(clearOnly = true)
            return@handler
        }

        val hadQueuedIncoming = hasQueuedIncoming()

        if (shouldCancelPackets()) {
            val now = System.currentTimeMillis()
            BlinkManager.flush { snapshot ->
                snapshot.origin == TransferOrigin.INCOMING && snapshot.timestamp <= now - currentDelay
            }
        } else if (hadQueuedIncoming) {
            BlinkManager.flush(TransferOrigin.INCOMING)
            clear()
        }

        if (!hasQueuedIncoming()) {
            currentDelay = delay.random()
        }
    }

    @Suppress("unused")
    private val attackHandler = handler<AttackEntityEvent> { event ->
        attackChronometer.reset() // Update the last attack time
        currentChance = (0..100).random()

        if (targetMode != Mode.ATTACK) {
            return@handler
        }

        val enemy = event.entity
        processTarget(enemy)
    }

    @Suppress("unused")
    private val rangeTargetHandler = handler<GameTickEvent> {
        if (targetMode != Mode.RANGE) return@handler

        val enemy = world.findEnemy(range)

        if (enemy == null) {
            clear()
            return@handler
        }

        processTarget(enemy)
    }

    private fun processTarget(enemy: Entity) {
        shouldPause = enemy is LivingEntity && enemy.hurtTime >= PauseOnHurtTime.hurtTime

        if (!shouldBacktrack(enemy)) {
            return
        }

        // Reset on enemy change
        if (enemy != target) {
            clear(resetChronometer = false)

            // Instantly set new position, so it does not look like the box was created with delay
            position.base = enemy.positionCodec.base
        }

        target = enemy
    }

    override fun onEnabled() {
        clear(false)
    }

    override fun onDisabled() {
        clear(true)
    }

    private fun clear(handlePackets: Boolean = true, clearOnly: Boolean = false, resetChronometer: Boolean = true) {
        if (handlePackets && !clearOnly) {
            BlinkManager.flush(TransferOrigin.INCOMING)
        } else if (clearOnly) {
            BlinkManager.packetQueue.removeIf { snapshot -> snapshot.origin == TransferOrigin.INCOMING }
        }

        if (target != null && resetChronometer) {
            chronometer.waitForAtLeast(nextBacktrackDelay.random().toLong())
        }

        target = null
        position.base = Vec3.ZERO
    }

    private fun shouldBacktrack(target: Entity): Boolean {
        val inRange = target.boxedDistanceTo(player) in range

        if (inRange) {
            trackingBufferChronometer.reset()
        }

        return (inRange || !trackingBufferChronometer.hasElapsed(trackingBuffer.toLong())) &&
            target.shouldBeAttacked() &&
            player.tickCount > 10 &&
            currentChance < chance &&
            chronometer.hasElapsed() &&
            !shouldPause() &&
            !attackChronometer.hasElapsed(lastAttackTimeToWork.toLong())
    }

    fun isLagging() = running && hasQueuedIncoming()

    private fun shouldPause() = pauseOnHurtTime.enabled && shouldPause

    fun shouldCancelPackets() =
        target?.let { target -> target.isAlive && shouldBacktrack(target) } == true

    private fun hasQueuedIncoming() =
        BlinkManager.packetQueue.any { snapshot -> snapshot.origin == TransferOrigin.INCOMING }

}
