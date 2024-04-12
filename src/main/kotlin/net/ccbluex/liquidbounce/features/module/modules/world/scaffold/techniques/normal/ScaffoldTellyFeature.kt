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
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.normal

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PlayerAfterJumpEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.ScaffoldNormalTechnique
import net.ccbluex.liquidbounce.utils.entity.getMovementDirectionOfInput
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.sqrtSpeed
import net.ccbluex.liquidbounce.utils.entity.strafe
import net.ccbluex.liquidbounce.utils.kotlin.random
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import kotlin.math.round

/**
 * Telly feature
 *
 * This is based on the telly technique and means that the player will jump when moving.
 * That allows for a faster scaffold.
 * Depending on the SameY setting, we might scaffold upwards.
 *
 * @see ModuleScaffold
 */
object ScaffoldTellyFeature : ToggleableConfigurable(ScaffoldNormalTechnique, "Telly", false) {

    object StrafeTelly : ToggleableConfigurable(ScaffoldTellyFeature, "Strafe", false) {

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

        val afterJumpHandler = handler<PlayerAfterJumpEvent> {
            val dirInput = DirectionalInput(player.input)

            // Taken from GodBridge feature
            val direction = getMovementDirectionOfInput(player.yaw, dirInput) + 180

            // Round to 45°-steps (NORTH, NORTH_EAST, etc.)
            val movingYaw = round(direction / 45) * 45
            val isMovingStraight = movingYaw % 90 == 0f

            player.strafe(speed = (if (isMovingStraight) straightSpeed else diagonalSpeed).random())
            ModuleDebug.debugParameter(ModuleScaffold, "Telly-Speed", "%.2f".format(player.sqrtSpeed))
        }

    }

    init {
        tree(StrafeTelly)
    }

    private val movementInputHandler = handler<MovementInputEvent> {
        if (player.moving && ModuleScaffold.hasBlockToBePlaced()) {
            it.jumping = true
        }
    }

}
