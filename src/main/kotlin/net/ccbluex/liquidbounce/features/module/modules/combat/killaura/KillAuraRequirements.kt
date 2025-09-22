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
package net.ccbluex.liquidbounce.features.module.modules.combat.killaura

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.utils.client.isOlderThanOrEqual1_8
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.input.InputTracker.isPressedOnAny
import net.ccbluex.liquidbounce.utils.input.InputTracker.wasPressedRecently
import net.ccbluex.liquidbounce.utils.item.isAxe
import net.ccbluex.liquidbounce.utils.item.isSword
import net.minecraft.item.ItemStack
import net.minecraft.item.MaceItem
import java.util.function.BooleanSupplier

@Suppress("unused")
enum class KillAuraRequirements(
    override val choiceName: String,
) : NamedChoice, BooleanSupplier {
    CLICK("Click"),
    WEAPON("Weapon"),
    VANILLA_NAME("VanillaName"),
    NOT_BREAKING("NotBreaking");

    override fun getAsBoolean(): Boolean =
        when (this) {
            CLICK -> mc.options.attackKey.isPressedOnAny || mc.options.attackKey.wasPressedRecently(250)
            WEAPON -> player.inventory.mainHandStack.isWeapon()
            VANILLA_NAME -> player.inventory.mainHandStack.customName == null
            NOT_BREAKING -> mc.interactionManager?.isBreakingBlock == false
        }
}

/**
 * Check if the item is a weapon.
 */
private fun ItemStack.isWeapon() = this.isSword || !isOlderThanOrEqual1_8 && this.isAxe
    || this.item is MaceItem
