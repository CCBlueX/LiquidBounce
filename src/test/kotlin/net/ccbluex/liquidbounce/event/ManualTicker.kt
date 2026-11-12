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
package net.ccbluex.liquidbounce.event

import it.unimi.dsi.fastutil.objects.ReferenceArrayList
import java.util.function.BooleanSupplier

/**
 * A [Ticker] advanced by hand, put into a coroutine context in place of [CoroutineTicker].
 */
internal class ManualTicker : Ticker {
    private val runningList = ReferenceArrayList<BooleanSupplier>()
    private val pendingList = ReferenceArrayList<BooleanSupplier>()

    val taskCount: Int
        get() = runningList.size + pendingList.size

    override fun register(task: BooleanSupplier) {
        pendingList.add(task)
    }

    override fun tick() {
        runningList.addAll(pendingList)
        pendingList.clear()
        runningList.removeIf { it.asBoolean }
    }

    fun tick(times: Int) = repeat(times) { tick() }
}
