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
package net.ccbluex.liquidbounce.features.module.modules.movement.noslow

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.blocking.NoSlowBlock
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.bow.NoSlowBow
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.consume.NoSlowConsume
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.fluid.NoSlowFluid
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.honey.NoSlowHoney
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.powdersnow.NoSlowPowderSnow
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.slime.NoSlowSlime
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.soulsand.NoSlowSoulsand
import net.ccbluex.liquidbounce.register.IncludeModule
import net.ccbluex.liquidbounce.utils.client.InteractionTracker
import net.minecraft.util.UseAction

/**
 * NoSlow module
 *
 * Cancels slowness effects caused by blocks and using items.
 */
@IncludeModule
object ModuleNoSlow : Module("NoSlow", Category.MOVEMENT) {

    init {
        tree(NoSlowBlock)
        tree(NoSlowConsume)
        tree(NoSlowBow)
        tree(NoSlowSoulsand)
        tree(NoSlowSlime)
        tree(NoSlowHoney)
        tree(NoSlowPowderSnow)
        tree(NoSlowFluid)
    }

    @Suppress("unused")
    val multiplierHandler = handler<PlayerUseMultiplier> { event ->
        val action = player.activeItem.useAction ?: return@handler
        val (forward, strafe) = multiplier(action)

        event.forward = forward
        event.sideways = strafe
    }

    private fun multiplier(action: UseAction) = when (action) {
        UseAction.NONE -> Pair(0.2f, 0.2f)
        UseAction.EAT, UseAction.DRINK -> if (NoSlowConsume.enabled) Pair(
            NoSlowConsume.forwardMultiplier, NoSlowConsume.sidewaysMultiplier
        ) else Pair(0.2f, 0.2f)

        UseAction.BLOCK, UseAction.SPYGLASS, UseAction.TOOT_HORN, UseAction.BRUSH ->
            if (NoSlowBlock.enabled && (!NoSlowBlock.onlySlowOnServerSide || !InteractionTracker.isBlocking)) Pair(
                NoSlowBlock.forwardMultiplier,
                NoSlowBlock.sidewaysMultiplier
            )
        else Pair(0.2f, 0.2f)

        UseAction.BOW, UseAction.CROSSBOW, UseAction.SPEAR -> if (NoSlowBow.enabled) Pair(
            NoSlowBow.forwardMultiplier, NoSlowBow.sidewaysMultiplier
        ) else Pair(0.2f, 0.2f)

    }
}
