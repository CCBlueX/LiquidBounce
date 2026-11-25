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
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.bridging

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleSafeWalk
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold.bridging
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.utils.RotationUtil.angleDifference
import net.ccbluex.liquidbounce.utils.entity.getMovementDirectionOfInput
import net.ccbluex.liquidbounce.utils.math.geometry.Line
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import kotlin.math.abs
import kotlin.math.round

/**
 * How the player bridges: the look scaffold holds, if any, and how an edge stops the player, [safeWalk] by default.
 */
sealed class ScaffoldBridging(name: String, safeWalk: String) : Mode(name) {

    final override val parent: ModeValueGroup<*>
        get() = bridging

    // An edge is only sneaked at while the view lines up, and jumped at once the jump has a block to come down on
    @Suppress("unused")
    private val safeWalkMode = modes<Mode>(this, "SafeWalk", { modes -> modes.indexOfFirst { it.name == safeWalk } }) {
        ModuleSafeWalk.safeWalkChoices(
            it,
            ModuleScaffold::shouldStopAtEdge,
            ModuleScaffold::isLiningUp,
            ModuleScaffold::canJumpAtEdge,
        ) { ModuleScaffold.blockCount }
    }

    /** Whether this bridging holds [look] rather than aiming at the face, even before it has one. */
    open val shouldHoldLook: Boolean
        get() = false

    /** The look to hold, null to aim at the face. */
    open fun look(): Rotation? = null

    /** The line the player walks and scaffold plans along: [row], through the middle of the blocks, by default. */
    open fun wantedLine(row: Line): Line = row

    /**
     * The 45 degree step of [ModuleScaffold.rawInput]'s movement. It stays the same block after block and only
     * follows the movement once that turned well into another step.
     */
    protected fun movement(): Float? {
        val input = ModuleScaffold.rawInput
        if (input != DirectionalInput.NONE) {
            val movement = player.getMovementDirectionOfInput(input)
            val held = step
            // The movement has to turn 10 degrees past halfway to the next 45 degree step before the look follows
            if (held == null || abs(angleDifference(movement, held)) > 45f / 2 + 10f) {
                step = round(movement / 45f) * 45f
            }
        }
        return step
    }

    companion object {

        /** The step [movement] holds, shared by every bridging; null before the player moved. */
        private var step: Float? = null

        fun reset() {
            step = null
            ScaffoldBridgingGodBridge.reset()
        }

    }

}
