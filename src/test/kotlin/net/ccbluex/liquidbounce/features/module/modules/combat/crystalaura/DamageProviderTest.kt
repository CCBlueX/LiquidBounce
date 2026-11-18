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
package net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DamageProviderTest {

    @Test
    fun `equal zero damages count as smallerThanOrEqual across providers`() {
        val zeroNormal = NormalDamageProvider(0f)
        val zeroAnd = AndBiDamageProvider(0f, 0f)
        val zeroOr = OrBiDamageProvider(0f, 0f)

        assertTrue(zeroNormal.isSmallerThanOrEqual(zeroAnd))
        assertTrue(zeroAnd.isSmallerThanOrEqual(zeroNormal))
        assertTrue(zeroOr.isSmallerThanOrEqual(zeroAnd))
    }

    @Test
    fun `custom DamageProvider fallback compares against other fixed value instead of self`() {
        val customHigh = object : DamageProvider by NormalDamageProvider(12f) {}
        val normalLow = NormalDamageProvider(4f)

        assertTrue(normalLow.isSmallerThanOrEqual(customHigh))
        assertFalse(NormalDamageProvider(16f).isSmallerThanOrEqual(customHigh))
    }

    @Test
    fun `isAnyGreaterThan catches spike in either current or predicted self damage`() {
        val currentSpike = AndBiDamageProvider(15f, 2f)
        val predictedSpike = AndBiDamageProvider(2f, 15f)
        val safeBoth = AndBiDamageProvider(4f, 6f)

        assertTrue(currentSpike.isAnyGreaterThan(12f))
        assertTrue(predictedSpike.isAnyGreaterThan(12f))
        assertFalse(safeBoth.isAnyGreaterThan(12f))
    }

    @Test
    fun `OrBiDamageProvider exposes peak damage for candidate sorting`() {
        val highCurrentLowPredicted = OrBiDamageProvider(14f, 1f)
        assertEquals(14f, highCurrentLowPredicted.getFixed())
    }

}
