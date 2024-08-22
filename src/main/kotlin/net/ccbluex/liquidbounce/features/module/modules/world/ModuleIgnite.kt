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
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.event.events.SimulatedTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationEngine
import net.ccbluex.liquidbounce.utils.aiming.RotationObserver
import net.ccbluex.liquidbounce.utils.aiming.tracking.RotationTracker
import net.ccbluex.liquidbounce.utils.aiming.utils.raycast
import net.ccbluex.liquidbounce.utils.block.doPlacement
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTargetFindingOptions
import net.ccbluex.liquidbounce.utils.block.targetfinding.CenterTargetPositionFactory
import net.ccbluex.liquidbounce.utils.block.targetfinding.findBestBlockPlacementTarget
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.ccbluex.liquidbounce.utils.entity.boxedDistanceTo
import net.ccbluex.liquidbounce.utils.inventory.Hotbar
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.block.Blocks
import net.minecraft.item.Items
import net.minecraft.util.Hand
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.Vec3i

/**
 * Ignite module
 *
 * Automatically sets targets around you on fire.
 */
object ModuleIgnite : Module("Ignite", Category.WORLD) {

    private val range by floatRange("Range", 3.0f..4.5f, 2f..6f)
    private val delay by int("Delay", 20, 0..400, "ticks")
    private val ignoreOpenInventory by boolean("IgnoreOpenInventory", true)

    private val targetTracker = tree(TargetTracker())

    private val rotationEngine = tree(RotationEngine(this))

    private val itemToTrapEnemy
        get() = Hotbar.findClosestItem(arrayOf(Items.LAVA_BUCKET, Items.FLINT_AND_STEEL))

    private val trapWorthyBlocks = arrayOf(Blocks.LAVA, Blocks.FIRE)

    private var hasToWait = false

    // override fun toggle() { hasToWait = false }

    override fun enable() {
        hasToWait = false
    }

    override fun disable() {
        hasToWait = false
    }

    @Suppress("unused")
    val rotationUpdateHandler = handler<SimulatedTickEvent> {
        if (hasToWait) {
            return@handler
        }

        targetTracker.validateLock { it.shouldBeAttacked() && it.boxedDistanceTo(player) in range }

        val slot = itemToTrapEnemy ?: return@handler

        for (target in targetTracker.enemies()) {
            if (target.boxedDistanceTo(player) !in range) {
                continue
            }

            val pos = target.blockPos
            val state = pos.getState() ?: continue

            if (state.block in trapWorthyBlocks) {
                continue
            }

            val options = BlockPlacementTargetFindingOptions(
                listOf(Vec3i(0, 0, 0)),
                slot.itemStack,
                CenterTargetPositionFactory,
                BlockPlacementTargetFindingOptions.PRIORITIZE_LEAST_BLOCK_DISTANCE,
                player.pos
            )

            val currentTarget = findBestBlockPlacementTarget(pos, options) ?: continue

            targetTracker.lock(target)

            RotationManager.aimAt(
                RotationTracker.withFixedAngleLine(rotationEngine, currentTarget.angleLine),
                // todo: implement inventory consideration
//                considerInventory = !ignoreOpenInventory,
                Priority.IMPORTANT_FOR_PLAYER_LIFE,
                this
            )

            return@handler
        }
    }

    @Suppress("unused")
    val placementHandler = repeatable {
        val target = targetTracker.lockedOnTarget ?: return@repeatable
        val raycast = raycast(RotationObserver.serverOrientation) ?: return@repeatable

        if (raycast.type != HitResult.Type.BLOCK || raycast.blockPos != target.blockPos.down()) {
            return@repeatable
        }

        val slot = itemToTrapEnemy ?: return@repeatable

        CombatManager.pauseCombatForAtLeast(1)

        SilentHotbar.selectSlotSilently(this, slot.hotbarSlotForServer, 1)

        doPlacement(raycast, Hand.MAIN_HAND)

        hasToWait = true

        targetTracker.cleanup()

        waitTicks(delay)

        hasToWait = false
    }
}
