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
    fun `normal damage respects strict and inclusive threshold boundaries`() {
        val damage = NormalDamageProvider(4f)

        assertTrue(damage.isSmallerThan(5f))
        assertFalse(damage.isSmallerThan(4f))
        assertTrue(damage.isSmallerThanOrEqual(4f))
        assertFalse(damage.isSmallerThanOrEqual(3f))
        assertTrue(damage.isGreaterThan(3f))
        assertFalse(damage.isGreaterThan(4f))
        assertTrue(damage.isAnyGreaterThanOrEqual(4f))
        assertFalse(damage.isAnyGreaterThanOrEqual(5f))
    }

    @Test
    fun `and damage requires both current and predicted values below minimum`() {
        val damage = AndBiDamageProvider(3f, 8f)

        assertFalse(damage.isSmallerThan(5f))
        assertTrue(damage.isSmallerThan(9f))
        assertFalse(damage.isSmallerThanOrEqual(7f))
        assertTrue(damage.isSmallerThanOrEqual(8f))
    }

    @Test
    fun `or damage accepts either current or predicted value below minimum`() {
        val damage = OrBiDamageProvider(3f, 8f)

        assertTrue(damage.isSmallerThan(5f))
        assertFalse(damage.isSmallerThan(3f))
        assertTrue(damage.isSmallerThanOrEqual(3f))
        assertFalse(damage.isSmallerThanOrEqual(2f))
    }

    @Test
    fun `and damage exceeds cap only when both values exceed it`() {
        val damage = AndBiDamageProvider(3f, 8f)

        assertTrue(damage.isGreaterThan(2f))
        assertFalse(damage.isGreaterThan(3f))
        assertFalse(damage.isGreaterThan(7f))
    }

    @Test
    fun `or damage exceeds cap when either value exceeds it`() {
        val damage = OrBiDamageProvider(3f, 8f)

        assertTrue(damage.isGreaterThan(2f))
        assertTrue(damage.isGreaterThan(3f))
        assertFalse(damage.isGreaterThan(8f))
    }

    @Test
    fun `lethal threshold detects a spike in either damage value`() {
        val currentSpike = AndBiDamageProvider(12f, 2f)
        val predictedSpike = AndBiDamageProvider(2f, 12f)
        val safe = AndBiDamageProvider(4f, 6f)

        assertTrue(currentSpike.isAnyGreaterThanOrEqual(12f))
        assertTrue(predictedSpike.isAnyGreaterThanOrEqual(12f))
        assertFalse(safe.isAnyGreaterThanOrEqual(12f))
    }

    @Test
    fun `or damage uses the same inclusive lethal threshold for either value`() {
        assertTrue(OrBiDamageProvider(12f, 2f).isAnyGreaterThanOrEqual(12f))
        assertTrue(OrBiDamageProvider(2f, 12f).isAnyGreaterThanOrEqual(12f))
        assertFalse(OrBiDamageProvider(11.9f, 2f).isAnyGreaterThanOrEqual(12f))
    }

    @Test
    fun `fixed damage exposes current normal and predicted bi values`() {
        assertEquals(4f, NormalDamageProvider(4f).getFixed())
        assertEquals(8f, AndBiDamageProvider(3f, 8f).getFixed())
    }

    @Test
    fun `normal damage comparison includes equality`() {
        val damage = NormalDamageProvider(4f)

        assertTrue(damage.isSmallerThanOrEqual(NormalDamageProvider(4f)))
        assertTrue(damage.isSmallerThanOrEqual(NormalDamageProvider(5f)))
        assertFalse(damage.isSmallerThanOrEqual(NormalDamageProvider(3f)))
    }
}
