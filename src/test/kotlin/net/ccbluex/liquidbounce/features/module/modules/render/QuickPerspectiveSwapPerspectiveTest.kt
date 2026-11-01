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
package net.ccbluex.liquidbounce.features.module.modules.render

import net.minecraft.client.CameraType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class QuickPerspectiveSwapPerspectiveTest {

    @Test
    fun `first-person view swaps to front view by default`() {
        assertEquals(CameraType.THIRD_PERSON_FRONT, swappedPerspective(CameraType.FIRST_PERSON, false))
    }

    @Test
    fun `rear view setting switches first-person to rear camera`() {
        assertEquals(CameraType.THIRD_PERSON_BACK, swappedPerspective(CameraType.FIRST_PERSON, true))
    }

    @Test
    fun `third-person views swap without double rotation`() {
        assertEquals(CameraType.THIRD_PERSON_FRONT, swappedPerspective(CameraType.THIRD_PERSON_BACK, false))
        assertEquals(CameraType.THIRD_PERSON_BACK, swappedPerspective(CameraType.THIRD_PERSON_FRONT, false))
    }

    @Test
    fun `F5 view changes are respected rather than accumulated`() {
        val vanillaViews = listOf(CameraType.FIRST_PERSON, CameraType.THIRD_PERSON_BACK, CameraType.THIRD_PERSON_FRONT)
        val expected = listOf(CameraType.THIRD_PERSON_FRONT, CameraType.THIRD_PERSON_FRONT, CameraType.THIRD_PERSON_BACK)

        assertEquals(expected, vanillaViews.map { swappedPerspective(it, false) })
        vanillaViews.forEach { assertNotEquals(CameraType.FIRST_PERSON, swappedPerspective(it, false)) }
    }
}
