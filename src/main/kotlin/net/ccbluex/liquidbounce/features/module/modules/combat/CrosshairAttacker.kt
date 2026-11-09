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

import net.ccbluex.fastutil.enumSetOf
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.SprintEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickUntil
import net.ccbluex.liquidbounce.event.waitTicks
import net.ccbluex.liquidbounce.features.module.modules.combat.criticals.ModuleCriticals.CriticalsSelectionMode
import net.ccbluex.liquidbounce.utils.clicking.Clicker
import net.ccbluex.liquidbounce.utils.client.interaction
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.item.WeaponType
import net.ccbluex.liquidbounce.utils.kotlin.matchesAny
import net.minecraft.client.KeyMapping
import net.minecraft.world.phys.EntityHitResult

/**
 * Clicks the attack key on the crosshair target for [ModuleAutoClicker] and [ModuleTriggerBot].
 *
 * Its settings are added to [parent], after the ones [parent] declared before creating it.
 */
class CrosshairAttacker<T>(
    parent: T,
    private val clicker: Clicker<T>,
    private val wantsToAttack: () -> Boolean,
) where T : ValueGroup, T : EventListener {

    private val onItemUse by parent.enumChoice("OnItemUse", Use.WAIT)
    private val weapon by parent.multiEnumChoice("Weapon", enumSetOf(WeaponType.ANY), canBeNone = false)
    private val criticalsSelectionMode by parent.enumChoice("Criticals", CriticalsSelectionMode.SMART)
    private val delayPostStopUse by parent.int("DelayPostStopUse", 0, 0..20, "ticks")

    val isAttacking: Boolean
        get() = wantsToAttack() && weapon.matchesAny(player.mainHandItem)

    @Suppress("unused")
    private val sprintHandler = parent.handler<SprintEvent> { event ->
        if (event.source == SprintEvent.Source.MOVEMENT_TICK || event.source == SprintEvent.Source.INPUT) {
            if (!isAttacking) {
                return@handler
            }

            val target = mc.hitResult as? EntityHitResult ?: return@handler
            if (criticalsSelectionMode.shouldStopSprinting(clicker, target.entity)) {
                event.sprint = false
            }
        }
    }

    /**
     * Clicks as often as [clicker] plans for this tick, unless a critical hit is still pending.
     *
     * @return false when the tick went to waiting for or stopping an item use instead
     */
    suspend fun attack(): Boolean {
        val crosshairTarget = mc.hitResult
        if (crosshairTarget is EntityHitResult) {
            ModuleAutoWeapon.onTarget(crosshairTarget.entity)

            if (!criticalsSelectionMode.isCriticalHit()) {
                return true
            }
        }

        if (player.isUsingItem && encounterItemUse()) {
            return false
        }

        clicker.click {
            KeyMapping.click(mc.options.keyAttack.key)
            true
        }
        return true
    }

    private suspend fun encounterItemUse(): Boolean {
        return when (onItemUse) {
            Use.WAIT -> {
                tickUntil { !player.isUsingItem }
                waitTicks(delayPostStopUse)
                true
            }

            Use.STOP -> {
                interaction.releaseUsingItem(player)
                waitTicks(delayPostStopUse)
                true
            }

            Use.IGNORE -> false
        }
    }

    private enum class Use(override val tag: String) : Tagged {
        WAIT("Wait"),
        STOP("Stop"),
        IGNORE("Ignore")
    }

}
