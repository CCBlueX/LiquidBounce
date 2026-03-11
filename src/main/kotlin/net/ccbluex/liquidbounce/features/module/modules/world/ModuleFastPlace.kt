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
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.UseCooldownEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.input.InputTracker.timeSinceLastPress
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.ProjectileItem
import java.util.function.Predicate

/**
 * FastPlace module
 *
 * Allows you to place blocks faster.
 */
object ModuleFastPlace : ClientModule("FastPlace", ModuleCategories.WORLD) {

    private val cooldown by intRange("Cooldown", 0..0, 0..4, "ticks")
    private val applyTo by multiEnumChoice("ApplyTo", ApplyTo.entries)
    private val startDelay by int("StartDelay", 0, 0..1000, "ms")

    @Suppress("unused")
    private val useCooldownHandler = handler<UseCooldownEvent> { event ->
        val mainHandItem = player.mainHandItem.item
        val offHandItem = player.offhandItem.item

        if (applyTo.any {
                it.condition.test(mainHandItem) || it.condition.test(offHandItem)
            } && (startDelay <= 0 || mc.options.keyUse.timeSinceLastPress >= startDelay)) {
            event.cooldown = cooldown.random()
        }
    }

    @Suppress("unused")
    private enum class ApplyTo(
        override val tag: String,
        val condition: Predicate<Item>
    ): Tagged {
        PROJECTILES("Projectiles", { item -> item is ProjectileItem }),
        BLOCKS("Blocks", { item -> item is BlockItem })
    }
}
