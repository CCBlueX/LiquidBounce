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

package net.ccbluex.liquidbounce.utils.aiming.point.preference

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.utils.clicking.Clicker
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.math.plus
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d

@Suppress("unused")
enum class PreferredBoxPoint(override val choiceName: String, val point: (Box, Vec3d) -> Vec3d) : NamedChoice {
    CLOSEST("Closest", { box, eyes ->
        Vec3d(
            eyes.x.coerceAtMost(box.maxX).coerceAtLeast(box.minX),
            eyes.y.coerceAtMost(box.maxY).coerceAtLeast(box.minY),
            eyes.z.coerceAtMost(box.maxZ).coerceAtLeast(box.minZ)
        )
    }),
    ASSIST("Assist", { box, eyes ->
        val vec3 = eyes + player.rotation.directionVector

        Vec3d(
            vec3.x.coerceAtMost(box.maxX).coerceAtLeast(box.minX),
            vec3.y.coerceAtMost(box.maxY).coerceAtLeast(box.minY),
            vec3.z.coerceAtMost(box.maxZ).coerceAtLeast(box.minZ)
        )
    }),
    STRAIGHT("Straight", { box, eyes ->
        Vec3d(
            box.center.x,
            eyes.y.coerceAtMost(box.maxY).coerceAtLeast(box.minY),
            box.center.z
        )
    }),
    CENTER("Center", { box, _ -> box.center }),
    RANDOM("Random", { box, _ ->
        Vec3d(
            Clicker.Companion.RNG.nextDouble(box.minX, box.maxX),
            Clicker.Companion.RNG.nextDouble(box.minY, box.maxY),
            Clicker.Companion.RNG.nextDouble(box.minZ, box.maxZ)
        )
    }),
    RANDOM_CENTER("RandomCenter", { box, _ ->
        Vec3d(
            Clicker.Companion.RNG.nextDouble(box.minX, box.maxX),
            box.center.y,
            Clicker.Companion.RNG.nextDouble(box.minZ, box.maxZ)
        )
    });
}
