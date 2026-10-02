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
package net.ccbluex.liquidbounce.deeplearn.combat

import net.ccbluex.liquidbounce.deeplearn.model.ModelFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CombatModelsTest {
    @Test
    fun `every bundled model fits the rotation slot`() {
        val slot = CombatModels.SLOT
        for (name in slot.bundled) {
            val file = assertNotNull(javaClass.getResourceAsStream(slot.resource(name)), name).use(ModelFile::read)
            assertTrue(slot.accepts(file), name)
            assertEquals(CombatOutputs.NETWORK.hidden, file.network.hidden, name)
            val turnCap = CombatModels.turnCap(file)
            assertTrue(turnCap in 10f..60f, "$name: $turnCap")
        }
    }
}
