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
import net.ccbluex.liquidbounce.utils.math.geometry.Line

/**
 * Holds the look of [ScaffoldBridgingGodBridge] and sneaks at the edges. On a straight row that look is 45 degrees off
 * straight back, so the player walks the row with two keys: sneaking, diagonal keys move about 40% faster than one
 * (MC-271065, kept on purpose since 25w04a).
 */
object ScaffoldBridgingNinja : ScaffoldBridging("Ninja", "Sneak") {

    override val shouldHoldLook: Boolean
        get() = true

    override fun look(): Rotation? = ScaffoldBridgingGodBridge.look()

    override fun wantedLine(row: Line): Line = ScaffoldBridgingGodBridge.wantedLine(row)

}
