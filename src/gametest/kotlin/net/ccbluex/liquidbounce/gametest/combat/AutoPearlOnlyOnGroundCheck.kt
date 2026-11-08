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

/**
 * AutoPearl: nothing is thrown in the air.
 */
fun CombatScene.checkAutoPearlOnlyOnGround() {
    startAutoPearl { ModuleAutoPearl.set("OnlyOnGround", "true") }

    world.server.runCommand("effect give @a minecraft:levitation 2 1 true")
    context.waitFor({ client -> checkNotNull(client.player).y > origin.y + 1 }, PEARL_TICKS)
    check(pearlThrownAfter(null, at(0.0, 1.5, 3.0)) == null) { "AutoPearl: threw while in the air" }
    world.server.runCommand("effect clear @a")
    context.waitFor({ client -> checkNotNull(client.player).onGround() }, PEARL_TICKS * 2)
    check(pearlThrownAfter(null, at(0.0, 1.5, 3.0)) != null) { "AutoPearl: did not throw on the ground" }

    context.onClient { ModuleAutoPearl.enabled = false }
}
