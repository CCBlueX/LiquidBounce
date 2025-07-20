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
 */
package net.ccbluex.liquidbounce.utils.entity

import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.math.times
import net.minecraft.client.world.ClientWorld
import net.minecraft.entity.projectile.ArrowEntity
import net.minecraft.entity.projectile.ProjectileUtil
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import net.minecraft.world.RaycastContext

class SimulatedArrow(
    val world: ClientWorld,
    var pos: Vec3d,
    var velocity: Vec3d,
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
            this.pos = it.pos
            this.inGround = true

            return it
        }

        pos = newPos

        return null
    }

    private fun updateCollision(pos: Vec3d, newPos: Vec3d): HitResult? {
        val world = this.world

        val arrowEntity = ArrowEntity(this.world, this.pos.x, this.pos.y, this.pos.z, ItemStack(Items.ARROW),
            null)

        // Get landing position
        val blockHitResult = world.raycast(
            RaycastContext(
                pos,
                newPos,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                arrowEntity
            )
        )

        if (this.collideEntities) {
//            val size = 0.3
            val size = 0.45

            val entityHitResult = ProjectileUtil.getEntityCollision(
                this.world,
                arrowEntity,
                pos,
                newPos,
                Box(
                    -size,
                    -size,
                    -size,
                    +size,
                    +size,
                    +size
                ).offset(pos).stretch(newPos.subtract(pos)).expand(1.0)
            ) {
                val canBeHit = !it.isSpectator && it.isAlive

                if (canBeHit && (it.canHit() || arrowEntity != mc.player && it == arrowEntity)) {
                    if (arrowEntity.isConnectedThroughVehicle(it)) return@getEntityCollision false
                } else {
                    return@getEntityCollision false
                }

                return@getEntityCollision true
            }

            // Check if arrow is landing
            if (entityHitResult != null && entityHitResult.type != HitResult.Type.MISS) {
                return entityHitResult
            }
        }

        if (blockHitResult != null && blockHitResult.type != HitResult.Type.MISS) {
            return blockHitResult
        }

        return null
    }

    @Suppress("FunctionOnlyReturningConstant")
    private fun isTouchingWater(): Boolean = false
}
