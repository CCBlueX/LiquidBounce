/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2023 CCBlueX
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

package net.ccbluex.liquidbounce.ultralight.translators;

import net.janrupf.ujr.api.cursor.UlCursor;
import org.lwjgl.glfw.GLFW;

/**
 * Helper class to translate Ultralight cursors to GLFW cursors.
 */
public class CursorTranslator {
    /**
     * Translates an Ultralight cursor to a GLFW cursor.
     *
     * @param cursor the Ultralight cursor
     * @return the GLFW standard cursor
     */
    public static int ultralightToGlfwCursor(UlCursor cursor) {
        switch (cursor) {
            case POINTER:
                return GLFW.GLFW_ARROW_CURSOR;
            case CROSS:
                return GLFW.GLFW_CROSSHAIR_CURSOR;
            case HAND:
                return GLFW.GLFW_POINTING_HAND_CURSOR;
            case I_BEAM:
                return GLFW.GLFW_IBEAM_CURSOR;
            case NORTH_SOUTH_RESIZE:
                return GLFW.GLFW_RESIZE_NS_CURSOR;
            case EAST_WEST_RESIZE:
                return GLFW.GLFW_RESIZE_EW_CURSOR;
            default:
                return 0;
        }
    }
}
