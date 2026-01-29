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
package net.ccbluex.liquidbounce.features.module.modules.movement.noslow

import it.unimi.dsi.fastutil.floats.FloatFloatImmutablePair
import net.ccbluex.liquidbounce.event.events.PlayerUseMultiplier
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.blocking.NoSlowBlock
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.bow.NoSlowBow
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.bundle.NoSlowBundle
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.consume.NoSlowConsume
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.fluid.NoSlowFluid
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.honey.NoSlowHoney
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.powdersnow.NoSlowPowderSnow
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.slime.NoSlowSlime
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.slowness.NoSlowSlowness
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.sneaking.NoSlowSneaking
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.soulsand.NoSlowSoulsand
import net.minecraft.world.item.ItemUseAnimation

/**
 * NoSlow module
 *
 * Cancels slowness effects caused by blocks and using items.
 */
object ModuleNoSlow : ClientModule("NoSlow", ModuleCategories.MOVEMENT) {

    init {
        tree(NoSlowBlock)
        tree(NoSlowConsume)
        tree(NoSlowBow)
        tree(NoSlowBundle)
        tree(NoSlowSneaking)
        tree(NoSlowSoulsand)
        tree(NoSlowSlime)
        tree(NoSlowHoney)
        tree(NoSlowPowderSnow)
        tree(NoSlowFluid)
        tree(NoSlowSlowness)
    }

    @Suppress("unused")
    private val multiplierHandler = handler<PlayerUseMultiplier> { event ->
        val action = player.useItem.useAnimation
        val mul = multiplier(action, event.forward, event.sideways)

        event.forward = mul.firstFloat()
        event.sideways = mul.secondFloat()
    }

    private fun multiplier(action: ItemUseAnimation, forward: Float, sideways: Float) = when (action) {
        ItemUseAnimation.NONE -> FloatFloatImmutablePair(forward, sideways)
        ItemUseAnimation.EAT, ItemUseAnimation.DRINK -> NoSlowConsume.getMultiplier(forward, sideways)
        ItemUseAnimation.BLOCK, ItemUseAnimation.SPYGLASS,
        ItemUseAnimation.TOOT_HORN, ItemUseAnimation.BRUSH -> NoSlowBlock.getMultiplier(
            forward,
            sideways
        )

        ItemUseAnimation.BOW, ItemUseAnimation.TRIDENT,
        ItemUseAnimation.CROSSBOW-> NoSlowBow.getMultiplier(
            forward,
            sideways
        )

        ItemUseAnimation.BUNDLE -> NoSlowBundle.getMultiplier(forward, sideways)

        // Vanilla spear doesn't make player slow down
        ItemUseAnimation.SPEAR -> FloatFloatImmutablePair(1F, 1F)
    }

}
