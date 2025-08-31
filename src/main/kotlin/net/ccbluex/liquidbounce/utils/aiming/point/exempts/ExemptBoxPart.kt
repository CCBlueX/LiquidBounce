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

package net.ccbluex.liquidbounce.utils.aiming.point.exempts

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.minecraft.util.math.Vec3d

enum class ExemptBoxPart(override val choiceName: String) : NamedChoice, ExemptPoint {

    HEAD("Head") {
        override fun predicate(
            context: ExemptContext,
            point: Vec3d
        ): Boolean {
            val length = context.box.lengthY / entries.size
            return point.y <= context.box.maxY &&
                point.y > context.box.maxY - length
        }
    },
    BODY("Body") {
        override fun predicate(
            context: ExemptContext,
            point: Vec3d
        ): Boolean {
            val length = context.box.lengthY / entries.size
            return point.y <= context.box.maxY - length &&
                point.y >= context.box.minY + length
        }
    },
    FEET("Feet") {
        override fun predicate(
            context: ExemptContext,
            point: Vec3d
        ): Boolean {
            val length = context.box.lengthY / entries.size
            return  point.y >= context.box.minY &&
                point.y < context.box.minY + length
        }
    },;

    /**
     * Check if this part of the box is higher than the other by the index of the enum.
     * So please DO NOT change the order of the enum.
     */
    fun isHigherThan(other: ExemptBoxPart) = entries.indexOf(this) < entries.indexOf(other)

}
