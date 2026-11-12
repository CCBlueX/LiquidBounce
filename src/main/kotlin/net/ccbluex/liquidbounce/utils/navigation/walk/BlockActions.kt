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
import net.ccbluex.liquidbounce.utils.aiming.Readiness
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
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult

/**
 * Mines, places and opens the blocks a walking path changes, one at a time: aims in [RotationUpdateEvent], acts in
 * [GameTickEvent] once the server has the aim.
 */
class BlockActions(
    private val owner: EventListener,
    private val rotations: RotationsValueGroup,
) : EventListener {

    private var action: Action? = null

    /** The aim of the current action, null while there is nothing to aim at. */
    var rotation: Rotation? = null
        private set

    private var useCooldown = 0
    private var ready: BlockHitResult? = null

    val isActive: Boolean
        get() = action != null

    fun mine(pos: BlockPos) {
        if ((action as? Action.Mine)?.pos != pos) {
            idle()
            action = Action.Mine(pos)
        }
    }

    /** Places a block at [pos] once [ready]; until then only aims. */
    fun place(pos: BlockPos, ready: Boolean) {
        val placing = action as? Action.Place
        if (placing?.pos == pos) {
            placing.ready = ready
            return
        }

        // From one block to the next, the aim and the slot stay.
        if (placing == null) {
            idle()
        }
        action = Action.Place(pos, ready)
    }

    /** Uses the door, trapdoor or fence gate at [pos]. */
    fun open(pos: BlockPos) {
        if (action !is Action.Open) {
            idle()
        }
        action = Action.Open(pos)
    }

    fun idle() {
        val stopped = action ?: return
        // Cleared first, so the cancel handler lets the stop through.
        action = null
        rotation = null
        if (stopped is Action.Mine) {
            mc.gameMode?.takeIf { it.isDestroying }?.stopDestroyBlock()
        }
        SilentHotbar.resetSlot(this)
    }

    @Suppress("unused")
    private val rotationUpdateHandler = handler<RotationUpdateEvent> {
        rotation = null
        ready = null
        val action = action ?: return@handler
        val aim = when (action) {
            is Action.Place -> {
                action.plan = placementPlan(action.pos)
                action.plan?.placementTarget?.rotation
            }

            else -> {
                val state = world.getBlockState(action.pos)
                val reach = player.blockInteractionRange()
                raytraceBlockRotation(player.eyePosition, action.pos, state, reach, reach)?.rotation
            }
        } ?: return@handler

        rotation = aim
        RotationManager.setRotationTarget(
            rotations.toRotationTarget(aim, considerInventory = true, whenReady = readiness(action)),
            Priority.IMPORTANT_FOR_USAGE_1,
            owner,
        )
    }

    /** Ready once the rotation going out hits what [action] works on, the way vanilla acts on its look. */
    private fun readiness(action: Action) = Readiness({ rotation ->
        when (action) {
            is Action.Place -> action.plan?.placementTarget?.verifyClick(rotation)
            else -> {
                // Mining may go on a little past the reach the break started from
                val reach = player.blockInteractionRange() + if (action is Action.Mine) 1.0 else 0.0
                raytraceBlock(reach, rotation, action.pos, world.getBlockState(action.pos))
                    ?.takeIf { it.type == HitResult.Type.BLOCK && it.blockPos == action.pos }
            }
        }
    }) { ready = it }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        if (useCooldown > 0) {
            useCooldown--
        }

        val hit = ready ?: return@handler
        ready = null
        when (val action = action) {
            is Action.Mine -> breakTick(action.pos, hit)
            is Action.Open -> openTick(action.pos, hit)
            is Action.Place -> action.plan?.takeIf { action.ready }?.let { placeTick(it, hit) }
            null -> Unit
        }
    }

    private fun breakTick(pos: BlockPos, hit: BlockHitResult) {
        val state = world.getBlockState(pos)
        if (state.getCollisionShape(world, pos).isEmpty) {
            return
        }

        Slots.Hotbar.findBestToolToMineBlock(state)
            ?.takeIf { it.itemStack.getDestroySpeedWithEnchantment(state) > 1f }
            ?.let { SilentHotbar.selectSlotSilently(this, it, SLOT_RESET_TICKS) }

        doBreak(hit)
    }

    private fun openTick(pos: BlockPos, hit: BlockHitResult) {
        val state = world.getBlockState(pos)
        if (useCooldown > 0 || state.getOptionalValue(BlockStateProperties.OPEN).orElse(true)) {
            return
        }

        interactBlock(hit)
        // The server gets time to answer before another use, which would close the block again.
        useCooldown = USE_COOLDOWN_TICKS
    }

    private fun placeTick(plan: PlacementPlan, hit: BlockHitResult) {
        val rotation = RotationManager.currentRotation ?: return
        if (SilentHotbar.selectSlotSilently(this, plan.hotbarItemSlot, SLOT_RESET_TICKS)) {
            doPlacement(hit, rotation, hand = plan.hotbarItemSlot.useHand)
        }
    }

    @Suppress("unused")
    private val cancelBlockBreakingHandler = handler<CancelBlockBreakingEvent> { event ->
        if (action is Action.Mine) {
            event.cancelEvent()
        }
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        action = null
        rotation = null
    }

    override fun parent() = owner

    private sealed class Action(val pos: BlockPos) {
        class Mine(pos: BlockPos) : Action(pos)
        class Open(pos: BlockPos) : Action(pos)
        class Place(pos: BlockPos, var ready: Boolean) : Action(pos) {
            var plan: PlacementPlan? = null
        }
    }

    private companion object {
        const val SLOT_RESET_TICKS = 2
        const val USE_COOLDOWN_TICKS = 10
    }

}

private fun placementPlan(pos: BlockPos): PlacementPlan? {
    val slot = placeableSlot() ?: return null
    // The top of the block below a pillar lies in no particular direction; the aim keeps to the way the player looks.
    val underFeet = pos.x == player.blockX && pos.z == player.blockZ && pos.y <= player.blockY
    val options = BlockPlacementTargetFindingOptions(
        BlockOffsetOptions.Default,
        FaceHandlingOptions(
            if (underFeet) NearestRotationTargetPositionFactory else ClickableCenterTargetPositionFactory,
        ),
        slot.itemStack,
        PlayerLocationOnPlacement(position = player.position()),
    )
    val target = findBestBlockPlacementTarget(pos, options) ?: return null
    return PlacementPlan(pos, target, slot)
}

private fun placeableSlot(): HotbarItemSlot? {
    val slots = Slots.OffhandWithHotbar
    return slots.findClosestSlot { stack ->
        ScaffoldBlockItemSelection.isValidBlock(stack) && !ScaffoldBlockItemSelection.isBlockUnfavourable(stack)
    } ?: slots.findClosestSlot { stack -> ScaffoldBlockItemSelection.isValidBlock(stack) }
}
