/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2022 CCBlueX
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

import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.entity.moving
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import java.util.*
import kotlin.math.abs

val AIMING_PATTERNS = arrayOf(
    GaussianPattern
)

interface Pattern {
    fun update()
    fun spot(box: Box): Vec3d
}

/**
 * A very basic human-like rotation pattern
 *
 * So the idea is that, when a player is standing still he is unlikely to move his mouse.
 * It is more likely to choose a new spot when a player is moving.
 * Is it also very unlikely to snap to a new spot. So I've implemented a speed-limit on how fast a new spot is being applied.
 *
 * We're also using the gaussian algorithm to make the rotations more human-like.
 *
 * This is of course more like a random-set of thoughts about values of a human-like pattern. Not something very.
 * Might train a data-set in the future.
 *
 * Tested on:
 * AAC
 */
object GaussianPattern : Pattern {

    private val random = Random()

    private var spot = gaussianVec
    private var nextSpot = gaussianVec

    private const val STANDING_CHANCE: Double = 0.91
    private const val MOVING_CHANCE: Double = 0.32

    private const val SPEED_HORIZONTAL_LIMITER: Double = 0.04
    private const val SPEED_VERTICAL_LIMITER: Double = 0.0787

    private val randomGaussian: Double
        get() = abs(random.nextGaussian() % 1.0)

    private val gaussianVec: Vec3d
        get() = Vec3d(randomGaussian, 1 - randomGaussian, randomGaussian)

    override fun update() {
        // Chance of generating new spot
        val newSpotChance = if (mc.player?.moving == false) {
            STANDING_CHANCE
        } else {
            MOVING_CHANCE
        }

        if (random.nextDouble() > newSpotChance) {
            nextSpot = gaussianVec
        }

        // Check if spot has to be moved
        if (spot != nextSpot) {
            val xSpeed = randomGaussian * SPEED_HORIZONTAL_LIMITER
            val ySpeed = randomGaussian * SPEED_VERTICAL_LIMITER
            val zSpeed = randomGaussian * SPEED_HORIZONTAL_LIMITER

            val diffX = (nextSpot.x - spot.x)
            spot.x += diffX.coerceIn(-xSpeed, xSpeed)

            val diffY = (nextSpot.y - spot.y)
            spot.y += diffY.coerceIn(-ySpeed, ySpeed)

            val diffZ = (nextSpot.z - spot.z)
            spot.z += diffZ.coerceIn(-zSpeed, zSpeed)
        }
    }

    override fun spot(box: Box) = Vec3d(
        box.minX + ((box.maxX - box.minX) * spot.x),
        box.minY + ((box.maxY - box.minY) * spot.y),
        box.minZ + ((box.maxZ - box.minZ) * spot.z)
    )

}
