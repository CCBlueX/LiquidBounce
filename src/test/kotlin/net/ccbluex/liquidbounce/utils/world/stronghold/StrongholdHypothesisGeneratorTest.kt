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
package net.ccbluex.liquidbounce.utils.world.stronghold

import net.ccbluex.liquidbounce.test.MinecraftBootstrap
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class StrongholdHypothesisGeneratorTest {

    companion object {
        @JvmStatic
        @BeforeAll
        fun bootstrapMinecraft() {
            MinecraftBootstrap.ensureInitialized()
        }
    }

    @Test
    fun `generator emits fixed stronghold count`() {
        val hypotheses = StrongholdHypothesisGenerator.generate(10, seed = 1337L)

        assertEquals(10, hypotheses.size)
        hypotheses.forEach {
            assertEquals(128, it.chunks.size)
        }
    }

    @Test
    fun `ring distribution matches vanilla concentric rings`() {
        val ringDistribution = StrongholdHypothesisGenerator.ringDistribution()

        assertArrayEquals(intArrayOf(3, 6, 10, 15, 21, 28, 36, 9), ringDistribution)
    }

    @Test
    fun `generation is deterministic for a fixed seed`() {
        val generatedA = StrongholdHypothesisGenerator.generate(1, seed = 42L).first()
        val generatedB = StrongholdHypothesisGenerator.generate(1, seed = 42L).first()

        assertArrayEquals(generatedA.chunks, generatedB.chunks)
    }
}
