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

package net.ccbluex.liquidbounce.utils.input

import com.mojang.blaze3d.platform.InputConstants
import net.ccbluex.liquidbounce.test.MinecraftBootstrap
import net.minecraft.network.chat.ClickEvent
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InputBindTest {

    @BeforeTest
    fun setUp() {
        MinecraftBootstrap.ensureInitialized()
    }

    @Test
    fun `renderText displays the bound keyboard key`() {
        assertRendersKey(InputConstants.Type.KEYSYM, InputConstants.KEY_K)
        assertRendersKey(InputConstants.Type.KEYSYM, InputConstants.KEY_LSHIFT)
        assertRendersKey(InputConstants.Type.KEYSYM, InputConstants.KEY_NUMPAD0)
    }

    /**
     * Mouse buttons used to be rendered as the keyboard key of the same name, because the rendered
     * key was resolved from [InputBind.keyName], which drops the category of the bound key.
     */
    @Test
    fun `renderText displays the bound mouse button`() {
        assertRendersKey(InputConstants.Type.MOUSE, InputConstants.MOUSE_BUTTON_LEFT)
        assertRendersKey(InputConstants.Type.MOUSE, InputConstants.MOUSE_BUTTON_RIGHT)
        assertRendersKey(InputConstants.Type.MOUSE, InputConstants.MOUSE_BUTTON_MIDDLE)

        // The remaining mouse buttons are named after their number and used to render as digit keys
        for (button in InputConstants.MOUSE_BUTTON_4..InputConstants.MOUSE_BUTTON_8) {
            assertRendersKey(InputConstants.Type.MOUSE, button)
        }
    }

    @Test
    fun `renderText displays an unbound key`() {
        val rendered = InputBind.UNBOUND.renderText()

        assertEquals(InputConstants.UNKNOWN.displayName.string, rendered.siblings.first().string)
    }

    private fun assertRendersKey(type: InputConstants.Type, code: Int) {
        val key = type.getOrCreate(code)
        // `renderText` threw a NumberFormatException for keys whose name is not a valid keyboard key
        val rendered = InputBind(key, InputBind.BindAction.TOGGLE, emptySet()).renderText()
        val keyPart = rendered.siblings.first()

        assertEquals(key.displayName.string, keyPart.string, "Rendered key of $key")
        assertEquals(
            ClickEvent.CopyToClipboard(key.name),
            keyPart.style.clickEvent,
            "Copyable content of $key"
        )
        assertTrue(rendered.string.endsWith("(${InputBind.BindAction.TOGGLE.tag})"))
    }

}
