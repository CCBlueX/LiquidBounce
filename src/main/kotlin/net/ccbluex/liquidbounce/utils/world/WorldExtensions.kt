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

package net.ccbluex.liquidbounce.utils.world

import net.minecraft.world.attribute.BedRule
import net.minecraft.world.attribute.EnvironmentAttributes
import net.minecraft.world.level.Level

/**
 * @return if water and ice evaporates in this world (e.g. nether)
 */
val Level.waterEvaporates: Boolean
    get() = this.environmentAttributes().getDimensionValue(EnvironmentAttributes.WATER_EVAPORATES)

val Level.bedRule: BedRule
    get() = this.environmentAttributes().getDimensionValue(EnvironmentAttributes.BED_RULE)

val Level.respawnAnchorWorks: Boolean
    get() = this.environmentAttributes().getDimensionValue(EnvironmentAttributes.RESPAWN_ANCHOR_WORKS)
