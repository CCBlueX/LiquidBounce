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

@file:Suppress("FunctionName", "NOTHING_TO_INLINE")

package net.ccbluex.liquidbounce.additions

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.projectile.FireworkRocketEntity

interface FireworkRocketEntityAddition {
    fun `liquidbounce$getShooter`(): LivingEntity?
}

internal inline val FireworkRocketEntity.shooter: LivingEntity?
    get() = (this as FireworkRocketEntityAddition).`liquidbounce$getShooter`()
