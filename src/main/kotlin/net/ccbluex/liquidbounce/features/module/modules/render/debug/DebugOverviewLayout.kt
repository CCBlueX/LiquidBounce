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

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

internal interface OverviewItem {
    val priority: DebugPriority
    val order: Int
}

/**
 * @param label null for the root owner's own section
 */
internal class OverviewSection<T : OverviewItem>(val label: String?, val items: List<T>)

internal class OverviewCard<T : OverviewItem>(val title: String, val sections: List<OverviewSection<T>>)

internal data class OverviewArea(val left: Float, val top: Float, val right: Float, val bottom: Float)

internal sealed interface OverviewRow<out T> {
    data class Header(val title: String) : OverviewRow<Nothing>
    data class Subheader(val label: String) : OverviewRow<Nothing>
    data class Item<out T>(val item: T, val indented: Boolean) : OverviewRow<T>
}

internal data class PlacedCard<T>(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val rows: List<OverviewRow<T>>,
)

/**
 * @param bottom lowest card edge, below [OverviewArea.bottom] when the overview has to scroll
 */
internal class OverviewLayout<T>(val cards: List<PlacedCard<T>>, val bottom: Float)

/**
 * Places every card with all of its rows, in as few columns as keep them above the bottom of the area, or in as
 * many as fit next to each other. Each card goes into the shortest column; what is still too tall scrolls.
 */
internal object DebugOverviewLayout {

    const val ROW_HEIGHT = 8f
    const val PADDING = 2f
    const val GAP = 2f

    fun <T : OverviewItem> layout(cards: List<OverviewCard<T>>, area: OverviewArea, width: Float): OverviewLayout<T> {
        val cardWidth = min(width, area.right - area.left)
        if (cardWidth <= 0f) {
            return OverviewLayout(emptyList(), area.top)
        }

        val maxColumns = max(1, floor((area.right - area.left + GAP) / (cardWidth + GAP)).toInt())
        val rows = sort(cards).map(::rows)
        var columns = 1
        var layout = place(rows, area, cardWidth, columns)
        while (layout.bottom > area.bottom && columns < maxColumns) {
            columns++
            layout = place(rows, area, cardWidth, columns)
        }
        return layout
    }

    // priority and order are read once, the caller may change them concurrently
    private class Entry<T>(val item: T, val priority: Int, val order: Int)

    private class Section<T>(val label: String?, val entries: List<Entry<T>>) {
        val priority = entries.minOf { it.priority }
        val order = entries.minOf { it.order }
    }

    private class Card<T>(val title: String, val sections: List<Section<T>>) {
        val priority = sections.minOf { it.priority }
        val order = sections.minOf { it.order }
    }

    private val ENTRY_ORDER = compareBy<Entry<*>>({ it.priority }, { it.order })
    private val SECTION_ORDER = compareBy<Section<*>>({ it.label != null }, { it.priority }, { it.order })
    private val CARD_ORDER = compareBy<Card<*>>({ it.priority }, { it.order })

    private fun <T : OverviewItem> sort(cards: List<OverviewCard<T>>): List<Card<T>> = cards.mapNotNull { card ->
        val sections = card.sections.mapNotNull { section ->
            if (section.items.isEmpty()) {
                null
            } else {
                val entries = section.items.map { Entry(it, it.priority.ordinal, it.order) }
                Section(section.label, entries.sortedWith(ENTRY_ORDER))
            }
        }

        if (sections.isEmpty()) null else Card(card.title, sections.sortedWith(SECTION_ORDER))
    }.sortedWith(CARD_ORDER)

    private fun <T> rows(card: Card<T>): List<OverviewRow<T>> = buildList {
        add(OverviewRow.Header(card.title))
        for (section in card.sections) {
            section.label?.let { add(OverviewRow.Subheader(it)) }
            for (entry in section.entries) {
                add(OverviewRow.Item(entry.item, section.label != null))
            }
        }
    }

    private fun <T> place(
        cards: List<List<OverviewRow<T>>>,
        area: OverviewArea,
        cardWidth: Float,
        columns: Int,
    ): OverviewLayout<T> {
        val bottoms = FloatArray(columns) { area.top }
        val placed = ArrayList<PlacedCard<T>>(cards.size)
        for (rows in cards) {
            var column = 0
            for (candidate in 1 until columns) {
                if (bottoms[candidate] < bottoms[column]) {
                    column = candidate
                }
            }

            val height = rows.size * ROW_HEIGHT + 2 * PADDING
            placed += PlacedCard(
                x = area.left + column * (cardWidth + GAP),
                y = bottoms[column],
                width = cardWidth,
                height = height,
                rows = rows,
            )
            bottoms[column] += height + GAP
        }

        return OverviewLayout(placed, max(area.top, bottoms.max() - GAP))
    }

}
