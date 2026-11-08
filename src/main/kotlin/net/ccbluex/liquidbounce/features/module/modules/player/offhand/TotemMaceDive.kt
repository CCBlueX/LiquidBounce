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
package net.ccbluex.liquidbounce.features.module.modules.player.offhand

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.ccbluex.liquidbounce.utils.math.horizontalDistanceTo
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.Items

/**
 * Wants a totem while an enemy holding a mace falls toward the player.
 */
internal object TotemMaceDive : ToggleableValueGroup(Totem, "MaceDive", false) {

    private val horizontalRange by float("HorizontalRange", 4f, 1f..10f)
    private val minFallSpeed by float("MinFallSpeed", 0.3f, 0.1f..2f, "b/t")

    fun isDiving() = enabled && world.entitiesForRendering().any { entity ->
        entity is LivingEntity &&
            entity.mainHandItem.`is`(Items.MACE) &&
            entity.y > player.y + 1.0 &&
            entity.yo - entity.y >= minFallSpeed &&
            entity.position().horizontalDistanceTo(player.position()) <= horizontalRange &&
            entity.shouldBeAttacked()
    }

}
