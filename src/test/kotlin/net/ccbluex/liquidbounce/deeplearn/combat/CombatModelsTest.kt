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
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CombatModelsTest {
    @Test
    fun `the bundled model fits its slot and validated aim and attacks`() {
        val slot = CombatModels.slot(CombatStyle.COOLDOWN)
        val file = assertNotNull(javaClass.getResourceAsStream(slot.resource)).use(ModelFile::read)
        assertTrue(slot.accepts(file))
        assertEquals(CombatOutputs.NETWORK.hidden, file.network.hidden)
        val info = CombatModels.info(file)
        assertTrue(info.heads.aim && info.heads.attacks, info.heads.description)
        assertTrue(info.turnCap in 10f..60f, "${info.turnCap}")
    }

    @Test
    fun `legacy combat has no bundled model`() {
        assertNull(javaClass.getResourceAsStream(CombatModels.slot(CombatStyle.LEGACY).resource))
    }
}
