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
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldClutchFeature
import net.ccbluex.liquidbounce.utils.block.searchBlocksInRangeSorted
import net.ccbluex.liquidbounce.utils.block.withoutShapeChanges
import net.ccbluex.liquidbounce.utils.entity.FallingPlayer
import net.ccbluex.liquidbounce.utils.entity.anyHorizontal
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FIRST_PRIORITY
import net.minecraft.world.phys.Vec3

/**
 * Clutch module
 *
 * Catches a deep fall, such as one after being knocked off an edge, by having [ModuleScaffold] build under it.
 */
object ModuleClutch : ClientModule("Clutch", ModuleCategories.WORLD) {

    /**
     * Falls that end before this are left alone. One into the void never ends.
     */
    private val minFallDistance by float("MinFallDistance", 5f, 2f..50f)

    // Further ahead than a fall of the largest MinFallDistance takes, and than any island below is worth waiting for
    private const val MAX_FALL_TICKS = 60

    private var path: List<Vec3>? = null

    /**
     * Where the coming ticks take the player on a fall that has to be caught, one position per tick.
     * [ModuleScaffold] builds with [ScaffoldClutchFeature] for as long as there is one.
     */
    val fall: List<Vec3>?
        get() = path.takeIf { running }

    /**
     * Whether Clutch looks after the player's falls right now. It stops only when it cannot catch one any more: it
     * has no blocks left, or the player falls with nothing in reach and nothing below to build against.
     */
    var guarding = false
        get() = field && running
        private set

    override fun onDisabled() {
        guarding = false
        if (path != null) {
            path = null
            release()
        }
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        guarding = false
        path = null
    }

    @Suppress("unused")
    private val fallHandler = handler<RotationUpdateEvent>(priority = FIRST_PRIORITY) {
        val wasFalling = path != null
        guarding = canCatch()
        path = if (guarding) predictFall() else null

        if (wasFalling && path == null) {
            release()
        }
    }

    private fun canCatch(): Boolean {
        if (player.isPassenger || player.isFallFlying || player.abilities.flying ||
            ModuleScaffold.enabled || ModuleScaffold.blockCount <= 0
        ) {
            return false
        }

        if (player.onGround()) {
            return true
        }

        val range = player.blockInteractionRange().toFloat()
        val inReach = player.eyePosition.searchBlocksInRangeSorted(range) { _, state -> !state.canBeReplaced() }
        return inReach.isNotEmpty() || withoutShapeChanges {
            FallingPlayer.fromPlayer(player).findCollision(MAX_FALL_TICKS) != null
        }
    }

    private fun predictFall(): List<Vec3>? {
        // On the ground only momentum takes the player where it does not walk itself
        val steered = player.input.keyPresses.anyHorizontal
        if (player.onGround() && (steered || player.deltaMovement.horizontalDistanceSqr() == 0.0)) {
            return null
        }

        // Blocks other modules only make up, such as AntiVoid's ghost blocks, would make the fall look like a landing
        return withoutShapeChanges { FallingPlayer.fromPlayer(player).fallPath(minFallDistance, MAX_FALL_TICKS) }
    }

    /**
     * Scaffold only cleans up when it is disabled, which it never was.
     */
    private fun release() {
        if (!ModuleScaffold.enabled) {
            ModuleScaffold.reset()
        }
    }

}
