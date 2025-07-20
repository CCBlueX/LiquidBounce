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
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket

/**
 * Elytra recast module
 *
 * Recasts elytra when holding the jump key
 *
 * @author Pivo1lovv
 */
object ModuleElytraRecast : ClientModule("ElytraRecast", Category.MOVEMENT) {

    init {
        enableLock()
    }

    private val shouldRecast: Boolean
        get() {
            val itemStack = player.getEquippedStack(EquipmentSlot.CHEST)

            return !player.abilities.flying && !player.hasVehicle() && !player.isClimbing &&
                !player.isTouchingWater && !player.hasStatusEffect(StatusEffects.LEVITATION) &&
                itemStack.isOf(Items.ELYTRA) && !itemStack.willBreakNextUse() && mc.options.jumpKey.isPressed
        }

    /**
     * Recast elytra when [shouldRecast] says it should
     *
     * @return true if elytra was recast
     */
    fun recastElytra(): Boolean {
        if (shouldRecast) {
            player.startGliding()
            network.sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.START_FALL_FLYING))
            return true
        }

        return false
    }

}
