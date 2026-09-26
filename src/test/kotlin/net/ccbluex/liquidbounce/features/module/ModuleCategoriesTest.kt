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
package net.ccbluex.liquidbounce.features.module

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ModuleCategoriesTest {

    @Test
    fun `a duplicate tag keeps the category registered first`() {
        val original = ModuleCategories.register(ModuleCategory("ModuleCategoriesTestDuplicate"))

        try {
            assertFailsWith<IllegalStateException> {
                ModuleCategories.register(ModuleCategory("modulecategoriestestduplicate"))
            }
            assertSame(original, ModuleCategories.byName("ModuleCategoriesTestDuplicate"))
        } finally {
            ModuleCategories.unregister(original)
        }
    }

    @Test
    fun `unregister only removes the identical instance`() {
        val registered = ModuleCategories.register(ModuleCategory("ModuleCategoriesTestUnregister"))

        try {
            assertFalse(ModuleCategories.unregister(ModuleCategory("ModuleCategoriesTestUnregister")))
            assertSame(registered, ModuleCategories.byName("ModuleCategoriesTestUnregister"))
        } finally {
            assertTrue(ModuleCategories.unregister(registered))
        }

        assertNull(ModuleCategories.byName("ModuleCategoriesTestUnregister"))
    }

}
