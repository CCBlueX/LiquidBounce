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
package net.ccbluex.liquidbounce.integration.backend

import net.ccbluex.liquidbounce.features.addon.AddonApi
import net.minecraft.client.gui.render.TextureSetup

/**
 * Represents a texture used by the browser.
 *
 * @param textureSetup The texture setup object.
 * @param width The width of the texture.
 * @param height The height of the texture.
 * @param bgra Whether the texture is in BGRA format (true) or RGBA format (false).
 * @param u1 [u1], [v1], [u2] and [v2] bound the page within the texture.
 */
@JvmRecord
@AddonApi
data class BrowserTexture(
    val textureSetup: TextureSetup,
    val width: Int,
    val height: Int,
    val bgra: Boolean,
    val u1: Float = 0f,
    val v1: Float = 0f,
    val u2: Float = 1f,
    val v2: Float = 1f
)
