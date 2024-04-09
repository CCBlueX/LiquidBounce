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
package net.ccbluex.liquidbounce.script.bindings.api

import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.aiming.Rotation
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.raytraceBox
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.entity.eyes
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.entity.Entity
import kotlin.math.sqrt

/**
 * A collection of useful rotation utilities for the ScriptAPI.
 * This SHOULD not be changed in a way that breaks backwards compatibility.
 *
 * This is a singleton object, so it can be accessed from the script API like this:
 * ```js
 * api.rotationUtil.newRaytracedRotationEntity(entity, 4.2, 0.0)
 * rotationUtil.newRotationEntity(entity)
 * rotationUtil.aimAtRotation(rotation, true)
 * ```
 */
object JsRotationUtil {

    /**
     * Creates a new [Rotation] from [entity]'s bounding box.
     * This uses raytracing, so it's guaranteed to be the best spot.
     *
     * It has a performance impact, so it's recommended to use [newRotationEntity] if you don't need the best spot.
     */
    @JvmName("newRaytracedRotationEntity")
    fun newRaytracedRotationEntity(entity: Entity, range: Double, throughWallsRange: Double): Rotation? {
        val box = entity.boundingBox

        // Finds the best spot (and undefined if no spot was found)
        val (rotation, _) = raytraceBox(
            mc.player!!.eyes,
            box,
            range = sqrt(range),
            wallsRange = throughWallsRange
        ) ?: return null

        return rotation
    }

    /**
     * Creates a new [Rotation] from [entity]'s bounding box.
     * This uses no raytracing, so it's not guaranteed to be the best spot.
     * It will aim at the center of the bounding box.
     *
     * It has almost zero performance impact, so it's recommended to use this if you don't need the best spot.
     */
    @JvmName("newRotationEntity")
    fun newRotationEntity(entity: Entity) = RotationManager.makeRotation(entity.boundingBox.center, mc.player!!.eyes)

    /**
     * Aims at the given [rotation] using the in-built RotationManager.
     *
     * @param rotation The rotation to aim at.
     * @param fixVelocity Whether to fix the player's velocity.
     *   This means bypassing anti-cheat checks for aim-related movement.
     */
    @JvmName("aimAtRotation")
    fun aimAtRotation(rotation: Rotation, fixVelocity: Boolean) {
        RotationManager.aimAt(
            rotation,
            configurable = RotationsConfigurable(
                object : Listenable { }
            ).also {
                it.fixVelocity = fixVelocity
            }, priority = Priority.NORMAL, provider = Module("ScriptAPI", Category.MISC)
        )
    }

}
