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

package net.ccbluex.liquidbounce.utils.client;

import net.minecraft.text.ClickEvent;

/**
 * Allows {@link net.minecraft.text.MutableText} to execute anything on click.
 * <p>
 * Known issue: This type cannot be resolved with {@link ClickEvent#CODEC}.
 */
public record RunnableClickEvent(Runnable action) implements ClickEvent, Runnable {
    @Override
    public ClickEvent.Action getAction() {
        return null;
    }

    @Override
    public void run() {
        action.run();
    }
}
