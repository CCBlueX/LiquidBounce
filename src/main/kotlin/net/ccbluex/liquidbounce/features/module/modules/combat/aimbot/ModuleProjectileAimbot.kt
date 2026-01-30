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

package net.ccbluex.liquidbounce.features.module.modules.combat.aimbot

import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsValueGroup
import net.ccbluex.liquidbounce.utils.aiming.projectiles.SituationalProjectileAngleCalculator
import net.ccbluex.liquidbounce.utils.combat.TargetSelector
import net.ccbluex.liquidbounce.utils.entity.handItems
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryData

object ModuleProjectileAimbot : ClientModule("ProjectileAimbot", ModuleCategories.COMBAT) {

    private val targetSelector = TargetSelector()
    private val rotations = RotationsValueGroup(this)

    init {
        tree(targetSelector)
        tree(rotations)
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        val target = targetSelector.targets().firstOrNull() ?: return@handler

        val rotation = player.handItems.firstNotNullOfOrNull {
            val (trajectory, _) = TrajectoryData.getRenderedTrajectoryInfo(
                player,
                it,
                true
            ) ?: return@firstNotNullOfOrNull null

            SituationalProjectileAngleCalculator.calculateAngleForEntity(trajectory, target)
        } ?: return@handler

        RotationManager.setRotationTarget(
            rotation,
            considerInventory = false,
            rotations,
            Priority.IMPORTANT_FOR_USAGE_1,
            ModuleProjectileAimbot
        )
    }



}
