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
package net.ccbluex.liquidbounce.addon

import net.ccbluex.liquidbounce.utils.block.SwingMode
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.combat.attackEntity
import net.ccbluex.liquidbounce.utils.combat.findEnemy
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.ccbluex.liquidbounce.utils.combat.shouldBeShown
import net.minecraft.world.entity.Entity

/**
 * Targeting as the user configured it with `.target` and the ClickGUI, plus attacking. To change how an
 * entity is classified, handle [net.ccbluex.liquidbounce.event.events.TagEntityEvent].
 */
object Combat {

    /** Not a friend, not a bot, of a kind the user wants attacked. */
    @JvmStatic
    fun shouldBeAttacked(entity: Entity): Boolean = entity.shouldBeAttacked()

    /** The same for ESPs and tracers, which usually include more. */
    @JvmStatic
    fun shouldBeShown(entity: Entity): Boolean = entity.shouldBeShown()

    /** The closest entity that [shouldBeAttacked] within [range] blocks, or null. */
    @JvmStatic
    fun findEnemy(range: Float): Entity? = Game.world.findEnemy(0f, range)

    /**
     * Attacks like a click would, firing [net.ccbluex.liquidbounce.event.events.AttackEntityEvent] first.
     *
     * @return false when the event was cancelled or the entity cannot be attacked
     */
    @JvmStatic
    @JvmOverloads
    fun attack(entity: Entity, swing: Boolean = true, keepSprint: Boolean = false): Boolean =
        attackEntity(entity, if (swing) SwingMode.DO_NOT_HIDE else SwingMode.HIDE_BOTH, keepSprint)

    /** Whether the client's combat modules have a target right now. */
    @JvmStatic
    val isInCombat: Boolean
        get() = CombatManager.isInCombat

    /** Keeps the client's combat modules from attacking for [ticks]. */
    @JvmStatic
    fun pauseCombat(ticks: Int) = CombatManager.pauseCombatForAtLeast(ticks)

    @JvmStatic
    fun pauseRotations(ticks: Int) = CombatManager.pauseRotationForAtLeast(ticks)

}
