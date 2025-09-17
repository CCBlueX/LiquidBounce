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
 *
 *
 */

package net.ccbluex.liquidbounce.features.module.modules.player.autobuff.features

import net.ccbluex.liquidbounce.event.Sequence
import net.ccbluex.liquidbounce.features.module.modules.player.autobuff.ModuleAutoBuff
import net.ccbluex.liquidbounce.features.module.modules.player.autobuff.ModuleAutoBuff.Rotations.RotationTimingMode.*
import net.ccbluex.liquidbounce.features.module.modules.player.autobuff.StatusEffectBasedBuff
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationManager.currentRotation
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.utils.withFixedYaw
import net.ccbluex.liquidbounce.utils.client.MovePacketType
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.ccbluex.liquidbounce.utils.entity.FallingPlayer
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.useHotbarSlotOrOffhand
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.kotlin.random
import net.minecraft.entity.AreaEffectCloudEntity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.projectile.thrown.PotionEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.LingeringPotionItem
import net.minecraft.item.SplashPotionItem

internal object Pot : StatusEffectBasedBuff("Pot") {

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

    private val tillGroundDistance by float("TillGroundDistance", 2f, 1f..5f)
    private val doNotBenefitOthers by boolean("DoNotBenefitOthers", true)

    private val allowLingering by boolean("AllowLingering", false)

    override suspend fun Sequence.execute(slot: HotbarItemSlot) {
        // TODO: Use movement prediction to splash against walls and away from the player
        //   See https://github.com/CCBlueX/LiquidBounce/issues/2051
        var rotation = Rotation(player.yaw, (85f..90f).random())

        when (ModuleAutoBuff.Rotations.rotationTiming) {
            NORMAL -> {
                RotationManager.setRotationTarget(
                    rotation,
                    configurable = ModuleAutoBuff.Rotations,
                    provider = ModuleAutoBuff,
                    priority = Priority.IMPORTANT_FOR_PLAYER_LIFE
                )

                waitUntil {
                    (currentRotation ?: player.rotation).pitch > 85
                }

                rotation = rotation.normalize()
            }
            ON_TICK -> {
                rotation = rotation.normalize()
                network.sendPacket(MovePacketType.FULL.generatePacket().apply {
                    yaw = rotation.yaw
                    pitch = rotation.pitch
                })
            }
            ON_USE -> {
                rotation = rotation.normalize()
            }
        }

        useHotbarSlotOrOffhand(
            slot,
            yaw = rotation.yaw,
            pitch = rotation.pitch,
        )

        when (ModuleAutoBuff.Rotations.rotationTiming) {
            ON_TICK -> {
                network.sendPacket(MovePacketType.FULL.generatePacket().apply {
                    yaw = player.withFixedYaw(currentRotation ?: player.rotation)
                    pitch = currentRotation?.pitch ?: player.pitch
                })
            }
            else -> { }
        }

        // Wait at least 1 tick to make sure, we do not continue with something else too early
        waitTicks(1)
    }

    override fun isValidPotion(stack: ItemStack) =
        stack.item is SplashPotionItem || stack.item is LingeringPotionItem && allowLingering

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
            it.squaredDistanceTo(player) <= BENEFICIAL_SQUARE_RANGE &&
                it.potionContentsComponent.effects.any { effect ->
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
