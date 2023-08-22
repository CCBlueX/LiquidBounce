/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.block.collideBlockIntersects
import net.ccbluex.liquidbounce.utils.block.isBlockAtPosition
import net.ccbluex.liquidbounce.utils.entity.box
import net.minecraft.block.FluidBlock
import net.minecraft.fluid.Fluids
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.util.shape.VoxelShapes

/**
 * LiquidWalk module
 *
 * Allows you to walk on water like jesus. Also known as Jesus module.
 */
object ModuleLiquidWalk : Module("LiquidWalk", Category.MOVEMENT) {

    private val modes = choices("Mode", Vanilla, arrayOf(Vanilla, NoCheatPlus))

    private object Vanilla : Choice("Vanilla") {

        override val parent: ChoiceConfigurable
            get() = modes

        val shapeHandler = handler<BlockShapeEvent> { event ->
            if (event.state.fluidState.isOf(Fluids.WATER) && !isBlockAtPosition(player.box) { it is FluidBlock } && !player.input.sneaking) {
                event.shape = VoxelShapes.fullCube()
            }
        }

    }

    /**
     * @anticheat NoCheatPlus
     * @anticheatVersion 3.16.1-SNAPSHOT-sMD5NET-b115s
     * @testedOn eu.loyisa.cn and poke.sexy
     */
    private object NoCheatPlus : Choice("NoCheatPlus") {

        override val parent: ChoiceConfigurable
            get() = modes

        private var tick = false

        val shapeHandler = handler<BlockShapeEvent> { event ->
            if (event.state.fluidState.isOf(Fluids.WATER) && !isBlockAtPosition(player.box) { it is FluidBlock } && !player.input.sneaking) {
                event.shape = VoxelShapes.fullCube()
            }
        }

        val repeatable = repeatable {
            if (isBlockAtPosition(player.box) { it is FluidBlock } && !player.input.sneaking) {
                player.velocity.y = 0.08
            }
        }

        val packetHandler = handler<PacketEvent> { event ->
            val packet = event.packet

            if (event.origin == TransferOrigin.SEND && packet is PlayerMoveC2SPacket) {
                val boundingBox = player.box
                val detectionBox = boundingBox.withMinY(boundingBox.minY - 0.5)

                // todo: fix moving passable flags on edges
                if (!player.input.sneaking && !player.isTouchingWater && standingOnWater() && !collideBlockIntersects(
                        detectionBox
                    ) { it !is FluidBlock }
                ) {
                    if (tick) {
                        packet.y -= 0.001
                    }
                    tick = !tick
                }
            }
        }

        val jumpHandler = handler<PlayerJumpEvent> { event ->
            val boundingBox = player.box

            if (isBlockAtPosition(boundingBox.withMinY(boundingBox.minY - 0.01)) { it is FluidBlock }) {
                event.cancelEvent()
            }
        }

    }

    /**
     * Check if player is standing on water
     */
    fun standingOnWater(): Boolean {
        val boundingBox = player.box
        val detectionBox = boundingBox.withMinY(boundingBox.minY - 0.01)

        return isBlockAtPosition(detectionBox) { it is FluidBlock }
    }

}
