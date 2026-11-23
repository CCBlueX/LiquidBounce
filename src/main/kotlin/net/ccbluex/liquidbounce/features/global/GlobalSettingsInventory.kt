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

package net.ccbluex.liquidbounce.features.global

import net.ccbluex.liquidbounce.config.types.group.ValueGroup

/** How the inventory manager deals with a server that answers late, or puts clicked items back. */
object GlobalSettingsInventory : ValueGroup(name = "Inventory") {

    /**
     * How long the server gets to answer: to open a container, to put a clicked item back, or to take that back
     * again. Inventory clicks wait this long wherever an answer may still change what they act on.
     */
    val responseTime by int("ResponseTime", 400, 50..2000, "ms")

    /**
     * How often the server may put an item back before it is left alone until the next world.
     */
    val lockAfterRejections by int("LockAfterRejections", 3, 1..10)

}
