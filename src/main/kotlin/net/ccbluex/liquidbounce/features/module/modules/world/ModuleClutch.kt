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
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.ScaffoldClutchTechnique
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

    // Further ahead than a fall of the largest MinFallDistance takes
    private const val MAX_FALL_TICKS = 60

    private var path: List<Vec3>? = null

    /**
     * Where the coming ticks take the player on a fall that has to be caught, one position per tick.
     * [ModuleScaffold] runs with [ScaffoldClutchTechnique] for as long as there is one.
     */
    val fall: List<Vec3>?
        get() = path.takeIf { running }

    override fun onDisabled() {
        if (path != null) {
            path = null
            release()
        }
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        path = null
    }

    @Suppress("unused")
    private val fallHandler = handler<RotationUpdateEvent>(priority = FIRST_PRIORITY) {
        val wasFalling = path != null
        // Predicted with scaffold off: its safe walk would keep the simulated player from the very fall
        path = null
        path = predictFall()

        if (wasFalling && path == null) {
            release()
        }
    }

    private fun predictFall(): List<Vec3>? {
        val carried = when {
            player.isPassenger || player.isFallFlying || player.abilities.flying -> false
            // On the ground only momentum takes the player where it does not walk itself
            player.onGround() ->
                !player.input.keyPresses.anyHorizontal && player.deltaMovement.horizontalDistanceSqr() > 0.0
            else -> true
        }

        if (!carried || ModuleScaffold.enabled || ModuleScaffold.blockCount <= 0) {
            return null
        }

        return FallingPlayer.fromPlayer(player).fallPath(minFallDistance, MAX_FALL_TICKS)
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
