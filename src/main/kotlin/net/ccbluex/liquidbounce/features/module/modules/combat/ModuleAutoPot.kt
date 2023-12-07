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

import net.ccbluex.liquidbounce.event.DummyEvent
import net.ccbluex.liquidbounce.event.Sequence
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.HotbarItemSlot
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemSlot
import net.ccbluex.liquidbounce.utils.aiming.Rotation
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.ccbluex.liquidbounce.utils.entity.FallingPlayer
import net.ccbluex.liquidbounce.utils.item.findInventorySlot
import net.ccbluex.liquidbounce.utils.item.isNothing
import net.ccbluex.liquidbounce.utils.item.runWithOpenedInventory
import net.ccbluex.liquidbounce.utils.item.useHotbarSlotOrOffhand
import net.ccbluex.liquidbounce.utils.kotlin.random
import net.minecraft.entity.AreaEffectCloudEntity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.item.ItemStack
import net.minecraft.item.LingeringPotionItem
import net.minecraft.item.SplashPotionItem
import net.minecraft.potion.PotionUtil
import net.minecraft.screen.slot.SlotActionType

/**
 * AutoPot module
 *
 * Automatically throws healing potions whenever your health is low.
 */
object ModuleAutoPot : Module("AutoPot", Category.COMBAT) {

    private const val BENEFICIAL_SQUARE_RANGE = 16.0

    private val delay by int("Delay", 10, 10..20)
    private val health by int("Health", 14, 1..19)
    private val tillGroundDistance by float("TillGroundDistance", 2f, 1f..5f)
    private val doNotBenefitOthers by boolean("DoNotBenefitOthers", true)

    private val healthPotion by boolean("HealthPotion", true)
    private val regenPotion by boolean("RegenPotion", true)
    private val strengthPotion by boolean("StrengthPotion", true)
    private val speedPotion by boolean("SpeedPotion", false)

    private val allowLingering by boolean("AllowLingering", false)

    val rotations = tree(RotationsConfigurable())

    val repeatable = repeatable {
        if (player.isDead) {
            return@repeatable
        }

        if (doNotBenefitOthers) {
            // Check if there is any entity that we care about that can benefit from the potion
            // This means we will only care about entities that are our enemies and are close enough to us
            // That means we will still throw the potion if there is a friendly friend or team member nearby
            val benefits = world.entities.filterIsInstance<LivingEntity>().any {
                it.shouldBeAttacked() && hasBenefit(it)
            }

            if (benefits) {
                return@repeatable
            }
        }

        if (isStandingInsideLingering()) {
            return@repeatable
        }

        val potionSlot = findInventorySlot { isPotion(it) } ?: return@repeatable

        if (potionSlot is HotbarItemSlot) {
            if (!tryUsePot(potionSlot)) {
                return@repeatable
            }

            waitTicks(delay)
        } else {
            if (!tryToMoveSlotInHotbar(potionSlot)) {
                return@repeatable
            }

            waitTicks(delay)
        }
    }

    private suspend fun Sequence<DummyEvent>.tryUsePot(foundPotSlot: HotbarItemSlot): Boolean {
        val collisionBlock = FallingPlayer.fromPlayer(player).findCollision(20)?.pos
        val isCloseGround = player.y - (collisionBlock?.y ?: 0) <= tillGroundDistance

        if (!isCloseGround || player.isBlocking) {
            return false
        }

        val potionStack = foundPotSlot.getItemStack()

        if (!potionStack.isNothing()) {
            if (!healthPotion && potionStack.item !is SplashPotionItem && potionStack.item !is LingeringPotionItem) {
                return false
            }

            if (!regenPotion && healthIsLow && !player.hasStatusEffect(StatusEffects.REGENERATION)) {
                return false
            }

            if (!strengthPotion && !player.hasStatusEffect(StatusEffects.STRENGTH)) {
                return false
            }

            if (!speedPotion && potionStack.item is SplashPotionItem && potionStack.item.potion.effects.any { effect ->
                    effect.effectType == StatusEffects.SPEED
                }) {
                useHotbarSlotOrOffhand(foundPotSlot)
                return true
            }
        }

        return false
    }

        useHotbarSlotOrOffhand(foundPotSlot)
        return true
    }

    /**
     * Moves the given slot to the hotbar if possible.
     *
     * @return true if a move occurred
     */
    private fun tryToMoveSlotInHotbar(foundPotSlot: ItemSlot): Boolean {
        val isSpaceInHotbar = (0..8).any { player.inventory.getStack(it).isNothing() }

        if (!isSpaceInHotbar) {
            return false
        }

        val serverSlotId = foundPotSlot.getIdForServerWithCurrentScreen() ?: return false

        runWithOpenedInventory {
            interaction.clickSlot(0, serverSlotId, 0, SlotActionType.QUICK_MOVE, player)

            true
        }

        return true
    }

    private fun isPotion(stack: ItemStack): Boolean {
        if (stack.isNothing()) {
            return false
        }
        if (stack.item !is SplashPotionItem && (stack.item !is LingeringPotionItem || !allowLingering)) {
            return false
        }

        val allowedStatusEffects = mutableListOf<StatusEffect>()
        val healthIsLow = player.health <= health
        if (healthPotion && healthIsLow) {
            allowedStatusEffects += StatusEffects.INSTANT_HEALTH
        }
        if (regenPotion && healthIsLow && !player.hasStatusEffect(StatusEffects.REGENERATION)) {
            allowedStatusEffects += StatusEffects.REGENERATION
        }
        if (strengthPotion && !player.hasStatusEffect(StatusEffects.STRENGTH)) {
            allowedStatusEffects += StatusEffects.STRENGTH
        }

        return PotionUtil.getPotionEffects(stack).any { allowedStatusEffects.contains(it.effectType) }
    }

    private fun hasBenefit(entity: LivingEntity): Boolean {
        if (!entity.isAffectedBySplashPotions) {
            return false
        }

        // If we look down about 90 degrees, the closet position of the potion is at the player foot
        val squareRange = entity.squaredDistanceTo(player)

        if (squareRange > BENEFICIAL_SQUARE_RANGE) {
            return false
        }

        return true

    }

    /**
     * Check if the player is standing inside a lingering potion cloud
     *
     * TODO: Use actual lingering radius instead of a square
     */
    private fun isStandingInsideLingering() = world.entities.filterIsInstance<AreaEffectCloudEntity>().any {
        it.squaredDistanceTo(player) <= BENEFICIAL_SQUARE_RANGE && it.potion.effects.any { effect ->
            effect.effectType == StatusEffects.REGENERATION || effect.effectType == StatusEffects.INSTANT_HEALTH
                || effect.effectType == StatusEffects.STRENGTH
        }
    }

}
