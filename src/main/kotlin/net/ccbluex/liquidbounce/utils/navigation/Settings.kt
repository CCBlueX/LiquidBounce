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
package net.ccbluex.liquidbounce.utils.navigation

import net.ccbluex.agent.navigation.Options
import net.ccbluex.agent.navigation.PlaceableBlocks
import net.ccbluex.agent.vanilla.toMinecraft
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ScaffoldBlockItemSelection

/** What a walking navigator may do on its way, as settings of [values]. */
class Settings(
    values: ValueGroup,
    allowBreak: Boolean,
    allowPlace: Boolean,
    allowParkour: Boolean,
) {

    private val allowBreak by values.boolean("AllowBreak", allowBreak)
    private val allowPlace by values.boolean("AllowPlace", allowPlace)
    private val allowParkour by values.boolean("AllowParkour", allowParkour)
    private val riskyJumps by values.boolean("RiskyJumps", false)
    private val maxFall by values.int("MaxFall", 3, 0..20, "blocks")
    private val pathRange by values.int("PathRange", 64, 16..128, "blocks")
    private val allowFly by values.boolean("AllowFly", true)
    private val allowElytra by values.boolean("AllowElytra", true)
    private val elytraRockets by values.boolean("ElytraRockets", true)

    fun options(): Options {
        // Scaffold places the floors, so the walk bridges even without AllowPlace and only runs across them
        val scaffold = ModuleScaffold.enabled
        return Options(
            canBreak = allowBreak,
            canPlace = allowPlace || scaffold,
            canParkour = allowParkour,
            riskyJumps = riskyJumps,
            maxFall = maxFall,
            pathRange = pathRange,
            canFly = allowFly,
            canElytra = allowElytra,
            elytraRockets = elytraRockets,
            // The camera stays the user's; the walk turns the player through the rotation manager
            lookAhead = false,
            fastBridges = scaffold,
            placesBridgeFloors = scaffold,
            placeableBlocks = SCAFFOLD_BLOCKS,
        )
    }

    private companion object {
        val SCAFFOLD_BLOCKS = PlaceableBlocks { ScaffoldBlockItemSelection.isValidBlock(it.toMinecraft()) }
    }

}
