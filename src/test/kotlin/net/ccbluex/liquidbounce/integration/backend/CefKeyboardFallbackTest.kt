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

package net.ccbluex.liquidbounce.integration.backend

import net.minecraft.client.input.InputQuirks
import org.cef.CefClient
import org.cef.browser.CefBrowser
import org.cef.handler.CefKeyboardHandler.CefKeyEvent
import org.cef.handler.CefKeyboardHandlerAdapter
import org.cef.misc.BoolRef
import sun.misc.Unsafe
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Tests the actual CEF callbacks after Fabric has applied the mixin, without starting Chromium. */
class CefKeyboardFallbackTest {

    @Test
    fun `unhandled F is consumed on macOS before it reaches the native menu`() {
        val client = clientWithoutNativeRuntime()
        assertEquals(InputQuirks.REPLACE_CTRL_KEY_WITH_CMD_KEY, client.onKeyEvent(null, fKey()))
    }

    @Test
    fun `the renderer can process F before the fallback is consumed`() {
        val client = clientWithoutNativeRuntime()
        val shortcut = BoolRef(false)
        assertFalse(client.onPreKeyEvent(null, fKey(), shortcut))
        assertFalse(shortcut.get())
    }

    @Test
    fun `registered keyboard handlers still run`() {
        val client = clientWithoutNativeRuntime()
        val event = fKey()
        var called = false
        val browser = Proxy.newProxyInstance(CefBrowser::class.java.classLoader,
            arrayOf(CefBrowser::class.java)) { _, _, _ -> null } as CefBrowser
        client.addKeyboardHandler(object : CefKeyboardHandlerAdapter() {
            override fun onKeyEvent(receivedBrowser: CefBrowser, receivedEvent: CefKeyEvent): Boolean {
                assertSame(browser, receivedBrowser)
                assertSame(event, receivedEvent)
                called = true
                return true
            }
        })

        assertTrue(client.onKeyEvent(browser, event))
        assertTrue(called)
    }

    private fun fKey(): CefKeyEvent {
        val constructor = CefKeyEvent::class.java.getDeclaredConstructor(CefKeyEvent.EventType::class.java,
            Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType,
            Boolean::class.javaPrimitiveType, Char::class.javaPrimitiveType, Char::class.javaPrimitiveType,
            Boolean::class.javaPrimitiveType)
        constructor.isAccessible = true
        return constructor.newInstance(CefKeyEvent.EventType.KEYEVENT_RAWKEYDOWN,
            0, 70, 3, false, 'f', 'f', true)
    }

    private fun clientWithoutNativeRuntime(): CefClient {
        // CEF's constructor creates a native client. These callbacks only need the Java handler field.
        val field = Unsafe::class.java.getDeclaredField("theUnsafe")
        field.isAccessible = true
        return (field.get(null) as Unsafe).allocateInstance(CefClient::class.java) as CefClient
    }

}
