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
package net.ccbluex.liquidbounce.utils.client

class Chronometer(private var lastUpdate: Long = 0) {

    val elapsed: Long
        get() = System.currentTimeMillis() - lastUpdate

    fun elapsedUntil(time: Long) = time - lastUpdate

    fun hasElapsed(ms: Long = 0) = lastUpdate + ms < System.currentTimeMillis()

    fun hasAtLeastElapsed(ms: Long = 0) = lastUpdate + ms <= System.currentTimeMillis()

    @JvmOverloads
    fun reset(lastUpdate: Long = System.currentTimeMillis()) {
        this.lastUpdate = lastUpdate
    }

    fun waitForAtLeast(ms: Long) {
        this.lastUpdate = this.lastUpdate.coerceAtLeast(System.currentTimeMillis() + ms)
    }

}
