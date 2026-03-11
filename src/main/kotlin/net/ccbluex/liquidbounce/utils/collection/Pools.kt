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

package net.ccbluex.liquidbounce.utils.collection

import com.mojang.blaze3d.vertex.PoseStack
import net.ccbluex.fastutil.Pool
import net.ccbluex.liquidbounce.utils.render.reset
import net.minecraft.core.BlockPos
import org.joml.Matrix3x2f
import org.joml.Vector2f
import org.joml.Vector3f

object Pools {
    @JvmField
    val MatStack = Pool(::PoseStack, PoseStack::reset)

    @JvmField
    val Mat3x2f = Pool(::Matrix3x2f, Matrix3x2f::identity)

    @JvmField
    val Vec3f = Pool(::Vector3f, Vector3f::zero)

    @JvmField
    val Vec2f = Pool(::Vector2f, Vector2f::zero)

    @JvmField
    val MutableBlockPos: Pool<BlockPos.MutableBlockPos> = Pool(
        initializer = BlockPos::MutableBlockPos,
    ) { it.set(0, 0, 0) }.synchronized()

    @JvmField
    val StringBuilder: Pool<StringBuilder> = Pool(
        initializer = { StringBuilder(128) },
    ) { it.setLength(0) }.synchronized()

    /**
     * Use [Pools.StringBuilder] to build [String].
     */
    inline fun buildStringPooled(
        builderAction: StringBuilder.() -> Unit,
    ): String {
        val sb = StringBuilder.borrow()
        sb.builderAction()
        return sb.toString().also {
            StringBuilder.recycle(sb)
        }
    }

    /**
     * Use [Pools.StringBuilder] to build [String].
     */
    inline fun buildStringPooled(
        capacity: Int,
        builderAction: StringBuilder.() -> Unit,
    ): String {
        val sb = StringBuilder.borrow().apply { ensureCapacity(capacity) }
        sb.builderAction()
        return sb.toString().also {
            StringBuilder.recycle(sb)
        }
    }
}
