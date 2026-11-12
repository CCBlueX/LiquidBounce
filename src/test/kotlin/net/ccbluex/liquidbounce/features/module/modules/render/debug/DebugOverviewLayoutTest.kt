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

import net.ccbluex.liquidbounce.features.module.modules.render.debug.DebugPriority.HIGH
import net.ccbluex.liquidbounce.features.module.modules.render.debug.DebugPriority.LOW
import net.ccbluex.liquidbounce.features.module.modules.render.debug.DebugPriority.NORMAL
import net.ccbluex.liquidbounce.features.module.modules.render.debug.OverviewRow.Header
import net.ccbluex.liquidbounce.features.module.modules.render.debug.OverviewRow.Item
import net.ccbluex.liquidbounce.features.module.modules.render.debug.OverviewRow.Subheader
import kotlin.test.Test
import kotlin.test.assertEquals

class DebugOverviewLayoutTest {

    private data class TestItem(
        val name: String,
        override val priority: DebugPriority,
        override val order: Int,
    ) : OverviewItem

    @Test
    fun `priority decides card, section and row order`() {
        val a1 = TestItem("a1", NORMAL, 0)
        val b1 = TestItem("b1", LOW, 1)
        val b2 = TestItem("b2", HIGH, 2)
        val c1 = TestItem("c1", NORMAL, 3)
        val cards = listOf(
            card("A", a1),
            OverviewCard("B", listOf(OverviewSection(null, listOf(b1, b2)), OverviewSection("Child", listOf(c1)))),
        )

        val placed = DebugOverviewLayout.layout(cards, OverviewArea(0f, 0f, 1000f, 1000f), 100f).cards

        assertEquals(
            listOf(Header("B"), Item(b2, false), Item(b1, false), Subheader("Child"), Item(c1, true)),
            placed[0].rows,
        )
        assertEquals(0f, placed[0].y)
        assertEquals(44f, placed[0].height)
        assertEquals(listOf(Header("A"), Item(a1, false)), placed[1].rows)
        assertEquals(46f, placed[1].y)
    }

    @Test
    fun `cards stay in one column while they fit`() {
        val layout = DebugOverviewLayout.layout(oneItemCards(3), OverviewArea(0f, 0f, 1000f, 64f), 100f)

        assertEquals(listOf(0f to 0f, 0f to 22f, 0f to 44f), layout.cards.map { it.x to it.y })
        assertEquals(64f, layout.bottom)
    }

    @Test
    fun `cards spread over more columns before the overview scrolls`() {
        val layout = DebugOverviewLayout.layout(oneItemCards(3), OverviewArea(0f, 0f, 1000f, 44f), 100f)

        assertEquals(listOf(0f to 0f, 102f to 0f, 0f to 22f), layout.cards.map { it.x to it.y })
        assertEquals(42f, layout.bottom)
    }

    @Test
    fun `cards past the last column go below the area and keep all rows`() {
        val items = List(10) { TestItem("a$it", NORMAL, it) }
        val cards = listOf(card("A", *items.toTypedArray())) + oneItemCards(2)

        val layout = DebugOverviewLayout.layout(cards, OverviewArea(0f, 0f, 204f, 44f), 100f)

        assertEquals(listOf<OverviewRow<TestItem>>(Header("A")) + items.map { Item(it, false) }, layout.cards[0].rows)
        assertEquals(listOf(0f to 0f, 102f to 0f, 102f to 22f), layout.cards.map { it.x to it.y })
        assertEquals(92f, layout.bottom)
    }

    private fun card(title: String, vararg items: TestItem) =
        OverviewCard(title, listOf(OverviewSection(null, items.toList())))

    private fun oneItemCards(count: Int) = (1..count).map { card("Card$it", TestItem("item$it", NORMAL, it)) }

}
