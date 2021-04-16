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
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.raycast
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.entity.eyesPos
import net.minecraft.block.ShapeContext
import net.minecraft.block.SideShapeType
import net.minecraft.util.Hand
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.*

/**
 * Scaffold module
 *
 * Places blocks under you.
 */
object ModuleScaffold : Module("Scaffold", Category.WORLD) {

    // Rotation
    val rotationsConfigurable = tree(RotationsConfigurable())

    var currentTarget: Target? = null

    val networkTickHandler = repeatable { event ->
        updateTarget()
    }

    val handleInputsEvents = handler<InputHandleEvent> {
        val player = mc.player ?: return@handler
        val world = world

        val curr = currentTarget ?: return@handler
        val serverRotation = RotationManager.serverRotation ?: return@handler

        val rayTraceResult = raycast(4.5, serverRotation)

        if (rayTraceResult?.type != HitResult.Type.BLOCK || rayTraceResult.blockPos != curr.blockPos || rayTraceResult.pos.y < curr.minY) {
            return@handler
        }

        val result = interaction.interactBlock(
            player,
            world,
            Hand.MAIN_HAND,
            rayTraceResult
        )

        if (result.isAccepted) {
            if (result.shouldSwingHand()) {
                player.swingHand(Hand.MAIN_HAND)
            }

            currentTarget = null
        }
    }

    fun updateTarget() {
        this.currentTarget = null

        val pos = player.blockPos.add(0, -1, 0)
        val state = pos.getState()

        if (state!!.isSideSolid(mc.world!!, pos, Direction.UP, SideShapeType.CENTER)) {
            return
        }

        val offsetsToInvestigate = arrayOf(
            Vec3i(0, 0, 0),
            Vec3i(-1, 0, 0),
            Vec3i(1, 0, 0),
            Vec3i(0, 0, -1),
            Vec3i(0, 0, 1),
            Vec3i(0, -1, 0),
            Vec3i(-1, -1, 0),
            Vec3i(1, -1, 0),
            Vec3i(0, -1, -1),
            Vec3i(0, -1, 1),
        )

        for (vec3i in offsetsToInvestigate) {
            val posToInvestigate = pos.add(vec3i)

            if (posToInvestigate.getState()!!.isSideSolid(mc.world!!, posToInvestigate, Direction.UP, SideShapeType.CENTER)) {
                continue
            }

            val directionsToInvestigate = arrayOf(
                Direction.DOWN,
                Direction.NORTH,
                Direction.EAST,
                Direction.SOUTH,
                Direction.WEST,
            )

            val first = directionsToInvestigate.mapNotNull { direction ->
                val normalVector = direction.opposite.vector
                val currPos = posToInvestigate.add(direction.vector)
                val currState = currPos.getState() ?: return@mapNotNull null

                if (currState.isAir) {
                    return@mapNotNull null
                }

                val delta = player.eyesPos.subtract(
                    Vec3d.of(currPos).add(0.5, 0.5, 0.5).add(Vec3d.of(normalVector).multiply(0.5))
                )

                val angle = delta.dotProduct(Vec3d.of(normalVector)) / delta.distanceTo(Vec3d.ZERO)

                if (angle < 0) return@mapNotNull null

                Pair(direction, angle)
            }.maxByOrNull { it.second }

            if (first != null) {
                val currPos = posToInvestigate.add(first.first.vector)

                val truncate = true // TODO Find this out

                val currState = currPos.getState()

                val face = currState!!.getVisualShape(
                    mc.world,
                    currPos,
                    ShapeContext.of(player)
                ).boundingBoxes.mapNotNull {
                    var face = it.getFace(first.first)

                    if (truncate) {
                        face = face.truncate(0.5) ?: return@mapNotNull null
                    }

                    Pair(face, Vec3d(face.from.x + (face.to.x - face.from.x) * 0.5, face.from.y + (face.to.y - face.from.y) * 0.5, face.from.z + (face.to.z - face.from.z) * 0.5))
                }.maxWithOrNull(Comparator.comparingDouble<Pair<Face, Vec3d>> { it.second.subtract(Vec3d(0.5, 0.5, 0.5)).multiply(Vec3d.of(first.first.vector)).lengthSquared() }.thenComparingDouble { it.second.y })
                    ?: continue

                RotationManager.aimAt(face.second.add(Vec3d.of(currPos)), player.eyesPos, configurable = rotationsConfigurable)

                currentTarget = Target(currPos, first.first, face.first.from.y + currPos.y)

                break
            }
        }
    }

    fun Box.getFace(direction: Direction): Face {
        return when (direction) {
            Direction.DOWN -> Face(Vec3d(this.minX, this.minY, this.minZ), Vec3d(this.maxX, this.minY, this.maxZ))
            Direction.UP -> Face(Vec3d(this.minX, this.maxY, this.minZ), Vec3d(this.maxX, this.maxY, this.maxZ))
            Direction.NORTH -> Face(Vec3d(this.minX, this.minY, this.maxZ), Vec3d(this.maxX, this.maxY, this.maxZ))
            Direction.SOUTH -> Face(Vec3d(this.minX, this.minY, this.minZ), Vec3d(this.maxX, this.maxY, this.minZ))
            Direction.WEST -> Face(Vec3d(this.maxX, this.minY, this.minZ), Vec3d(this.maxX, this.maxY, this.maxZ))
            Direction.EAST -> Face(Vec3d(this.minX, this.minY, this.minZ), Vec3d(this.minX, this.maxY, this.maxZ))
        }
    }

    data class Face(val from: Vec3d, val to: Vec3d) {

        fun truncate(minY: Double): Face? {
            val newFace = Face(Vec3d(this.from.x, this.from.y.coerceAtLeast(minY), this.from.z), Vec3d(this.to.x, this.to.y.coerceAtLeast(minY), this.to.z))

            if (newFace.from.y > newFace.to.y) {
                return null
            }

            return newFace
        }

    }

    data class Target(val blockPos: BlockPos, val direction: Direction, val minY: Double)

}
