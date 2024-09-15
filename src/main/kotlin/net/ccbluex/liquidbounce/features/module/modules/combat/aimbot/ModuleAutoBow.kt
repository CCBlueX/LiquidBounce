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
package net.ccbluex.liquidbounce.features.module.modules.combat.aimbot

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.renderEnvironmentForGUI
import net.ccbluex.liquidbounce.utils.aiming.Rotation
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.MovePacketType
import net.ccbluex.liquidbounce.utils.client.toRadians
import net.ccbluex.liquidbounce.utils.combat.PriorityEnum
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.ccbluex.liquidbounce.utils.entity.*
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.geometry.Line
import net.ccbluex.liquidbounce.utils.render.OverlayTargetRenderer
import net.minecraft.client.network.AbstractClientPlayerEntity
import net.minecraft.entity.Entity
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.item.BowItem
import net.minecraft.item.TridentItem
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.Box
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import net.minecraft.world.RaycastContext
import java.util.*
import kotlin.math.*

/**
 * AutoBow module
 *
 * Automatically shoots with your bow when it's fully charged
 *  + and make it possible to shoot faster
 */
object ModuleAutoBow : Module("AutoBow", Category.COMBAT, aliases = arrayOf("BowAssist", "BowAimbot")) {
    const val ACCELERATION = -0.006
    const val REAL_ACCELERATION = -0.005

    private val random = Random()

    /**
     * Keeps track of the last bow shot that has taken place
     */
    private val lastShotTimer = Chronometer()

    @JvmStatic
    fun onStopUsingItem() {
        if (player.activeItem.item is BowItem) {
            lastShotTimer.reset()
        }
    }

    /**
     * Automatically shoots with your bow when you aim correctly at an enemy or when the bow is fully charged.
     */
    private object AutoShootOptions : ToggleableConfigurable(this, "AutoShoot", true) {

        val charged by int("Charged", 15, 3..20, suffix = "ticks")

        val chargedRandom by floatRange("ChargedRandom",
            0.0F..0.0F,
            -10.0F..10.0F,
            suffix = "ticks"
        )
        val delayBetweenShots by float("DelayBetweenShots", 0.0F, 0.0F..5.0F, suffix = "s")
        val aimThreshold by float("AimThreshold", 1.5F, 1.0F..4.0F, suffix = "°")
        val requiresHypotheticalHit by boolean("RequiresHypotheticalHit", false)

        var currentChargeRandom: Int? = null

        fun updateChargeRandom() {
            val lenHalf = (chargedRandom.endInclusive - chargedRandom.start) / 2.0F
            val mid = chargedRandom.start + lenHalf

            currentChargeRandom =
                (mid + random.nextGaussian() * lenHalf).toInt()
                    .coerceIn(chargedRandom.start.toInt()..chargedRandom.endInclusive.toInt())
        }

        fun getChargedRandom(): Int {
            if (currentChargeRandom == null) {
                updateChargeRandom()
            }

            return currentChargeRandom!!
        }

        @Suppress("unused")
        val tickRepeatable = handler<GameTickEvent> {
            val currentItem = player.activeItem?.item

            // Should check if player is using bow
            if (currentItem !is BowItem && currentItem !is TridentItem) {
                return@handler
            }

            if (player.itemUseTime < charged + getChargedRandom()) { // Wait until the bow is fully charged
                return@handler
            }
            if (!lastShotTimer.hasElapsed((delayBetweenShots * 1000.0F).toLong())) {
                return@handler
            }

            if (requiresHypotheticalHit) {
                val hypotheticalHit = getHypotheticalHit()

                if (hypotheticalHit == null || !hypotheticalHit.shouldBeAttacked()) {
                    return@handler
                }
            } else if (BowAimbotOptions.enabled) {
                if (BowAimbotOptions.targetTracker.lockedOnTarget == null) {
                    return@handler
                }

                val targetRotation = RotationManager.storedAimPlan ?: return@handler

                val aimDifference = RotationManager.rotationDifference(
                    RotationManager.serverRotation, targetRotation.rotation
                )

                if (aimDifference > aimThreshold) {
                    return@handler
                }
            }

            interaction.stopUsingItem(player)
            updateChargeRandom()
        }

