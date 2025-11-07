/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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

package net.ccbluex.liquidbounce.utils.render;

import net.minecraft.client.gui.ScreenPos;
import net.minecraft.client.gui.navigation.NavigationAxis;
import net.minecraft.client.gui.navigation.NavigationDirection;

/**
 * Float version of {@link ScreenPos}
 */
public record ScreenPosF(float x, float y) {
    public static ScreenPosF of(NavigationAxis axis, float sameAxis, float otherAxis) {
        ScreenPosF var10000;
        switch (axis) {
            case HORIZONTAL -> var10000 = new ScreenPosF(sameAxis, otherAxis);
            case VERTICAL -> var10000 = new ScreenPosF(otherAxis, sameAxis);
            default -> throw new MatchException((String) null, (Throwable) null);
        }

        return var10000;
    }

    public ScreenPosF add(NavigationDirection direction) {
        ScreenPosF var10000;
        switch (direction) {
            case DOWN -> var10000 = new ScreenPosF(this.x, this.y + 1);
            case UP -> var10000 = new ScreenPosF(this.x, this.y - 1);
            case LEFT -> var10000 = new ScreenPosF(this.x - 1, this.y);
            case RIGHT -> var10000 = new ScreenPosF(this.x + 1, this.y);
            default -> throw new MatchException((String) null, (Throwable) null);
        }

        return var10000;
    }

    public float getComponent(NavigationAxis axis) {
        float var10000;
        switch (axis) {
            case HORIZONTAL -> var10000 = this.x;
            case VERTICAL -> var10000 = this.y;
            default -> throw new MatchException((String) null, (Throwable) null);
        }

        return var10000;
    }
}
