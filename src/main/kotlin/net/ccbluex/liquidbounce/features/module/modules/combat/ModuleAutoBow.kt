/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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

import net.ccbluex.liquidbounce.event.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.engine.Vec3
import net.ccbluex.liquidbounce.utils.extensions.*
import net.minecraft.item.BowItem
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import kotlin.math.*

/**
 * AutoBow module
 *
 * Automatically shoots with your bow when it's fully charged
 *  + and make it possible to shoot faster
 */
object ModuleAutoBow : Module("AutoBow", Category.COMBAT) {

    /**
     * Automatically shoots with your bow when you aim correctly at an enemy or when the bow is fully charged.
     */
    private object AutoShootOptions : ToggleableConfigurable(this, "AutoShoot", true) {

        val charged by int("Charged", 20, 3..20)

        val tickRepeatable = repeatable {
            val currentItem = player.activeItem

            // Should check if player is using bow
            if (currentItem?.item is BowItem) {
                // Wait until bow is fully charged
                if (player.itemUseTime < charged)
                    return@handler

                println("gay")

                // Send stop using item to server
                network.sendPacket(
                    PlayerActionC2SPacket(
                        PlayerActionC2SPacket.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN,
                        Direction.DOWN
                    )
                )
                // Stop using item client-side
                player.stopUsingItem()
            }
        }
    }

    /**
     * Bow aimbot automatically aims at enemy targets
     */
    private object BowAimbotOptions : ToggleableConfigurable(this, "BowAimbot", false) {

        // Target
        val targetTracker = TargetTracker(TargetTracker.Priority.DISTANCE)

        // Rotation
        val rotationConfigurable = RotationsConfigurable()

        val predictSize by float("PredictionCofactor", 1.0f, 0.0f..1.5f)

        init {
            tree(targetTracker)
            tree(rotationConfigurable)
        }

        val tickRepeatable = handler<GameTickEvent> {
            targetTracker.update()

            targetTracker.lockedOnTarget = null

            val world = mc.world ?: return@handler
            val player = player


            // Should check if player is using bow
            if (player.activeItem?.item !is BowItem)
                return@handler


            val eyePos = player.eyesPos

            val target = targetTracker.firstOrNull() ?: return@handler

            var deltaPos = target.boundingBox.center.subtract(eyePos)

            val basePrediction = predictBow(deltaPos, FastChargeOptions.enabled)

            val v0 =
                cos(Math.toRadians(basePrediction.rotation.pitch.toDouble())) * basePrediction.pullProgress * 3.0F * 0.7f
            val ticksTravelled = (predictSize * basePrediction.travelledOnX) / v0 + 1.0f
            val realTravelTime = getTravelTime(ticksTravelled, basePrediction.travelledOnX, v0)

            if (!ticksTravelled.isNaN()) {
                deltaPos = deltaPos.add(
                    (target.x - target.prevX) * realTravelTime,
                    (target.y - target.prevY) * realTravelTime,
                    (target.z - target.prevZ) * realTravelTime,
                )
            }

            val rotation = predictBow(deltaPos, FastChargeOptions.enabled).rotation

            if (rotation.yaw.isNaN() || rotation.pitch.isNaN())
                return@handler

            player.yaw = rotation.yaw
            player.pitch = rotation.pitch

            RotationManager.aimAt(rotation, configurable = rotationConfigurable)
        }

    }

    val ONE_OVER_LN = 1 / ln(0.99)

    fun getTravelTime(oldTickDelta: Double, dist: Double, v0: Double, iterations: Int = 10): Float {
        return log((v0 / (ln(0.99)) + dist) / (v0 / (ln(0.99))), 0.99).toFloat()
    }

    override fun disable() {
        BowAimbotOptions.targetTracker.cleanup()
    }

    fun predictBow(target: Vec3d, assumeElongated: Boolean): BowPredictionResult {
        val player = player

        val travelledOnX = sqrt(target.x * target.x + target.z * target.z)

        var velocity: Float = if (assumeElongated) 1f else player.itemUseTime / 20f

        velocity = (velocity * velocity + velocity * 2.0f) / 3.0f

        if (velocity > 1.0f)
            velocity = 1f

        return BowPredictionResult(
            Rotation(
                (atan2(target.z, target.x) * 180.0f / Math.PI).toFloat() - 90.0f,
                (-Math.toDegrees(Math.atan((velocity * velocity - Math.sqrt(velocity * velocity * velocity * velocity - 0.006f * (0.006f * (travelledOnX * travelledOnX) + 2 * target.y * (velocity * velocity)))) / (0.006f * travelledOnX)))).toFloat()
            ),
            velocity,
            travelledOnX
        )
    }

    class BowPredictionResult(val rotation: Rotation, val pullProgress: Float, val travelledOnX: Double)

    fun getRotationVector(pitch: Float, yaw: Float): Vec3 {
        val f = pitch * 0.017453292f
        val g = -yaw * 0.017453292f

        val h = MathHelper.cos(g)
        val i = MathHelper.sin(g)
        val j = MathHelper.cos(f)
        val k = MathHelper.sin(f)

        return Vec3(i * j, -k, h * j)
    }

    /**
     * @desc Fast charge options (like FastBow) can be used to charge the bow faster.
     * @warning Should only be used on vanilla minecraft. Most anti cheats patch these kinds of exploits
     *
     * TODO: Add version specific options
     */
    private object FastChargeOptions : ToggleableConfigurable(this, "FastCharge", true) {

        val packets by int("Packets", 20, 3..20)

        val tickRepeatable = repeatable {
            val currentItem = player.activeItem

            // Should accelerated game ticks when using bow
            if (currentItem?.item is BowItem) {
                repeat(packets) {
                    // Send movement packet to simulate ticks (has been patched in 1.19)
                    network.sendPacket(PlayerMoveC2SPacket(true))
                    // Just show visual effect (not required to work - but looks better)
                    player.tickActiveItemStack()
                }

                // Shoot with bow (auto shoot has to be enabled)
                // TODO: Depend on Auto Shoot
            }
        }

    }

    init {
        tree(AutoShootOptions)
        tree(BowAimbotOptions)
        tree(FastChargeOptions)
    }

}


