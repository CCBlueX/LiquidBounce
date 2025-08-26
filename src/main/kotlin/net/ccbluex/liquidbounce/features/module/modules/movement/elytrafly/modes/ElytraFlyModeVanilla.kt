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
package net.ccbluex.liquidbounce.features.module.modules.movement.elytrafly.modes

import net.ccbluex.liquidbounce.features.module.modules.movement.elytrafly.ModuleElytraFly
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.withStrafe

internal object ElytraFlyModeVanilla : ElytraFlyMode("Vanilla") {

    override fun onTick() {
        if (player.moving) {
            player.velocity = player.velocity.withStrafe(speed = ModuleElytraFly.Speed.horizontal.toDouble())
        }

        player.velocity.y = when {
            mc.options.jumpKey.isPressed -> ModuleElytraFly.Speed.vertical.toDouble()
            mc.options.sneakKey.isPressed -> -ModuleElytraFly.Speed.vertical.toDouble()
            else -> return
        }
    }

}
