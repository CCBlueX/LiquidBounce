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
package net.ccbluex.liquidbounce.integration.backend.backends.ultralight

import com.mojang.blaze3d.platform.InputConstants
import com.mojang.blaze3d.platform.cursor.CursorType
import com.mojang.blaze3d.platform.cursor.CursorTypes
import net.janrupf.ujr.api.UltralightKeyEventBuilder
import net.janrupf.ujr.api.cursor.UlCursor
import net.janrupf.ujr.api.event.UlKeyCode
import net.janrupf.ujr.api.event.UlKeyEvent
import net.janrupf.ujr.api.event.UlKeyEventModifiers
import net.janrupf.ujr.api.event.UlMouseButton
import org.lwjgl.sdl.SDLScancode.*
import java.util.EnumSet

/**
 * Translates the SDL input Minecraft hands us into Ultralight events, which use Windows virtual key codes.
 */
internal object UltralightInput {

    private val virtualKeyCodes = mapOf(
        SDL_SCANCODE_RETURN to UlKeyCode.RETURN,
        SDL_SCANCODE_ESCAPE to UlKeyCode.ESCAPE,
        SDL_SCANCODE_BACKSPACE to UlKeyCode.BACK,
        SDL_SCANCODE_TAB to UlKeyCode.TAB,
        SDL_SCANCODE_SPACE to UlKeyCode.SPACE,
        SDL_SCANCODE_MINUS to UlKeyCode.OEM_MINUS,
        SDL_SCANCODE_EQUALS to UlKeyCode.OEM_PLUS,
        SDL_SCANCODE_LEFTBRACKET to UlKeyCode.OEM_4,
        SDL_SCANCODE_RIGHTBRACKET to UlKeyCode.OEM_6,
        SDL_SCANCODE_BACKSLASH to UlKeyCode.OEM_5,
        SDL_SCANCODE_NONUSBACKSLASH to UlKeyCode.OEM_102,
        SDL_SCANCODE_SEMICOLON to UlKeyCode.OEM_1,
        SDL_SCANCODE_APOSTROPHE to UlKeyCode.OEM_7,
        SDL_SCANCODE_GRAVE to UlKeyCode.OEM_3,
        SDL_SCANCODE_COMMA to UlKeyCode.OEM_COMMA,
        SDL_SCANCODE_PERIOD to UlKeyCode.OEM_PERIOD,
        SDL_SCANCODE_SLASH to UlKeyCode.OEM_2,
        SDL_SCANCODE_CAPSLOCK to UlKeyCode.CAPITAL,
        SDL_SCANCODE_PRINTSCREEN to UlKeyCode.SNAPSHOT,
        SDL_SCANCODE_SCROLLLOCK to UlKeyCode.SCROLL,
        SDL_SCANCODE_PAUSE to UlKeyCode.PAUSE,
        SDL_SCANCODE_INSERT to UlKeyCode.INSERT,
        SDL_SCANCODE_HOME to UlKeyCode.HOME,
        SDL_SCANCODE_PAGEUP to UlKeyCode.PRIOR,
        SDL_SCANCODE_DELETE to UlKeyCode.DELETE,
        SDL_SCANCODE_END to UlKeyCode.END,
        SDL_SCANCODE_PAGEDOWN to UlKeyCode.NEXT,
        SDL_SCANCODE_RIGHT to UlKeyCode.RIGHT,
        SDL_SCANCODE_LEFT to UlKeyCode.LEFT,
        SDL_SCANCODE_DOWN to UlKeyCode.DOWN,
        SDL_SCANCODE_UP to UlKeyCode.UP,
        SDL_SCANCODE_NUMLOCKCLEAR to UlKeyCode.NUMLOCK,
        SDL_SCANCODE_KP_DIVIDE to UlKeyCode.DIVIDE,
        SDL_SCANCODE_KP_MULTIPLY to UlKeyCode.MULTIPLY,
        SDL_SCANCODE_KP_MINUS to UlKeyCode.SUBTRACT,
        SDL_SCANCODE_KP_PLUS to UlKeyCode.ADD,
        SDL_SCANCODE_KP_ENTER to UlKeyCode.RETURN,
        SDL_SCANCODE_KP_PERIOD to UlKeyCode.DECIMAL,
        SDL_SCANCODE_KP_0 to UlKeyCode.NUMPAD0,
        SDL_SCANCODE_APPLICATION to UlKeyCode.APPS,
        SDL_SCANCODE_LCTRL to UlKeyCode.LCONTROL,
        SDL_SCANCODE_LSHIFT to UlKeyCode.LSHIFT,
        SDL_SCANCODE_LALT to UlKeyCode.LMENU,
        SDL_SCANCODE_LGUI to UlKeyCode.LWIN,
        SDL_SCANCODE_RCTRL to UlKeyCode.RCONTROL,
        SDL_SCANCODE_RSHIFT to UlKeyCode.RSHIFT,
        SDL_SCANCODE_RALT to UlKeyCode.RMENU,
        SDL_SCANCODE_RGUI to UlKeyCode.RWIN,
        SDL_SCANCODE_0 to UlKeyCode.NUMBER_0,
    )

