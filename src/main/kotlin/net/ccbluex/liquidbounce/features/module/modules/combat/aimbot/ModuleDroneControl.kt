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

package net.ccbluex.liquidbounce.features.module.modules.combat.aimbot

import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.projectiles.SituationalProjectileAngleCalculator
import net.ccbluex.liquidbounce.utils.entity.ConstantPositionExtrapolation
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryInfo
import net.minecraft.entity.Entity
import net.minecraft.util.Hand
import net.minecraft.util.math.Vec3d

object ModuleDroneControl : ClientModule("DroneControl", Category.COMBAT) {

    private val rotationsConfigurable = tree(RotationsConfigurable(this))

    var screen: DroneControlScreen? = null

    override fun onEnabled() {
        screen = DroneControlScreen()

        mc.setScreen(screen)
    }

    override fun onDisabled() {
        if (mc.currentScreen == screen) {
            mc.setScreen(null)
        }

        screen = null
    }

    var currentTarget: Pair<Entity, Vec3d>? = null
    var mayShoot = false

    private val repeatable = tickHandler {
        val currentRotation = currentTarget?.let { (entity, pos) ->
            SituationalProjectileAngleCalculator.calculateAngleFor(
                TrajectoryInfo.BOW_FULL_PULL,
                sourcePos = player.eyePos,
                targetPosFunction = ConstantPositionExtrapolation(pos),
                targetShape = entity.dimensions
            )
        }

        if (currentRotation != null) {
            RotationManager.setRotationTarget(
                rotation = currentRotation,
                configurable = rotationsConfigurable,
                priority = Priority.NORMAL,
                provider = ModuleDroneControl
            )
        }

        if (mayShoot) {
            interaction.stopUsingItem(player)

            mayShoot = false
        } else {
            interaction.interactItem(player, Hand.MAIN_HAND)
        }
    }

}
