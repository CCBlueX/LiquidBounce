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
package net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.drawCircleOutline
import net.ccbluex.liquidbounce.render.drawGradientCircle
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.utils.rainbow
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.render.withPush
import net.ccbluex.liquidbounce.utils.client.clientStartDurationMs
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager.isInventoryOpen
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.util.Mth
import net.minecraft.world.entity.LivingEntity
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Renders a circle around the player indicating the KillAura attack range.
 * Synced with KillAura settings for Range, WallRange, and IgnoreOpenInventory.
 */
object KillAuraRangeIndicator : ToggleableValueGroup(ModuleKillAura, "RangeIndicator", false) {

    private val colorMode by enumChoice("ColorMode", ColorMode.STATIC)
    private val idleColor by color("IdleColor", Color4b(255, 50, 50, 80))
    private val activeColor by color("ActiveColor", Color4b(50, 255, 50, 80))

    private val outline by boolean("Outline", true)
    private val outlineColor by color("OutlineColor", Color4b(255, 255, 255, 120))

    private val pulseAnimation by boolean("PulseAnimation", false)
    private val pulseSpeed by float("PulseSpeed", 2f, 0.5f..5f)
    private val pulseIntensity by float("PulseIntensity", 0.15f, 0.05f..0.5f)

    private val fadeAnimation by boolean("FadeAnimation", true)
    private val fadeSpeed by float("FadeSpeed", 0.1f, 0.01f..0.5f)

    private val wallRangeColor by color("WallRangeColor", Color4b(255, 165, 0, 0))
    private val scanRangeColor by color("ScanRangeColor", Color4b(100, 100, 255, 0))
    private val opponentRangeColor by color("OpponentRangeColor", Color4b(255, 0, 0, 0))

    private val hideWhenDead by boolean("HideWhenDead", true)
    private val hideWhenSpectator by boolean("HideWhenSpectator", true)
    private val hideInVehicle by boolean("HideInVehicle", false)
    private val respectInventorySetting by boolean("RespectInventorySetting", true)

    private val canBeCovered by boolean("CanBeCovered", false)

    private var colorFactor = 0f

    private enum class ColorMode(override val tag: String) : Tagged {
        STATIC("Static"),
        RAINBOW("Rainbow"),
        DISTANCE("Distance")
    }

    fun render(env: WorldRenderEnvironment, partialTicks: Float) {
        if (!enabled || !canRender()) return

        val target = ModuleKillAura.targetTracker.target
        updateColorFactor(target != null)
        renderIndicator(env, partialTicks, target)
    }

    private fun renderIndicator(env: WorldRenderEnvironment, partialTicks: Float, target: LivingEntity?) {
        val range = ModuleKillAura.range.interactionRange
        val pos = player.interpolateCurrentPosition(partialTicks)
            .add(0.0, 0.001, 0.0) // Prevent z-fighting with the ground
        val pulseOffset = calculatePulse(range)
        val distance = target?.let { sqrt(player.squaredBoxedDistanceTo(it)).toFloat() }

        with(env) {
            startBatch()
            withPositionRelativeToCamera(pos) {
                renderCircles(range, pulseOffset, distance, target != null)
            }
            commitBatch()
        }
    }

    private fun calculatePulse(range: Float): Float {
        return if (pulseAnimation) {
            val time = clientStartDurationMs / 1000.0F * pulseSpeed
            sin(time * Mth.TWO_PI) * pulseIntensity * range
        } else {
            0f
        }
    }

    private fun WorldRenderEnvironment.renderCircles(
        range: Float,
        pulseOffset: Float,
        distance: Float?,
        hasTarget: Boolean
    ) {
        drawRangeCircle(range + pulseOffset, getColor(distance, range))

        if (wallRangeColor.a > 0 && ModuleKillAura.range.interactionThroughWallsRange < range) {
            val color = if (hasTarget) {
                wallRangeColor.fade(1.5f)
            } else {
                wallRangeColor
            }
            drawRangeCircle(ModuleKillAura.range.interactionThroughWallsRange + pulseOffset * 0.5f, color, 80)
        }

        if (scanRangeColor.a > 0) {
            drawRangeCircle(range + 2.5f, scanRangeColor, 60)
        }

        if (opponentRangeColor.a > 0 && hasTarget) {
            drawRangeCircle(3f, opponentRangeColor, 100)
        }
    }

    private fun canRender(): Boolean {
        return !((hideWhenDead && player.isDeadOrDying) ||
            (hideWhenSpectator && player.isSpectator) ||
            (hideInVehicle && player.vehicle != null) ||
            (respectInventorySetting && !ModuleKillAura.ignoreOpenInventory &&
                (isInventoryOpen || mc.screen is ContainerScreen)))
    }

    private fun WorldRenderEnvironment.drawRangeCircle(radius: Float, color: Color4b, outlineAlpha: Int = 255) {
        drawGradientCircle(radius, 0f, color, Color4b.TRANSPARENT, noDepthTest = !canBeCovered)
        if (outline) {
            matrixStack.withPush {
                translate(0.0, 0.001, 0.0) // Slightly above the filled circle to prevent z-fighting
                drawCircleOutline(radius, outlineColor.alpha(outlineAlpha), noDepthTest = !canBeCovered)
            }
        }
    }

    private fun updateColorFactor(hasTarget: Boolean) {
        val target = if (hasTarget) 1f else 0f
        colorFactor = if (fadeAnimation) {
            Mth.lerp(fadeSpeed, colorFactor, target)
        } else {
            target
        }
    }

    private fun getColor(distance: Float?, range: Float): Color4b = when (colorMode) {
        ColorMode.RAINBOW -> rainbow(alpha = 0.5f)
        ColorMode.DISTANCE -> distance?.let {
            activeColor.interpolateTo(idleColor, (it / range).coerceIn(0f, 1f).toDouble())
        } ?: idleColor
        ColorMode.STATIC -> idleColor.interpolateTo(activeColor, colorFactor.toDouble())
    }

}
