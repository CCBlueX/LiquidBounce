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

import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.PlayerMoveEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.movement.elytrafly.ModuleElytraFly
import net.ccbluex.liquidbounce.utils.entity.withStrafe

internal object ElytraFlyModeStatic : ElytraFlyMode("Static") {

    /**
     * Only runs the exploit while the player isn't moving.
     * This might save some durability points
     * while not moving as some anti-cheats just detect this exploit when you move.
     */
    val durabilityExploitNotWhileMove by boolean("DurabilityExploitNotWhileNoMove", false)

    /**
     * Allows you to add a glide effect when you're not moving.
     * This can prevent you from getting kicked for "flying is not enabled on this server" when you're not moving.
     */
    object Glide : ToggleableConfigurable(this, "Glide", false) {

        /**
         * How fast the static glide should be.
         */
        val verticalGlide by float("Vertical", 0.01f, 0f..1f)
        val horizontalGlide by float("Horizontal", 0f, 0f..1f)

    }

    init {
        tree(Glide)
    }

    @Suppress("unused")
    private val moveHandler = handler<PlayerMoveEvent> { event ->
        if (ModuleElytraFly.shouldNotOperate() || !player.isGliding) {
            return@handler
        }

        val speed = ModuleElytraFly.Speed.enabled
        val input = player.input.playerInput
        val isMoving = input.forward || input.backward || input.left || input.right
        if (speed && isMoving) {
            event.movement = event.movement.withStrafe(speed = ModuleElytraFly.Speed.horizontal.toDouble())
        } else {
            var glideX = 0.0
            var glideZ = 0.0
            if (Glide.running) {
                val normalized = event.movement.normalize()
                glideX = normalized.x * Glide.horizontalGlide.toDouble()
                glideZ = normalized.z * Glide.horizontalGlide.toDouble()
            }

            event.movement.x = glideX
            event.movement.z = glideZ
        }

        event.movement.y = when {
            mc.options.jumpKey.isPressed && speed -> ModuleElytraFly.Speed.vertical.toDouble()
            mc.options.sneakKey.isPressed && speed -> -ModuleElytraFly.Speed.vertical.toDouble()
            else -> if (Glide.running) -Glide.verticalGlide.toDouble() else 0.0
        }
    }

}
