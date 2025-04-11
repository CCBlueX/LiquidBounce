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
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.events.UseCooldownEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.item.ProjectileItem
import java.util.function.Predicate

/**
 * FastPlace module
 *
 * Allows you to place blocks faster.
 */
object ModuleFastPlace : ClientModule("FastPlace", Category.WORLD) {
    private val cooldown by int("Cooldown", 0, 0..4, "ticks").apply { tagBy(this) }
    private val applyTo by multiEnumChoice("ApplyTo", ApplyTo.entries)

    @Suppress("unused")
    private val useCooldownHandler = handler<UseCooldownEvent> { event ->
        val mainHandItem = player.mainHandStack.item
        val offHandItem = player.offHandStack.item
        if (applyTo.any {
            it.condition.test(mainHandItem) || it.condition.test(offHandItem)
        }) {
            event.cooldown = cooldown
        }
    }

    @Suppress("unused")
    private enum class ApplyTo(
        override val choiceName: String,
        val condition: Predicate<Item>
    ): NamedChoice {
        PROJECTILES("Projectiles", { item -> item is ProjectileItem }),
        BLOCKS("Blocks", { item -> item is BlockItem })
    }
}
