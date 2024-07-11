/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.utils.aiming

import net.ccbluex.liquidbounce.utils.aiming.angleSmooth.AngleSmoothMode
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.minecraft.entity.Entity
import net.minecraft.util.math.Vec3d

/**
 * An aim plan is a plan to aim at a certain rotation.
 * It is being used to calculate the next rotation to aim at.
 *
 * @param rotation The rotation we want to aim at.
 * @param angleSmooth The mode of the smoother.
 */
@Suppress("LongParameterList")
class AimPlan(
    val rotation: Rotation,
    val vec3d: Vec3d? = null,
    val entity: Entity? = null,
    /**
     * If we do not want to smooth the angle, we can set this to null.
     */
    val angleSmooth: AngleSmoothMode?,
    val ticksUntilReset: Int,
    /**
     * The reset threshold defines the threshold at which we are going to reset the aim plan.
     * The threshold is being calculated by the distance between the current rotation and the rotation we want to aim.
     */
    val resetThreshold: Float,
    /**
     * Consider if the inventory is open or not. If the inventory is open, we might not want to continue updating.
     */
    val considerInventory: Boolean,
    val applyVelocityFix: Boolean,
    val changeLook: Boolean
) {

    /**
     * Calculates the next rotation to aim at.
     * [fromRotation] is the current rotation or rather last rotation we aimed at. It is being used to calculate the
     * next rotation.
     *
     * We might even return null if we do not want to aim at anything yet.
     */
    fun nextRotation(fromRotation: Rotation, isResetting: Boolean): Rotation {
        val angleSmooth = angleSmooth ?: return rotation

        if (isResetting) {
            return angleSmooth.limitAngleChange(fromRotation, mc.player!!.rotation)
        }

        return angleSmooth.limitAngleChange(fromRotation, rotation, vec3d, entity)
    }

}
