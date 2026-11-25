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

import net.ccbluex.liquidbounce.utils.aiming.data.Rotation

/**
 * Holds the look straight back against the movement and steeply down, sneaking at the edges.
 */
object ScaffoldBridgingNinja : ScaffoldBridging("Ninja", "Sneak") {

    override val shouldHoldLook: Boolean
        get() = true

    override fun look(): Rotation? {
        val movement = movement() ?: return null
        // Along a diagonal the face is further away
        return Rotation(movement + 180f, if (movement % 90f == 0f) 80f else 75.6f)
    }

}
