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
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.item.isGlider
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.EquipmentSlot

/**
 * Elytra recast module
 *
 * Recasts elytra when holding the jump key, or when the movement input jumps
 *
 * @author Pivo1lovv
 */
object ModuleElytraRecast : ClientModule("ElytraRecast", ModuleCategories.MOVEMENT) {

    private val canOpenElytra: Boolean
        get() {
            val itemStack = player.getItemBySlot(EquipmentSlot.CHEST)

            return !player.onGround() && !player.isFallFlying && !player.abilities.flying && !player.isPassenger &&
                !player.onClimbable() && !player.isInLiquid && !player.hasEffect(MobEffects.LEVITATION) &&
                itemStack.isGlider && !itemStack.nextDamageWillBreak()
        }

    /**
     * The game opens the elytra only on a fresh jump press in the air, so a held jump is let go for one tick.
     * Letting go also allows the next jump right on landing.
     */
    @Suppress("unused")
    private val inputHandler = handler<MovementInputEvent> { event ->
        if (event.jump && player.input.keyPresses.jump() && canOpenElytra) {
            event.jump = false
        }
    }

}
