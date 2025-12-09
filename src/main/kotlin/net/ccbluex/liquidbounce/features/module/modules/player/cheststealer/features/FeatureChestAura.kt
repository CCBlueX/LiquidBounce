/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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
 *
 *
 */
package net.ccbluex.liquidbounce.features.module.modules.player.cheststealer.features

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.waitTicks
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickConditional
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.player.cheststealer.ModuleChestStealer
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.utils.raytraceBlock
import net.ccbluex.liquidbounce.utils.aiming.utils.raytraceBlockRotation
import net.ccbluex.liquidbounce.utils.block.anotherChestPartDirection
import net.ccbluex.liquidbounce.utils.block.getCenterDistanceSquaredEyes
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.block.searchBlocksInCuboid
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.inventory.findBlocksEndingWith
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.READ_FINAL_STATE
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.kotlin.emptyEnumSet
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.ChestBlock
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import java.util.function.BooleanSupplier

/**
 * ChestAura feature
 */
/**
 * ChestAura feature is responsible for automatically interacting with storage blocks (such as chests)
 * within a specified range and line of sight of the player.
 */
object FeatureChestAura : ToggleableConfigurable(ModuleChestStealer, "Aura", true) {

    // Configuration fields with appropriate names
    private val interactionRange by float("Range", 3F, 1F..6F)
    private val wallInteractionRange by float("WallRange", 0f, 0F..6F).onChange {
        // Ensure that wallInteractionRange does not exceed interactionRange
        minOf(interactionRange, it)
    }
    private val interactionDelay by int("Delay", 5, 1..80, "ticks")
    private val shouldDisplayVisualSwing by boolean("VisualSwing", true)

    private val notDuringCombat by boolean("NotDuringCombat", true)

    private val trackManualInteractions by boolean("TrackManualInteractions", true)

    private val pauseOn by multiEnumChoice("PauseOn", emptyEnumSet<PauseCondition>())

    @Suppress("unused")
    private enum class PauseCondition(override val choiceName: String) : NamedChoice, BooleanSupplier {
        COMBAT("Combat") {
            override fun getAsBoolean() = CombatManager.isInCombat
        },
        USING_ITEM("UsingItem"){
            override fun getAsBoolean() = player.isUsingItem
        };
    }

