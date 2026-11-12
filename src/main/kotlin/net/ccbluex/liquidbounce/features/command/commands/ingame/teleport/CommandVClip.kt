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
package net.ccbluex.liquidbounce.features.command.commands.ingame.teleport

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.FloatArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import net.ccbluex.liquidbounce.features.command.CommandRegistrar
import net.ccbluex.liquidbounce.features.command.brigadier.ClientCommandSource
import net.ccbluex.liquidbounce.features.command.brigadier.CmdI18n
import net.ccbluex.liquidbounce.features.command.brigadier.get
import net.ccbluex.liquidbounce.features.command.brigadier.register
import net.ccbluex.liquidbounce.features.global.GlobalSettingsTeleport
import net.ccbluex.liquidbounce.utils.block.canStandOn
import net.ccbluex.liquidbounce.utils.block.collisionShape
import net.ccbluex.liquidbounce.utils.client.MessageMetadata
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.markAsError
import net.ccbluex.liquidbounce.utils.client.player
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.shapes.BooleanOp
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import kotlin.math.abs

/** Clips the player, or its vehicle, up or down: `by` a distance, or `smart` onto the next block to stand on. */
object CommandVClip : CommandRegistrar {

    private val entity: Entity
        get() = player.vehicle ?: player

    override fun register(dispatcher: CommandDispatcher<ClientCommandSource>) {
        dispatcher.register("vclip") {
            requires { it.isIngame }

            literal("by") {
                argument("distance", FloatArgumentType.floatArg()) { distance ->
                    exec { ctx ->
                        clip(ctx.get(distance).toDouble())
                        1
                    }
                }
            }
            literal("smart") {
                for (direction in arrayOf(Direction.UP, Direction.DOWN)) {
                    literal(direction.serializedName) {
                        optional("max", IntegerArgumentType.integer(), default = 10) { max ->
                            exec { ctx ->
                                smartClip(abs(ctx.get(max)), direction)
                                1
                            }
                        }
                    }
                }
            }
        }
    }

    private fun clip(dy: Double) = GlobalSettingsTeleport.teleport(entity.x, entity.y + dy, entity.z)

    private fun CmdI18n.smartClip(max: Int, direction: Direction) {
        // Down starts below the block the player is on.
        var pos = entity.blockPosition().let { if (direction == Direction.DOWN) it.below() else it }

        for (step in 0 until max) {
            pos = pos.relative(direction)
            val shape = pos.collisionShape
            if (!canTpOn(pos, shape)) {
                continue
            }

            // The top of the shape, so fences and the like work.
            val dy = (pos.y + shape.max(Direction.Axis.Y)) - entity.y
            if (abs(dy) > max) {
                break
            }

            clip(dy)
            return
        }

        chat(markAsError(t("noPositionFound")), metadata = MessageMetadata(id = "CVClip#info"))
    }

    private fun canTpOn(pos: BlockPos, shape: VoxelShape): Boolean {
        val box = entity.boundingBox
        if (lacksSpaceAbove(pos, box, shape) || player.isPassenger && lacksSpaceAbove(pos, player.boundingBox, shape)) {
            return false
        }
        if (pos.canStandOn()) {
            return true
        }

        // A block without a sturdy top still carries a box that rests on its shape.
        val placed = shape.move(pos)
        val dy = placed.min(Direction.Axis.Y) - box.minY
        return Shapes.joinIsNotEmpty(placed, Shapes.create(box.move(0.0, dy, 0.0)), BooleanOp.AND)
    }

    private fun lacksSpaceAbove(pos: BlockPos, box: AABB, shape: VoxelShape): Boolean {
        val requiredHeight = box.ysize - (1.0 - shape.max(Direction.Axis.Y))
        var height = 0.0
        var above = pos

        while (height < requiredHeight) {
            above = above.above()
            val ceiling = above.collisionShape
            if (!ceiling.isEmpty && ceiling.min(Direction.Axis.Y) < requiredHeight - height) {
                return true
            }

            height += 1.0
        }

        return false
    }

}
