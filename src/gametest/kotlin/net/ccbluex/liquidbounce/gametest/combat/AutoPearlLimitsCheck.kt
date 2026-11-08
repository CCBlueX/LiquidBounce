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
package net.ccbluex.liquidbounce.gametest.combat

import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleAutoPearl
import net.ccbluex.liquidbounce.gametest.onClient
import net.minecraft.world.item.Items

/**
 * AutoPearl: the same pearl, thrown 3 blocks in front and landing far behind it, is followed
 * until either the thrower or the landing spot is beyond its limit.
 */
fun CombatScene.checkAutoPearlLimits() {
    val from = at(0.0, 1.5, 3.0)
    startAutoPearl {}
    context.waitFor({ client -> checkNotNull(client.player).inventory.getItem(0).`is`(Items.ENDER_PEARL) },
        CombatScene.ENTITY_TIMEOUT_TICKS)

    check(pearlThrownAfter(null, from) != null) { "AutoPearl: did not follow a pearl within the limits" }

    context.onClient { ModuleAutoPearl.set("Limits/MaxThrowerDistance", "2") }
    check(pearlThrownAfter(null, from) == null) { "AutoPearl: followed a pearl thrown beyond MaxThrowerDistance" }

    context.onClient {
        ModuleAutoPearl.set("Limits/MaxThrowerDistance", "256")
        ModuleAutoPearl.set("Limits/MaxLandingDistance", "10")
    }
    check(pearlThrownAfter(null, from) == null) { "AutoPearl: followed a pearl landing beyond MaxLandingDistance" }

    context.onClient { ModuleAutoPearl.enabled = false }
}
