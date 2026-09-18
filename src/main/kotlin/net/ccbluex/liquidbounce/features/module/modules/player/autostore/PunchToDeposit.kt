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
package net.ccbluex.liquidbounce.features.module.modules.player.autostore

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.event.waitTicks
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsValueGroup
import net.ccbluex.liquidbounce.utils.aiming.utils.selectBlockTarget
import net.ccbluex.liquidbounce.utils.block.SwingMode
import net.ccbluex.liquidbounce.utils.block.searchBlocksInRangeSorted
import net.ccbluex.liquidbounce.utils.block.state
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.collection.blockSortedSetOf
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.raytracing.raytraceBlock
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import java.util.function.BooleanSupplier

/**
 * (Server): When enabled allows you to punch a chest to deposit a stack of items.
 */
internal object PunchToDeposit : ToggleableValueGroup(AutoDeposit, "PunchToDeposit", false) {

    private val interactionRange by float("Range", 3F, 1F..6F)
    private val wallInteractionRange by float("WallRange", 0f, 0F..6F).onChange {
        minOf(interactionRange, it)
    }
    private val interactionDelay by int("Delay", 5, 1..80, "ticks")
    private val swingMode by enumChoice("SwingMode", SwingMode.DO_NOT_HIDE)

    private val pauseOn by multiEnumChoice<PauseCondition>("PauseOn")

    @Suppress("unused")
    private enum class PauseCondition(override val tag: String) : Tagged, BooleanSupplier {
        COMBAT("Combat") {
            override fun getAsBoolean() = CombatManager.isInCombat
        },
        USING_ITEM("UsingItem") {
            override fun getAsBoolean() = player.isUsingItem
        };
    }

    private val validStorageBlocks by blocks(
        "ValidStorageBlocks",
        blockSortedSetOf(Blocks.CHEST, Blocks.ENDER_CHEST),
    )

    private val rotateToTarget by boolean("RotateToTarget", false)
    private val rotations = tree(RotationsValueGroup(this))

    private var currentTargetBlock: BlockPos? = null

    override val running: Boolean
        get() = super.running && pauseOn.none { it.asBoolean }

    override fun onDisabled() {
        currentTargetBlock = null
        super.onDisabled()
    }

    /**
     * Sends a full left-click (START_DESTROY_BLOCK + ABORT_DESTROY_BLOCK) on a storage block.
     */
    private fun clickStorage(hit: BlockHitResult) {
        interaction.startPrediction(world) { sequence ->
            ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                hit.blockPos, hit.direction, sequence
            )
        }
        network.send(
            ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK,
                hit.blockPos, Direction.DOWN
            )
        )
    }

    @Suppress("unused")
    private val simulatedTickHandler = handler<RotationUpdateEvent> {
        if (!rotateToTarget || AutoDeposit.matchingSlots(inventory = false).isEmpty()) {
            return@handler
        }

        currentTargetBlock = selectBlockTarget(
            player.eyePosition,
            interactionRange,
            wallInteractionRange,
            player.eyePosition.searchBlocksInRangeSorted(interactionRange) { pos, state ->
                state.block in validStorageBlocks
            },
            rotations,
            AutoDeposit
        )
    }

    @Suppress("unused")
    private val clickTask = tickHandler {
        // A container is already open (being processed by ModuleAutoStore)
        if (mc.gui.screen() is AbstractContainerScreen<*>) {
            return@tickHandler
        }

        val slot = AutoDeposit.matchingSlots(inventory = false).firstOrNull() as? HotbarItemSlot ?: return@tickHandler

        val rayTraceResult = if (rotateToTarget) {
            val targetBlockPos = currentTargetBlock ?: return@tickHandler
            val currentPlayerRotation = RotationManager.serverRotation

            val hit = raytraceBlock(
                interactionRange.toDouble(),
                currentPlayerRotation,
                targetBlockPos,
                targetBlockPos.state ?: return@tickHandler
            )

            if (hit?.type != HitResult.Type.BLOCK || hit.blockPos != targetBlockPos) {
                return@tickHandler
            }
            hit
        } else {
            val hit = mc.hitResult as? BlockHitResult ?: return@tickHandler
            val state = hit.blockPos.state ?: return@tickHandler
            if (state.block !in validStorageBlocks) {
                return@tickHandler
            }
            hit
        }

        SilentHotbar.selectSlotSilently(this, slot, 1)
        clickStorage(rayTraceResult)
        swingMode.swing(InteractionHand.MAIN_HAND)

        if (rotateToTarget) {
            currentTargetBlock = null
        }

        waitTicks(interactionDelay)
    }

}
