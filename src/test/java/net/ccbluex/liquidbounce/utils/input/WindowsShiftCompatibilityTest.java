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

package net.ccbluex.liquidbounce.utils.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.ccbluex.liquidbounce.test.MinecraftBootstrap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.sdl.SDLEvents;
import org.lwjgl.sdl.SDLKeycode;
import org.lwjgl.sdl.SDL_KeyboardEvent;
import org.lwjgl.sdl.SDL_Event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WindowsShiftCompatibilityTest {
    private static final int WINDOW = 42;

    @Test
    void unknownNativeEventCannotMatchDefaultClickGuiBindUntilRecovered() {
        MinecraftBootstrap.INSTANCE.ensureInitialized();
        var bind = new InputBind(InputConstants.Type.KEYBOARD, InputConstants.KEY_RSHIFT, InputBind.BindAction.TOGGLE);
        try (var event = SDL_KeyboardEvent.calloc()) {
            event.raw((short) 0xE036).windowID(WINDOW).down(true);
            assertFalse(bind.matchesKey(event.scancode()));
            WindowsShiftCompatibility.repair(event);
            assertTrue(bind.matchesKey(event.scancode()));
            assertFalse(InputBind.UNBOUND.matchesKey(event.scancode()));
        }
    }

    @AfterEach
    void releaseShift() {
        try (var event = SDL_KeyboardEvent.calloc()) {
            event.scancode(229).windowID(WINDOW).down(false);
            WindowsShiftCompatibility.repair(event);
        }
    }

    @Test
    void recoversExtendedRightShiftPressAndReleaseWithoutLosingOtherModifiers() {
        try (var event = SDL_KeyboardEvent.calloc()) {
            event.raw((short) 0xE036).windowID(WINDOW).down(true)
                .mod((short) (SDLKeycode.SDL_KMOD_NUM | SDLKeycode.SDL_KMOD_LSHIFT));
            WindowsShiftCompatibility.repair(event);
            assertEquals(229, event.scancode());
            assertEquals(SDLKeycode.SDLK_RSHIFT, event.key());
            assertEquals(SDLKeycode.SDL_KMOD_NUM | SDLKeycode.SDL_KMOD_SHIFT, event.mod());

            event.scancode(0).key(0).down(false);
            WindowsShiftCompatibility.repair(event);
            assertEquals(229, event.scancode());
            assertEquals(SDLKeycode.SDL_KMOD_NUM | SDLKeycode.SDL_KMOD_LSHIFT, event.mod());
        }
    }

    @Test
    void preservesUnknownKeysAndAlreadyRecognizedEvents() {
        try (var event = SDL_KeyboardEvent.calloc()) {
            event.raw((short) 0xE035).windowID(WINDOW).down(true);
            WindowsShiftCompatibility.repair(event);
            assertEquals(0, event.scancode());
            assertEquals(0, event.key());
            assertEquals(0, event.mod());

            event.raw((short) 0xE036).scancode(19).key('p');
            WindowsShiftCompatibility.repair(event);
            assertEquals(19, event.scancode());
            assertEquals('p', event.key());
        }
    }

    @Test
    void maintainsShiftCombinationsAndClearsThemOnRelease() {
        try (var shift = SDL_KeyboardEvent.calloc(); var letter = SDL_KeyboardEvent.calloc()) {
            shift.raw((short) 0xE036).windowID(WINDOW).down(true);
            WindowsShiftCompatibility.repair(shift);
            letter.scancode(19).key('p').windowID(WINDOW).down(true);
            WindowsShiftCompatibility.repair(letter);
            assertEquals(SDLKeycode.SDL_KMOD_RSHIFT, letter.mod());

            shift.scancode(0).key(0).down(false);
            WindowsShiftCompatibility.repair(shift);
            letter.mod((short) 0);
            WindowsShiftCompatibility.repair(letter);
            assertEquals(0, letter.mod());
        }
    }

    @Test
    void clearsRecoveredShiftOnFocusLossAndDoesNotAffectAnotherWindow() {
        try (var shift = SDL_KeyboardEvent.calloc(); var letter = SDL_KeyboardEvent.calloc();
             var focus = SDL_Event.calloc()) {
            shift.raw((short) 0xE036).windowID(WINDOW).down(true);
            WindowsShiftCompatibility.repair(shift);
            letter.scancode(19).key('p').windowID(WINDOW + 1).down(true);
            WindowsShiftCompatibility.repair(letter);
            assertEquals(0, letter.mod());

            focus.window().type(SDLEvents.SDL_EVENT_WINDOW_FOCUS_LOST).windowID(WINDOW);
            WindowsShiftCompatibility.windowEvent(focus);
            letter.windowID(WINDOW);
            WindowsShiftCompatibility.repair(letter);
            assertEquals(0, letter.mod());
        }
    }
}
