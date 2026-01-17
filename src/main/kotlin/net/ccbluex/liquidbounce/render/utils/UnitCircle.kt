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

package net.ccbluex.liquidbounce.render.utils

import net.ccbluex.liquidbounce.utils.client.fastCos
import net.ccbluex.liquidbounce.utils.client.fastSin
import net.minecraft.util.Mth

object UnitCircle {

    const val CIRCLE_RES = 40

    @JvmField
    val POINTS = FloatArray((CIRCLE_RES + 1) * 2).apply {
        repeat(CIRCLE_RES + 1) {
            val theta = Mth.TWO_PI * it / CIRCLE_RES
            this[it * 2] = theta.fastCos()
            this[it * 2 + 1] = theta.fastSin()
        }
    }

    @JvmStatic
    inline fun forEach(radius: Float = 1F, consumer: (x: Float, y: Float) -> Unit) {
        repeat(CIRCLE_RES + 1) {
            val x = POINTS[it * 2] * radius
            val y = POINTS[it * 2 + 1] * radius
            consumer(x, y)
        }
    }

}
