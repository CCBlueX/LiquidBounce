/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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

import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.combat.CpsScheduler
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.minecraft.util.Hand
import net.minecraft.util.hit.EntityHitResult

/**
 * Trigger module
 *
 * Automatically attacks enemy on your crosshair.
 */
object ModuleTrigger : Module("Trigger", Category.COMBAT) {

    // CPS means clicks per second
    val cps by intRange("CPS", 5..8, 1..20)
    val cooldown by boolean("Cooldown", true)

    private val cpsTimer = CpsScheduler()

    val repeatable = repeatable {
        val crosshair = mc.crosshairTarget

        if (crosshair is EntityHitResult && crosshair.entity.shouldBeAttacked()) {
            val clicks = cpsTimer.clicks(condition = { !cooldown || player.getAttackCooldownProgress(0.0f) >= 1.0f }, cps)

            repeat(clicks) {
                interaction.attackEntity(player, crosshair.entity)
                player.swingHand(Hand.MAIN_HAND)
            }
        }
    }

}
