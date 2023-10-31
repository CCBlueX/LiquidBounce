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

package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.raycast
import net.ccbluex.liquidbounce.utils.block.getBlock
import net.ccbluex.liquidbounce.utils.block.targetFinding.BlockPlacementTarget
import net.ccbluex.liquidbounce.utils.block.targetFinding.BlockPlacementTargetFindingOptions
import net.ccbluex.liquidbounce.utils.block.targetFinding.CenterTargetPositionFactory
import net.ccbluex.liquidbounce.utils.block.targetFinding.findBestBlockPlacementTarget
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.entity.FallingPlayer
import net.minecraft.block.Blocks
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.Vec3i
import kotlin.math.abs

/**
 * NoFall module
 *
 * Protects you from taking fall damage.
 */

object ModuleNoFall : Module("NoFall", Category.PLAYER) {

    private val modes = choices(
        "Mode", SpoofGround, arrayOf(
            SpoofGround, NoGround, Packet, MLG, Spartan524Flag, Vulcan, Verus
        )
    )

    private object SpoofGround : Choice("SpoofGround") {

        override val parent: ChoiceConfigurable
            get() = modes

        val packetHandler = handler<PacketEvent> {
            val packet = it.packet

            if (packet is PlayerMoveC2SPacket) {
                packet.onGround = true
            }

        }

    }

    private object NoGround : Choice("NoGround") {

        override val parent: ChoiceConfigurable
            get() = modes

        val packetHandler = handler<PacketEvent> {
            val packet = it.packet

            if (packet is PlayerMoveC2SPacket) {
                packet.onGround = false
            }

        }

    }

    private object Packet : Choice("Packet") {

        override val parent: ChoiceConfigurable
            get() = modes

        val repeatable = repeatable {
            if (player.fallDistance > 2f) {
                network.sendPacket(PlayerMoveC2SPacket.OnGroundOnly(true))
            }
        }

    }

    /**
     * @anticheat Spartan
     * @anticheatVersion phase 524
     * @testedOn minecraft.vagdedes.com
     * @note it gives you 6 flags for 50 blocks, which isn't enough to get kicked
     */
    private object Spartan524Flag : Choice("Spartan524Flag") {

        override val parent: ChoiceConfigurable
            get() = modes

        val repeatable = repeatable {
            if (player.fallDistance > 2f) {
                network.sendPacket(PlayerMoveC2SPacket.OnGroundOnly(true))
                wait { 1 }
            }
        }

    }

    object MLG : Choice("MLG") {
        override val parent: ChoiceConfigurable
            get() = modes

        val minFallDist by float("MinFallDistance", 5f, 2f..50f)

        val rotationsConfigurable = tree(RotationsConfigurable())

        var currentTarget: BlockPlacementTarget? = null

        val itemForMLG
            get() = findClosestItem(
                arrayOf(
                    Items.WATER_BUCKET, Items.COBWEB, Items.POWDER_SNOW_BUCKET, Items.HAY_BLOCK, Items.SLIME_BLOCK
                )
            )

        val tickMovementHandler = handler<PlayerNetworkMovementTickEvent> {
            if (it.state != EventState.PRE || player.fallDistance <= minFallDist || itemForMLG == null) {
                return@handler
            }

            val collision = FallingPlayer.fromPlayer(player).findCollision(20)?.pos ?: return@handler

            if (collision.getBlock() in arrayOf(
                    Blocks.WATER, Blocks.COBWEB, Blocks.POWDER_SNOW, Blocks.HAY_BLOCK, Blocks.SLIME_BLOCK
                )
            ) {
                return@handler
            }

            val options = BlockPlacementTargetFindingOptions(
                listOf(Vec3i(0, 0, 0)),
                player.inventory.getStack(itemForMLG!!),
                CenterTargetPositionFactory,
                BlockPlacementTargetFindingOptions.PRIORITIZE_LEAST_BLOCK_DISTANCE
            )

            currentTarget = findBestBlockPlacementTarget(collision.up(), options)

            val target = currentTarget ?: return@handler

            RotationManager.aimAt(target.rotation, configurable = rotationsConfigurable)
        }

