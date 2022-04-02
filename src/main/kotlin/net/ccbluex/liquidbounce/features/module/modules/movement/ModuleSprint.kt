/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.entity.moving
import net.minecraft.entity.effect.StatusEffects

object ModuleSprint : Module("Sprint", Category.MOVEMENT) {

    private val omni by boolean("Omni", true)
    private val blindness by boolean("Blindless", true)
    private val food by boolean("Food", true)

    private val checkServerSide by boolean("CheckServerSide", false)
    private val checkServerSideGround by boolean("CheckServerSideOnlyGround", false)

    val repeatable = repeatable {
        if (!player.moving || player.isSneaking || blindness && player.hasStatusEffect(StatusEffects.BLINDNESS) ||
            food && !(player.hungerManager.foodLevel > 6.0f || player.abilities.allowFlying)
            || (checkServerSide && (player.isOnGround || !checkServerSideGround) && !omni)) {
            player.isSprinting = false
            return@repeatable
        }


        if (omni || player.input.movementForward >= 0.8f)
            player.isSprinting = true
    }
}
