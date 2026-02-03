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

package net.ccbluex.liquidbounce.features.module.modules.player.autobuff.features

import net.ccbluex.liquidbounce.event.tickUntil
import net.ccbluex.liquidbounce.event.waitTicks
import net.ccbluex.liquidbounce.features.module.modules.player.autobuff.ModuleAutoBuff
import net.ccbluex.liquidbounce.features.module.modules.player.autobuff.ModuleAutoBuff.Rotations.RotationTimingMode.NORMAL
import net.ccbluex.liquidbounce.features.module.modules.player.autobuff.ModuleAutoBuff.Rotations.RotationTimingMode.ON_TICK
import net.ccbluex.liquidbounce.features.module.modules.player.autobuff.ModuleAutoBuff.Rotations.RotationTimingMode.ON_USE
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
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.AreaEffectCloud
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.LingeringPotionItem
import net.minecraft.world.item.SplashPotionItem

internal object Pot : StatusEffectBasedBuff("Pot") {

    private const val BENEFICIAL_SQUARE_RANGE = 16.0

    override val passesRequirements: Boolean
        get() {
            if (doNotBenefitOthers) {
                // Check if there is any entity that we care about that can benefit from the potion
                // This means we will only care about entities that are our enemies and are close enough to us
                // That means we will still throw the potion if there is a friendly friend or team member nearby
                val benefits = world.entitiesForRendering().any {
                    it is LivingEntity && it.shouldBeAttacked() && hasBenefit(it)
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

    override suspend fun execute(slot: HotbarItemSlot) {
        // TODO: Use movement prediction to splash against walls and away from the player
        //   See https://github.com/CCBlueX/LiquidBounce/issues/2051
        var rotation = Rotation(player.yRot, (85f..90f).random())

        when (ModuleAutoBuff.Rotations.rotationTiming) {
            NORMAL -> {
                RotationManager.setRotationTarget(
                    rotation,
                    valueGroup = ModuleAutoBuff.Rotations,
                    provider = ModuleAutoBuff,
                    priority = Priority.IMPORTANT_FOR_PLAYER_LIFE
                )

                tickUntil {
                    (currentRotation ?: player.rotation).pitch > 85
                }

                rotation = rotation.normalize()
            }

            ON_TICK -> {
                rotation = rotation.normalize()
                network.send(MovePacketType.FULL.generatePacket().apply {
                    yRot = rotation.yaw
                    xRot = rotation.pitch
                })
            }

            ON_USE -> {
                rotation = rotation.normalize()
            }
        }

        useHotbarSlotOrOffhand(
            slot,
            yRot = rotation.yaw,
            xRot = rotation.pitch,
        )

        when (ModuleAutoBuff.Rotations.rotationTiming) {
            ON_TICK -> {
                network.send(MovePacketType.FULL.generatePacket().apply {
                    yRot = player.withFixedYaw(currentRotation ?: player.rotation)
                    xRot = currentRotation?.pitch ?: player.xRot
                })
            }

            else -> {}
        }

        // Wait at least 1 tick to make sure, we do not continue with something else too early
        waitTicks(1)
    }

    override fun isValidPotion(stack: ItemStack) =
        stack.item is SplashPotionItem || stack.item is LingeringPotionItem && allowLingering

    private fun hasBenefit(entity: LivingEntity): Boolean {
        if (!entity.isAffectedByPotions) {
            return false
        }

        // If we look down about 90 degrees, the closet position of the potion is at the player foot
        val squareRange = entity.distanceToSqr(player)

        if (squareRange > BENEFICIAL_SQUARE_RANGE) {
            return false
        }

        return true

    }

    /**
     * Check if the player is standing inside a lingering potion cloud
     */
    private fun isStandingInsideLingering() =
        world.entitiesForRendering().any {
            it is AreaEffectCloud &&
                it.distanceToSqr(player) <= BENEFICIAL_SQUARE_RANGE &&
                it.potionContents.allEffects.any { effect ->
                effect.effect == MobEffects.REGENERATION || effect.effect == MobEffects.INSTANT_HEALTH
                    || effect.effect == MobEffects.STRENGTH
            }
        }

    /**
     * Check if splash potion is nearby to prevent throwing a potion that is not needed
     */
    private fun isSplashNearby() =
        world.entitiesForRendering().any {
            it is AbstractThrownPotion &&
                it.distanceToSqr(player) <= BENEFICIAL_SQUARE_RANGE
        }

}
