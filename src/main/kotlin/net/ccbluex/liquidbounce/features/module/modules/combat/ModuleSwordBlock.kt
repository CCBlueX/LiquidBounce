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
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.KillAuraAutoBlock
import net.ccbluex.liquidbounce.utils.item.isSword
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ShieldItem

/**
 * This module allows the user to block with swords. This makes sense to be used on servers with ViaVersion.
 */
object ModuleSwordBlock : ClientModule("SwordBlock", Category.COMBAT, aliases = listOf("OldBlocking")) {

    val hideShieldSlot by boolean("HideShieldSlot", false).doNotIncludeAlways()
    private val alwaysHideShield by boolean("AlwaysHideShield", false).doNotIncludeAlways()

    @JvmStatic
    val Player.isBlockingWithOffhandShield
        get() = isUsingItem && offhandItem.item is ShieldItem && useItem === offhandItem

    @JvmOverloads
    fun shouldHideOffhand(
        offHandStack: ItemStack = player.offhandItem,
        mainHandStack: ItemStack = player.mainHandItem
    ): Boolean {
        if (!running && !KillAuraAutoBlock.blockVisual) {
            return false
        }

        if (offHandStack.item !is ShieldItem) {
            return false
        }

        return mainHandStack.isSword || alwaysHideShield
    }

}