        val tickHandler = handler<GameTickEvent> {
            val target = currentTarget ?: return@handler
            val rotation = RotationManager.currentRotation ?: return@handler

            val rayTraceResult = raycast(4.5, rotation) ?: return@handler

            if (rayTraceResult.type != HitResult.Type.BLOCK || rayTraceResult.blockPos != target.interactedBlockPos || rayTraceResult.side != target.direction) {
                return@handler
            }

            val item = itemForMLG ?: return@handler

            SilentHotbar.selectSlotSilently(this, item, 1)

            doPlacement(rayTraceResult)

            currentTarget = null
        }

        private fun findClosestItem(items: Array<Item>): Int? {
            return (0..8)
                .filter { player.inventory.getStack(it).item in items }
                .minByOrNull { abs(player.inventory.selectedSlot - it) }
        }

        fun doPlacement(
            rayTraceResult: BlockHitResult,
            hand: Hand = Hand.MAIN_HAND,
            onPlacementSuccess: () -> Boolean = { true },
            onItemUseSuccess: () -> Boolean = { true }
        ) {
            val stack = player.mainHandStack
            val count = stack.count

            val interactionResult = interaction.interactBlock(player, hand, rayTraceResult)

            when {
                interactionResult == ActionResult.FAIL -> {
                    return
                }

                interactionResult == ActionResult.PASS -> {
                    // Ok, we cannot place on the block, so let's just use the item in the direction
                    // without targeting a block (for buckets, etc.)
                    handlePass(hand, stack, onItemUseSuccess)
                    return
                }

                interactionResult.isAccepted -> {
                    val wasStackUsed = !stack.isEmpty && (stack.count != count || interaction.hasCreativeInventory())

                    handleActionsOnAccept(hand, interactionResult, wasStackUsed, onPlacementSuccess)
                }
            }
        }

        /**
         * Swings item, resets equip progress and hand swing progress
         *
         * @param wasStackUsed was an item consumed in order to place the block
         */
        private fun handleActionsOnAccept(
            hand: Hand,
            interactionResult: ActionResult,
            wasStackUsed: Boolean,
            onPlacementSuccess: () -> Boolean,
        ) {
            if (!interactionResult.shouldSwingHand()) {
                return
            }

            if (onPlacementSuccess()) {
                player.swingHand(hand)
            }

            if (wasStackUsed) {
                mc.gameRenderer.firstPersonRenderer.resetEquipProgress(hand)
            }

            return
        }

        /**
         * Just interacts with the item in the hand instead of using it on the block
         */
        private fun handlePass(hand: Hand, stack: ItemStack, onItemUseSuccess: () -> Boolean) {
            if (stack.isEmpty) {
                return
            }

            val actionResult = interaction.interactItem(player, hand)

            handleActionsOnAccept(hand, actionResult, true, onItemUseSuccess)
        }
    }

    /**
     * @anticheat Vulcan
     * @anticheatVersion 2.7.7
     * @testedOn eu.loyisa.cn
     */
    object Vulcan : Choice("Vulcan") {
        override val parent: ChoiceConfigurable
            get() = modes

        val packetHandler = handler<PacketEvent> {
            val packet = it.packet
            if (packet is PlayerMoveC2SPacket && player.fallDistance > 7.0) {
                packet.onGround = true
                player.fallDistance = 0f
                player.velocity.y = 0.0
            }
        }
    }


    /**
     * @anticheat Verus
     * @anticheatVersion b3896
     * @testedOn eu.loyisa.cn
     */
    object Verus : Choice("Verus") {
        override val parent: ChoiceConfigurable
            get() = modes

        val packetHandler = handler<PacketEvent> {
            val packet = it.packet
            if (packet is PlayerMoveC2SPacket && player.fallDistance > 3.35) {
                packet.onGround = true
                player.fallDistance = 0f
                player.velocity.y = 0.0
            }
        }
    }
}
