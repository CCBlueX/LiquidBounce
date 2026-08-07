package net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.traindata

import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.floor

class SurroundingsMapper(private val player: Player, private val level: Level) {

    private val gridRadius = 7 // -7 to +7 = 15x15
    private val mapSize = 225

    val floorMap = ShortArray(mapSize)
    val ceilMap = ShortArray(mapSize)
    val poiMap = IntArray(mapSize)

    fun compute() {
        val posX = player.x
        val posY = player.y
        val posZ = player.z
        val yawRad = Math.toRadians(player.yRot.toDouble()).toFloat()

        val yawCos = cos(-yawRad.toDouble())
        val yawSin = sin(-yawRad.toDouble())

        // Cache other player block positions to check them quickly for POI maps
        val otherPlayers = level.players()
            .filter { it != player && !it.isSpectator }
            .map { BlockPos(floor(it.x).toInt(), floor(it.y).toInt(), floor(it.z).toInt()) }
            .toSet()

        var idx = 0
        for (zOffset in -gridRadius..gridRadius) {
            for (xOffset in -gridRadius..gridRadius) {
                // Rotate coordinates relative to player yaw
                val rotX = xOffset * yawCos - zOffset * yawSin
                val rotZ = xOffset * yawSin + zOffset * yawCos

                val worldX = posX + rotX
                val worldZ = posZ + rotZ

                floorMap[idx] = calculateFloorDistance(worldX, posY, worldZ)
                ceilMap[idx] = calculateCeilDistance(worldX, posY, worldZ)
                poiMap[idx] = calculateBestPOI(worldX, posY, worldZ, otherPlayers)

                idx++
            }
        }
    }

    private fun calculateDistance(start: Vec3, end: Vec3, relativeToY: Double): Short {
        val hit = level.clip(ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player))
        val dist = if (hit.type != HitResult.Type.MISS) hit.location.y - relativeToY else (end.y - relativeToY)
        return (dist * 256.0).toInt().toShort()
    }

    private fun calculateFloorDistance(worldX: Double, posY: Double, worldZ: Double): Short {
        val start = Vec3(worldX, posY + 1.5, worldZ)
        val end = Vec3(worldX, posY - 10.0, worldZ)
        return calculateDistance(start, end, posY)
    }

    private fun calculateCeilDistance(worldX: Double, posY: Double, worldZ: Double): Short {
        val headY = posY + player.eyeHeight
        val start = Vec3(worldX, headY, worldZ)
        val end = Vec3(worldX, headY + 4.0, worldZ)
        return calculateDistance(start, end, headY)
    }

    private fun calculateBestPOI(worldX: Double, posY: Double, worldZ: Double, otherPlayers: Set<BlockPos>): Int {
        var bestPoi = 0
        val blockX = floor(worldX).toInt()
        val blockZ = floor(worldZ).toInt()
        val baseBlockY = floor(posY).toInt()

        for (yOffset in -3..3) {
            val blockPos = BlockPos(blockX, baseBlockY + yOffset, blockZ)

            // Check for other players first (highest priority)
            if (otherPlayers.contains(blockPos)) {
                return POIProvider.playerCategoryNumber
            }

            val block = level.getBlockState(blockPos).block
            val poiId = POIProvider.getPOIType(block)

            if (poiId > bestPoi) {
                bestPoi = poiId
            }
        }
        return bestPoi
    }
}
