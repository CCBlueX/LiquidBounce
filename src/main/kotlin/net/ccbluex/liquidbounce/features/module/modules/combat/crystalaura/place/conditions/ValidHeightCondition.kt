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
import net.minecraft.util.math.BlockPos

/**
 * Crystals only deal damage if they are placed below the target, because obsidian or bedrock base blocks will block the
 * explosion otherwise.
 *
 * If we know it's above the target, why calculate everything for the position?
 */
object ValidHeightCondition : PlacementCondition {

    override fun isValid(context: PlacementContext, cache: CandidateCache, candidate: BlockPos.Mutable): Boolean {
        return candidate.y.toDouble() + 1.0 < context.target.boundingBox.maxY
    }

}
