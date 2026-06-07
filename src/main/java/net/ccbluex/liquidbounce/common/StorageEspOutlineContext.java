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
package net.ccbluex.liquidbounce.common;

public final class StorageEspOutlineContext {

    private static final ThreadLocal<Integer> OUTLINE_COLOR = ThreadLocal.withInitial(() -> 0);

    private StorageEspOutlineContext() {
    }

    public static int getOutlineColor() {
        return OUTLINE_COLOR.get();
    }

    public static int push(int outlineColor) {
        int previous = OUTLINE_COLOR.get();
        OUTLINE_COLOR.set(outlineColor);
        return previous;
    }

    public static void restore(int outlineColor) {
        if (outlineColor == 0) {
            OUTLINE_COLOR.remove();
        } else {
            OUTLINE_COLOR.set(outlineColor);
        }
    }

}
