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
package net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.baseplace

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class BasePlaceDecisionsTest {

    @Test
    fun `obsidian remains preferred when both base slots exist`() {
        assertEquals(3, selectBaseSlot(obsidianSlot = 3, bedrockSlot = 7))
    }

    @Test
    fun `bedrock is the fallback when obsidian is missing`() {
        assertEquals(7, selectBaseSlot(obsidianSlot = null, bedrockSlot = 7))
    }

    @Test
    fun `missing base items produce no slot`() {
        assertNull(selectBaseSlot<Int>(obsidianSlot = null, bedrockSlot = null))
    }

    @Test
    fun `immediately placeable target needs no support search`() {
        val path = findBasePlaceSupportPath(
            target = "target",
            hasSolidNeighbor = { throw AssertionError("Immediate placement must not query support") },
            canClickPlace = { it == "target" },
            neighbors = { throw AssertionError("Immediate placement must not expand neighbors") },
            maxIterations = 1,
        )

        assertEquals(listOf("target"), path)
    }

    @Test
    fun `connected clickable solid anchor supports the target`() {
        val graph = mapOf("target" to listOf("anchor"))

        val path = findBasePlaceSupportPath(
            target = "target",
            hasSolidNeighbor = { it == "anchor" },
            canClickPlace = { it == "anchor" },
            neighbors = { graph[it].orEmpty() },
            maxIterations = 4,
        )

        assertEquals(listOf("target", "anchor"), path)
    }

    @Test
    fun `support path includes every required intermediate position`() {
        val graph = mapOf(
            "target" to listOf("middle"),
            "middle" to listOf("anchor"),
        )

        val path = findBasePlaceSupportPath(
            target = "target",
            hasSolidNeighbor = { it == "anchor" },
            canClickPlace = { it != "target" },
            neighbors = { graph[it].orEmpty() },
            maxIterations = 4,
        )

        assertEquals(listOf("target", "middle", "anchor"), path)
    }

    @Test
    fun `solid foundation without click placement is not accepted as an anchor`() {
        val graph = mapOf("target" to listOf("solid-but-unreachable"))

        val path = findBasePlaceSupportPath(
            target = "target",
            hasSolidNeighbor = { true },
            canClickPlace = { false },
            neighbors = { graph[it].orEmpty() },
            maxIterations = 4,
        )

        assertNull(path)
    }

    @Test
    fun `clickable position without a solid foundation is not accepted as an anchor`() {
        val graph = mapOf("target" to listOf("floating"))

        val path = findBasePlaceSupportPath(
            target = "target",
            hasSolidNeighbor = { false },
            canClickPlace = { it == "floating" },
            neighbors = { graph[it].orEmpty() },
            maxIterations = 4,
        )

        assertNull(path)
    }

    @Test
    fun `neighbor filtering excludes obstructed positions while preserving a valid detour`() {
        val graph = mapOf(
            "target" to listOf("obstructed", "open"),
            "obstructed" to listOf("anchor"),
            "open" to listOf("middle"),
            "middle" to listOf("anchor"),
        )

        val path = findBasePlaceSupportPath(
            target = "target",
            hasSolidNeighbor = { it == "anchor" },
            canClickPlace = { it != "target" },
            neighbors = { current -> graph[current].orEmpty().filterNot { it == "obstructed" } },
            maxIterations = 6,
        )

        assertEquals(listOf("target", "open", "middle", "anchor"), path)
    }

    @Test
    fun `disconnected support positions return no path`() {
        val path = findBasePlaceSupportPath(
            target = "target",
            hasSolidNeighbor = { it == "anchor" },
            canClickPlace = { it == "anchor" },
            neighbors = { emptyList() },
            maxIterations = 4,
        )

        assertNull(path)
    }

    @Test
    fun `iteration budget does not mistake an unexplored anchor for support`() {
        val graph = mapOf(
            "target" to listOf("middle"),
            "middle" to listOf("anchor"),
        )

        val path = findBasePlaceSupportPath(
            target = "target",
            hasSolidNeighbor = { it == "anchor" },
            canClickPlace = { it != "target" },
            neighbors = { graph[it].orEmpty() },
            maxIterations = 2,
        )

        assertNull(path)
    }

    @Test
    fun `support search rejects a nonpositive iteration budget`() {
        assertFailsWith<IllegalArgumentException> {
            findBasePlaceSupportPath(
                target = "target",
                hasSolidNeighbor = { true },
                canClickPlace = { true },
                neighbors = { emptyList() },
                maxIterations = 0,
            )
        }
    }
}