    // Sub-configurable for managing the await container settings
    private object AwaitContainerSettings : ToggleableConfigurable(this, "AwaitContainer", true) {
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
            add(Blocks.DECORATED_POT)
        }
    )

    init {
        tree(AwaitContainerSettings)
    }

    // Rotation configuration settings
    private val rotationConfigurable = tree(RotationsConfigurable(this))

    // The block position currently being interacted with
    private var currentTargetBlock: BlockPos? = null
    val interactedBlocksSet: Set<BlockPos>
        field = hashSetOf<BlockPos>()

    // Counter for the number of tries performed to interact with a block
    private var interactionAttempts = 0

    override val running: Boolean
        get() = super.running && pauseOn.none { it.asBoolean }

    override fun onDisabled() {
        interactedBlocksSet.clear()
        interactionAttempts = 0
        currentTargetBlock = null
        super.onDisabled()
    }

    // Event handler responsible for updating the target block
    @Suppress("unused")
    private val simulatedTickHandler = handler<RotationUpdateEvent> {
        fun isUnblockedChestOrNotChest(state: BlockState, pos: BlockPos): Boolean {
            if (state.block !is ChestBlock) return true
            if (ChestBlock.isChestBlocked(world, pos)) return false

            return !ChestBlock.isChestBlocked(world, pos.offset(state.anotherChestPartDirection() ?: return true))
        }

        val searchRadius = interactionRange + 1
        val searchRadiusSquared = searchRadius * searchRadius
        val playerEyesPosition = player.eyePos

        if (notDuringCombat && CombatManager.isInCombat) {
            currentTargetBlock = null
            return@handler
        }

        // Select blocks for processing within the search radius
        val nearbyStorageBlocks = playerEyesPosition.searchBlocksInCuboid(searchRadius) { pos, state ->
            pos !in interactedBlocksSet && state.block in validStorageBlocks && isUnblockedChestOrNotChest(state, pos)
        }.sortedBy { it.first.getCenterDistanceSquaredEyes() }

        var nextTargetBlock: BlockPos? = null

        // Find the next block to interact with
        for ((blockPos, state) in nearbyStorageBlocks) {
            val (rotation, _) = raytraceBlockRotation(
                player.eyePos,
                blockPos,
                state,
                range = interactionRange.toDouble(),
                wallsRange = wallInteractionRange.toDouble()
            ) ?: continue

            // Update the player rotation to aim at the new target
            RotationManager.setRotationTarget(
                rotation,
                considerInventory = true,
                configurable = rotationConfigurable,
                priority = Priority.IMPORTANT_FOR_USAGE_1,
                ModuleChestStealer
            )

            nextTargetBlock = blockPos
            break
        }

        // If the current target has changed, reset the retries counter
        if (currentTargetBlock != nextTargetBlock) {
            interactionAttempts = 0
        }

        // Update the current target block
        currentTargetBlock = nextTargetBlock
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent>(READ_FINAL_STATE) { event ->
        if (trackManualInteractions && event.packet is PlayerInteractBlockC2SPacket && !event.isCancelled) {
            mc.execute {
                track(event.packet.blockHitResult.blockPos)
            }
        }
    }

    // Task that repeats to interact with the target block
    @Suppress("unused")
    private val interactionRepeatableTask = tickHandler {
        if (mc.currentScreen is HandledScreen<*>) {
            // Do not proceed if a screen is open which implies player might be in a GUI
            return@tickHandler
        }

        val targetBlockPos = currentTargetBlock ?: return@tickHandler
        val currentPlayerRotation = RotationManager.serverRotation

        // Trace a ray from the player to the target block position
        val rayTraceResult = raytraceBlock(
            interactionRange.toDouble(),
            currentPlayerRotation,
            targetBlockPos,
            targetBlockPos.getState() ?: return@tickHandler
        )

        // Verify if the block is hit and is the correct target
        if (rayTraceResult?.type != HitResult.Type.BLOCK || rayTraceResult.blockPos != targetBlockPos) {
            return@tickHandler
        }

        // Attempt to interact with the block
        if (interaction.interactBlock(player, Hand.MAIN_HAND, rayTraceResult) == ActionResult.SUCCESS) {
            // Swing hand visually if the setting is enabled, else send packet for the action
            if (shouldDisplayVisualSwing) {
                player.swingHand(Hand.MAIN_HAND)
            } else {
                network.sendPacket(HandSwingC2SPacket(Hand.MAIN_HAND))
            }

            var wasInteractionSuccessful = false

            if (AwaitContainerSettings.enabled) {
                tickConditional(AwaitContainerSettings.retryTimeout) {
                    val currentScreen = mc.currentScreen
                    if (currentScreen is HandledScreen<*>) { // TODO: check if the inner type matches?
                        // Interaction was successful if the inventory screen is open
                        wasInteractionSuccessful = true
                        true
                    } else {
                        false
                    }
                }
            } else {
                track(targetBlockPos)
                currentTargetBlock = null
                wasInteractionSuccessful = true

                // Delay until next interaction
                waitTicks(interactionDelay)
            }

            // Update interacted block set and reset target if successful or exceeded retries
            if (wasInteractionSuccessful || interactionAttempts >= AwaitContainerSettings.maxInteractionRetries) {
                track(targetBlockPos)
                currentTargetBlock = null
            } else {
                interactionAttempts++
            }
        }
    }

    private fun track(blockPos: BlockPos) {
        fun BlockPos.recordAnotherChestPart(state: BlockState?) {
            interactedBlocksSet += offset(state.anotherChestPartDirection() ?: return)
        }

        interactedBlocksSet += blockPos
        blockPos.recordAnotherChestPart(blockPos.getState())
    }

}
