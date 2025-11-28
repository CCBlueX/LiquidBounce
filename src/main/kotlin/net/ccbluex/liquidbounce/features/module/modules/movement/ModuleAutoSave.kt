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

package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule

import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.block.getBlock
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket
import net.minecraft.util.math.BlockPos
import kotlin.math.ceil
import kotlin.math.floor

object ModuleAutoSave : ClientModule("AutoSave", Category.MOVEMENT) {


    private object AutoScaffold : ToggleableConfigurable(this, "AutoScaffold", true) {
        val scaffoldOnlyVoid by boolean("ScaffoldOnlyVoid", false)
        val scaffoldVoidDistance by int("ScaffoldVoidDistance", 15, 1..50, "blocks")
    }

    init {
        tree(AutoScaffold)
    }

    private val pauseOnFlag by int("PauseOnFlag", 20, 0..100, "ticks")

    private const val LOWEST_Y = -64
    private const val BLOCK_EDGE = 0.3
    private const val RECEIVE_HIT_TICKS = 30

    private var lastGroundY = LOWEST_Y
    private var stuckSaving = false
    private var scaffoldSaving = false
    private var wasSpectator = false
    private var receiveHitTicks = 0
    private var pauseTicks = 0
    private var damage = false

    private fun reset(disable: Boolean) {
        if (disable) {
            if (scaffoldSaving) ModuleScaffold.enabled = false
        }

        lastGroundY = LOWEST_Y
        stuckSaving = false
        scaffoldSaving = false
        receiveHitTicks = 0
        pauseTicks = 0
        damage = false
    }

    private fun aboveVoid(voidDistance: Int = -1): Boolean {
        if (player.isOnGround) return false

        val xRange = mutableListOf(0)
        val zRange = mutableListOf(0)
        if (player.x - floor(player.x) <= BLOCK_EDGE) {
            xRange.add(-1)
        } else if (ceil(player.x) - player.x <= BLOCK_EDGE) {
            xRange.add(1)
        }
        if (player.z - floor(player.z) <= BLOCK_EDGE) {
            zRange.add(-1)
        } else if (ceil(player.z) - player.z <= BLOCK_EDGE) {
            zRange.add(1)
        }

        for (xOffset in xRange) {
            for (zOffset in zRange) {
                for (y in if (voidDistance == -1) LOWEST_Y..lastGroundY else lastGroundY - voidDistance..lastGroundY) {
                    val block = BlockPos(player.x.toInt() + xOffset, y, player.z.toInt() + zOffset).getBlock()
                    if (block?.translationKey != "block.minecraft.air") {
                        return false
                    }
                }
            }
        }

        return true
    }

    @Suppress("unused")
    private val worldChangeEventHandler = handler<WorldChangeEvent> {
        reset(true)
    }

    @Suppress("unused")
    private val packetEventHandler = handler<PacketEvent> { event ->
        val packet = event.packet

        if (packet is PlayerPositionLookS2CPacket) {
            reset(true)
            pauseTicks = pauseOnFlag
        }

        if (packet is EntityDamageS2CPacket && packet.entityId == player.id) {
            damage = true
        }


    }

    @Suppress("unused")
    private val tickHandler = tickHandler {
        if (player.isSpectator || player.abilities.flying) {
            if (!wasSpectator) {
                wasSpectator = true
                reset(true)
            }
            return@tickHandler
        } else {
            if (wasSpectator) wasSpectator = false
        }

        if (pauseTicks > 0) pauseTicks--
        if (receiveHitTicks > 0) receiveHitTicks--
        if (player.hurtTime > 0) {
            receiveHitTicks = RECEIVE_HIT_TICKS
        }

        if (player.isOnGround) {
            lastGroundY = player.y.toInt() - 1
        }

        if (pauseTicks > 0) return@tickHandler


        if (AutoScaffold.enabled) {
            if (receiveHitTicks > 0
                && (!ModuleKillAura.running || ModuleKillAura.targetTracker.target == null)
                && aboveVoid(
                    if (AutoScaffold.scaffoldOnlyVoid) -1
                    else AutoScaffold.scaffoldVoidDistance
                )
            ) {
                if (!scaffoldSaving && !ModuleScaffold.enabled) {
                    ModuleScaffold.enabled = true
                    scaffoldSaving = true
                }
            } else {
                if (scaffoldSaving) {
                    ModuleScaffold.enabled = false
                    scaffoldSaving = false
                }
            }
        }
    }

    override fun onEnabled() {
        reset(false)
        wasSpectator = false
    }

}
