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
package net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.slowness

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.ModuleNoSlow
import net.minecraft.resources.Identifier
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes

internal object NoSlowSlowness : ToggleableValueGroup(ModuleNoSlow, "Slowness", true) {
    val multiplier by float("PerLevelMultiplier", 0f, 0f..0.15f)

    @Suppress("unused")
    val tickHandler = tickHandler { setSlownessMultiplier(multiplier) }

    private fun setSlownessMultiplier(multiplier: Float) {
        val slowness = player.activeEffectsMap[MobEffects.SLOWNESS]?.amplifier ?: return
        player.attributes.getInstance(Attributes.MOVEMENT_SPEED)?.addOrUpdateTransientModifier(
            AttributeModifier(
                Identifier.parse("effect.slowness"),
                -multiplier * (slowness + 1.0),
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            )
        )
    }

    override fun onDisabled() {
        setSlownessMultiplier(0.15f)
    }
}
