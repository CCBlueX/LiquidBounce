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
import net.ccbluex.liquidbounce.utils.network.MovePacketType
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.ccbluex.liquidbounce.utils.entity.FallingPlayer
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.ccbluex.liquidbounce.utils.inventory.useHotbarSlotOrOffhand
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.yaw
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryInfo
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryInfoRenderer
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryType
import net.ccbluex.liquidbounce.utils.world.any
import net.ccbluex.liquidbounce.utils.world.entityGetter
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.LingeringPotionItem
import net.minecraft.world.item.SplashPotionItem
import net.minecraft.world.level.entity.EntityTypeTest
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import java.util.concurrent.ThreadLocalRandom

internal object Pot : StatusEffectBasedBuff("Pot") {

    private const val BENEFICIAL_SQUARED_RANGE = 16.0

    override val passesRequirements: Boolean
        get() {
            if (InventoryManager.isInventoryOpen) {
                return false
            }

            if (doNotBenefitOthers) {
                // Check if there is any entity that we care about that can benefit from the potion
                // This means we will only care about entities that are our enemies and are close enough to us
                // That means we will still throw the potion if there is a friendly friend or team member nearby
                val benefits = world.entitiesForRendering().any {
                    it is LivingEntity && it.shouldBeAttacked() && isAffectedByPotionInRange(it)
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
        var rotation = calculateSplashRotation()

        when (ModuleAutoBuff.Rotations.rotationTiming) {
            NORMAL -> {
                RotationManager.setRotationTarget(
                    rotation,
                    valueGroup = ModuleAutoBuff.Rotations,
                    provider = ModuleAutoBuff,
                    priority = Priority.IMPORTANT_FOR_PLAYER_LIFE
                )

                tickUntil {
                    !inGame || (currentRotation ?: player.rotation).pitch >= rotation.pitch - 1
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

        if (!inGame) return // TODO: I think we should edit the continuation interceptor here

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

    /**
     * Calculates a rotation that lands the splash potion near the player's predicted position.
     *
     * The potion inherits the player's velocity, so while moving the landing point drifts. We simulate the
     * exact potion trajectory and iteratively adjust the yaw towards the predicted landing spot.
     *
     * ponytail: pitch stays near vertical and relies on the splash's large radius to cover the player;
     *   precise landing control / throwing against walls (see #2051) is deferred.
     */
    private fun calculateSplashRotation(): Rotation {
        var rotation = Rotation(player.yRot, ThreadLocalRandom.current().nextFloat(85f, 90f))

        repeat(4) { _ ->
            val sim = TrajectoryInfoRenderer.getHypotheticalTrajectory(
                simulationOwner = player,
                trajectoryInfo = TrajectoryInfo.POTION,
                trajectoryType = TrajectoryType.Potion,
                rotation = rotation,
            ).runSimulation(300)

            val hitPos = (sim.hitResult as? BlockHitResult)?.location ?: return rotation
            val flightTicks = sim.positions.size.toDouble()
            // Predict where the player will be by the time the potion lands (the potion inherits velocity)
            val predictedPos = player.position().add(
                player.deltaMovement.x * flightTicks,
                0.0,
                player.deltaMovement.z * flightTicks,
            )
            val target = Vec3(predictedPos.x, player.eyeY - 0.1, predictedPos.z)
            val diff = target.subtract(hitPos)

            // Good enough: the splash radius (4 blocks) covers the player
            if (diff.horizontalDistanceSqr() < 3.0 * 3.0) {
                return rotation
            }

            rotation = Rotation(diff.yaw, rotation.pitch)
        }

        return rotation
    }

    override fun isValidPotion(stack: ItemStack) =
        stack.item is SplashPotionItem || stack.item is LingeringPotionItem && allowLingering

    private fun isAffectedByPotionInRange(entity: LivingEntity): Boolean {
        if (!entity.isAffectedByPotions) {
            return false
        }

        // If we look down about 90 degrees, the closest position of the potion is at the player foot
        val squareRange = entity.distanceToSqr(player)

        if (squareRange > BENEFICIAL_SQUARED_RANGE) {
            return false
        }

        return true

    }

    /**
     * Check if the player is standing inside a lingering potion cloud
     */
    private fun isStandingInsideLingering() =
        world.entityGetter.any(EntityTypes.AREA_EFFECT_CLOUD) {
            it.distanceToSqr(player) <= BENEFICIAL_SQUARED_RANGE &&
                it.potionContents.allEffects.any { effect ->
                    effect.effect == MobEffects.REGENERATION || effect.effect == MobEffects.INSTANT_HEALTH
                        || effect.effect == MobEffects.STRENGTH
                }
        }

    /**
     * Check if splash potion is nearby to prevent throwing a potion that is not needed
     */
    private fun isSplashNearby() =
        world.entityGetter.any(EntityTypeTest.forClass(AbstractThrownPotion::class.java)) {
            it.distanceToSqr(player) <= BENEFICIAL_SQUARED_RANGE
        }

}
