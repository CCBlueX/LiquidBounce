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
package net.ccbluex.liquidbounce.features.module.modules.movement.step

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.event.events.PlayerJumpEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.block.getBlock
import net.ccbluex.liquidbounce.utils.entity.FallingPlayer
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.shapes.Shapes

/**
 * ReverseStep module
 *
 * Allows you to step down blocks faster.
 */

object ModuleReverseStep : ClientModule("ReverseStep", ModuleCategories.MOVEMENT) {

    private var modes = choices("Mode", Instant, arrayOf(Instant, Strict, Accelerator)).apply { tagBy(this) }
    private val maximumFallDistance by float("MaximumFallDistance", 1f, 1f..50f)

    /**
     * Keeps us from reverse stepping when the user intentionally jumps.
     * We either check for the PlayerJumpEvent or the velocity of the player.
     */
    private var initiatedJump = false

    private val unwantedBlocksBelow: Boolean
        get() {
            val collision = FallingPlayer
                .fromPlayer(player)
                .findCollision(20)?.pos ?: return false
            return collision.getBlock() in arrayOf(
                Blocks.WATER, Blocks.COBWEB, Blocks.POWDER_SNOW, Blocks.HAY_BLOCK,
                Blocks.SLIME_BLOCK
            )
        }

    @Suppress("unused")
    val jumpHandler = handler<PlayerJumpEvent> {
        initiatedJump = true
    }

    val repeatable = tickHandler {
        if (player.deltaMovement.y > 0.0) {
            initiatedJump = true
        } else if (player.onGround()) {
            initiatedJump = false
        }
    }

    object Instant : Mode("Instant") {
        override val parent: ModeValueGroup<Mode>
            get() = modes

        private val ticks by int("Ticks", 20, 1..40, "ticks")
        private val simulateFalling by boolean("SimulateFalling", false)

        val repeatable = tickHandler {
            if (!initiatedJump && !player.onGround() && !unwantedBlocksBelow) {
                if (isFallingTooFar()) {
                    return@tickHandler
                }

                val simInput = SimulatedPlayer.SimulatedPlayerInput.fromClientPlayer(DirectionalInput.NONE)
                val simulatePlayer = SimulatedPlayer.fromClientPlayer(simInput)

                val simulationQueue = mutableListOf<ServerboundMovePlayerPacket>()
                for (tick in 0..ticks) {
                    // If the simulated player is on ground, we can stop the simulation and set the player position
                    // to the end.
                    // If we do not reach this point, this means we will go out of the loop by reaching the tick limit
                    // and therefore lose the simulation.
                    if (simulatePlayer.onGround) {
                        if (simulationQueue.isNotEmpty()) {
                            simulationQueue.forEach(network::send)
                        }

                        player.setPos(simulatePlayer.pos)
                        break
                    }

                    simulatePlayer.tick()
                    if (simulateFalling) {
                        simulationQueue += ServerboundMovePlayerPacket.Pos(
                            simulatePlayer.pos.x,
                            simulatePlayer.pos.y, simulatePlayer.pos.z, simulatePlayer.onGround,
                            simulatePlayer.horizontalCollision
                        )
                    }
                }

                simulationQueue.clear()
            }
        }

    }

    object Accelerator : Mode("Accelerator") {
        override val parent: ModeValueGroup<Mode>
            get() = modes

        private val factor by float("Factor", 1.0F, 0.1F..5.0F)

        val repeatable = tickHandler {
            if (!initiatedJump && !player.onGround() && player.deltaMovement.y < 0.0 && !unwantedBlocksBelow) {
                if (isFallingTooFar()) {
                    return@tickHandler
                }

                player.deltaMovement = player.deltaMovement.multiply(0.0, factor.toDouble(), 0.0)
            }
        }

    }

    object Strict : Mode("Strict") {

        override val parent: ModeValueGroup<Mode>
            get() = modes

        private val motion by float("Motion", 1.0F, 0.1F..5.0F)

        val repeatable = tickHandler {
            if (!initiatedJump && !player.onGround() && !unwantedBlocksBelow) {
                if (isFallingTooFar()) {
                    return@tickHandler
                }

                player.deltaMovement.y = -motion.toDouble()
            }
        }
    }

    private fun isFallingTooFar(): Boolean {
        if (player.fallDistance > maximumFallDistance) {
            return true
        }

        // If there is no collision after maximum fall distance, we do not want to reverse step and
        // risk falling deep.
        val boundingBox = player.boundingBox.move(0.0, (-maximumFallDistance).toDouble(), 0.0)
        return world.getBlockCollisions(player, boundingBox).all { shape -> shape == Shapes.empty() }
    }

}
