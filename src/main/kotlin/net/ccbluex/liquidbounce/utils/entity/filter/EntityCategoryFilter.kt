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
package net.ccbluex.liquidbounce.utils.entity.filter

import net.ccbluex.fastutil.objectLinkedSetOf
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.utils.client.player
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.TamableAnimal
import net.minecraft.world.entity.player.Player
import java.util.SequencedSet

class EntityCategoryFilter : ValueGroup("Entities") {

    private val specificEntitiesBacking: SequencedSet<EntityType<*>> = objectLinkedSetOf(EntityType.PLAYER)
    val specificEntities by entityTypes("Entities", specificEntitiesBacking)

    fun matches(entity: Entity): Boolean {
        return entity.type in specificEntitiesBacking
    }

    fun matchesLiving(
        entity: LivingEntity,
        ignoreTeam: Boolean = true,
        ignoreInvisible: Boolean = false,
        ignoreSleeping: Boolean = false,
        ignoreCustomNamed: Boolean = false,
        ignorePassive: Boolean = true,
        ignoreTamed: Boolean = true,
    ): Boolean {
        if (!matches(entity)) return false
        if (ignoreTeam && entity.isAlliedTo(player)) return false
        if (ignoreInvisible && entity.isInvisible) return false
        if (ignoreSleeping && entity is Player && entity.isSleeping) return false
        if (ignoreCustomNamed && entity.customName != null) return false
        if (ignoreTamed && entity is TamableAnimal) {
            val owner = entity.ownerReference?.uuid
            if (owner != null && owner != player.uuid) return false
        }
        return true
    }
}
