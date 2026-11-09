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

package net.ccbluex.liquidbounce.utils.navigation.walk

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.CancelBlockBreakingEvent
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ScaffoldBlockItemSelection
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsValueGroup
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.utils.raytraceBlockRotation
import net.ccbluex.liquidbounce.utils.block.doBreak
import net.ccbluex.liquidbounce.utils.block.doPlacement
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockOffsetOptions
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTargetFindingOptions
import net.ccbluex.liquidbounce.utils.block.targetfinding.ClickableCenterTargetPositionFactory
import net.ccbluex.liquidbounce.utils.block.targetfinding.FaceHandlingOptions
import net.ccbluex.liquidbounce.utils.block.targetfinding.NearestRotationTargetPositionFactory
import net.ccbluex.liquidbounce.utils.block.targetfinding.PlacementPlan
import net.ccbluex.liquidbounce.utils.block.targetfinding.PlayerLocationOnPlacement
import net.ccbluex.liquidbounce.utils.block.targetfinding.findBestBlockPlacementTarget
import net.ccbluex.liquidbounce.utils.block.targetfinding.verifyClick
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.entity.interactBlock
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.findBestToolToMineBlock
import net.ccbluex.liquidbounce.utils.inventory.findClosestSlot
import net.ccbluex.liquidbounce.utils.item.getDestroySpeedWithEnchantment
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.raytracing.raytraceBlock
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.HitResult

/**
 * Mines, places and opens the blocks a walking path changes: aims in [RotationUpdateEvent], acts in [GameTickEvent]
 * once the server has the aim.
 */
