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
package net.ccbluex.liquidbounce.features.module.modules.player.autodeposit

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.requires
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.utils.aiming.RotationTarget
import net.ccbluex.liquidbounce.utils.aiming.RotationsValueGroup
import net.ccbluex.liquidbounce.utils.aiming.lookAt
import net.ccbluex.liquidbounce.utils.aiming.utils.selectBlockTarget
import net.ccbluex.liquidbounce.utils.block.SwingMode
import net.ccbluex.liquidbounce.utils.block.searchBlocksInRangeSorted
import net.ccbluex.liquidbounce.utils.block.state
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.collection.blockSortedSetOf
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.raytracing.raytraceBlock
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import java.util.function.BooleanSupplier

/**
 * (Server): When enabled allows you to punch a chest to deposit a stack of items.
 */
internal object PunchToDeposit : ToggleableValueGroup(ModuleAutoDeposit, "PunchToDeposit", false) {

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

    /** The storage block to punch and the target that aims at it, chosen anew every tick. */
    private var currentTarget: Pair<BlockPos, RotationTarget>? = null

    override val running: Boolean
        get() = super.running && pauseOn.none { it.asBoolean }

    override fun onDisabled() {
        currentTarget = null
        cooldownLeft = 0
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
        currentTarget = null
        if (!rotateToTarget || ModuleAutoDeposit.matchingSlots(inventory = false).isEmpty()) {
            return@handler
        }

        currentTarget = selectBlockTarget(
            player.eyePosition,
            interactionRange,
            wallInteractionRange,
            player.eyePosition.searchBlocksInRangeSorted(interactionRange) { pos, state ->
                state.block in validStorageBlocks
            },
            rotations,
        )
    }

    private fun depositSlot() = ModuleAutoDeposit.matchingSlots(inventory = false).firstOrNull() as? HotbarItemSlot

    /** Ticks of [interactionDelay] left after the last deposit. */
    private var cooldownLeft = 0

    @Suppress("unused")
    private val tickHandler = tickHandler {
        if (cooldownLeft > 0) {
            cooldownLeft--
            return@tickHandler
        }

        if (mc.gui.screen() is AbstractContainerScreen<*>) {
            return@tickHandler
        }

        if (rotateToTarget) {
            currentTarget ?: return@tickHandler
            depositSlot() ?: return@tickHandler

            val slot by requires { depositSlot() }
            val target by requires { currentTarget }
            requires { rotateToTarget }
            requires { mc.gui.screen() !is AbstractContainerScreen<*> }

            val hit = lookAt(Priority.IMPORTANT_FOR_USAGE_1, { target.second }) { going ->
                val (pos, _) = target
                val state = pos.state ?: return@lookAt null
                raytraceBlock(interactionRange.toDouble(), going, pos, state)
                    ?.takeIf { it.type == HitResult.Type.BLOCK && it.blockPos == pos }
            }

            deposit(slot, hit)
            currentTarget = null
        } else {
            val slot = depositSlot() ?: return@tickHandler
            val hit = mc.hitResult as? BlockHitResult ?: return@tickHandler
            val state = hit.blockPos.state ?: return@tickHandler
            if (state.block !in validStorageBlocks) {
                return@tickHandler
            }

            deposit(slot, hit)
        }

        cooldownLeft = interactionDelay
    }

    private fun deposit(slot: HotbarItemSlot, hit: BlockHitResult) {
        SilentHotbar.selectSlotSilently(this, slot, 1)
        clickStorage(hit)
        swingMode.attackSwing()
    }

}
