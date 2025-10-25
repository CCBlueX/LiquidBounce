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
 *
 */

package net.ccbluex.liquidbounce.features.module.modules.combat.aimbot.autobow

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.combat.aimbot.ModuleAutoBow
import net.ccbluex.liquidbounce.render.renderEnvironmentForGUI
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.projectiles.SituationalProjectileAngleCalculator
import net.ccbluex.liquidbounce.utils.combat.TargetPriority
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.render.OverlayTargetRenderer
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryData
import net.minecraft.item.BowItem
import net.minecraft.item.TridentItem

/**
 * Automatically shoots with your bow when you aim correctly at an enemy or when the bow is fully charged.
 */
object AutoBowAimbotFeature : ToggleableConfigurable(ModuleAutoBow, "BowAimbot", true) {

    // Target
    val targetTracker = TargetTracker(TargetPriority.DISTANCE)

    // Rotation
    val rotationConfigurable = RotationsConfigurable(this)

    init {
        tree(targetTracker)
        tree(rotationConfigurable)
    }

    private val targetRenderer = tree(OverlayTargetRenderer(ModuleAutoBow))

    @Suppress("unused")
    private val tickRepeatable = tickHandler {
        targetTracker.reset()

        // Should check if player is using bow
        val activeItem = player.activeItem?.item
        if (activeItem !is BowItem && activeItem !is TridentItem) {
            return@tickHandler
        }

        val projectileInfo = TrajectoryData.getRenderedTrajectoryInfo(
            player,
            activeItem,
            true
        ) ?: return@tickHandler

        var rotation: Rotation? = null
        targetTracker.selectFirst { enemy ->
            rotation = SituationalProjectileAngleCalculator.calculateAngleForEntity(projectileInfo, enemy)
            rotation != null
        } ?: return@tickHandler

        RotationManager.setRotationTarget(
            rotation!!,
            priority = Priority.IMPORTANT_FOR_USAGE_1,
            provider = ModuleAutoBow,
            configurable = rotationConfigurable
        )
    }

    @Suppress("unused")
    private val renderHandler = handler<OverlayRenderEvent> { event ->
        val target = targetTracker.target ?: return@handler

        renderEnvironmentForGUI(event) {
            targetRenderer.render(this, target, event.tickDelta)
        }
    }

}
