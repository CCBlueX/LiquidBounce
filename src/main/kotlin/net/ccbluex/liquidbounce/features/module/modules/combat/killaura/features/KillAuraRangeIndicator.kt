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
package net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.drawGradientCircle
import net.ccbluex.liquidbounce.render.drawCircleOutline
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition

/**
 * Renders a circle around the player indicating the KillAura attack range.
 * Uses the same range values as KillAura (Range and WallRange).
 * - Red/idle color when no enemy is in range
 * - Green/active color when an enemy is in range and being targeted
 */
object KillAuraRangeIndicator : ToggleableConfigurable(ModuleKillAura, "RangeIndicator", false) {

    private val idleColor by color("IdleColor", Color4b(255, 50, 50, 80))
    private val activeColor by color("ActiveColor", Color4b(50, 255, 50, 80))
    private val outline by boolean("Outline", true)
    private val outlineColor by color("OutlineColor", Color4b(255, 255, 255, 120))
    private val showWallRange by boolean("ShowWallRange", false)
    private val wallRangeColor by color("WallRangeColor", Color4b(255, 165, 0, 60))

    fun render(env: WorldRenderEnvironment, partialTicks: Float) {
        if (!enabled) return

        val hasTarget = ModuleKillAura.targetTracker.target != null
        val color = if (hasTarget) activeColor else idleColor
        val range = ModuleKillAura.range
        val wallRange = ModuleKillAura.wallRange

        val pos = player.interpolateCurrentPosition(partialTicks)

        with(env) {
            withPositionRelativeToCamera(pos) {
                // Main attack range circle
                drawGradientCircle(range, 0f, color, color.with(a = 0))
                
                if (outline) {
                    drawCircleOutline(range, outlineColor)
                }

                // Wall range circle (smaller, for attacking through walls)
                if (showWallRange && wallRange < range) {
                    drawGradientCircle(wallRange, 0f, wallRangeColor, wallRangeColor.with(a = 0))
                    if (outline) {
                        drawCircleOutline(wallRange, outlineColor.with(a = 80))
                    }
                }
            }
        }
    }

}
