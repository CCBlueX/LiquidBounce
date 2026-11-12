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
package net.ccbluex.liquidbounce.features.module.modules.render.debug

import net.ccbluex.liquidbounce.features.misc.DebuggedOwner
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.minecraft.util.FormattedCharSequence
import java.util.concurrent.ConcurrentHashMap

/**
 * What one [DebuggedOwner] captured.
 *
 * @param root the owner at the top of the parent chain, which gets the card
 * @param label the chain below [root], null for [root] itself
 * @param path the whole chain, for the title filter and the debug log
 * @param global whether [root] is not a module
 */
@PublishedApi
internal class DebugSection(
    val root: DebuggedOwner,
    val label: String?,
    val path: String,
    val accepted: Boolean,
    val global: Boolean,
) {
    val parameters = ConcurrentHashMap<String, DebugParameter>()
    val geometry = ConcurrentHashMap<String, DebuggedGeometry>()
}

internal class DebugParameter(val name: String, override val order: Int, priority: DebugPriority) : OverviewItem {

    @Volatile
    override var priority = priority

    @Volatile
    var value: Any? = null
        private set

    @Volatile
    var time = 0L
        private set

    @Volatile
    var stamp = 0
        private set

    fun update(value: Any?, time: Long) {
        this.value = value
        this.time = time
        stamp++
    }

    // Render thread only, kept by DebugOverview
    var nameWidth = -1f
    var drawnName: FormattedCharSequence = FormattedCharSequence.EMPTY
    var drawnNameMax = -1f
    var drawnValue: FormattedCharSequence = FormattedCharSequence.EMPTY
    var drawnValueMax = -1f
    var formattedStamp = -1
    var valueColor = Color4b.WHITE

}
