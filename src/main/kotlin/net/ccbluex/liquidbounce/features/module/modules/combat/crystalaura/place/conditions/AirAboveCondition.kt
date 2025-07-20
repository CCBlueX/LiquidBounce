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
package net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.place.conditions

import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.place.CandidateCache
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.place.PlacementCondition
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.place.PlacementContext
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.place.SubmoduleCrystalPlacer
import net.ccbluex.liquidbounce.utils.block.getState
import net.minecraft.util.math.BlockPos

/**
 * In 1.13+ crystals need one block air above to be placed.
 */
object AirAboveCondition : PlacementCondition {

    override fun isValid(context: PlacementContext, cache: CandidateCache, candidate: BlockPos.Mutable): Boolean {
        return cache.up.getState()!!.isAir
    }

}

/**
 * In 1.12.2 crystals need two blocks air above to be placed.
 */
object AirOldVersionCondition : PlacementCondition {

    override fun isValid(context: PlacementContext, cache: CandidateCache, candidate: BlockPos.Mutable): Boolean {
        return !SubmoduleCrystalPlacer.oldVersion || candidate.up(2).getState()!!.isAir
    }

}
