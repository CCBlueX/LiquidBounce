/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2024 CCBlueX
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
 *
 */

package net.ccbluex.liquidbounce.render.shader.shaders

import net.ccbluex.liquidbounce.render.shader.Shader
import net.ccbluex.liquidbounce.utils.io.resourceToString

/**
 * In-built background shader
 *
 * TODO: Use theme specific shader
 */
val backgroundShader by lazy {
    Shader(
        resourceToString("/assets/liquidbounce/shaders/vertex.vert"),
        resourceToString("/assets/liquidbounce/shaders/fragment/background.frag")
    )
}
