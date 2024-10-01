/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.event.events.UseCooldownEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.item.BlockItem

/**
 * FastPlace module
 *
 * Allows you to place blocks faster.
 */
object ModuleFastPlace : Module("FastPlace", Category.WORLD) {

    private val cooldown by int("Cooldown", 0, 0..4, "ticks").apply { tagBy(this) }
    private val onlyBlock by boolean("OnlyBlock", true)

    @Suppress("unused")
    private val useCooldownHandler = handler<UseCooldownEvent> { event ->
        if (onlyBlock && player.mainHandStack.item !is BlockItem) return@handler

        event.cooldown = cooldown
    }

}
