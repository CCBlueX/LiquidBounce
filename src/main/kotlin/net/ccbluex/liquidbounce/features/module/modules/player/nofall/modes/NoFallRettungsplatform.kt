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
package net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.ModuleNoFall
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.utils.block.getBlock
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.entity.FallingPlayer
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.findClosestSlot
import net.ccbluex.liquidbounce.utils.inventory.useHotbarSlotOrOffhand
import net.minecraft.item.Items

/**
 * Uses an item called Rettungsplatform or Rettungskapsel to prevent fall damage.
 * This is an item of the game-mode BedWars on the server GommeHD.net
 *
 * https://www.gommehd.net/
 *
 * As such module is mostly used by German players, the name of the module is in German.
 * That is unusual for LiquidBounce, but it is the best name for this module.
 */
internal object NoFallRettungsplatform : Choice("Rettungsplatform") {

    override val parent: ChoiceConfigurable<*>
        get() = ModuleNoFall.modes

    /**
     * The item used to create a platform.
     * This is either a blaze rod or a magma cream.
     * We are not checking for the item name, as there are different language options causing issues.
     */
    private val itemToPlatform
        get() = Slots.Hotbar.findClosestSlot(Items.BLAZE_ROD, Items.MAGMA_CREAM)

    val repatable = tickHandler {
        if (player.fallDistance > 2f) {
            val itemToPlatform = itemToPlatform ?: return@tickHandler

            // Are we actually going to fall into the void?
            // todo: check if the fall damage is actually high enough to kill us
            val collision = FallingPlayer.fromPlayer(player).findCollision(90)?.pos
            ModuleDebug.debugParameter(ModuleNoFall, "Collision", collision?.getBlock().toString())
            if (collision != null && collision.getState()?.isAir == false) {
                return@tickHandler
            }

            useHotbarSlotOrOffhand(itemToPlatform)

            // Wait 5 seconds
            waitTicks(20 * 5)
        }
    }

}
