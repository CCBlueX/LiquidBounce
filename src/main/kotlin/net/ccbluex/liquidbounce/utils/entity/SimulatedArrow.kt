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

import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.math.times
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.world.entity.projectile.ProjectileUtil
import net.minecraft.world.entity.projectile.arrow.Arrow
import net.minecraft.world.item.Items
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3

class SimulatedArrow(
    val level: ClientLevel,
    var pos: Vec3,
    var velocity: Vec3,
    private val collideEntities: Boolean = true
) {
    var inGround = false

    fun tick(): HitResult? {
        if (this.inGround) {
            return null
        }

        val newPos = pos + velocity

        val drag = if (isTouchingWater()) {
            0.6
        } else {
            0.99
        }

        velocity *= drag

        velocity.y -= 0.05000000074505806

        updateCollision(pos, newPos)?.let {
            this.pos = it.location
            this.inGround = true

            return it
        }

        pos = newPos

        return null
    }

    @Suppress("CognitiveComplexMethod")
    private fun updateCollision(pos: Vec3, newPos: Vec3): HitResult? {
        val world = this.level

        val arrowEntity = Arrow(
            this.level, this.pos.x, this.pos.y, this.pos.z,
            Items.ARROW.defaultInstance, null
        )

        // Get landing position
        val blockHitResult = world.clip(
            ClipContext(
                pos,
                newPos,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                arrowEntity
            )
        )

        if (this.collideEntities) {
//            val size = 0.3
            val size = 0.45

            val entityHitResult = ProjectileUtil.getEntityHitResult(
                this.level,
                arrowEntity,
                pos,
                newPos,
                AABB(
                    -size,
                    -size,
                    -size,
                    +size,
                    +size,
                    +size
                ).move(pos).expandTowards(newPos.subtract(pos)).inflate(1.0)
            ) {
                val canBeHit = !it.isSpectator && it.isAlive

                if (canBeHit && (it.isPickable || arrowEntity != mc.player && it == arrowEntity)) {
                    if (arrowEntity.isPassengerOfSameVehicle(it)) return@getEntityHitResult false
                } else {
                    return@getEntityHitResult false
                }

                return@getEntityHitResult true
            }

            // Check if arrow is landing
            if (entityHitResult != null && entityHitResult.type != HitResult.Type.MISS) {
                return entityHitResult
            }
        }

        return blockHitResult.takeIf { it.type != HitResult.Type.MISS }
    }

    @Suppress("FunctionOnlyReturningConstant")
    private fun isTouchingWater(): Boolean = false
}
