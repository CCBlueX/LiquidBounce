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

package net.ccbluex.liquidbounce.render.effect

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment

/**
 * How a feature puts its world geometry on screen.
 */
sealed class Rendering(name: String, final override val parent: ModeValueGroup<*>) : Mode(name) {

    abstract fun environment(event: WorldRenderEvent): WorldRenderEnvironment

    /**
     * Straight onto the scene, translucent colors and all.
     */
    class Plain(parent: ModeValueGroup<*>) : Rendering("Plain", parent) {
        override fun environment(event: WorldRenderEvent) = event.environment
    }

}

/**
 * The `Render` choice: [Rendering.Plain] first, or a [ShaderLayer].
 *
 * @param solid thin geometry like lines, which shader styles fill instead of only surrounding it
 */
fun ValueGroup.rendering(solid: Boolean = false): ModeValueGroup<Rendering> =
    modes(null, "Render", { 0 }) { arrayOf(Rendering.Plain(it), ShaderLayer(it, solid)) }
