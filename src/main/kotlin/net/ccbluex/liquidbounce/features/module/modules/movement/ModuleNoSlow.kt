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
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.NoneChoice
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.block.getState
import net.minecraft.block.HoneyBlock
import net.minecraft.block.SlimeBlock
import net.minecraft.block.SoulSandBlock
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket
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
        val onlySlowOnServerSide by boolean("OnlySlowOnServerSide", false)

        val modes = choices(this, "Mode", { it.choices[0] }) {
            arrayOf(NoneChoice(it), Reuse, Rehold)
        }

        /**
         * The hand that is currently blocking on the server
         *
         * Why are we not using [player.isBlocking] instead? Because on certain modules, we do block client-side,
         * but not server-side. This is the case for [ModuleKillAura] for example.
         */
        var blockingHand: Hand? = null
        private var nextIsIgnored = false

        object Reuse : Choice("Reuse") {

            override val parent: ChoiceConfigurable
                get() = modes

            val onNetworkTick = handler<PlayerNetworkMovementTickEvent> { event ->
                if (blockingHand != null) {
                    when (event.state) {
                        EventState.PRE -> {
                            nextIsIgnored = true
                            network.sendPacket(
                                PlayerActionC2SPacket(
                                    PlayerActionC2SPacket.Action.RELEASE_USE_ITEM,
                                    BlockPos.ORIGIN,
                                    Direction.DOWN
                                )
                            )
                        }

                        EventState.POST -> {
                            nextIsIgnored = true
                            interaction.sendSequencedPacket(world) { sequence ->
                                PlayerInteractItemC2SPacket(blockingHand, sequence)
                            }
                        }
                    }
                }
            }


        }

        object Rehold : Choice("Rehold") {

            override val parent: ChoiceConfigurable
                get() = modes

            val onNetworkTick = handler<PlayerNetworkMovementTickEvent> { event ->
                if (blockingHand == Hand.MAIN_HAND) {
                    when (event.state) {
                        EventState.PRE -> {
                            nextIsIgnored = true
                            network.sendPacket(UpdateSelectedSlotC2SPacket((0..8).random()))
                        }

                        EventState.POST -> {
                            nextIsIgnored = true
                            network.sendPacket(UpdateSelectedSlotC2SPacket(player.inventory.selectedSlot))

                            nextIsIgnored = true
                            interaction.sendSequencedPacket(world) { sequence ->
                                PlayerInteractItemC2SPacket(blockingHand, sequence)
                            }
                        }
                    }
                }
            }


        }

        val packetHandler = handler<PacketEvent> {
            val packet = it.packet

            when (packet) {
                is PlayerActionC2SPacket -> {
                    // Ignores our own module packets
                    if (nextIsIgnored) {
                        nextIsIgnored = false
                        return@handler
                    }

                    if (packet.action == PlayerActionC2SPacket.Action.RELEASE_USE_ITEM) {
                        blockingHand = null
                    }
                }

                is PlayerInteractItemC2SPacket -> {
                    // Ignores our own module packets
                    if (nextIsIgnored) {
                        nextIsIgnored = false
                        return@handler
                    }

                    if (player.getStackInHand(packet.hand).useAction == UseAction.BLOCK) {
                        blockingHand = packet.hand
                    }
                }

                is UpdateSelectedSlotC2SPacket -> {
                    // Ignores our own module packets
                    if (nextIsIgnored) {
                        nextIsIgnored = false
                        return@handler
                    }

                    blockingHand = null
                }
            }
        }

    }

    val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet

        if (packet is PlayerInteractBlockC2SPacket) {
            val useAction = player.getStackInHand(packet.hand)?.useAction ?: return@handler
            val blockPos = packet.blockHitResult?.blockPos

            // Check if we might click a block that is not air
            if (blockPos != null && blockPos.getState()?.isAir != true) {
                return@handler
            }

            val consumeAction = (useAction == UseAction.EAT || useAction == UseAction.DRINK)
                && Consume.enabled && Consume.noInteract
            val bowAction = useAction == UseAction.BOW && Bow.enabled && Bow.noInteract

            if (consumeAction || bowAction) {
                event.cancelEvent()
            }
        }
    }

    private object Consume : ToggleableConfigurable(this, "Consume", true) {
        val forwardMultiplier by float("Forward", 1f, 0.2f..1f)
        val sidewaysMultiplier by float("Sideways", 1f, 0.2f..1f)
        val noInteract by boolean("NoInteract", false)

        val modes = choices(this, "Mode", { it.choices[0] }) {
            arrayOf(NoneChoice(it), Grim, OldGrim)
        }


        // Works on 1.9+ grim servers.
        object Grim : Choice("Grim") {

            override val parent: ChoiceConfigurable
                get() = modes

            val onNetworkTick = handler<PlayerNetworkMovementTickEvent> { event ->
                if (player.isUsingItem && event.state == EventState.PRE) {
                    val hand = player.activeHand;
                    if (hand == Hand.MAIN_HAND) {
                        // Send offhand interact packet
                        // so that grim focuses on offhand noslow checks that dont exist.
                        network.sendPacket(PlayerInteractItemC2SPacket(Hand.OFF_HAND, 0))
                    } else if (hand == Hand.OFF_HAND) {
                        // Switch slots (based on 1.8 grim switch noslow)
                        network.sendPacket(UpdateSelectedSlotC2SPacket(player.inventory.selectedSlot % 8 + 1))
                        network.sendPacket(UpdateSelectedSlotC2SPacket(player.inventory.selectedSlot))
                    }
                }
            }

        }

        // Works on newest grim, but only 1.8 servers since switching items on 1.9 stops the item from being used.
        object OldGrim : Choice("1.8Grim") {
            override val parent: ChoiceConfigurable
                get() = modes

            val onNetworkTick = handler<PlayerNetworkMovementTickEvent> { event ->
                if (player.isUsingItem && event.state == EventState.PRE) {
                    // Switch slots so grim exempts noslow...
                    // Introduced with https://github.com/GrimAnticheat/Grim/issues/874
                    network.sendPacket(UpdateSelectedSlotC2SPacket(player.inventory.selectedSlot % 8 + 1))
                    network.sendPacket(UpdateSelectedSlotC2SPacket(player.inventory.selectedSlot))
                }
            }

        }

    }

    private object Bow : ToggleableConfigurable(this, "Bow", true) {
        val forwardMultiplier by float("Forward", 1f, 0.2f..1f)
        val sidewaysMultiplier by float("Sideways", 1f, 0.2f..1f)
        val noInteract by boolean("NoInteract", false)
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

        UseAction.BLOCK, UseAction.SPYGLASS, UseAction.TOOT_HORN, UseAction.BRUSH ->
            if (Block.enabled && (!Block.onlySlowOnServerSide || Block.blockingHand == null))
                Pair(Block.forwardMultiplier, Block.sidewaysMultiplier)
            else
                Pair(0.2f, 0.2f)

        UseAction.BOW, UseAction.CROSSBOW, UseAction.SPEAR -> if (Bow.enabled) Pair(
            Bow.forwardMultiplier,
            Bow.sidewaysMultiplier
        ) else Pair(0.2f, 0.2f)
    }
}
