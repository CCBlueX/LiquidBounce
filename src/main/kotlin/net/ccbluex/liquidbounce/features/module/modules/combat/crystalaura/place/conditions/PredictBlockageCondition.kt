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

import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.PredictFeature
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.place.CandidateCache
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.place.PlacementCondition
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.place.PlacementContext
import net.minecraft.util.math.BlockPos

/**
 * Uses the prediction to check if the future crystal will be blocked by players in the next ticks.
 */
object PredictBlockageCondition : PlacementCondition {

    override fun isValid(context: PlacementContext, cache: CandidateCache, candidate: BlockPos.Mutable): Boolean {
        val up = cache.up
        return !PredictFeature.willBeBlocked(
            context.expectedCrystal.offset(up.x.toDouble(), up.y.toDouble(), up.z.toDouble()),
            context.target,
            !cache.canPlace
        )
    }

}
