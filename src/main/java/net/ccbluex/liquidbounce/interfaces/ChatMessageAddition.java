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
package net.ccbluex.liquidbounce.interfaces;

/**
 * Additions to {@link net.minecraft.client.gui.hud.ChatHudLine} and
 * {@link net.minecraft.client.gui.hud.ChatHudLine.Visible}.
 */
public interface ChatMessageAddition {

    /**
     * Sets the ID for the chat message.
     * The ID will be used for removing chat messages.
     */
    void liquid_bounce$setId(String id);

    /**
     * Gets the ID of the chat message.
     */
    String liquid_bounce$getId();

}
