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
package net.ccbluex.liquidbounce.utils.collection

import it.unimi.dsi.fastutil.objects.AbstractObjectList

/**
 * Read-only list **view** that interleaves [separator] between the elements of [source],
 * i.e. iterating yields `source[0], separator, source[1], separator, source[2], ...`.
 *
 * Changes to the original list are reflected in this view.
 */
class JoinedList<T>(
    val source: List<T>,
    val separator: T,
) : AbstractObjectList<T>(), RandomAccess {

    override val size: Int
        get() = if (source.isEmpty()) 0 else (source.size shl 1) - 1

    override fun get(index: Int): T {
        ensureRestrictedIndex(index)
        return if (index and 1 == 0) source[index shr 1] else separator
    }

}
