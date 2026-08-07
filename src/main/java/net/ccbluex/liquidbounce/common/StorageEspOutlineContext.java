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

    private static final ScopedValue<Integer> OUTLINE_COLOR = ScopedValue.newInstance();

    private StorageEspOutlineContext() {
    }

    public static int getOutlineColor() {
        return OUTLINE_COLOR.isBound() ? OUTLINE_COLOR.get() : 0;
    }

    public static void render(int outlineColor, Runnable render) {
        if (outlineColor == 0) {
            render.run();
            return;
        }

        ScopedValue.where(OUTLINE_COLOR, outlineColor).run(render);
    }

}
