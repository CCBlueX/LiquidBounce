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
package net.ccbluex.liquidbounce.utils.aiming.features

import net.ccbluex.liquidbounce.config.types.NamedChoice

/**
 * Corrects movement when aiming away from client-side view direction.
 */
enum class MovementCorrection(override val choiceName: String) : NamedChoice {

    /**
     * No movement correction is applied. This feels the best, as it does not
     * change the movement of the player and also not affects Sprinting.
     * However, this can be detected by anti-cheats.
     */
    OFF("Off"),

    /**
     * Corrects movement by changing the yaw when updating the movement.
     */
    STRICT("Strict"),

    /**
     * Correct movement by changing the yaw when updating the movement,
     * but also tweaks the keyboard input to not aggressively change the
     * players walk direction.
     */
    SILENT("Silent"),

    /**
     * Corrects movement by changing the actual look direction of the player.
     */
    CHANGE_LOOK("ChangeLook")

}
