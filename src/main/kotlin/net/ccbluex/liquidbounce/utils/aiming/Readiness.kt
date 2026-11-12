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
package net.ccbluex.liquidbounce.utils.aiming

import net.ccbluex.liquidbounce.features.addon.AddonApi
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.utils.RotationUtil
import net.ccbluex.liquidbounce.utils.aiming.utils.RotationUtil.angleDifference
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.math.fma
import net.ccbluex.liquidbounce.utils.raytracing.hasLineOfSight
import net.ccbluex.liquidbounce.utils.raytracing.isLookingAtEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import kotlin.jvm.optionals.getOrNull
import kotlin.math.abs
import kotlin.math.max

/**
 * What a module does once the rotation going out this tick is good enough, which is when vanilla acts on a look too:
 * [hit] gets that rotation and the target it belongs to and returns what an action with it would hit, null while it
 * misses; [then] runs with that, once.
 */
@AddonApi
class Readiness<T : Any> private constructor(
    private val hit: (Rotation, RotationTarget) -> T?,
    private val then: (T) -> Unit,
) {

    /**
     * Ready once [hit] returns something for the rotation going out.
     */
    constructor(hit: (Rotation) -> T?, then: (T) -> Unit) : this({ rotation, _ -> hit(rotation) }, then)

    private var done = false

    /**
     * Runs [then] if [rotation] hits, unless it ran already.
     */
    internal fun offer(rotation: Rotation, target: RotationTarget) {
        if (done) {
            return
        }

        val result = hit(rotation, target) ?: return
        done = true
        then(result)
    }

    @AddonApi
    companion object {

        /**
         * Ready once the rotation meets its own target: hits its box or entity, or else is within its tolerance of
         * the axes it turns. A rotation on the mouse grid cannot come closer than half a step.
         */
        @JvmStatic
        fun reached(then: (Rotation) -> Unit) = Readiness<Rotation>({ rotation, target ->
            rotation.takeIf { target.isMetBy(rotation) }
        }, then)

        /**
         * Ready once an attack would hit [entity]: within [range], or within [throughWallsRange] with blocks in the
         * way.
         */
        @JvmStatic
        @JvmOverloads
        fun entity(
            entity: Entity,
            range: Double,
            throughWallsRange: Double = 0.0,
            then: (EntityHitResult) -> Unit,
        ) = Readiness({ rotation ->
            isLookingAtEntity(player, entity, rotation, range, throughWallsRange)
        }, then)

        /**
         * Ready once the first block the ray meets within [range] is [pos], on [side] if it is given.
         */
        @JvmStatic
        @JvmOverloads
        fun block(
            pos: BlockPos,
            range: Double,
            side: Direction? = null,
            then: (BlockHitResult) -> Unit,
        ) = Readiness({ rotation ->
            val eyes = player.eyePosition
            val result = world.clip(
                ClipContext(
                    eyes,
                    eyes.fma(range, rotation.directionVector),
                    ClipContext.Block.OUTLINE,
                    ClipContext.Fluid.NONE,
                    player,
                )
            )
            result.takeIf { it.type == HitResult.Type.BLOCK && it.blockPos == pos && (side == null || it.direction == side) }
        }, then)

    }

}

/**
 * Whether [rotation] meets this target: the ray from the player's eyes hits [RotationTarget.hitBox], in sight for an
 * entity, or else the turned axes are within [RotationTarget.tolerance], at least half a mouse step.
 */
internal fun RotationTarget.isMetBy(rotation: Rotation): Boolean {
    val box = hitBox
    if (box != null) {
        return player.eyePosition.hits(rotation, box, inSight = entity != null)
    }

    val margin = max(tolerance, RotationUtil.gcd.toFloat() / 2)
    val yaw = yaw
    val pitch = pitch
    return (yaw == null || abs(angleDifference(rotation.yaw, yaw)) <= margin) &&
        (pitch == null || abs(rotation.pitch - pitch) <= margin)
}

/**
 * Whether the ray of [rotation] from these eyes hits [box], and with [inSight] does so with no block in the way.
 */
internal fun Vec3.hits(rotation: Rotation, box: AABB, inSight: Boolean): Boolean {
    if (box.contains(this)) {
        return true
    }

    val reach = distanceTo(box.center) + box.size
    val point = box.clip(this, fma(reach, rotation.directionVector)).getOrNull() ?: return false
    return !inSight || hasLineOfSight(this, point)
}
