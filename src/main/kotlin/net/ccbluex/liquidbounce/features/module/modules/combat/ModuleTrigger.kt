/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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
import net.ccbluex.liquidbounce.event.Event
import net.ccbluex.liquidbounce.event.Sequence
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.combat.CpsScheduler
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.minecraft.item.AxeItem
import net.minecraft.item.SwordItem
import net.minecraft.util.Hand
import net.minecraft.util.hit.EntityHitResult
import kotlin.random.Random

/**
 * Trigger module
 *
 * Automatically attacks enemy on your crosshair.
 */
object ModuleTrigger : Module("Trigger", Category.COMBAT) {

    // CPS means clicks per second
    val cps by intRange("CPS", 5..8, 1..20)
    val cooldown by boolean("Cooldown", true)
    val failRate by int("FailRate", 0, 0..100)
    val onItemUse by enumChoice("OnItemUse", Use.WAIT, Use.values())
    val weapon by enumChoice("Weapon", Weapon.ANY, Weapon.values())
    val delayPostStopUse by int("DelayPostStopUse", 0, 0..20)

    private val cpsTimer = tree(CpsScheduler())

    val repeatable = repeatable {
        val crosshair = mc.crosshairTarget

        if (crosshair is EntityHitResult && crosshair.entity.shouldBeAttacked()) {
            val clicks = cpsTimer.clicks(
                condition = { (!cooldown || player.getAttackCooldownProgress(0.0f) >= 1.0f) && isWeaponSelected() && !ModuleCriticals.shouldWaitForCrit() },
                cps
            )

            repeat(clicks) {
                if (player.usingItem) {
                    val encounterItemUse = this.encounterItemUse()

                    if (encounterItemUse) {
                        return@repeatable
                    }
                }

                if (failRate > 0 && failRate > Random.nextInt(100)) {
                    player.swingHand(Hand.MAIN_HAND)
                } else {
                    interaction.attackEntity(player, crosshair.entity)
                    player.swingHand(Hand.MAIN_HAND)
                }
            }
        }
    }

    fun isWeaponSelected(): Boolean {
        val item = player.mainHandStack.item

        return when (weapon) {
            Weapon.SWORD -> item is SwordItem
            Weapon.AXE -> item is AxeItem
            Weapon.BOTH -> item is SwordItem || item is AxeItem
            Weapon.ANY -> true
        }
    }

    private suspend fun <T : Event> Sequence<T>.encounterItemUse(): Boolean {
        val player = mc.player ?: return true

        return when (onItemUse) {
            Use.WAIT -> {
                this.waitUntil { !player.isUsingItem }

                if (delayPostStopUse > 0) {
                    wait(delayPostStopUse)
                }

                true
            }

            Use.STOP -> {
                interaction.stopUsingItem(player)

                if (delayPostStopUse > 0) {
                    wait(delayPostStopUse)
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