    /**
     * The Windows virtual key code of a key.
     *
     * Letters and digits follow the keyboard layout, the other keys their position.
     */
    private fun virtualKeyCode(keyCode: Int, scanCode: Int) = when (keyCode) {
        in 'a'.code..'z'.code -> UlKeyCode.A + (keyCode - 'a'.code)
        in '0'.code..'9'.code -> UlKeyCode.NUMBER_0 + (keyCode - '0'.code)
        else -> when (scanCode) {
            in SDL_SCANCODE_A..SDL_SCANCODE_Z -> UlKeyCode.A + (scanCode - SDL_SCANCODE_A)
            in SDL_SCANCODE_1..SDL_SCANCODE_9 -> UlKeyCode.NUMBER_1 + (scanCode - SDL_SCANCODE_1)
            in SDL_SCANCODE_F1..SDL_SCANCODE_F12 -> UlKeyCode.F1 + (scanCode - SDL_SCANCODE_F1)
            in SDL_SCANCODE_F13..SDL_SCANCODE_F24 -> UlKeyCode.F13 + (scanCode - SDL_SCANCODE_F13)
            in SDL_SCANCODE_KP_1..SDL_SCANCODE_KP_9 -> UlKeyCode.NUMPAD1 + (scanCode - SDL_SCANCODE_KP_1)
            else -> virtualKeyCodes[scanCode] ?: UlKeyCode.UNKNOWN
        }
    }

    private fun modifiers(modifiers: Int): EnumSet<UlKeyEventModifiers> =
        EnumSet.noneOf(UlKeyEventModifiers::class.java).apply {
            if (modifiers and InputConstants.MOD_SHIFT != 0) add(UlKeyEventModifiers.SHIFT)
            if (modifiers and InputConstants.MOD_CONTROL != 0) add(UlKeyEventModifiers.CTRL)
            if (modifiers and InputConstants.MOD_ALT != 0) add(UlKeyEventModifiers.ALT)
            if (modifiers and InputConstants.MOD_SUPER != 0) add(UlKeyEventModifiers.META)
        }

    /**
     * The key down or up events for a key, followed by the character events SDL doesn't send as text.
     */
    fun keyEvents(pressed: Boolean, keyCode: Int, scanCode: Int, modifiers: Int): List<UlKeyEvent> {
        val virtualKeyCode = virtualKeyCode(keyCode, scanCode)
        val builder = if (pressed) UltralightKeyEventBuilder.rawDown() else UltralightKeyEventBuilder.up()

        val keyEvent = builder
            .virtualKeyCode(virtualKeyCode)
            .nativeKeyCode(scanCode)
            .keyIdentifier(UlKeyEvent.keyIdentifierFromVirtualKeyCode(virtualKeyCode))
            .modifiers(modifiers(modifiers))
            .keypad(scanCode in SDL_SCANCODE_KP_DIVIDE..SDL_SCANCODE_KP_PERIOD)
            .build()

        val text = when (virtualKeyCode) {
            UlKeyCode.RETURN -> "\r"
            UlKeyCode.TAB -> "\t"
            else -> null
        }

        return if (pressed && text != null) listOf(keyEvent, characterEvent(text)) else listOf(keyEvent)
    }

    fun characterEvent(codepoint: Int) = characterEvent(String(Character.toChars(codepoint)))

    private fun characterEvent(text: String): UlKeyEvent = UltralightKeyEventBuilder.character()
        .text(text)
        .unmodifiedText(text)
        .build()

    fun mouseButton(button: Int) = when (button) {
        InputConstants.MOUSE_BUTTON_LEFT -> UlMouseButton.LEFT
        InputConstants.MOUSE_BUTTON_MIDDLE -> UlMouseButton.MIDDLE
        InputConstants.MOUSE_BUTTON_RIGHT -> UlMouseButton.RIGHT
        else -> null
    }

    fun cursor(cursor: UlCursor): CursorType = when (cursor) {
        UlCursor.HAND -> CursorTypes.POINTING_HAND
        UlCursor.I_BEAM, UlCursor.VERTICAL_TEXT -> CursorTypes.IBEAM
        UlCursor.CROSS, UlCursor.CELL -> CursorTypes.CROSSHAIR
        UlCursor.NORTH_RESIZE, UlCursor.SOUTH_RESIZE, UlCursor.NORTH_SOUTH_RESIZE, UlCursor.ROW_RESIZE ->
            CursorTypes.RESIZE_NS
        UlCursor.EAST_RESIZE, UlCursor.WEST_RESIZE, UlCursor.EAST_WEST_RESIZE, UlCursor.COLUMN_RESIZE ->
            CursorTypes.RESIZE_EW
        UlCursor.MOVE, UlCursor.GRAB, UlCursor.GRABBING -> CursorTypes.RESIZE_ALL
        UlCursor.NOT_ALLOWED, UlCursor.NO_DROP -> CursorTypes.NOT_ALLOWED
        else -> CursorTypes.ARROW
    }

}
