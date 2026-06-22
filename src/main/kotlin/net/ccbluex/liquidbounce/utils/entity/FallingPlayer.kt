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

import net.ccbluex.liquidbounce.utils.client.world
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.util.Mth
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.Pose
import net.minecraft.world.phys.Vec3
import kotlin.jvm.optionals.getOrNull
import kotlin.math.sqrt

@Suppress("LongParameterList")
class FallingPlayer(
    private val player: LocalPlayer,
    var x: Double,
    var y: Double,
    var z: Double,
    private var motionX: Double,
    private var motionY: Double,
    private var motionZ: Double,
    private val yRot: Float
) {
    companion object {
        @JvmStatic
        fun fromPlayer(player: LocalPlayer): FallingPlayer {
            return FallingPlayer(
                player,
                player.x,
                player.y,
                player.z,
                player.deltaMovement.x,
                player.deltaMovement.y,
                player.deltaMovement.z,
                player.yRot
            )
        }
    }

    private var simulatedTicks: Int = 0

    private fun calculateForTick(rotationVec: Vec3) {
        var d = 0.08
        val bl: Boolean = motionY <= 0.0

        if (bl && hasStatusEffect(MobEffects.SLOW_FALLING)) {
            d = 0.01
        }

        val j: Double = this.player.xRot.toDouble() * Mth.DEG_TO_RAD

        val k = sqrt(rotationVec.x * rotationVec.x + rotationVec.z * rotationVec.z)
        val l = sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ)

        val m = rotationVec.length()
        var n = Mth.cos(j)

        n = (n.toDouble() * n.toDouble() * 1.0.coerceAtMost(m / 0.4)).toFloat()

        var vec3d5 = Vec3(this.motionX, this.motionY, this.motionZ).add(0.0, d * (-1.0 + n.toDouble() * 0.75), 0.0)

        var q: Double
        if (vec3d5.y < 0.0 && k > 0.0) {
            q = vec3d5.y * -0.1 * n.toDouble()
            vec3d5 = vec3d5.add(rotationVec.x * q / k, q, rotationVec.z * q / k)
        }

        if (j < 0.0 && k > 0.0) {
            q = l * (-Mth.sin(j)).toDouble() * 0.04
            vec3d5 = vec3d5.add(-rotationVec.x * q / k, q * 3.2, -rotationVec.z * q / k)
        }

        if (k > 0.0) {
            vec3d5 = vec3d5.add((rotationVec.x / k * l - vec3d5.x) * 0.1, 0.0, (rotationVec.z / k * l - vec3d5.z) * 0.1)
        }

        vec3d5.add(
            Entity.getInputVector(
                Vec3(
                    this.player.input.movementSideways.toDouble() * 0.98,
                    0.0,
                    this.player.input.movementForward.toDouble() * 0.98
                ),
                0.02F,
                yRot
            )
        )

        val velocityCoFactor: Float = this.player.blockSpeedFactor

        this.motionX = vec3d5.x * 0.9900000095367432 * velocityCoFactor
        this.motionY = vec3d5.y * 0.9800000190734863
        this.motionZ = vec3d5.z * 0.9900000095367432 * velocityCoFactor

        this.x += this.motionX
        this.y += this.motionY
        this.z += this.motionZ

        this.simulatedTicks++
    }

    private fun hasStatusEffect(effect: Holder<MobEffect>): Boolean {
        val instance = player.getEffect(effect) ?: return false

        return instance.duration >= this.simulatedTicks
    }

    fun findCollision(ticks: Int): CollisionResult? {
        val rotationVec = player.lookAngle

        for (i in 0 until ticks) {
            val start = Vec3(x, y, z)

            calculateForTick(rotationVec)

            val end = Vec3(x, y, z)

            val box = player.getDimensions(Pose.STANDING).makeBoundingBox(start).expandTowards(end.subtract(start))

            world.findSupportingBlock(player, box).getOrNull()?.let {
                return CollisionResult(it, i)
            }
        }
        return null
    }

    class CollisionResult(val pos: BlockPos?, val tick: Int)
}
