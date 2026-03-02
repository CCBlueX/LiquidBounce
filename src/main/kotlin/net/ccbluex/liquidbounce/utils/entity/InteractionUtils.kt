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

package net.ccbluex.liquidbounce.utils.entity

import net.ccbluex.liquidbounce.utils.block.SwingMode
import net.ccbluex.liquidbounce.utils.client.isOlderThanOrEquals1_7_10
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionResult.SwingSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.EntityHitResult

/**
 * Simulated [net.minecraft.world.phys.HitResult.Type.ENTITY] branch in vanilla
 *
 * @see net.minecraft.client.Minecraft.startUseItem
 */
fun interactEntity(
    entity: Entity,
    hitResult: EntityHitResult = EntityHitResult(entity),
    hand: InteractionHand = InteractionHand.MAIN_HAND,
    swingMode: SwingMode = SwingMode.DO_NOT_HIDE,
): InteractionResult? {
    val level = entity.level()
    val gameMode = mc.gameMode!!
    if (!level.worldBorder.isWithinBounds(entity.blockPosition())) {
        return null
    }
    // Skipped check:
    // player.isWithinEntityInteractionRange(entity, 0.0)

    val result = when {
        // ~1.7.10
        isOlderThanOrEquals1_7_10 -> gameMode.interact(player, entity, hand)

        // 1.8~1.21.11
        else -> {
            val result = gameMode.interactAt(player, entity, hitResult, hand)
            // In vanilla 1.21.11 only ArmorStand can skip this
            if (!result.consumesAction()) {
                gameMode.interact(player, entity, hand)
            } else {
                result
            }
        }
    }

    if (result is InteractionResult.Success) {
        if (result.swingSource() === SwingSource.CLIENT) {
            swingMode.swing(hand)
        }
    }

    return result
}
