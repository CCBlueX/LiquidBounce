/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2025 CCBlueX
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
 *
 */
package net.ccbluex.liquidbounce.integration.backend

import net.minecraft.util.Identifier

/**
 * Represents a texture used by the browser.
 *
 * @param textureId The OpenGL texture ID.
 * @param identifier The identifier of the texture, used for rendering.
 * @param width The width of the texture.
 * @param height The height of the texture.
 * @param bgra Whether the texture is in BGRA format (true) or RGBA format (false).
 */
data class BrowserTexture(
    val textureId: Int,
    val identifier: Identifier,
    val width: Int,
    val height: Int,
    val bgra: Boolean
)