        fun getHypotheticalHit(): AbstractClientPlayerEntity? {
            val rotation = RotationManager.serverRotation
            val yaw = rotation.yaw
            val pitch = rotation.pitch

            val velocity = getHypotheticalArrowVelocity(false)

            val vX = -MathHelper.sin(yaw.toRadians()) * MathHelper.cos(pitch.toRadians()) * velocity
            val vY = -MathHelper.sin(pitch.toRadians()) * velocity
            val vZ = MathHelper.cos(yaw.toRadians()) * MathHelper.cos(pitch.toRadians()) * velocity

            val arrow = SimulatedArrow(
                world,
                player.eyes,
                Vec3d(vX.toDouble(), vY.toDouble(), vZ.toDouble()),
                collideEntities = false
            )

            val players = findAndBuildSimulatedPlayers()

            for (i in 0 until 40) {
                val lastPos = arrow.pos

                arrow.tick()

                players.forEach { (entity, player) ->
                    val playerSnapshot = player.getSnapshotAt(i)

                    val playerHitBox =
                        Box(-0.3, 0.0, -0.3, 0.3, 1.8, 0.3)
                            .expand(0.3)
                            .offset(playerSnapshot.pos)

                    val raycastResult = playerHitBox.raycast(lastPos, arrow.pos)

                    raycastResult.orElse(null)?.let {
                        return entity
                    }
                }
            }

            return null
        }

        fun findAndBuildSimulatedPlayers(): List<Pair<AbstractClientPlayerEntity, SimulatedPlayerCache>> {
            return world.players.filter {
                it != player &&
                        Line(player.pos, player.rotationVector).squaredDistanceTo(it.pos) < 10.0 * 10.0
            }.map {
                Pair(it, PlayerSimulationCache.getSimulationForOtherPlayers(it))
            }
        }
    }

    /**
     * Bow aimbot automatically aims at enemy targets
     */
    private object BowAimbotOptions : ToggleableConfigurable(this, "BowAimbot", true) {

        // Target
        val targetTracker = TargetTracker(PriorityEnum.DISTANCE)

        // Rotation
        val rotationConfigurable = RotationsConfigurable(this)

        val minExpectedPull by int("MinExpectedPull", 5, 0..20, suffix = "ticks")

        init {
            tree(targetTracker)
            tree(rotationConfigurable)
        }

        private val targetRenderer = tree(OverlayTargetRenderer(ModuleAutoBow))

        @Suppress("unused")
        val tickRepeatable = repeatable {
            targetTracker.cleanup()

            // Should check if player is using bow
            val activeItem = player.activeItem?.item
            if (activeItem !is BowItem && activeItem !is TridentItem) {
                return@repeatable
            }

            val eyePos = player.eyes

            var rotation: Rotation? = null

            for (enemy in targetTracker.enemies()) {
                val rot = getRotationToTarget(enemy.box.center, eyePos, enemy) ?: continue

                targetTracker.lock(enemy)
                rotation = rot
                break
            }

            if (rotation == null) {
                return@repeatable
            }

            RotationManager.aimAt(
                rotation,
                priority = Priority.IMPORTANT_FOR_USAGE_1,
                provider = ModuleAutoBow,
                configurable = rotationConfigurable
            )
        }

        @Suppress("unused")
        val renderHandler = handler<OverlayRenderEvent> { event ->
            val target = targetTracker.lockedOnTarget ?: return@handler

            renderEnvironmentForGUI {
                targetRenderer.render(this, target, event.tickDelta)
            }
        }

    }

    private fun getRotationToTarget(
        targetPos: Vec3d,
        eyePos: Vec3d,
        target: Entity,
    ): Rotation? {
        var deltaPos = targetPos.subtract(eyePos)
        val basePrediction = predictBow(deltaPos, FastChargeOptions.enabled)

        val realTravelTime =
            getTravelTime(
                basePrediction.travelledOnX,
                cos(basePrediction.rotation.pitch.toRadians()) * basePrediction.pullProgress * 3.0 * 0.7,
            )

        if (!realTravelTime.isNaN()) {
            deltaPos =
                deltaPos.add(
                    (target.x - target.prevX) * realTravelTime,
                    (target.y - target.prevY) * realTravelTime,
                    (target.z - target.prevZ) * realTravelTime,
                )
        }

        val finalPrediction = predictBow(deltaPos, FastChargeOptions.enabled)
        val rotation = finalPrediction.rotation

        if (rotation.yaw.isNaN() || rotation.pitch.isNaN()) {
            return null
        }

        val pullProgress = finalPrediction.pullProgress
        val vertex = getHighestPointOfTrajectory(deltaPos, rotation, pullProgress)

        val positions = mutableListOf<Vec3d>()

        positions.add(eyePos)

        if (vertex != null) positions.add(vertex.add(eyePos))

        positions.add(targetPos)

        for (i in 0 until positions.lastIndex) {
            val raycast =
                world.raycast(
                    RaycastContext(
                        positions[i],
                        positions[i + 1],
                        RaycastContext.ShapeType.OUTLINE,
                        RaycastContext.FluidHandling.ANY,
                        player,
                    ),
                )

            if (raycast.type == HitResult.Type.BLOCK) return null
        }

        return rotation
    }

