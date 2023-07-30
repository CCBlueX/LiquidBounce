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

package net.ccbluex.liquidbounce.ultralight.window;

public interface WindowStateListener {
    void onFramebufferSizeChange(int width, int height);

    void onCursorPos(double x, double y);

    void onMouseButton(int button, int action, int mods);

    void onScroll(double x, double y);

    void onCharMods(int codepoint, int mods);

    void onKey(int key, int scancode, int action, int mods);

    void onFocusChange(boolean isFocused);
}
