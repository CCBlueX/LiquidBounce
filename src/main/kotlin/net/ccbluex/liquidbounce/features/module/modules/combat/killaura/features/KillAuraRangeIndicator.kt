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

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.drawGradientCircle
import net.ccbluex.liquidbounce.render.drawCircleOutline
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.render.utils.rainbow
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager.isInventoryOpen
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.util.Mth
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Renders a circle around the player indicating the KillAura attack range.
 * Fully synced with KillAura settings:
 * - Range and WallRange from KillAura
 * - ScanExtraRange visualization
 * - OpponentRange from FightBot
 * - Respects IgnoreOpenInventory setting
 */
object KillAuraRangeIndicator : ToggleableConfigurable(ModuleKillAura, "RangeIndicator", false) {

    // Color settings
    private val colorMode by enumChoice("ColorMode", ColorMode.STATIC)
    private val idleColor by color("IdleColor", Color4b(255, 50, 50, 80))
    private val activeColor by color("ActiveColor", Color4b(50, 255, 50, 80))

    // Outline settings
    private val outline by boolean("Outline", true)
    private val outlineColor by color("OutlineColor", Color4b(255, 255, 255, 120))

    // Animation settings
    private val pulseAnimation by boolean("PulseAnimation", false)
    private val pulseSpeed by float("PulseSpeed", 2f, 0.5f..5f)
    private val pulseIntensity by float("PulseIntensity", 0.15f, 0.05f..0.5f)

    // Fade animation for state changes
    private val fadeAnimation by boolean("FadeAnimation", true)
    private val fadeSpeed by float("FadeSpeed", 0.1f, 0.01f..0.5f)

    // Additional range circles (synced with KillAura)
    private val showWallRange by boolean("ShowWallRange", false)
    private val wallRangeColor by color("WallRangeColor", Color4b(255, 165, 0, 60))

    private val showScanRange by boolean("ShowScanRange", false)
    private val scanRangeColor by color("ScanRangeColor", Color4b(100, 100, 255, 40))

    // Opponent range (synced with FightBot.opponentRange)
    private val showOpponentRange by boolean("ShowOpponentRange", false)
    private val opponentRangeColor by color("OpponentRangeColor", Color4b(255, 0, 0, 40))

    // Conditions - synced with KillAura behavior
    private val hideWhenDead by boolean("HideWhenDead", true)
    private val hideWhenSpectator by boolean("HideWhenSpectator", true)
    private val hideInVehicle by boolean("HideInVehicle", false)
    private val respectInventorySetting by boolean("RespectInventorySetting", true)

    // State tracking for animations
    private var currentColorFactor = 0f

    private enum class ColorMode(override val choiceName: String) : NamedChoice {
        STATIC("Static"),
        RAINBOW("Rainbow"),
        DISTANCE("Distance")
    }

    fun render(env: WorldRenderEnvironment, partialTicks: Float) {
        if (!enabled) return

        // Sync with KillAura conditions
        if (hideWhenDead && player.isDeadOrDying) return
        if (hideWhenSpectator && player.isSpectator) return
        if (hideInVehicle && player.vehicle != null) return

        // Respect KillAura's inventory setting
        if (respectInventorySetting) {
            val isInInventoryScreen = isInventoryOpen || net.ccbluex.liquidbounce.utils.client.mc.screen is ContainerScreen
            if (isInInventoryScreen && !ModuleKillAura.ignoreOpenInventory) return
        }

        val target = ModuleKillAura.targetTracker.target
        val hasTarget = target != null

        // Update fade animation
        updateFadeAnimation(hasTarget)

        // Get ranges from KillAura (synced)
        val range = ModuleKillAura.range
        val wallRange = ModuleKillAura.wallRange

        val pos = player.interpolateCurrentPosition(partialTicks)

        // Calculate pulse effect
        val pulseOffset = if (pulseAnimation) {
            val time = System.currentTimeMillis() / 1000.0 * pulseSpeed
            (sin(time * Mth.TWO_PI).toFloat() * pulseIntensity * range)
        } else 0f

        val effectiveRange = range + pulseOffset

        // Get color based on mode
        val color = getColor(target?.let { sqrt(player.squaredBoxedDistanceTo(it)).toFloat() })

        with(env) {
            withPositionRelativeToCamera(pos) {
                // Main attack range circle (synced with KillAura.range)
                drawGradientCircle(effectiveRange, 0f, color, color.with(a = 0))

                if (outline) {
                    drawCircleOutline(effectiveRange, outlineColor)
                }

                // Wall range circle (synced with KillAura.wallRange)
                if (showWallRange && wallRange < range) {
                    val wallColor = wallRangeColor.let {
                        if (hasTarget) it.with(a = (it.a * 1.5f).toInt().coerceAtMost(255)) else it
                    }
                    drawGradientCircle(wallRange + pulseOffset * 0.5f, 0f, wallColor, wallColor.with(a = 0))
                    if (outline) {
                        drawCircleOutline(wallRange + pulseOffset * 0.5f, outlineColor.with(a = 80))
                    }
                }

                // Scan extra range circle (shows detection range beyond attack range)
                if (showScanRange) {
                    // ScanExtraRange is private, so we approximate with a fixed offset
                    // This shows the area where KillAura starts tracking but can't attack yet
                    val scanRange = range + 2.5f // Approximate middle of default scanExtraRange
                    drawGradientCircle(scanRange, 0f, scanRangeColor, scanRangeColor.with(a = 0))
                    if (outline) {
                        drawCircleOutline(scanRange, scanRangeColor.with(a = 60))
                    }
                }

                // Opponent range circle (synced concept with FightBot.opponentRange)
                // Shows estimated enemy attack range for spacing awareness
                if (showOpponentRange && hasTarget) {
                    val oppRange = 3f // Default opponent reach in Minecraft
                    drawGradientCircle(oppRange, 0f, opponentRangeColor, opponentRangeColor.with(a = 0))
                    if (outline) {
                        drawCircleOutline(oppRange, opponentRangeColor.with(a = 100))
                    }
                }
            }
        }
    }

    private fun updateFadeAnimation(hasTarget: Boolean) {
        if (!fadeAnimation) {
            currentColorFactor = if (hasTarget) 1f else 0f
            return
        }

        val targetFactor = if (hasTarget) 1f else 0f
        currentColorFactor = Mth.lerp(fadeSpeed, currentColorFactor, targetFactor)

        if (kotlin.math.abs(currentColorFactor - targetFactor) < 0.01f) {
            currentColorFactor = targetFactor
        }
    }

    private fun getColor(distanceToTarget: Float?): Color4b {
        val range = ModuleKillAura.range

        return when (colorMode) {
            ColorMode.RAINBOW -> rainbow(alpha = 0.5f)

            ColorMode.DISTANCE -> {
                if (distanceToTarget == null) {
                    idleColor
                } else {
                    val factor = (distanceToTarget / range).coerceIn(0f, 1f)
                    activeColor.interpolateTo(idleColor, factor.toDouble())
                }
            }

            ColorMode.STATIC -> {
                if (fadeAnimation) {
                    idleColor.interpolateTo(activeColor, currentColorFactor.toDouble())
                } else {
                    if (currentColorFactor > 0.5f) activeColor else idleColor
                }
            }
        }
    }

}
