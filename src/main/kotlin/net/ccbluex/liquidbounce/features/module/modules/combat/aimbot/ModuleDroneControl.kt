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
import net.ccbluex.liquidbounce.utils.entity.PositionExtrapolation
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryInfo
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3

object ModuleDroneControl : ClientModule("DroneControl", ModuleCategories.COMBAT) {

    private val rotations = tree(RotationsValueGroup(this))

    var screen: DroneControlScreen? = null

    override fun onEnabled() {
        screen = DroneControlScreen()

        mc.setScreen(screen)
    }

    override fun onDisabled() {
        if (mc.screen == screen) {
            mc.setScreen(null)
        }

        screen = null
    }

    var currentTarget: Pair<Entity, Vec3>? = null
    var mayShoot = false

    @Suppress("unused")
    private val repeatable = handler<GameTickEvent> {
        val currentRotation = currentTarget?.let { (entity, pos) ->
            SituationalProjectileAngleCalculator.calculateAngleFor(
                TrajectoryInfo.BOW_FULL_PULL,
                sourcePos = player.eyePosition,
                targetPosFunction = PositionExtrapolation.constant(pos),
                targetShape = entity.dimensions
            )
        }

        if (currentRotation != null) {
            RotationManager.setRotationTarget(
                rotation = currentRotation,
                valueGroup = rotations,
                priority = Priority.NORMAL,
                provider = ModuleDroneControl
            )
        }

        if (mayShoot) {
            interaction.releaseUsingItem(player)

            mayShoot = false
        } else {
            interaction.useItem(player, InteractionHand.MAIN_HAND)
        }
    }

}
