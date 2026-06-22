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

package net.ccbluex.liquidbounce.features.module.modules.combat.elytratarget

import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.Vec3

@Suppress("unused")
internal enum class TargetRotatePosition(
    override val tag: String,
    val position: (LivingEntity) -> Vec3
) : Tagged {
    EYES("Eyes", { target ->
        target.eyePosition
    }),
    CENTER("Center", { target ->
        target.position().add(0.0, target.bbHeight / 2.0, 0.0)
    })
}
