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
package net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.place

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CrystalAuraActionGenerationTest {

    @Test
    fun `only the newest generation is current`() {
        val actions = CrystalAuraActionGeneration()
        val first = actions.next()
        val second = actions.next()

        assertFalse(actions.isCurrent(first))
        assertTrue(actions.isCurrent(second))
    }

    @Test
    fun `invalidated generation cannot execute its callback`() {
        val actions = CrystalAuraActionGeneration()
        val token = actions.next()
        actions.invalidate()
        var executions = 0

        val executed = actions.runIfCurrent(token) {
            executions++
            true
        }

        assertFalse(executed)
        assertEquals(0, executions)
    }

    @Test
    fun `current generation executes and propagates its result`() {
        val actions = CrystalAuraActionGeneration()
        val token = actions.next()
        var executions = 0

        val executed = actions.runIfCurrent(token) {
            executions++
            true
        }

        assertTrue(executed)
        assertEquals(1, executions)
    }

    @Test
    fun `failed dispatch never runs post-dispatch bookkeeping`() {
        var bookkeeping = 0

        val dispatched = completeCrystalAuraDispatch({ false }) {
            bookkeeping++
        }

        assertFalse(dispatched)
        assertEquals(0, bookkeeping)
    }

    @Test
    fun `successful dispatch runs bookkeeping exactly once`() {
        var bookkeeping = 0

        val dispatched = completeCrystalAuraDispatch({ true }) {
            bookkeeping++
        }

        assertTrue(dispatched)
        assertEquals(1, bookkeeping)
    }
}
