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

import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleElytraRecast.shouldRecast
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.Items

/**
 * Elytra recast module
 *
 * Recasts elytra when holding the jump key
 *
 * @author Pivo1lovv
 */
object ModuleElytraRecast : ClientModule("ElytraRecast", ModuleCategories.MOVEMENT) {

    private val shouldRecast: Boolean
        get() {
            val itemStack = player.getItemBySlot(EquipmentSlot.CHEST)

            return !player.abilities.flying && !player.isPassenger && !player.onClimbable() &&
                !player.isInWater && !player.hasEffect(MobEffects.LEVITATION) &&
                itemStack.`is`(Items.ELYTRA) && !itemStack.nextDamageWillBreak() && mc.options.keyJump.isDown
        }

    /**
     * Recast elytra when [shouldRecast] says it should
     *
     * @return true if elytra was recast
     */
    fun recastElytra(): Boolean {
        if (shouldRecast) {
            player.startFallFlying()
            network.send(
                ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING)
            )
            return true
        }

        return false
    }

}
