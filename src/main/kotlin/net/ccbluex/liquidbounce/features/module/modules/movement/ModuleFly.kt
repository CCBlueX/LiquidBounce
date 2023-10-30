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
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.aiming.Rotation
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.block.isBlockAtPosition
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.entity.box
import net.ccbluex.liquidbounce.utils.entity.strafe
import net.ccbluex.liquidbounce.utils.item.findHotbarSlot
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.movement.zeroXZ
import net.minecraft.block.Block
import net.minecraft.block.FluidBlock
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket
import net.minecraft.util.Hand
import net.minecraft.util.shape.VoxelShapes
import org.apache.commons.lang3.RandomUtils
import kotlin.math.cos
import kotlin.math.sin

/**
 * Fly module
 *
 * Allows you to fly.
 */

object ModuleFly : Module("Fly", Category.MOVEMENT) {

    private val modes = choices(
        "Mode", Vanilla, arrayOf(
            Vanilla, Jetpack, VerusOld, Enderpearl, Spartan524, Sentinel27thOct, VerusDamage
        )
    )

    private object Visuals : ToggleableConfigurable(this, "Visuals", true) {

        private val stride by boolean("Stride", true)

        val strideHandler = handler<PlayerStrideEvent> { event ->
            if (stride) {
                event.strideForce = 0.1.coerceAtMost(player.velocity.horizontalLength()).toFloat()
            }

        }

    }

    private object Vanilla : Choice("Vanilla") {

        val horizontalSpeed by float("Horizontal", 0.44f, 0.1f..5f)
        val verticalSpeed by float("Vertical", 0.44f, 0.1f..5f)

        val glide by float("Glide", 0.0f, -1f..1f)

        val bypassVanillaCheck by boolean("BypassVanillaCheck", true)

        override val parent: ChoiceConfigurable
            get() = modes

        val repeatable = repeatable {
            player.strafe(speed = horizontalSpeed.toDouble())
            player.velocity.y = when {
                player.input.jumping -> verticalSpeed.toDouble()
                player.input.sneaking -> (-verticalSpeed).toDouble()
                else -> glide.toDouble()
            }

            // Most basic bypass for vanilla fly check
            // This can also be done via packets, but this is easier.
            if (bypassVanillaCheck && player.age % 40 == 0) {
                wait(1)
                player.velocity.y = -0.04
                wait(1)
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

    private object VerusOld : Choice("VerusOld") {

        override val parent: ChoiceConfigurable
            get() = modes

        val onGround by boolean("OnGround", true)

        val packetHandler = handler<PacketEvent> { event ->
            if (event.packet is PlayerMoveC2SPacket) {
                event.packet.onGround = onGround
            }
        }
        val shapeHandler = handler<BlockShapeEvent> { event ->
            if (event.state.block !is FluidBlock && event.pos.y < player.y) {
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
            val slot = findHotbarSlot(Items.ENDER_PEARL)

            if (player.isDead || player.isSpectator || player.abilities.creativeMode) {
                return@repeatable
            }

            if (!threwPearl && !canFly) {
                if (slot != null) {
                    if (slot != player.inventory.selectedSlot) {
                        network.sendPacket(UpdateSelectedSlotC2SPacket(slot))
                    }

                    if (player.pitch <= 80) {
                        RotationManager.aimAt(
                            Rotation(player.yaw, RandomUtils.nextFloat(80f, 90f)), configurable = rotations
                        )
                    }

                    wait(2)
                    interaction.sendSequencedPacket(world) { sequence ->
                        PlayerInteractItemC2SPacket(Hand.MAIN_HAND, sequence)
                    }

                    if (slot != player.inventory.selectedSlot) {
                        network.sendPacket(UpdateSelectedSlotC2SPacket(player.inventory.selectedSlot))
                    }

                    threwPearl = true
                }
            } else if (!threwPearl && canFly) {
                player.strafe(speed = speed.toDouble())
                player.velocity.y = when {
                    mc.options.jumpKey.isPressed -> speed.toDouble()
                    mc.options.sneakKey.isPressed -> -speed.toDouble()
                    else -> 0.0
                }
                return@repeatable
            }
        }

        val packetHandler = handler<PacketEvent> { event ->
            if (event.origin == TransferOrigin.SEND && event.packet is TeleportConfirmC2SPacket && isABitAboveGround() && threwPearl) {
                threwPearl = false
                canFly = true
            }
        }

        fun isABitAboveGround(): Boolean {
            for (y in 0..5) {
                val boundingBox = player.box
                val detectionBox = boundingBox.withMinY(boundingBox.minY - y)

                return isBlockAtPosition(detectionBox) { it is Block }
            }
            return false
        }
    }

    /**
     * @anticheat Spartan
     * @anticheatVersion phase 524
     * @testedOn minecraft.vagdedes.com
     * @note spartan flags less if your motion is stable, that's why we use PlayerMoveEvent
     */
    private object Spartan524 : Choice("Spartan524") {

        override val parent: ChoiceConfigurable
            get() = modes

        val moveHandler = handler<PlayerMoveEvent> { event ->
            val yaw = Math.toRadians(player.yaw.toDouble())
            event.movement.x = -sin(yaw) * 0.28
            event.movement.y = 0.0
            event.movement.z = cos(yaw) * 0.28
        }
    }

    /**
     * @anticheat Sentinel
     * @anticheatVersion 27.10.2023
     * @testedOn cubecraft.net
     *
     * @note Tested in SkyWars and EggWars, works fine and no automatic ban.
     * @note This is a very simple fly, it's not the best, but it's not bad either.
     * Bypasses Sentinel's fly check and is a little faster. Might can be improved.
     * This fly does not require any disabler.
     */
    private object Sentinel27thOct : Choice("Sentinel27thOct") {

        override val parent: ChoiceConfigurable
            get() = modes

        val repeatable = repeatable {
            player.velocity.y = 0.2
            player.strafe(speed = 0.34)
            wait(4)
        }

    }

    /**
     * @anticheat Verus
     * @anticheatVersion b3896
     * @testedOn eu.loyisa.cn
     * @note it gives you ~2 flags for damage
     */
    private object VerusDamage : Choice("VerusDamage") {

        override val parent: ChoiceConfigurable
            get() = modes

        var flyTicks = 0
        var shouldStop = false
        var gotDamage = false

        override fun enable() {
            network.sendPacket(PlayerMoveC2SPacket.PositionAndOnGround(player.x, player.y, player.z, false))
            network.sendPacket(PlayerMoveC2SPacket.PositionAndOnGround(player.x, player.y + 3.25, player.z, false))
            network.sendPacket(PlayerMoveC2SPacket.PositionAndOnGround(player.x, player.y, player.z, false))
            network.sendPacket(PlayerMoveC2SPacket.PositionAndOnGround(player.x, player.y, player.z, true))
        }

        val failRepeatable = repeatable {
            if (!gotDamage) {
                wait { 20 }
                if (!gotDamage) {
                    chat("Failed to self-damage")
                    shouldStop = true
                }
            }
        }
        val repeatable = repeatable {
            if (player.hurtTime > 0)
                gotDamage = true
            if (!gotDamage) {
                return@repeatable
            }
            if (++flyTicks > 20 || shouldStop) {
                enabled = false
                return@repeatable
            }
            player.strafe(speed = 9.95)
            player.velocity.y = 0.0
            Timer.requestTimerSpeed(0.1f, Priority.IMPORTANT_FOR_USAGE)
        }

        override fun disable() {
            flyTicks = 0
            player.zeroXZ()
        }
    }
}
