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
package net.ccbluex.liquidbounce.features.module.modules.player.cheststealer.features

import net.ccbluex.fastutil.enumSetOf
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.requires
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.player.cheststealer.ModuleChestStealer
import net.ccbluex.liquidbounce.utils.aiming.RotationTarget
import net.ccbluex.liquidbounce.utils.aiming.RotationsValueGroup
import net.ccbluex.liquidbounce.utils.aiming.lookAt
import net.ccbluex.liquidbounce.utils.aiming.utils.selectBlockTarget
import net.ccbluex.liquidbounce.utils.block.SwingMode
import net.ccbluex.liquidbounce.utils.block.anotherChestPartDirection
import net.ccbluex.liquidbounce.utils.block.searchBlocksInRangeSorted
import net.ccbluex.liquidbounce.utils.block.state
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.entity.shouldSwingHand
import net.ccbluex.liquidbounce.utils.inventory.findBlocksEndingWith
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.READ_FINAL_STATE
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.raytracing.raytraceBlock
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.ChestBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.HitResult
import java.util.function.BooleanSupplier

/**
 * ChestAura feature
 */
/**
 * ChestAura feature is responsible for automatically interacting with storage blocks (such as chests)
 * within a specified range and line of sight of the player.
 */
object FeatureChestAura : ToggleableValueGroup(ModuleChestStealer, "Aura", true) {

    // Configuration fields with appropriate names
    private val interactionRange by float("Range", 3F, 1F..6F)
    private val wallInteractionRange by float("WallRange", 0f, 0F..6F).onChange {
        // Ensure that wallInteractionRange does not exceed interactionRange
        minOf(interactionRange, it)
    }
    private val interactionDelay by int("Delay", 5, 1..80, "ticks")
    private val swingMode by enumChoice("SwingMode", SwingMode.DO_NOT_HIDE, SwingMode.INTERACT_CHOICES)

    private val notDuringCombat by boolean("NotDuringCombat", true)

    private val trackManualInteractions by boolean("TrackManualInteractions", true)

    private val pauseOn by multiEnumChoice("PauseOn", enumSetOf<PauseCondition>())

    @Suppress("unused")
    private enum class PauseCondition(override val tag: String) : Tagged, BooleanSupplier {
        COMBAT("Combat") {
            override fun getAsBoolean() = CombatManager.isInCombat
        },
        USING_ITEM("UsingItem"){
            override fun getAsBoolean() = player.isUsingItem
        };
    }

    // Sub-configurable for managing the await container settings
    private object AwaitContainerSettings : ToggleableValueGroup(this, "AwaitContainer", true) {
        val retryTimeout by int("Timeout", 10, 1..80, "ticks")
        val maxInteractionRetries by int("MaxRetries", 4, 1..10)
    }

    // Set of block names that are considered as storage blocks
    private val validStorageBlocks by blocks(
        "ValidStorageBlocks",
        findBlocksEndingWith("CHEST", "SHULKER_BOX", "BARREL", "FURNACE").apply {
            add(Blocks.BREWING_STAND)
            add(Blocks.DISPENSER)
            add(Blocks.HOPPER)
        }
    )

    init {
        tree(AwaitContainerSettings)
    }

    // Rotation configuration settings
    private val rotations = tree(RotationsValueGroup(this))

    // The block position currently being interacted with and the target that aims at it
    private var currentTarget: Pair<BlockPos, RotationTarget>? = null
    val interactedBlocksSet: Set<BlockPos>
        field = hashSetOf<BlockPos>()

    // Counter for the number of tries performed to interact with a block
    private var interactionAttempts = 0

    override val running: Boolean
        get() = super.running && pauseOn.none { it.asBoolean }

    override fun onDisabled() {
        interactedBlocksSet.clear()
        interactionAttempts = 0
        currentTarget = null
        awaitedContainer = null
        cooldownLeft = 0
        super.onDisabled()
    }

