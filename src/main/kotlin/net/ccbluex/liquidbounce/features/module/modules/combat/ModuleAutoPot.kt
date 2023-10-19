/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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

package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.aiming.Rotation
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.entity.FallingPlayer
import net.ccbluex.liquidbounce.utils.item.*
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.item.ItemStack
import net.minecraft.item.SplashPotionItem
import net.minecraft.potion.PotionUtil
import net.minecraft.screen.slot.SlotActionType
import org.apache.commons.lang3.RandomUtils

/**
 * AutoPot module
 *
 * Automatically throws healing potions whenever your health is low.
 */

object ModuleAutoPot : Module("AutoPot", Category.COMBAT) {
    private val delay by int("Delay", 10, 10..20)
    private val health by int("Health", 18, 1..20)
    private val tillGroundDistance by float("TillGroundDistance", 2f, 1f..5f)

    val rotations = tree(RotationsConfigurable())

    val repeatable = repeatable {
        if (player.isDead || player.health >= health) {
            return@repeatable
        }

        val potionSlot = findInventorySlot { isPotion(it) } ?: return@repeatable

        if (isHotbarSlot(potionSlot)) {
            if (!tryPot(potionSlot)) {
                return@repeatable
            }

            wait(delay)
        } else {
            tryToMoveSlotInHotbar(potionSlot)
        }

        return@repeatable
    }

    private fun tryPot(foundPotSlot: Int): Boolean {
        val collisionBlock = FallingPlayer.fromPlayer(player).findCollision(20)?.pos

        val isCloseGround = player.y - (collisionBlock?.y ?: 0) <= tillGroundDistance

        if (isCloseGround || player.isBlocking) {
            return false
        }

        if (RotationManager.serverRotation.pitch <= 80) {
            RotationManager.aimAt(
                Rotation(player.yaw, RandomUtils.nextFloat(80f, 90f)),
                configurable = rotations,
            )

            return false
        }

        clickHotbarOrOffhand(foundPotSlot)

        return true
    }

    private fun tryToMoveSlotInHotbar(foundPotSlot: Int) {
        val isSpaceInHotbar = (0..8).any { player.inventory.getStack(it).isNothing() }

        if (!isSpaceInHotbar) {
            return
        }

        val serverSlot = convertClientSlotToServerSlot(foundPotSlot)

        runWithOpenedInventory {
            interaction.clickSlot(0, serverSlot, 0, SlotActionType.QUICK_MOVE, player)

            true
        }
    }

    private fun isPotion(stack: ItemStack): Boolean {
        if (stack.item !is SplashPotionItem) {
            return false
        }

        return PotionUtil.getPotionEffects(stack).any { it.effectType == StatusEffects.INSTANT_HEALTH }
    }
}
