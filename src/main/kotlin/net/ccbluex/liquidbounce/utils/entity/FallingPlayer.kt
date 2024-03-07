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
package net.ccbluex.liquidbounce.utils.entity

import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.entity.Entity
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import net.minecraft.world.RaycastContext
import kotlin.math.sqrt

class FallingPlayer(
    private val player: ClientPlayerEntity,
    var x: Double,
    var y: Double,
    var z: Double,
    private var motionX: Double,
    private var motionY: Double,
    private var motionZ: Double,
    private val yaw: Float
) {
    companion object {
        fun fromPlayer(player: ClientPlayerEntity): FallingPlayer {
            return FallingPlayer(
                player,
                player.x,
                player.y,
                player.z,
                player.velocity.x,
                player.velocity.y,
                player.velocity.z,
                player.yaw
            )
        }
    }

    private var simulatedTicks: Int = 0

    private fun calculateForTick(rotationVec: Vec3d) {
        var d = 0.08
        val bl: Boolean = motionY <= 0.0

        if (bl && hasStatusEffect(StatusEffects.SLOW_FALLING)) {
            d = 0.01
        }

        val j: Float = this.player.pitch * 0.017453292f

        val k = sqrt(rotationVec.x * rotationVec.x + rotationVec.z * rotationVec.z)
        val l = sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ)

        val m = rotationVec.length()
        var n = MathHelper.cos(j)

        n = (n.toDouble() * n.toDouble() * 1.0.coerceAtMost(m / 0.4)).toFloat()

        var vec3d5 = Vec3d(this.motionX, this.motionY, this.motionZ).add(0.0, d * (-1.0 + n.toDouble() * 0.75), 0.0)

        var q: Double
        if (vec3d5.y < 0.0 && k > 0.0) {
            q = vec3d5.y * -0.1 * n.toDouble()
            vec3d5 = vec3d5.add(rotationVec.x * q / k, q, rotationVec.z * q / k)
        }

        if (j < 0.0f && k > 0.0) {
            q = l * (-MathHelper.sin(j)).toDouble() * 0.04
            vec3d5 = vec3d5.add(-rotationVec.x * q / k, q * 3.2, -rotationVec.z * q / k)
        }

        if (k > 0.0) {
            vec3d5 = vec3d5.add((rotationVec.x / k * l - vec3d5.x) * 0.1, 0.0, (rotationVec.z / k * l - vec3d5.z) * 0.1)
        }

        vec3d5.add(
            Entity.movementInputToVelocity(
                Vec3d(
                    this.player.input.movementSideways.toDouble() * 0.98,
                    0.0,
                    this.player.input.movementForward.toDouble() * 0.98
                ),
                0.02F,
                yaw
            )
        )

        val velocityCoFactor: Float = this.player.velocityMultiplier

        this.motionX = vec3d5.x * 0.9900000095367432 * velocityCoFactor
        this.motionY = vec3d5.y * 0.9800000190734863
        this.motionZ = vec3d5.z * 0.9900000095367432 * velocityCoFactor

        this.x += this.motionX
        this.y += this.motionY
        this.z += this.motionZ

        this.simulatedTicks++
    }

    private fun hasStatusEffect(effect: RegistryEntry<StatusEffect>): Boolean {
        val instance = player.getStatusEffect(effect) ?: return false

        return instance.duration >= this.simulatedTicks
    }

    fun findCollision(ticks: Int): CollisionResult? {
        val rotationVec = player.rotationVector

        for (i in 0 until ticks) {
            val start = Vec3d(x, y, z)

            calculateForTick(rotationVec)

            val end = Vec3d(x, y, z)
            var raytracedBlock: BlockPos?
            val w = player.width / 2.0

            if (rayTrace(start, end).also { raytracedBlock = it } != null) return CollisionResult(raytracedBlock, i)
            if (rayTrace(start.add(w, 0.0, w), end).also { raytracedBlock = it } != null) return CollisionResult(
                raytracedBlock,
                i
            )
            if (rayTrace(start.add(-w, 0.0, w), end).also { raytracedBlock = it } != null) return CollisionResult(
                raytracedBlock,
                i
            )
            if (rayTrace(start.add(w, 0.0, -w), end).also { raytracedBlock = it } != null) return CollisionResult(
                raytracedBlock,
                i
            )
            if (rayTrace(start.add(-w, 0.0, -w), end).also { raytracedBlock = it } != null) return CollisionResult(
                raytracedBlock,
                i
            )
            if (rayTrace(start.add(w, 0.0, w / 2f), end).also {
                raytracedBlock = it
            } != null
            ) return CollisionResult(raytracedBlock, i)
            if (rayTrace(start.add(-w, 0.0, w / 2f), end).also {
                raytracedBlock = it
            } != null
            ) return CollisionResult(raytracedBlock, i)
            if (rayTrace(start.add(w / 2f, 0.0, w), end).also {
                raytracedBlock = it
            } != null
            ) return CollisionResult(raytracedBlock, i)
            if (rayTrace(start.add(w / 2f, 0.0, -w), end).also {
                raytracedBlock = it
            } != null
            ) return CollisionResult(raytracedBlock, i)
        }
        return null
    }

    private fun rayTrace(start: Vec3d, end: Vec3d): BlockPos? {
        val result = mc.world!!.raycast(
            RaycastContext(
                start,
                end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.ANY,
                player
            )
        )

        return if (result != null && result.type == HitResult.Type.BLOCK && result.side == Direction.UP) {
            result.blockPos
        } else null
    }

    class CollisionResult(val pos: BlockPos?, val tick: Int)
}
