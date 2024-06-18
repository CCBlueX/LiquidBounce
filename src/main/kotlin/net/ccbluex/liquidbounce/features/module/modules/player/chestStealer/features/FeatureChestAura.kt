/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015-2024 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.player.chestStealer.features

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.SimulatedTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.modules.player.chestStealer.ModuleChestStealer
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.raytraceBlock
import net.ccbluex.liquidbounce.utils.block.getCenterDistanceSquared
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.block.searchBlocksInCuboid
import net.ccbluex.liquidbounce.utils.entity.eyes
import net.ccbluex.liquidbounce.utils.entity.getNearestPoint
import net.ccbluex.liquidbounce.utils.inventory.findBlocksEndingWith
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box

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
        if (it > interactionRange) {
            interactionRange
        } else {
            it
        }
    }
    private val interactionDelay by int("Delay", 5, 1..80, "ticks")
    private val shouldDisplayVisualSwing by boolean("VisualSwing", true)

    // Sub-configurable for managing the await container settings
    private object AwaitContainerSettings : ToggleableConfigurable(this, "AwaitContainer", true) {
        val retryTimeout by int("Timeout", 10, 1..80, "ticks")
        val maxInteractionRetries by int("MaxRetries", 4, 1..10)
    }

    init {
        tree(AwaitContainerSettings)
    }

    // Rotation configuration settings
    private val rotationConfigurable = tree(RotationsConfigurable(this))

    // The block position currently being interacted with
    private var currentTargetBlock: BlockPos? = null
    val interactedBlocksSet = hashSetOf<BlockPos>()

    // Counter for the number of tries performed to interact with a block
    private var interactionAttempts = 0

    // Set of block names that are considered as storage blocks
    private val validStorageBlocks = findBlocksEndingWith("CHEST", "SHULKER_BOX", "BARREL").toHashSet()

    // Event handler responsible for updating the target block
    val simulatedTickHandler = handler<SimulatedTickEvent> {
        val searchRadius = interactionRange + 1
        val searchRadiusSquared = searchRadius * searchRadius
        val playerEyesPosition = player.eyes

        // Select blocks for processing within the search radius
        val nearbyStorageBlocks = searchBlocksInCuboid(searchRadius, playerEyesPosition) { pos, state ->
            validStorageBlocks.contains(state.block) && pos !in interactedBlocksSet && getNearestPoint(
                playerEyesPosition,
                Box.enclosing(pos, pos.add(1, 1, 1))
            ).squaredDistanceTo(playerEyesPosition) <= searchRadiusSquared
        }.sortedBy { it.first.getCenterDistanceSquared() }

        var nextTargetBlock: BlockPos? = null

        // Find the next block to interact with
        for ((blockPos, state) in nearbyStorageBlocks) {
            val (rotation, _) = raytraceBlock(
                player.eyes,
                blockPos,
                state,
                range = interactionRange.toDouble(),
                wallsRange = wallInteractionRange.toDouble()
            ) ?: continue

            // Update the player rotation to aim at the new target
            RotationManager.aimAt(
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

    // Task that repeats to interact with the target block
    val interactionRepeatableTask = repeatable { event ->
        if (mc.currentScreen is GenericContainerScreen) {
            // Do not proceed if a screen is open which implies player might be in a GUI
            return@repeatable
        }

        val targetBlockPos = currentTargetBlock ?: return@repeatable
        val currentPlayerRotation = RotationManager.serverRotation

        // Trace a ray from the player to the target block position
        val rayTraceResult = raytraceBlock(
            interactionRange.toDouble(),
            currentPlayerRotation,
            targetBlockPos,
            targetBlockPos.getState() ?: return@repeatable
        )

        // Verify if the block is hit and is the correct target
        if (rayTraceResult?.type != HitResult.Type.BLOCK || rayTraceResult.blockPos != targetBlockPos) {
            return@repeatable
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
                waitConditional(AwaitContainerSettings.retryTimeout) {
                    if (mc.currentScreen is GenericContainerScreen) {
                        // Interaction was successful if the inventory screen is open
                        wasInteractionSuccessful = true
                        true
                    } else {
                        false
                    }
                }
            } else {
                interactedBlocksSet.add(targetBlockPos)
                currentTargetBlock = null
                wasInteractionSuccessful = true

                // Delay until next interaction
                waitTicks(interactionDelay)
            }

            // Update interacted block set and reset target if successful or exceeded retries
            if (wasInteractionSuccessful || interactionAttempts >= AwaitContainerSettings.maxInteractionRetries) {
                interactedBlocksSet.add(targetBlockPos)
                currentTargetBlock = null
            } else {
                interactionAttempts++
            }
        }
    }

}
