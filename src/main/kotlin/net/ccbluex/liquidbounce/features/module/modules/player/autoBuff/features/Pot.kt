/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2024 CCBlueX
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
 *
 *
 */

package net.ccbluex.liquidbounce.features.module.modules.player.autoBuff.features

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.Sequence
import net.ccbluex.liquidbounce.features.module.modules.player.autoBuff.Buff
import net.ccbluex.liquidbounce.features.module.modules.player.autoBuff.ModuleAutoBuff
import net.ccbluex.liquidbounce.features.module.modules.player.autoBuff.features.Pot.isPotion
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.aiming.Rotation
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.ccbluex.liquidbounce.utils.entity.FallingPlayer
import net.ccbluex.liquidbounce.utils.inventory.useHotbarSlotOrOffhand
import net.ccbluex.liquidbounce.utils.item.getPotionEffects
import net.ccbluex.liquidbounce.utils.item.isNothing
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.kotlin.random
import net.minecraft.entity.AreaEffectCloudEntity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.projectile.thrown.PotionEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.LingeringPotionItem
import net.minecraft.item.SplashPotionItem

object Pot : Buff("Pot", isValidItem = { stack, forUse -> isPotion(stack, forUse) }) {

    private const val BENEFICIAL_SQUARE_RANGE = 16.0

    override val passesRequirements: Boolean
        get() {
            if (doNotBenefitOthers) {
                // Check if there is any entity that we care about that can benefit from the potion
                // This means we will only care about entities that are our enemies and are close enough to us
                // That means we will still throw the potion if there is a friendly friend or team member nearby
                val benefits = world.entities.filterIsInstance<LivingEntity>().any {
                    it.shouldBeAttacked() && hasBenefit(it)
                }

                if (benefits) {
                    return false
                }
            }

            if (isStandingInsideLingering()) {
                return false
            }

            val collisionBlock = FallingPlayer.fromPlayer(player).findCollision(20)?.pos
            val isCloseGround = player.y - (collisionBlock?.y ?: 0) <= tillGroundDistance

            // Do not check for health pass requirements, because this is already done in the potion check
            return isCloseGround && !isSplashNearby()
        }


    private object HealthPotion : ToggleableConfigurable(Drink, "HealthPotion", true) {
        private val healthPercent by int("Health", 40, 1..100, "%HP")

        val health
            get() = player.maxHealth * healthPercent / 100

    }

    private object RegenPotion : ToggleableConfigurable(Drink, "RegenPotion", true) {
        private val healthPercent by int("Health", 70, 1..100, "%HP")

        val health
            get() = player.maxHealth * healthPercent / 100

    }

    init {
        tree(HealthPotion)
        tree(RegenPotion)
    }

    private val strengthPotion by boolean("StrengthPotion", true)
    private val speedPotion by boolean("SpeedPotion", true)

    private val tillGroundDistance by float("TillGroundDistance", 2f, 1f..5f)
    private val doNotBenefitOthers by boolean("DoNotBenefitOthers", true)

    private val allowLingering by boolean("AllowLingering", false)

    override suspend fun execute(sequence: Sequence<*>, slot: HotbarItemSlot) {
        sequence.waitUntil {
            // TODO: Use movement prediction to splash against walls and away from the player
            //   See https://github.com/CCBlueX/LiquidBounce/issues/2051
            RotationManager.aimAt(
                Rotation(player.yaw, (85f..90f).random().toFloat()),
                configurable = ModuleAutoBuff.rotations,
                provider = ModuleAutoBuff,
                priority = Priority.IMPORTANT_FOR_PLAYER_LIFE
            )

            RotationManager.serverRotation.pitch > 85
        }

        useHotbarSlotOrOffhand(slot)

        // Wait at least 1 tick to make sure, we do not continue with something else too early
        sequence.waitTicks(1)
    }

    private fun isPotion(stack: ItemStack, forUse: Boolean): Boolean {
        if (stack.isNothing() || !isValidPotion(stack)) {
            return false
        }

        val health = if (forUse) player.health else 0f
        return stack.getPotionEffects().any { foundTargetEffect(it, health) }
    }

    private fun releaseUseKey() {
        mc.options.useKey.isPressed = false
    }

    override fun disable() {
        releaseUseKey()
        super.disable()
    }

    private fun isValidPotion(stack: ItemStack) =
        stack.item is SplashPotionItem || stack.item is LingeringPotionItem && allowLingering

    private fun foundTargetEffect(effect: StatusEffectInstance, playerHealth: Float) =
        when (effect.effectType) {
            StatusEffects.INSTANT_HEALTH -> HealthPotion.enabled && playerHealth <= HealthPotion.health
            StatusEffects.REGENERATION -> RegenPotion.enabled && playerHealth <= RegenPotion.health
                && !player.hasStatusEffect(StatusEffects.REGENERATION)
            StatusEffects.STRENGTH -> strengthPotion && !player.hasStatusEffect(StatusEffects.STRENGTH)
            StatusEffects.SPEED -> speedPotion && !player.hasStatusEffect(StatusEffects.SPEED)
            else -> false
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
     */
    private fun isStandingInsideLingering() =
        world.entities.filterIsInstance<AreaEffectCloudEntity>().any {
            it.squaredDistanceTo(player) <= BENEFICIAL_SQUARE_RANGE && it.potionContentsComponent.effects.any { effect ->
                effect.effectType == StatusEffects.REGENERATION || effect.effectType == StatusEffects.INSTANT_HEALTH
                    || effect.effectType == StatusEffects.STRENGTH
            }
        }

    /**
     * Check if splash potion is nearby to prevent throwing a potion that is not needed
     */
    private fun isSplashNearby() =
        world.entities.filterIsInstance<PotionEntity>().any {
            it.squaredDistanceTo(player) <= BENEFICIAL_SQUARE_RANGE
        }

}