@Suppress("TooManyFunctions")
class WalkActions(
    private val owner: EventListener,
    private val rotations: RotationsValueGroup,
) : EventListener {

    var breaking: BlockPos? = null
        private set

    var placing: BlockPos? = null
        private set

    var opening: BlockPos? = null
        private set

    /**
     * The aim of the current action, null while there is nothing to aim at.
     */
    var rotation: Rotation? = null
        private set

    private var placeReady = false
    private var placement: PlacementPlan? = null
    private var useCooldown = 0

    val isActive: Boolean
        get() = breaking != null || placing != null || opening != null

    fun mine(pos: BlockPos) {
        clearPlacing()
        clearOpening()
        if (breaking != pos) {
            stopMining()
            breaking = pos
        }
    }

    /**
     * Places a block at [pos] once [ready]; until then only aims.
     */
    fun place(pos: BlockPos, ready: Boolean) {
        stopMining()
        clearOpening()
        if (placing != pos) {
            placement = null
            placing = pos
        }
        placeReady = ready
    }

    /**
     * Opens the door, trapdoor or fence gate at [pos] by using it.
     */
    fun open(pos: BlockPos) {
        stopMining()
        clearPlacing()
        opening = pos
    }

    fun idle() {
        stopMining()
        clearPlacing()
        clearOpening()
    }

    private fun stopMining() {
        if (breaking == null) {
            return
        }

        // Cleared first, so the cancel handler lets the stop through.
        breaking = null
        rotation = null
        mc.gameMode?.takeIf { it.isDestroying }?.stopDestroyBlock()
        SilentHotbar.resetSlot(this)
    }

    private fun clearPlacing() {
        if (placing == null) {
            return
        }

        placing = null
        placement = null
        placeReady = false
        rotation = null
        SilentHotbar.resetSlot(this)
    }

    private fun clearOpening() {
        if (opening != null) {
            opening = null
            rotation = null
        }
    }

    @Suppress("unused")
    private val rotationUpdateHandler = handler<RotationUpdateEvent> {
        rotation = null
        placement = null

        val reach = player.blockInteractionRange()

        (breaking ?: opening)?.let { pos ->
            val state = world.getBlockState(pos)
            val aim = raytraceBlockRotation(player.eyePosition, pos, state, reach, reach) ?: return@handler
            aim(aim.rotation)
            return@handler
        }

        val pos = placing ?: return@handler
        val slot = placeableSlot() ?: return@handler
        // A pillar block goes where the feet are or were; any part of the top below is in reach, and its middle lies
        // in no particular direction, so the aim keeps to the way the player looks.
        val underFeet = pos.x == player.blockX && pos.z == player.blockZ && pos.y <= player.blockY
        val options = BlockPlacementTargetFindingOptions(
            BlockOffsetOptions.Default,
            FaceHandlingOptions(
                if (underFeet) NearestRotationTargetPositionFactory else ClickableCenterTargetPositionFactory,
            ),
            slot.itemStack,
            PlayerLocationOnPlacement(position = player.position()),
        )
        val target = findBestBlockPlacementTarget(pos, options) ?: return@handler

        placement = PlacementPlan(pos, target, slot)
        aim(target.rotation)
    }

    private fun aim(rotation: Rotation) {
        this.rotation = rotation
        RotationManager.setRotationTarget(
            rotation,
            valueGroup = rotations,
            priority = Priority.IMPORTANT_FOR_USAGE_1,
            provider = owner,
        )
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        if (useCooldown > 0) {
            useCooldown--
        }

        breaking?.let { pos ->
            breakTick(pos)
            return@handler
        }

        opening?.let { pos ->
            openTick(pos)
            return@handler
        }

        if (placeReady) {
            placement?.let(::placeTick)
        }
    }

    private fun breakTick(pos: BlockPos) {
        val state = world.getBlockState(pos)
        if (state.getCollisionShape(world, pos).isEmpty) {
            return
        }

        Slots.Hotbar.findBestToolToMineBlock(state)
            ?.takeIf { it.itemStack.getDestroySpeedWithEnchantment(state) > 1f }
            ?.let { SilentHotbar.selectSlotSilently(this, it, SLOT_RESET_TICKS) }

        // The action goes out before this tick's rotation, so it has to match the rotation the server has.
        val hit = raytraceBlock(
            player.blockInteractionRange() + 1.0,
            RotationManager.serverRotation,
            pos,
            state,
        ) ?: return

        if (hit.type == HitResult.Type.BLOCK && hit.blockPos == pos) {
            doBreak(hit)
        }
    }

    private fun openTick(pos: BlockPos) {
        val state = world.getBlockState(pos)
        if (useCooldown > 0 || state.getOptionalValue(BlockStateProperties.OPEN).orElse(true)) {
            return
        }

        val hit = raytraceBlock(player.blockInteractionRange(), RotationManager.serverRotation, pos, state) ?: return
        if (hit.type == HitResult.Type.BLOCK && hit.blockPos == pos) {
            interactBlock(hit)
            // The server gets time to answer before another use, which would close the block again.
            useCooldown = USE_COOLDOWN_TICKS
        }
    }

    private fun placeTick(plan: PlacementPlan) {
        val rotation = RotationManager.serverRotation
        val hit = plan.placementTarget.verifyClick(rotation) ?: return

        if (!SilentHotbar.selectSlotSilently(this, plan.hotbarItemSlot, SLOT_RESET_TICKS)) {
            return
        }

        doPlacement(hit, rotation, hand = plan.hotbarItemSlot.useHand)
    }

    private fun placeableSlot(): HotbarItemSlot? {
        val slots = Slots.OffhandWithHotbar
        return slots.findClosestSlot { stack ->
            ScaffoldBlockItemSelection.isValidBlock(stack) && !ScaffoldBlockItemSelection.isBlockUnfavourable(stack)
        } ?: slots.findClosestSlot { stack -> ScaffoldBlockItemSelection.isValidBlock(stack) }
    }

    @Suppress("unused")
    private val cancelBlockBreakingHandler = handler<CancelBlockBreakingEvent> { event ->
        if (breaking != null) {
            event.cancelEvent()
        }
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        breaking = null
        placing = null
        opening = null
        placement = null
        placeReady = false
        rotation = null
    }

    override fun parent() = owner

    private companion object {
        const val SLOT_RESET_TICKS = 2
        const val USE_COOLDOWN_TICKS = 10
    }

}
