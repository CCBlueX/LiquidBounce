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

package net.ccbluex.liquidbounce.features.module.modules.combat.grimvelocity.mode

import com.google.common.collect.Queues
import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.TickPacketProcessEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.combat.grimvelocity.GrimVelocityMode
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes.NoFallGrim

import net.ccbluex.liquidbounce.utils.client.handlePacket
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.network.packet.Packet
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

object GrimVelocityDelay : GrimVelocityMode("Delay") {

    private object DelayInAir : Choice("InAir") {
        override val parent: ChoiceConfigurable<*>
            get() = mode

        val jumpReset by boolean("JumpReset", true)
    }

    private object DelayByTicks : Choice("ByTicks") {
        override val parent: ChoiceConfigurable<*>
            get() = mode

        val delay by intRange("Delay", 5..10, 0..60, "ticks")
    }

    private val mode = choices(
        "Mode", DelayByTicks, arrayOf(
            DelayInAir,
            DelayByTicks
        )
    )
    private val requireKillAura by boolean("RequireKillAura", true)

    private var delaying = false
    private var damage = false
    private var delayTicks = 0
    private var jump = false
    private val packets = Queues.newConcurrentLinkedQueue<Packet<*>>()

    override val shouldStopBacktrack: Boolean
        get() = delaying

    override fun enable() {
        delaying = false
        damage = false
        delayTicks = 0
        jump = false
        packets.clear()
    }

    private fun handle() {
        packets.removeIf {
            handlePacket(it)
            true
        }
        delaying = false
    }

    @Suppress("unused")
    private val packetEventHandler = handler<PacketEvent> { event ->
        if (event.origin != TransferOrigin.INCOMING) return@handler

        val packet = event.packet

        if (delaying) {
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
            damage = true
        }

        if (damage && packet is EntityVelocityUpdateS2CPacket && packet.entityId == player.id) {
            if (!requireKillAura || (ModuleKillAura.running && ModuleKillAura.targetTracker.target != null)) {
                delayTicks = when (mode.activeChoice) {
                    DelayInAir -> 30
                    DelayByTicks -> DelayByTicks.delay.random()
                    else -> 0
                }
                delaying = true
                event.cancelEvent()
                packets.add(packet)
            }
            damage = false
        }
    }

    @Suppress("unused")
    private val tickHandler = tickHandler {
        if (delaying) {
            delayTicks--
        }
    }

    @Suppress("unused")
    private val tickPacketProcessEventHandler = handler<TickPacketProcessEvent> {
        if (delaying && when (mode.activeChoice) {
                DelayInAir -> player.isOnGround || delayTicks <= 0
                DelayByTicks -> delayTicks <= 0
                else -> true
            }
        ) {
            handle()
            if (mode.activeChoice == DelayInAir && DelayInAir.jumpReset) {
                jump = true
            }
        }
    }

    @Suppress("unused")
    private val movementInputEventHandler = handler<MovementInputEvent> { event ->
        if (jump) {
            if (!InventoryManager.isInventoryOpen
                && mc.currentScreen !is GenericContainerScreen
                && player.isOnGround
                && !(NoFallGrim.running && NoFallGrim.jumping)
            ) {
                event.jump = true
            }
            jump = false
        }
    }

}
