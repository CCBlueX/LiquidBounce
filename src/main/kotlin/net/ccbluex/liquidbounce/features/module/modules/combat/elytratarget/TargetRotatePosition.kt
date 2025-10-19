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

package net.ccbluex.liquidbounce.features.module.modules.combat.elytratarget

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.minecraft.entity.LivingEntity
import net.minecraft.util.math.Vec3d

@Suppress("unused")
internal enum class TargetRotatePosition(
    override val choiceName: String,
    val position: (LivingEntity) -> Vec3d
) : NamedChoice {
    EYES("Eyes", { target ->
        target.eyePos
    }),
    CENTER("Center", { target ->
        target.pos.add(0.0, target.height / 2.0, 0.0)
    })
}
