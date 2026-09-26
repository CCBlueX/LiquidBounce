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

package net.ccbluex.liquidbounce.features.module.modules.render.customambience.worldeffects

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.features.module.modules.render.customambience.ModuleCustomAmbience
import net.ccbluex.liquidbounce.features.module.modules.render.customambience.worldeffects.modes.WorldParticlesSimple
import net.ccbluex.liquidbounce.utils.collection.ExpiringList.Companion.ExpiringList
import net.minecraft.world.phys.Vec3

object WorldParticles : ToggleableValueGroup(ModuleCustomAmbience, "WorldParticles", false) {

    val coords = ExpiringList<Vec3>()

    val modes = choices("Mode", 0) {
        arrayOf(
            WorldParticlesSimple,
            // TODO: Create WireFrame mode
        )
    }.apply { onChanged { coords.clear() } }

    override fun onDisabled() = coords.clear()

}