    private fun getHighestPointOfTrajectory(
        deltaPos: Vec3d,
        rotation: Rotation,
        pullProgress: Float,
    ): Vec3d? {
        val v0 = pullProgress * 3.0F * 0.7f

        val v_x = cos(Math.toRadians(rotation.pitch.toDouble())) * v0
        val v_y = sin(Math.toRadians(rotation.pitch.toDouble())) * v0

        val maxX = -(v_x * v_y) / (2.0 * ACCELERATION) // -(v_x*v_y)/a

        val maxY = -(v_y * v_y) / (4.0 * ACCELERATION) // -v_y^2/(2*a)

        val xPitch = cos(Math.toRadians(rotation.yaw.toDouble() - 90.0F))
        val zPitch = sin(Math.toRadians(rotation.yaw.toDouble() - 90.0F))

        val vertex =
            if (maxX < 0 && maxX * maxX < deltaPos.x * deltaPos.x + deltaPos.z * deltaPos.z) {
                Vec3d(
                    xPitch * maxX,
                    maxY,
                    zPitch * maxX,
                )
            } else {
                null
            }
        return vertex
    }

    fun getTravelTime(
        dist: Double,
        v0: Double,
    ): Float {
        return log((v0 / (ln(0.99)) + dist) / (v0 / (ln(0.99))), 0.99).toFloat()
    }

    override fun disable() {
        BowAimbotOptions.targetTracker.cleanup()
    }

    @Suppress("MaxLineLength")
    fun predictBow(
        target: Vec3d,
        assumeElongated: Boolean,
    ): BowPredictionResult {
        val travelledOnX = sqrt(target.x * target.x + target.z * target.z)

        val velocity = getHypotheticalArrowVelocity(assumeElongated)

        val yaw = (atan2(target.z, target.x) * 180.0f / Math.PI).toFloat() - 90.0f
        val pitch = (-Math.toDegrees(atan(
            (velocity * velocity - sqrt(
                velocity * velocity * velocity * velocity - 0.006f * (0.006f * (travelledOnX * travelledOnX) + 2
                    * target.y * (velocity * velocity)))
                ) / (0.006f * travelledOnX),
        ))).toFloat()

        return BowPredictionResult(
            Rotation(yaw, pitch),
            velocity,
            travelledOnX,
        )
    }

    private fun getHypotheticalArrowVelocity(
        assumeElongated: Boolean,
    ): Float {
        var velocity: Float =
            if (assumeElongated)
                1.0F
            else
                player.itemUseTime.coerceAtLeast(BowAimbotOptions.minExpectedPull) / 20.0F

        velocity = (velocity * velocity + velocity * 2.0f) / 3.0f

        if (velocity > 1.0f) {
            velocity = 1f
        }
        return velocity
    }

    class BowPredictionResult(val rotation: Rotation, val pullProgress: Float, val travelledOnX: Double)

    /**
     * @desc Fast charge options (like FastBow) can be used to charge the bow faster.
     * @warning Should only be used on vanilla minecraft. Most anti cheats patch these kinds of exploits
     *
     * TODO: Add version specific options
     */
    private object FastChargeOptions : ToggleableConfigurable(this, "FastCharge", false) {

        private val speed by int("Speed", 20, 3..20)

        private val notInTheAir by boolean("NotInTheAir", true)
        private val notDuringMove by boolean("NotDuringMove", false)
        private val notDuringRegeneration by boolean("NotDuringRegeneration", false)

        private val packetType by enumChoice("PacketType", MovePacketType.FULL)

        @Suppress("unused")
        val tickRepeatable = repeatable {
            val currentItem = player.activeItem

            // Should accelerated game ticks when using bow
            if (currentItem?.item is BowItem) {
                if (notInTheAir && !player.isOnGround) {
                    return@repeatable
                }

                if (notDuringMove && player.moving) {
                    return@repeatable
                }

                if (notDuringRegeneration && player.hasStatusEffect(StatusEffects.REGENERATION)) {
                    return@repeatable
                }

                repeat(speed) {
                    if (!player.isUsingItem) {
                        return@repeat
                    }

                    // Accelerate ticks (MC 1.8)
                    network.sendPacket(packetType.generatePacket())

                    // Just show visual effect (not required to work - but looks better)
                    player.tickActiveItemStack()
                }
            }
        }
    }

    init {
        tree(AutoShootOptions)
        tree(BowAimbotOptions)
        tree(FastChargeOptions)
    }
}
