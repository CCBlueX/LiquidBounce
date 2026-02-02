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

import net.ccbluex.liquidbounce.additions.forceSneak
import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerNetworkMovementTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.block.collisionShape
import net.ccbluex.liquidbounce.utils.block.getBlock
import net.ccbluex.liquidbounce.utils.client.ceilToInt
import net.ccbluex.liquidbounce.utils.client.floorToInt
import net.ccbluex.liquidbounce.utils.client.isNewerThanOrEquals1_21_6
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.client.send1_21_5StartSneaking
import net.ccbluex.liquidbounce.utils.client.send1_21_5StopSneaking
import net.ccbluex.liquidbounce.utils.client.usesViaFabricPlus
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.entity.immuneToMagmaBlocks
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.set
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket
import net.minecraft.world.level.block.MagmaBlock
import net.minecraft.world.phys.AABB

/**
 * Sneak module
 *
 * Automatically sneaks all the time.
 */
object ModuleSneak : ClientModule("Sneak", ModuleCategories.MOVEMENT) {

    private val modes = choices("Mode", Vanilla, arrayOf(Legit, Vanilla, Switch)).apply { tagBy(this) }
    private val notDuringMove by boolean("NotDuringMove", false)

    private object Legit : Mode("Legit") {

        private val onMagmaBlocksOnly by boolean("OnMagmaBlocksOnly", false)

        override val parent: ModeValueGroup<Mode>
            get() = modes

        @Suppress("unused")
        private val inputHandler = handler<MovementInputEvent> { event ->
            if (player.moving && notDuringMove) {
                return@handler
            }

            if (onMagmaBlocksOnly && (player.immuneToMagmaBlocks || !isOnMagmaBlock(event.directionalInput))) {
                return@handler
            }

            // Temporarily override sneaking
            event.sneak = true
        }

    }

    private object Vanilla : Mode("Vanilla") {

        override val parent: ModeValueGroup<Mode>
            get() = modes

        @Suppress("unused")
        private val sneakNetworkHandler = handler<PacketEvent> { event ->
            if ((player.moving && notDuringMove) || event.packet !is ServerboundPlayerInputPacket) {
                return@handler
            }

            event.packet.forceSneak = true
        }

    }

    private object Switch : Mode("Switch") {

        private var networkSneaking = false

        override val parent: ModeValueGroup<Mode>
            get() = modes

        override fun enable() {
            if (!usesViaFabricPlus || isNewerThanOrEquals1_21_6) {
                notification(
                    "Protocol Error",
                    "This mode can only be used on server with version earlier than 1.21.6.",
                    NotificationEvent.Severity.ERROR,
                )
            }
            super.enable()
        }

        @Suppress("unused")
        private val networkTick = handler<PlayerNetworkMovementTickEvent> { event ->
            if (player.moving && notDuringMove) {
                disable()
                return@handler
            }

            when (event.state) {
                EventState.PRE -> {
                    if (networkSneaking) {
                        network.send1_21_5StopSneaking()
                        networkSneaking = false
                    }
                }

                EventState.POST -> {
                    if (!networkSneaking) {
                        network.send1_21_5StartSneaking()
                        networkSneaking = true
                    }
                }
            }
        }

        override fun disable() {
            if (networkSneaking) {
                network.send1_21_5StopSneaking()
                networkSneaking = false
            }
        }
    }

    private fun isOnMagmaBlock(directionalInput: DirectionalInput): Boolean {
        val simulatedInput = SimulatedPlayer.SimulatedPlayerInput.fromClientPlayer(directionalInput)
        simulatedInput.set(jump = false)

        // Doesn't keep the player stuck at the edge of a magma block while sneaking
        simulatedInput.ignoreClippingAtLedge = true

        val simulatedPlayer = SimulatedPlayer.fromClientPlayer(simulatedInput)
        simulatedPlayer.pos = player.position()

        simulatedPlayer.tick()
        val isOnMagmaBlockAfterOneTick = isOnMagmaBlock(simulatedPlayer.boundingBox)

        simulatedPlayer.tick()
        val isOnMagmaBlockAfterTwoTicks = isOnMagmaBlock(simulatedPlayer.boundingBox)

        return isOnMagmaBlockAfterOneTick || isOnMagmaBlockAfterTwoTicks
    }

    /**
     * [boundingBox] - the specific bounding box of a player, mob or even another block.
     */
    private fun isOnMagmaBlock(boundingBox: AABB): Boolean {

        // Blocks that are the height of a trapdoor or lower
        // (such as snow layers, carpets, repeaters, or comparators)
        // do not prevent a magma block from damaging mobs and players above it.

        // Therefore, we expand the box downward by 0.2 blocks.
        val expandedBox = boundingBox
            .inflate(0.0, 0.1,0.0)
            .move(0.0, -0.1, 0.0)

        return BlockPos.betweenClosed(
            expandedBox.minX.floorToInt(),
            expandedBox.minY.floorToInt(),
            expandedBox.minZ.floorToInt(),
            expandedBox.maxX.ceilToInt(),
            expandedBox.minY.ceilToInt(),
            expandedBox.maxZ.ceilToInt(),
        ).any {
            it.getBlock() is MagmaBlock &&
                expandedBox.intersects(it.collisionShape.bounds().move(it))
        }
    }
}
