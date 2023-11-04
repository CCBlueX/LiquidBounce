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

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.combat.CpsScheduler
import net.minecraft.client.option.KeyBinding
import net.minecraft.util.hit.HitResult

/**
 * AutoClicker module
 *
 * Clicks automatically when holding down a mouse button.
 */

object ModuleAutoClicker : Module("AutoClicker", Category.COMBAT) {

    object Left : ToggleableConfigurable(this, "Left", true) {
        val cps by intRange("CPS", 5..8, 1..20)
        val cooldown by boolean("Cooldown", true)
    }

    object Right : ToggleableConfigurable(this, "Right", false) {
        val cps by intRange("CPS", 5..8, 1..20)
    }

    init {
        tree(Left)
        tree(Right)
    }

    val cpsScheduler = tree(CpsScheduler())

    val attack: Boolean
        get() = mc.options.attackKey.isPressed

    val use: Boolean
        get() = mc.options.useKey.isPressed

    val shouldTargetBlock: Boolean
        get() = player.abilities.creativeMode || mc.crosshairTarget?.type != HitResult.Type.BLOCK

    val tickHandler = repeatable {
        Left.let {
            repeat(cpsScheduler.clicks({ it.enabled && attack && shouldTargetBlock &&
                (!it.cooldown || player.getAttackCooldownProgress(0.0f) >= 1.0f) }, it.cps)) {
                KeyBinding.onKeyPressed(mc.options.attackKey.boundKey)
            }
        }

        Right.let {
            repeat(cpsScheduler.clicks({ it.enabled && use }, it.cps)) {
                KeyBinding.onKeyPressed(mc.options.useKey.boundKey)
            }
        }
    }
}
