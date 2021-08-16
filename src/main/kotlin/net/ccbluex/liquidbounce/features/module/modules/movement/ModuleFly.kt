/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.strafe
import net.minecraft.block.Blocks
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.util.shape.VoxelShapes

/**
 * Fly module
 *
 * Allows you to fly.
 */
object ModuleFly : Module("Fly", Category.MOVEMENT) {

    private val modes = choices("Mode", Vanilla, arrayOf(Vanilla, Jetpack, Verus))

    var startY: Double = 0.0

    override fun enable() {
        startY = mc.player?.y ?: 0.0
    }

    private object Visuals : ToggleableConfigurable(this, "Visuals", true) {

        private val stride by boolean("Stride", true)

        val strideHandler = handler<PlayerStrideEvent> { event ->
            if (stride) {
                event.strideForce = 0.1.coerceAtMost(player.velocity.horizontalLength()).toFloat()
            }

        }

    }

    private object Vanilla : Choice("Vanilla") {

        override val parent: ChoiceConfigurable
            get() = modes

        val repeatable = repeatable {
            player.strafe(speed = 0.44)
            player.velocity.y = when {
                player.input.jumping -> 0.31
                player.input.sneaking -> -0.31
                else -> 0.0
            }
        }

    }

    private object Jetpack : Choice("Jetpack") {

        override val parent: ChoiceConfigurable
            get() = modes

        val repeatable = repeatable {
            if (player.input.jumping) {
                player.velocity.x *= 1.1
                player.velocity.y += 0.15
                player.velocity.z *= 1.1
            }
        }

    }

    private object Verus : Choice("Verus") {

        override val parent: ChoiceConfigurable
            get() = modes

        val tickHandler = handler<PlayerTickEvent> {
            val player = mc.player ?: return@handler
            val world = mc.world ?: return@handler

            /* Updates the startPosition if the player lands on higher ground */
            if(player.isOnGround && player.y > startY && !world.isAir(player.blockPos.down())) {
                startY = player.y
            }

            if(player.moving){
                player.strafe(speed = 0.31)
                if(player.isOnGround){
                    player.jump()
                }
            } else {
                player.strafe()
            }

        }

        val shapeHandler = handler<BlockShapeEvent> { event ->
            if (event.state.block == Blocks.AIR && event.pos.y < startY) {
                event.shape = VoxelShapes.fullCube()
            }
        }

    }

    init {
        tree(Visuals)
    }

}
