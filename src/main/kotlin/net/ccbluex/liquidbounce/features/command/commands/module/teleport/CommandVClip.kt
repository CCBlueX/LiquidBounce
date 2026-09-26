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
package net.ccbluex.liquidbounce.features.command.commands.module.teleport

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.FloatArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import net.ccbluex.liquidbounce.features.command.CommandRegistrar
import net.ccbluex.liquidbounce.features.command.brigadier.ClientCommandSource
import net.ccbluex.liquidbounce.features.command.brigadier.CmdI18n
import net.ccbluex.liquidbounce.features.command.brigadier.get
import net.ccbluex.liquidbounce.features.command.brigadier.register
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleTeleport
import net.ccbluex.liquidbounce.utils.block.canStandOn
import net.ccbluex.liquidbounce.utils.block.collisionShape
import net.ccbluex.liquidbounce.utils.client.MessageMetadata
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.markAsError
import net.ccbluex.liquidbounce.utils.client.player
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.shapes.BooleanOp
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import kotlin.math.abs

/**
 * VClip Command
 *
 * Allows you to clip through blocks.
 *
 * Module: [ModuleTeleport]
 */
object CommandVClip : CommandRegistrar {
    override fun register(dispatcher: CommandDispatcher<ClientCommandSource>) {
        dispatcher.register("vclip") {
            requires { it.isIngame }

            literal("by") {
                argument("distance", FloatArgumentType.floatArg()) { distance ->
                    exec { ctx ->
                        val dy = ctx.get(distance)

                        ModuleTeleport.indicateTeleport(getX(), getY() + dy, getZ())
                        1
                    }
                }
            }
            literal("smart") {
                literal("up") {
                    optional("max", IntegerArgumentType.integer(), default = 10) { max ->
                        exec { ctx ->
                            performAutomaticClip(abs(ctx.get(max)), Direction.UP)
                            1
                        }
                    }
                }
                literal("down") {
                    optional("max", IntegerArgumentType.integer(), default = 10) { max ->
                        exec { ctx ->
                            performAutomaticClip(abs(ctx.get(max)), Direction.DOWN)
                            1
                        }
                    }
                }
            }
        }
    }

    private fun CmdI18n.performAutomaticClip(max: Int, direction: Direction) {
        val blockPos = player.vehicle?.blockPosition() ?: player.blockPosition()
        val pos = player.vehicle?.position() ?: player.position()

        var newPos = blockPos

        // avoid clipping on the block we're already on
        if (direction == Direction.DOWN) {
            newPos = newPos.below()
        }

        for (x in 0 until max) {
            // go to the next position in the direction
            newPos = newPos.relative(direction)

            val shape = newPos.collisionShape

            // we have to be able to stand on the position
            if (canTpOn(newPos, shape)) {
                // allows clipping on fences, etc.
                val vOffset = shape.max(Direction.Axis.Y)

                val dy = (newPos.y + vOffset) - pos.y

                // check if the found position is too far away
                if (abs(dy) > max) {
                    break
                }

                // teleport
                ModuleTeleport.indicateTeleport(getX(), getY() + dy, getZ())
                return
            }
        }

        chat(
            markAsError(t("noPositionFound")),
            metadata = MessageMetadata(id = "CVClip#info")
        )
    }

    private fun canTpOn(pos: BlockPos, posCollisionShape: VoxelShape): Boolean {
        // check if there is enough space at the new position
        val boundingBox = player.vehicle?.boundingBox ?: player.boundingBox

        if (isNotEnoughSpaceAboveBlock(pos, boundingBox, posCollisionShape)) {
            return false
        }

        player.vehicle?.let {
            if (isNotEnoughSpaceAboveBlock(pos, player.boundingBox, posCollisionShape)) {
                return false
            }
        }

        // a simple case, we can stand on the position
        if (pos.canStandOn()) {
            return true
        }

        // even tho canStandOn returns false the block might not be full on the upper side, but we can stand on it tho
        val shape = posCollisionShape.move(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())
        val dy = shape.min(Direction.Axis.Y) - boundingBox.min(Direction.Axis.Y)
        return Shapes.joinIsNotEmpty(
            shape,
            Shapes.create(boundingBox.move(0.0, dy, 0.0)),
            BooleanOp.AND
        )
    }

    private fun isNotEnoughSpaceAboveBlock(pos: BlockPos, boundingBox: AABB, posCollisionShape: VoxelShape): Boolean {
        val requiredHeight = boundingBox.maxY - boundingBox.minY - (1.0 - posCollisionShape.max(Direction.Axis.Y))
        var accumulatedHeight = 0.0
        var newPos = pos

        while (accumulatedHeight < requiredHeight) {
            newPos = newPos.above()
            val collisionShape = newPos.collisionShape

            if (!collisionShape.isEmpty) {
                val maxAvailableHeight = collisionShape.min(Direction.Axis.Y)
                if (maxAvailableHeight < requiredHeight - accumulatedHeight) {
                    return true
                }
            }

            accumulatedHeight += 1.0
        }

        return false
    }

    private fun getX() = player.vehicle?.x ?: player.x

    private fun getY() = player.vehicle?.y ?: player.y

    private fun getZ() = player.vehicle?.z ?: player.z

}
