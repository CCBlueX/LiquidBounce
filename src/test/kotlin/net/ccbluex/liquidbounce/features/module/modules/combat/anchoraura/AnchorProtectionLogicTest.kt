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
package net.ccbluex.liquidbounce.features.module.modules.combat.anchoraura

import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AnchorProtectionLogicTest {

    private val feet = Vec3(0.5, -0.5, 0.5)
    private val anchor = Vec3(3.5, -0.5, 0.5)

    @Test
    fun `solid block centered between feet and anchor is accepted`() {
        val distance = AnchorProtectionLogic.lineDistance(BlockPos(1, -1, 0), feet, anchor)

        assertEquals(0.0, assertNotNull(distance), 0.0001)
    }

    @Test
    fun `small offset from the lower-hitbox line is accepted`() {
        val distance = AnchorProtectionLogic.lineDistance(BlockPos(1, -1, 1), feet, anchor)

        assertEquals(1.0, assertNotNull(distance), 0.0001)
    }

    @Test
    fun `blocks behind the player or beyond the anchor are rejected`() {
        assertNull(AnchorProtectionLogic.lineDistance(BlockPos(-1, -1, 0), feet, anchor))
        assertNull(AnchorProtectionLogic.lineDistance(BlockPos(4, -1, 0), feet, anchor))
    }

    @Test
    fun `blocks too far from the blast line are rejected`() {
        assertNull(AnchorProtectionLogic.lineDistance(BlockPos(1, -1, 2), feet, anchor))
        assertNull(AnchorProtectionLogic.lineDistance(BlockPos(1, 1, 0), feet, anchor))
    }

    @Test
    fun `one glowstone cannot cover both charging and protection`() {
        assertFalse(AnchorProtectionLogic.canProtectWithGlowstone(availableCount = 1, reserveCharge = true))
        assertTrue(AnchorProtectionLogic.canProtectWithGlowstone(availableCount = 1, reserveCharge = false))
    }

    @Test
    fun `charge reservation requires a second glowstone`() {
        assertTrue(AnchorProtectionLogic.canProtectWithGlowstone(availableCount = 2, reserveCharge = true))
        assertFalse(AnchorProtectionLogic.canProtectWithGlowstone(availableCount = 0, reserveCharge = false))
    }

    @Test
    fun `coincident feet and anchor have no protection line`() {
        assertNull(AnchorProtectionLogic.lineDistance(BlockPos.ZERO, feet, feet))
    }

}
