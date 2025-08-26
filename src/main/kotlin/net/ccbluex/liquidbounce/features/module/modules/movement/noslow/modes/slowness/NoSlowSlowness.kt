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
package net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.slowness

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.ModuleNoSlow
import net.minecraft.entity.attribute.EntityAttributeModifier
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.util.Identifier

internal object NoSlowSlowness : ToggleableConfigurable(ModuleNoSlow, "Slowness", true) {
    val multiplier by float("PerLevelMultiplier", 0f, 0f..0.15f)

    @Suppress("unused")
    val tickHandler = tickHandler { setSlownessMultiplier(multiplier) }

    private fun setSlownessMultiplier(multiplier: Float) {
        val slowness = player.activeStatusEffects[StatusEffects.SLOWNESS]?.amplifier ?: return
        player.attributes.getCustomInstance(EntityAttributes.MOVEMENT_SPEED)?.updateModifier(
            EntityAttributeModifier(
                Identifier.of("effect.slowness"),
                -multiplier * (slowness + 1.0),
                EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            )
        )
    }

    override fun onDisabled() {
        setSlownessMultiplier(0.15f)
    }
}
