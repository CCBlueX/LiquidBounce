/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.movement.noslow

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.blocking.Block
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.consume.Consume
import net.ccbluex.liquidbounce.utils.block.getState
import net.minecraft.block.HoneyBlock
import net.minecraft.block.SlimeBlock
import net.minecraft.block.SoulSandBlock
import net.minecraft.network.packet.c2s.play.*
import net.minecraft.util.UseAction

/**
 * NoSlow module
 *
 * Cancels slowness effects caused by blocks and using items.
 */

object ModuleNoSlow : Module("NoSlow", Category.MOVEMENT) {

    private object Bow : ToggleableConfigurable(this, "Bow", true) {
        val forwardMultiplier by float("Forward", 1f, 0.2f..1f)
        val sidewaysMultiplier by float("Sideways", 1f, 0.2f..1f)
        val noInteract by boolean("NoInteract", false)
    }

    private object Soulsand : ToggleableConfigurable(this, "Soulsand", true) {

        val multiplier by float("Multiplier", 1f, 0.4f..2f)

        @Suppress("unused")
        val blockVelocityHandler = handler<BlockVelocityMultiplierEvent> { event ->
            if (event.block is SoulSandBlock) {
                event.multiplier = multiplier
            }
        }

    }

    object Slime : ToggleableConfigurable(this, "SlimeBlock", true) {
        private val multiplier by float("Multiplier", 1f, 0.4f..2f)

        @Suppress("unused")
        val blockSlipperinessMultiplierHandler = handler<BlockSlipperinessMultiplierEvent> { event ->
            if (event.block is SlimeBlock) {
                event.slipperiness = 0.6f
            }
        }

        @Suppress("unused")
        val blockVelocityHandler = handler<BlockVelocityMultiplierEvent> { event ->
            if (event.block is SlimeBlock) {
                event.multiplier = multiplier
            }
        }
    }

    private object Honey : ToggleableConfigurable(this, "HoneyBlock", true) {
        val multiplier by float("Multiplier", 1f, 0.4f..2f)

        @Suppress("unused")
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
        @Suppress("unused")
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

    @Suppress("unused")
    val multiplierHandler = handler<PlayerUseMultiplier> { event ->
        val action = player.activeItem.useAction ?: return@handler
        val (forward, strafe) = multiplier(action)

        event.forward = forward
        event.sideways = strafe
    }

    val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet

        if (packet is PlayerInteractBlockC2SPacket) {
            val useAction =
                player.getStackInHand(packet.hand)?.useAction ?: return@handler
            val blockPos = packet.blockHitResult?.blockPos

            // Check if we might click a block that is not air
            if (blockPos != null && blockPos.getState()?.isAir != true) {
                return@handler
            }

            val consumeAction =
                (useAction == UseAction.EAT || useAction == UseAction.DRINK) && Consume.enabled && Consume.noInteract
            val bowAction = useAction == UseAction.BOW && Bow.enabled && Bow.noInteract

            if (consumeAction || bowAction) {
                event.cancelEvent()
            }
        }
    }

    private fun multiplier(action: UseAction) = when (action) {
        UseAction.NONE -> Pair(0.2f, 0.2f)
        UseAction.EAT, UseAction.DRINK -> if (Consume.enabled) Pair(
            Consume.forwardMultiplier, Consume.sidewaysMultiplier
        ) else Pair(0.2f, 0.2f)

        UseAction.BLOCK, UseAction.SPYGLASS, UseAction.TOOT_HORN, UseAction.BRUSH ->
            if (Block.enabled && (!Block.onlySlowOnServerSide || Block.blockingHand == null)) Pair(
                Block.forwardMultiplier,
                Block.sidewaysMultiplier
            )
        else Pair(0.2f, 0.2f)

        UseAction.BOW, UseAction.CROSSBOW, UseAction.SPEAR -> if (Bow.enabled) Pair(
            Bow.forwardMultiplier, Bow.sidewaysMultiplier
        ) else Pair(0.2f, 0.2f)
    }
}
