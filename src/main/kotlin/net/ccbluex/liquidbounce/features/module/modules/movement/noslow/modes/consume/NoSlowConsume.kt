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
package net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.consume

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.NoneChoice
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.ModuleNoSlow
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.shared.NoSlowSharedIntave14
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.shared.NoSlowNoBlockInteract
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.shared.NoSlowSharedGrim2360
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.shared.NoSlowSharedGrim2364MC18
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.shared.NoSlowSharedInvalidHand
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.item.isConsumable
import net.minecraft.util.UseAction

object NoSlowConsume : ToggleableConfigurable(ModuleNoSlow, "Consume", true) {

    val forwardMultiplier by float("Forward", 1f, 0.2f..1f)
    val sidewaysMultiplier by float("Sideways", 1f, 0.2f..1f)

    @Suppress("unused")
    private val noBlockInteract = tree(NoSlowNoBlockInteract(this) { action ->
        action == UseAction.EAT || action == UseAction.DRINK
    })

    @Suppress("unused")
    private val modes = choices<Choice>(this, "Mode", { it.choices[0] }) {
        arrayOf(NoneChoice(it), NoSlowSharedGrim2360(it), NoSlowSharedGrim2364MC18(it), NoSlowSharedInvalidHand(it), NoSlowSharedIntave14(it))
    }

    override fun handleEvents(): Boolean {
        if (!super.handleEvents() || !inGame) {
            return false
        }

        // Check if we are using a consume item
        return player.isUsingItem && player.activeItem.isConsumable
    }

}
