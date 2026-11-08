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
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.events.EntityMarginEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.ccbluex.liquidbounce.utils.entity.wouldBlockHit
import net.ccbluex.liquidbounce.utils.math.sq
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity

/**
 * Hitbox module
 *
 * Enlarges the hitbox of other entities.
 */
object ModuleHitbox : ClientModule("Hitbox", ModuleCategories.COMBAT) {

    val size by float("Size", 0.1f, 0f..1f).apply { tagBy(this) }

    val applyToDebugHitbox by boolean("ApplyToDebugHitbox", true)

    /**
     * Apply to [net.minecraft.world.item.component.AttackRange.hitboxMargin]
     */
    val applyToComponent by boolean("ApplyToComponent", true)

    private val range by float("Range", 64f, 1f..64f, "blocks")
    private val throughWalls by boolean("ThroughWalls", true)

    /**
     * Also enlarges targets whose shield would block the hit.
     */
    private val ignoreShield by boolean("IgnoreShield", true)

    fun shouldEnlarge(entity: Entity): Boolean =
        entity.shouldBeAttacked()
            // Not [squaredBoxedDistanceTo], which asks for the margin again
            && entity.boundingBox.distanceToSqr(player.eyePosition) <= range.sq()
            && (ignoreShield || entity !is LivingEntity || !entity.wouldBlockHit)
            && (throughWalls || player.hasLineOfSight(entity))

    @Suppress("unused")
    private val marginHandler = handler<EntityMarginEvent> { event ->
        if (shouldEnlarge(event.entity)) {
            event.margin = size
        }
    }

}
