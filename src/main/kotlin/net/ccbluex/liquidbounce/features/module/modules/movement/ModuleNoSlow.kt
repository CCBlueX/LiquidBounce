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
import net.ccbluex.liquidbounce.config.NoneChoice
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.block.HoneyBlock
import net.minecraft.block.SlimeBlock
import net.minecraft.block.SoulSandBlock
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket
import net.minecraft.util.Hand
import net.minecraft.util.UseAction
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction

/**
 * NoSlow module
 *
 * Cancels slowness effects caused by blocks and using items.
 */

object ModuleNoSlow : Module("NoSlow", Category.MOVEMENT) {

    private object Block : ToggleableConfigurable(this, "Blocking", true) {

        val forwardMultiplier by float("Forward", 1f, 0.2f..1f)
        val sidewaysMultiplier by float("Sideways", 1f, 0.2f..1f)

        val modes = choices("Choice", Reuse) {
            arrayOf(NoneChoice(it), Reuse, Rehold)
        }

        object Reuse : Choice("Reuse") {

            override val parent: ChoiceConfigurable
                get() = modes

            val onNetworkTick = handler<PlayerNetworkMovementTickEvent> { event ->
                if (player.isBlocking) {
                    when (event.state) {
                        EventState.PRE -> network.sendPacket(
                            PlayerActionC2SPacket(
                                PlayerActionC2SPacket.Action.RELEASE_USE_ITEM,
                                BlockPos.ORIGIN,
                                Direction.DOWN
                            )
                        )

                        EventState.POST -> interaction.sendSequencedPacket(world) { sequence ->
                            PlayerInteractItemC2SPacket(player.activeHand, sequence)
                        }
                    }
                }
            }


        }

        object Rehold : Choice("Rehold") {

            override val parent: ChoiceConfigurable
                get() = modes

            val onNetworkTick = handler<PlayerNetworkMovementTickEvent> { event ->
                if (player.isBlocking && player.activeHand == Hand.MAIN_HAND) {
                    when (event.state) {
                        EventState.PRE -> {
                            network.sendPacket(UpdateSelectedSlotC2SPacket((0..8).random()))
                        }

                        EventState.POST -> {
                            network.sendPacket(UpdateSelectedSlotC2SPacket(player.inventory.selectedSlot))
                            interaction.sendSequencedPacket(world) { sequence ->
                                PlayerInteractItemC2SPacket(player.activeHand, sequence)
                            }
                        }
                    }
                }
            }


        }

    }

    private object Consume : ToggleableConfigurable(this, "Consume", true) {
        val forwardMultiplier by float("Forward", 1f, 0.2f..1f)
        val sidewaysMultiplier by float("Sideways", 1f, 0.2f..1f)
    }

    private object Bow : ToggleableConfigurable(this, "Bow", true) {
        val forwardMultiplier by float("Forward", 1f, 0.2f..1f)
        val sidewaysMultiplier by float("Sideways", 1f, 0.2f..1f)
    }

    private object Soulsand : ToggleableConfigurable(this, "Soulsand", true) {

        val multiplier by float("Multiplier", 1f, 0.4f..2f)

        val blockVelocityHandler = handler<BlockVelocityMultiplierEvent> { event ->
            if (event.block is SoulSandBlock) {
                event.multiplier = multiplier
            }
        }

    }

    object Slime : ToggleableConfigurable(this, "SlimeBlock", true) {
        private val multiplier by float("Multiplier", 1f, 0.4f..2f)

        val blockSlipperinessMultiplierHandler = handler<BlockSlipperinessMultiplierEvent> { event ->
            if (event.block is SlimeBlock) {
                event.slipperiness = 0.6f
            }
        }

        val blockVelocityHandler = handler<BlockVelocityMultiplierEvent> { event ->
            if (event.block is SlimeBlock) {
                event.multiplier = multiplier
            }
        }
    }

    private object Honey : ToggleableConfigurable(this, "HoneyBlock", true) {
        val multiplier by float("Multiplier", 1f, 0.4f..2f)

        val blockVelocityHandler = handler<BlockVelocityMultiplierEvent> { event ->
            if (event.block is HoneyBlock) {
                event.multiplier = multiplier
            }
        }
    }

    object PowderSnow : ToggleableConfigurable(this, "PowderSnow", true) {
        val multiplier by float("Multiplier", 1f, 0.4f..2f)
    }

    private object Fluid : ToggleableConfigurable(this, "Fluid", true) {
        val fluidPushHandler = handler<FluidPushEvent> {
            it.cancelEvent()
        }
    }

    init {
        tree(Block)
        tree(Consume)
        tree(Bow)
        tree(Soulsand)
        tree(Slime)
        tree(Honey)
        tree(PowderSnow)
        tree(Fluid)
    }

    val multiplierHandler = handler<PlayerUseMultiplier> { event ->
        val action = player.activeItem.useAction ?: return@handler
        val (forward, strafe) = multiplier(action)

        event.forward = forward
        event.sideways = strafe
    }

    private fun multiplier(action: UseAction) = when (action) {
        UseAction.NONE -> Pair(0.2f, 0.2f)
        UseAction.EAT, UseAction.DRINK -> if (Consume.enabled) Pair(
            Consume.forwardMultiplier,
            Consume.sidewaysMultiplier
        ) else Pair(0.2f, 0.2f)

        UseAction.BLOCK, UseAction.SPYGLASS, UseAction.TOOT_HORN, UseAction.BRUSH -> if (Block.enabled) Pair(
            Block.forwardMultiplier,
            Block.sidewaysMultiplier
        ) else Pair(0.2f, 0.2f)

        UseAction.BOW, UseAction.CROSSBOW, UseAction.SPEAR -> if (Bow.enabled) Pair(
            Bow.forwardMultiplier,
            Bow.sidewaysMultiplier
        ) else Pair(0.2f, 0.2f)
    }
}