    // Event handler responsible for updating the target block
    @Suppress("unused")
    private val simulatedTickHandler = handler<RotationUpdateEvent> {
        fun isUnblockedChestOrNotChest(state: BlockState, pos: BlockPos): Boolean {
            if (state.block !is ChestBlock) return true
            if (ChestBlock.isChestBlockedAt(world, pos)) return false

            return !ChestBlock.isChestBlockedAt(world, pos.relative(state.anotherChestPartDirection() ?: return true))
        }

        if (notDuringCombat && CombatManager.isInCombat) {
            currentTarget = null
            return@handler
        }

        // Select blocks for processing within the search radius
        val nearbyStorageBlocks = player.eyePosition.searchBlocksInRangeSorted(interactionRange) { pos, state ->
            pos !in interactedBlocksSet && state.block in validStorageBlocks && isUnblockedChestOrNotChest(state, pos)
        }

        val nextTarget = selectBlockTarget(
            player.eyePosition,
            interactionRange,
            wallInteractionRange,
            nearbyStorageBlocks,
            rotations,
        )

        // If the current target has changed, reset the retries counter
        if (currentTarget?.first != nextTarget?.first) {
            interactionAttempts = 0
        }

        // Update the current target block
        currentTarget = nextTarget
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent>(READ_FINAL_STATE) { event ->
        if (trackManualInteractions && event.packet is ServerboundUseItemOnPacket && !event.isCancelled) {
            mc.execute {
                track(event.packet.hitResult.blockPos)
            }
        }
    }

    // The storage block interacted with last, while waiting for its container to open
    private var awaitedContainer: BlockPos? = null
    private var awaitTicksLeft = 0

    /** Ticks of [interactionDelay] left after the last interaction. */
    private var cooldownLeft = 0

    // Task that repeats to interact with the target block
    @Suppress("unused")
    private val interactionRepeatableTask = tickHandler {
        val containerOpen = mc.gui.screen() is AbstractContainerScreen<*>

        awaitedContainer?.let { pos ->
            when {
                // TODO: check if the inner type matches?
                containerOpen -> finish(pos)
                awaitTicksLeft > 0 -> {
                    awaitTicksLeft--
                    return@tickHandler
                }
                interactionAttempts >= AwaitContainerSettings.maxInteractionRetries -> finish(pos)
                else -> {
                    interactionAttempts++
                    awaitedContainer = null
                }
            }
        }

        // Do not proceed if a screen is open which implies player might be in a GUI
        if (containerOpen) {
            return@tickHandler
        }

        if (cooldownLeft > 0) {
            cooldownLeft--
            return@tickHandler
        }

        currentTarget ?: return@tickHandler
        val target by requires { currentTarget }
        requires { mc.gui.screen() !is AbstractContainerScreen<*> }

        val rayTraceResult = lookAt(Priority.IMPORTANT_FOR_USAGE_1, { target.second }) { going ->
            val (pos, _) = target
            val state = pos.state ?: return@lookAt null
            raytraceBlock(interactionRange.toDouble(), going, pos, state)
                ?.takeIf { it.type == HitResult.Type.BLOCK && it.blockPos == pos }
        }

        // Attempt to interact with the block
        val useItemOnResult = interaction.useItemOn(player, InteractionHand.MAIN_HAND, rayTraceResult)
        if (useItemOnResult !is InteractionResult.Success) {
            return@tickHandler
        }

        if (useItemOnResult.shouldSwingHand()) {
            swingMode.interactSwing(InteractionHand.MAIN_HAND)
        }

        if (AwaitContainerSettings.enabled) {
            awaitedContainer = rayTraceResult.blockPos
            awaitTicksLeft = AwaitContainerSettings.retryTimeout
        } else {
            finish(rayTraceResult.blockPos)
            cooldownLeft = interactionDelay
        }
    }

    /**
     * Done with the storage block at [pos], whether its container opened or it ran out of retries.
     */
    private fun finish(pos: BlockPos) {
        track(pos)
        currentTarget = null
        awaitedContainer = null
    }

    private fun track(blockPos: BlockPos) {
        fun BlockPos.recordAnotherChestPart(state: BlockState?) {
            interactedBlocksSet += relative(state.anotherChestPartDirection() ?: return)
        }

        interactedBlocksSet += blockPos
        blockPos.recordAnotherChestPart(blockPos.state)
    }

}
