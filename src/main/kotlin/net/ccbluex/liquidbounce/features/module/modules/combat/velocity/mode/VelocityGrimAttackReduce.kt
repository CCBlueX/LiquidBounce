/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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

package net.ccbluex.liquidbounce.features.module.modules.combat.velocity.mode

import com.google.common.collect.Queues
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.TickPacketProcessEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler

import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.combat.velocity.ModuleVelocity
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.utils.raytraceEntity
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.handlePacket
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.ccbluex.liquidbounce.utils.entity.boxedDistanceTo
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.ccbluex.liquidbounce.utils.math.sq
import net.minecraft.entity.Entity
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket
import net.minecraft.network.packet.s2c.common.KeepAliveS2CPacket
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket
import net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket
import net.minecraft.sound.SoundEvents
import net.minecraft.util.Hand


/*
* Velocity for grim, and Chinese HeyPixel.
 */
internal object VelocityGrimAttackReduce: VelocityMode("GrimAttackReduce") {
    private val attackCount by intRange("AttackCount", 3..3, 0..20)
    private val delayRange by floatRange("DelayRange", 2.5f..3.5f, 0f..10f)
    private val delay by int("Delay", 5, 0..100, "ticks")
    private val debug by boolean("Debug", false)

    private var target: Entity? = null
    private var attackQueue = 0
    private var receiveDamage = false
    private var delayTicks = -1
    private val packets = Queues.newConcurrentLinkedQueue<Packet<*>>()

    override fun disable() {
        target = null
        attackQueue = 0
        receiveDamage = false
        delayTicks = -1
        packets.clear()
    }

    private fun findTarget(): Boolean {
        if (ModuleKillAura.running && ModuleKillAura.targetTracker.target != null) {
            target = ModuleKillAura.targetTracker.target
            return true
        }

        target = raytraceEntity(
            ModuleKillAura.range.toDouble(),
            RotationManager.serverRotation
        ) { !it.isRemoved && it.shouldBeAttacked() }?.entity

        if (target != null) {
            return true
        }

        val farTarget = ModuleKillAura.targetTracker.targets()
            .filter { entity -> entity.squaredBoxedDistanceTo(player) <= delayRange.endInclusive.sq() }
            .minByOrNull { entity -> if (entity.squaredBoxedDistanceTo(player) <= delayRange.endInclusive.sq()) 0 else 1 }

        return farTarget != null
    }

    private fun handle() {
        packets.removeIf {
            handlePacket(it)
            true
        }
        delayTicks = -1
    }

    @Suppress("unused")
    private val packetEventHandler = handler<PacketEvent> { event ->
        if (event.origin != TransferOrigin.INCOMING) return@handler

        val packet = event.packet

        if (delayTicks >= 0) {
            when (packet) {
                is ChatMessageS2CPacket,
                is GameMessageS2CPacket,
                is KeepAliveS2CPacket -> {
                    return@handler
                }

                is PlayerPositionLookS2CPacket,
                is DisconnectS2CPacket,
                is PlayerRespawnS2CPacket,
                is GameJoinS2CPacket -> {
                    handle()
                    return@handler
                }

                is PlaySoundS2CPacket -> {
                    if (packet.sound.value() == SoundEvents.ENTITY_PLAYER_HURT) {
                        return@handler
                    }
                }

                is HealthUpdateS2CPacket -> {
                    if (packet.health <= 0) {
                        handle()
                        return@handler
                    }
                }
            }

            event.cancelEvent()
            packets.add(packet)
            return@handler
        }

        if (packet is EntityDamageS2CPacket && packet.entityId == player.id) {
            receiveDamage = true
        }

        if (packet is EntityVelocityUpdateS2CPacket && packet.entityId == player.id && receiveDamage) {
            receiveDamage = false
            if (!findTarget()) return@handler
            if (player.boxedDistanceTo(target!!) >= delayRange.start) {
                if (debug) chat("Delay $delay ticks", ModuleVelocity)
                delayTicks = delay
                event.cancelEvent()
                packets.add(packet)
                return@handler
            }
            findTarget()
            attackQueue = attackCount.random()
        }
    }

    @Suppress("unused")
    private val tickPacketProcessEventHandler = handler<TickPacketProcessEvent> {
        if (delayTicks == 0) {
            handle()
            attackQueue = attackCount.random()
            if (debug) chat("Finish delay", ModuleVelocity)
        }
    }

    @Suppress("unused")
    private val tickHandler = tickHandler {
        if (delayTicks > 0) {
            delayTicks--
        }

        if (attackQueue > 0 && delayTicks == -1) {
            if (target == null) {
                attackQueue = 0
                return@tickHandler
            }

            while (attackQueue >= 1) {
                network.sendPacket(PlayerInteractEntityC2SPacket.attack(target, false))
                player.setVelocity(
                    player.velocity.x * 0.6,
                    player.velocity.y,
                    player.velocity.z * 0.6
                )
                player.isSprinting = false
                player.swingHand(Hand.MAIN_HAND)
                attackQueue--
            }
        }
    }
}
