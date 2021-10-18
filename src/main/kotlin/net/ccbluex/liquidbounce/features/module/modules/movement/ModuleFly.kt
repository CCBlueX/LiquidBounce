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
import net.ccbluex.liquidbounce.utils.aiming.Rotation
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.entity.strafe
import net.ccbluex.liquidbounce.utils.item.findHotbarSlot
import net.minecraft.block.Blocks
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket
import net.minecraft.sound.SoundEvents
import net.minecraft.util.Hand
import net.minecraft.util.shape.VoxelShapes
import org.apache.commons.lang3.RandomUtils

/**
 * Fly module
 *
 * Allows you to fly.
 */
object ModuleFly : Module("Fly", Category.MOVEMENT) {

    private val modes = choices("Mode", Vanilla, arrayOf(Vanilla, Jetpack, Verus, Enderpearl))

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

        val packetHandler = handler<PacketEvent> { event ->
            if (event.packet is PlayerMoveC2SPacket) {
                event.packet.onGround = true
            }
        }
        val shapeHandler = handler<BlockShapeEvent> { event ->
            if (event.state.block == Blocks.AIR && event.pos.y < player.y) {
                event.shape = VoxelShapes.fullCube()
            }
        }
        val jumpEvent = handler<PlayerJumpEvent> { event ->
            event.cancelEvent()
        }
    }

    init {
        tree(Visuals)
    }

    private object Enderpearl : Choice("Enderpearl") {

        override val parent: ChoiceConfigurable
            get() = modes

        val speed by float("Speed", 1f, 0.5f..2f)

        var threwPearl = false
        var canFly = false

        val rotations = tree(RotationsConfigurable())

        override fun enable() {
            threwPearl = false
            canFly = false
        }

        val repeatable = repeatable {
            if (player.isSpectator || player.isDead || player.abilities.creativeMode) {
                return@repeatable
            }

            val slot = findHotbarSlot(Items.ENDER_PEARL)

            // Make sure the player is STILL flying
            if (canFly) {
                player.strafe(speed = speed.toDouble())
                player.velocity.y = when {
                    mc.options.keyJump.isPressed -> speed.toDouble()
                    mc.options.keySneak.isPressed -> -speed.toDouble()
                    else -> 0.0
                }
            }

            if (!threwPearl) {
                if (slot != null) {
                    if (slot != player.inventory.selectedSlot) {
                        network.sendPacket(UpdateSelectedSlotC2SPacket(slot))
                    }

                    if (player.pitch <= 80) {
                        RotationManager.aimAt(
                            Rotation(player.yaw, RandomUtils.nextFloat(80f, 90f)),
                            configurable = rotations
                        )
                    }

                    wait(2)
                    network.sendPacket(PlayerInteractItemC2SPacket(Hand.MAIN_HAND))

                    if (slot != player.inventory.selectedSlot) {
                        network.sendPacket(UpdateSelectedSlotC2SPacket(player.inventory.selectedSlot))
                    }

                    threwPearl = true
                }
            } else if (threwPearl && canFly && player.hurtTime > 0) {
                player.strafe(speed = speed.toDouble())
                player.velocity.y = when {
                    mc.options.keyJump.isPressed -> speed.toDouble()
                    mc.options.keySneak.isPressed -> -speed.toDouble()
                    else -> 0.0
                }
            }
        }

        val packetHandler = handler<PacketEvent> { event ->
            if (event.packet is PlaySoundS2CPacket && event.packet.sound == SoundEvents.ENTITY_ENDER_PEARL_THROW && threwPearl) {
                canFly = true
            }
        }
    }
}
