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

package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.command.commands.module.teleport.CommandPlayerTeleport
import net.ccbluex.liquidbounce.features.command.commands.module.teleport.CommandTeleport
import net.ccbluex.liquidbounce.features.command.commands.module.teleport.CommandVClip
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.exploit.disabler.ModuleDisabler
import net.ccbluex.liquidbounce.utils.client.MovePacketType
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.sendPacketSilently
import net.ccbluex.liquidbounce.utils.client.variable
import net.ccbluex.liquidbounce.utils.client.warning
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.world.phys.Vec3
import java.text.DecimalFormat
import kotlin.math.abs
import kotlin.math.floor

/**
 * Teleport Module
 *
 * Configuration for teleport commands.
 *
 * Commands: [CommandVClip], [CommandTeleport], [CommandPlayerTeleport]
 */
object ModuleTeleport : ClientModule("Teleport", ModuleCategories.EXPLOIT, aliases = listOf("tp")) {

    private val allFull by boolean("AllFullPacket", false)
    private val paperExploit by boolean("PaperBypass", false)
    val highTp by boolean("HighTP", false)
    val highTpAmount by float("HighTPAmount", 200.0F, 0.0F..500.0F)
    private val groundMode by enumChoice("GroundMode", GroundMode.CORRECT)
    private val resetMotion by boolean("ResetMotion", true)

    private val functionAfterServerTeleport by int("FunctionAfterTeleports", 0, 0..5)
    private val withDisabler by boolean("WithDisablerOnWait", false)

    private val decimalFormat = DecimalFormat("##0.000")

    enum class GroundMode(override val tag: String) : Tagged {
        TRUE("True"),
        FALSE("False"),
        CORRECT("Correct")
    }

    private var indicatedTeleport: Vec3? = null
    private var teleportsToWait: Int = 0

    override fun onEnabled() {
        if (indicatedTeleport == null) {
            chat(warning(message("useCommand")))

            // Disables module on next render tick
            mc.execute {
                this.enabled = false
            }
        }
    }

    override fun onDisabled() {
        indicatedTeleport = null
        teleportsToWait = 0
    }

    fun indicateTeleport(x: Double = player.x, y: Double = player.y, z: Double = player.z) {
        if (functionAfterServerTeleport == 0) {
            teleport(x, y, z)
            return
        }

        this.indicatedTeleport = Vec3(x, y, z)
        this.teleportsToWait = functionAfterServerTeleport
        this.enabled = true

        if (teleportsToWait == 1 && withDisabler) {
            ModuleDisabler.enabled = true
        }

        chat(variable(message("teleportsLeft", teleportsToWait)))
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> {
        if (it.packet is ClientboundPlayerPositionPacket) {
            val indicatedTeleport = indicatedTeleport ?: return@handler

            if (teleportsToWait > 1) {
                teleportsToWait--
                chat(variable(message("teleportsLeft", teleportsToWait)))
                return@handler
            }

            sendPacketSilently(MovePacketType.FULL.generatePacket().apply {
                val change = it.packet.change
                this.x = change.position.x
                this.y = change.position.y
                this.z = change.position.z
                this.yRot = change.yRot
                this.xRot = change.xRot
                this.onGround = false
            })

            teleport(indicatedTeleport.x, indicatedTeleport.y, indicatedTeleport.z)
            this.indicatedTeleport = null
            it.cancelEvent()

            if (withDisabler) {
                ModuleDisabler.enabled = false
            }
        }
    }

    fun teleport(x: Double = player.x, y: Double = player.y, z: Double = player.z) {
        if (paperExploit) {
            val deltaX = x - player.x
            val deltaY = y - player.y
            val deltaZ = z - player.z

            val times = (floor((abs(deltaX) + abs(deltaY) + abs(deltaZ)) / 10) - 1).toInt()
            val packetToSend = if (allFull) MovePacketType.FULL else MovePacketType.POSITION_AND_ON_GROUND
            repeat(times) {
                network.send(packetToSend.generatePacket().apply {
                    this.x = player.x
                    this.y = player.y
                    this.z = player.z
                    this.yRot = player.yRot
                    this.xRot = player.xRot
                    this.onGround = when (groundMode) {
                        GroundMode.TRUE -> true
                        GroundMode.FALSE -> false
                        GroundMode.CORRECT -> player.onGround()
                    }
                })
            }

            network.send(packetToSend.generatePacket().apply {
                this.x = x
                this.y = y
                this.z = z
                this.yRot = player.yRot
                this.xRot = player.xRot
                this.onGround = when (groundMode) {
                    GroundMode.TRUE -> true
                    GroundMode.FALSE -> false
                    GroundMode.CORRECT -> player.onGround()
                }
            })
        }

        val entity = player.vehicle ?: player
        entity.absSnapTo(x, y, z)

        if (resetMotion) {
            entity.deltaMovement = entity.deltaMovement.multiply(0.0, 0.0, 0.0)
        }

        chat(regular(
            message("teleported",
            variable(decimalFormat.format(x)),
            variable(decimalFormat.format(y)),
            variable(decimalFormat.format(z)))
        ))
        this.enabled = false
    }

}
