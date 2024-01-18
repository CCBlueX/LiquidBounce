/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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

import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.event.Sequence
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.combat.ClickScheduler
import net.ccbluex.liquidbounce.utils.combat.attack
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.minecraft.item.AxeItem
import net.minecraft.item.SwordItem
import net.minecraft.util.hit.EntityHitResult

/**
 * Trigger module
 *
 * Automatically attacks enemy on your crosshair.
 */
object ModuleTrigger : Module("Trigger", Category.COMBAT) {

    // CPS means clicks per second
    private val clickScheduler = tree(ClickScheduler(ModuleTrigger, true))
    // TODO: Implement FailSwing option
    private val onItemUse by enumChoice("OnItemUse", Use.WAIT, Use.values())
    private val weapon by enumChoice("Weapon", Weapon.ANY, Weapon.values())
    private val delayPostStopUse by int("DelayPostStopUse", 0, 0..20)

    val repeatable = repeatable {
        val crosshair = mc.crosshairTarget

        if (crosshair is EntityHitResult && crosshair.entity.shouldBeAttacked()) {
            if (!clickScheduler.goingToClick || !isWeaponSelected() || ModuleCriticals.shouldWaitForCrit()) {
                return@repeatable
            }

            if (player.usingItem) {
                val encounterItemUse = encounterItemUse()

                if (encounterItemUse) {
                    return@repeatable
                }
            }

            clickScheduler.clicks {
                crosshair.entity.attack(true)

                true
            }
        }
    }

    private fun isWeaponSelected(): Boolean {
        val item = player.mainHandStack.item

        return when (weapon) {
            Weapon.SWORD -> item is SwordItem
            Weapon.AXE -> item is AxeItem
            Weapon.BOTH -> item is SwordItem || item is AxeItem
            Weapon.ANY -> true
        }
    }

    private suspend fun Sequence<*>.encounterItemUse(): Boolean {
        return when (onItemUse) {
            Use.WAIT -> {
                this.waitUntil { !player.isUsingItem }

                if (delayPostStopUse > 0) {
                    waitTicks(delayPostStopUse)
                }

                true
            }

            Use.STOP -> {
                interaction.stopUsingItem(player)

                if (delayPostStopUse > 0) {
                    waitTicks(delayPostStopUse)
                }

                true
            }

            Use.IGNORE -> false
        }
    }

    enum class Weapon(override val choiceName: String) : NamedChoice {
        SWORD("Sword"), AXE("Axe"), BOTH("Both"), ANY("Any")
    }

    enum class Use(override val choiceName: String) : NamedChoice {
        WAIT("Wait"), STOP("Stop"), IGNORE("Ignore")
    }
}
