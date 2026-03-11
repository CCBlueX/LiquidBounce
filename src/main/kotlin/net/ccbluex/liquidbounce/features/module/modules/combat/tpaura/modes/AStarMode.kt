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

package net.ccbluex.liquidbounce.features.module.modules.combat.tpaura.modes

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ccbluex.fastutil.WeightedSortedList
import net.ccbluex.fastutil.mapToArray
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.event.tickUntil
import net.ccbluex.liquidbounce.event.waitTicks
import net.ccbluex.liquidbounce.features.module.modules.combat.tpaura.ModuleTpAura
import net.ccbluex.liquidbounce.features.module.modules.combat.tpaura.ModuleTpAura.clicker
import net.ccbluex.liquidbounce.features.module.modules.combat.tpaura.ModuleTpAura.desyncPlayerPosition
import net.ccbluex.liquidbounce.features.module.modules.combat.tpaura.ModuleTpAura.stuckChronometer
import net.ccbluex.liquidbounce.features.module.modules.combat.tpaura.ModuleTpAura.targetSelector
import net.ccbluex.liquidbounce.features.module.modules.combat.tpaura.TpAuraMode
import net.ccbluex.liquidbounce.render.drawLineStrip
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.utils.block.AStarPathBuilder
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.markAsError
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.ccbluex.liquidbounce.utils.math.set
import net.ccbluex.liquidbounce.utils.math.sq
import net.ccbluex.liquidbounce.utils.math.toVec3d
import net.ccbluex.liquidbounce.utils.math.toVec3f
import net.minecraft.core.BlockPos
import net.minecraft.core.Vec3i
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.AABB

object AStarMode : TpAuraMode("AStar"), AStarPathBuilder {

    private val maximumDistance by int("MaximumDistance", 95, 50..250)
    private val maximumCost by int("MaximumCost", 250, 50..500)
    private val tickDistance by int("TickDistance", 3, 1..7)
    override val allowDiagonal by boolean("AllowDiagonal", false)
    private val tpBack by boolean("TpBack", true)

    private val stickAt by int("Stick", 5, 1..10, "ticks")

    private var pathCache: PathCache? = null

    private val pathStart = BlockPos.MutableBlockPos()

    private val pathContext = Dispatchers.Default + CoroutineName("${ModuleTpAura.name}-${name}")

    @Suppress("unused")
    private val tickHandler = tickHandler {
        pathCache = withContext(pathContext) {
            val playerEyePos = player.eyePosition
            val playerPosition = pathStart.takeIf { it != BlockPos.ZERO } ?: player.blockPosition()

            val maximumDistanceSq = maximumDistance.sq().toDouble()

            targetSelector.targets().toCollection(
                WeightedSortedList(
                    upperBound = maximumDistanceSq,
                    weighter = { it.squaredBoxedDistanceTo(playerEyePos) }
                )
            ).firstNotNullOfOrNull { enemy ->
                val path = findPath(playerPosition, enemy.blockPosition(), maximumCost)

                // Skip if the path is empty
                if (path.isNotEmpty()) {
                    // Stop searching when the pathCache is ready
                    PathCache(enemy, path)
                } else {
                    null
                }
            }
        }

        val (_, path) = pathCache ?: return@tickHandler

        tickUntil {
            clicker.isClickTick
        }

        travel(path)
        waitTicks(stickAt)
        if (tpBack) {
            travel(path.asReversed())
            pathStart.set(BlockPos.ZERO)
        } else {
            desyncPlayerPosition?.let {
                pathStart.set(it)
            }
        }
        desyncPlayerPosition = null
    }

    override fun disable() {
        desyncPlayerPosition = null
        pathStart.set(BlockPos.ZERO)
        pathCache = null
        super.disable()
    }

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        val matrixStack = event.matrixStack
        val (_, path) = pathCache ?: return@handler

        renderEnvironmentForWorld(matrixStack) {
            drawLineStrip(
                argb = Color4b.WHITE.argb,
                positions = path.mapToArray {
                    relativeToCamera(it.toVec3d(0.5, 0.5, 0.5)).toVec3f()
                }
            )
        }
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> {
        val packet = it.packet

        if (packet is ServerboundMovePlayerPacket) {
            val position = desyncPlayerPosition ?: return@handler

            // Set the packet position to the player position
            packet.x = position.x
            packet.y = position.y
            packet.z = position.z
            packet.hasPos = true
        } else if (packet is ClientboundPlayerPositionPacket) {
            val change = packet.change.position
            chat(markAsError("Server setback detected - teleport failed at ${change.x} ${change.y} ${change.z}!"))
            stuckChronometer.reset()
            pathCache = null
            desyncPlayerPosition = null
        }
    }

    private fun travel(path: List<Vec3i>) {
        // Currently path is a list of positions we need to go one by one, however we can split it into chunks
        // to use less packets and teleport more efficiently.
        // However, we cannot teleport if there are blocks in the way, so we need to check if the path is clear.
        val pathChunks = path.chunked(tickDistance)

        for (chunk in pathChunks) {
            // Check if the path is clear, this can be done by raycasting the start and end position of the chunk.
            val start = chunk.first().toVec3d(0.5, 0.5, 0.5)
            val end = chunk.last().toVec3d(0.5, 0.5, 0.5)

            if (world.getBlockCollisions(player, AABB(start, end)).any()) {
                // If the path is not clear, we need to go one by one.
                for (position in chunk) {
                    network.send(
                        Pos(
                            position.x + 0.5, position.y.toDouble(), position.z + 0.5, false, false
                        )
                    )
                    desyncPlayerPosition = position.toVec3d(xOffset = 0.5, zOffset = 0.5)
                }
                continue
            } else {
                // If the path is clear, we can teleport to the last position of the chunk.
                network.send(Pos(end.x, end.y, end.z, false, false))
                desyncPlayerPosition = end
            }
        }
    }

    @JvmRecord
    private data class PathCache(val enemy: LivingEntity, val path: List<Vec3i>)

}
