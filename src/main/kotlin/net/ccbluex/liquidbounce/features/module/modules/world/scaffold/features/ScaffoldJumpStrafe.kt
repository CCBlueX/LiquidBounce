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
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.PlayerAfterJumpEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.entity.getMovementDirectionOfInput
import net.ccbluex.liquidbounce.utils.entity.sqrtSpeed
import net.ccbluex.liquidbounce.utils.entity.withStrafe
import net.ccbluex.liquidbounce.utils.kotlin.random
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import kotlin.math.round

object ScaffoldJumpStrafe : ToggleableConfigurable(ModuleScaffold, "StrafeOnJump", false) {

    /**
     * Allows to adjust the speed of the strafe.
     *
     * Since Hypixel likes to patch values we randomize it a bit.
     */
    private val straightSpeed by floatRange("StraightSpeed", 0.48f..0.49f, 0.1f..1f)

    /**
     * In case of Hypixel, we should be slower when moving diagonally because otherwise we place
     * blocks too fast and get flagged.
     */
    private val diagonalSpeed by floatRange("DiagonalSpeed", 0.48f..0.49f, 0.1f..1f)

    @Suppress("unused")
    private val afterJumpHandler = handler<PlayerAfterJumpEvent> {
        val dirInput = DirectionalInput(player.input)

        // Taken from GodBridge feature
        val direction = getMovementDirectionOfInput(player.yaw, dirInput) + 180

        // Round to 45°-steps (NORTH, NORTH_EAST, etc.)
        val movingYaw = round(direction / 45) * 45
        val isMovingStraight = movingYaw % 90 == 0f

        val speed = if (isMovingStraight) straightSpeed else diagonalSpeed
        player.velocity = player.velocity.withStrafe(speed = speed.random().toDouble())
        ModuleDebug.debugParameter(ModuleScaffold, "Telly-Speed", "%.2f".format(player.sqrtSpeed))
    }

}
