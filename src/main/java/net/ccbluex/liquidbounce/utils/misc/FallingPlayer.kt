/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.misc

import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.utils.extensions.plus
import net.ccbluex.liquidbounce.utils.extensions.toRadians
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.util.math.BlockPos
import net.minecraft.util.Direction
import net.minecraft.util.MovingObjectPosition.MovingObjectType.BLOCK
import net.minecraft.util.Vec3d
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class FallingPlayer(
    private var x: Double = mc.player.posX,
    private var y: Double = mc.player.posY,
    private var z: Double = mc.player.posZ,
    private var velocityX: Double = mc.player.velocityX,
    private var velocityY: Double = mc.player.velocityY,
    private var velocityZ: Double = mc.player.velocityZ,
    private val yaw: Float = mc.player.rotationYaw,
    private var strafe: Float = mc.player.moveStrafing,
    private var forward: Float = mc.player.moveForward
) : MinecraftInstance() {
    constructor(player: EntityPlayerSP, predict: Boolean = false) : this(
        if (predict) player.posX + player.velocityX else player.posX,
        if (predict) player.posY + player.velocityY else player.posY,
        if (predict) player.posZ + player.velocityZ else player.posZ,
        player.velocityX,
        player.velocityY,
        player.velocityZ,
        player.rotationYaw,
        player.moveStrafing,
        player.moveForward
    )

    private fun calculateForTick() {
        strafe *= 0.98f
        forward *= 0.98f

        var v = strafe * strafe + forward * forward
        if (v >= 0.0001f) {
            v = mc.player.jumpMovementFactor / sqrt(v).coerceAtLeast(1f)

            strafe *= v
            forward *= v

            val f1 = sin(yaw.toRadians())
            val f2 = cos(yaw.toRadians())

            velocityX += (strafe * f2 - forward * f1).toDouble()
            velocityZ += (forward * f2 + strafe * f1).toDouble()
        }

        velocityY -= 0.08
        velocityX *= 0.91
        velocityY *= 0.9800000190734863
        velocityY *= 0.91
        velocityZ *= 0.91

        x += velocityX
        y += velocityY
        z += velocityZ
    }

    fun findCollision(ticks: Int): CollisionResult? {
        repeat(ticks) { i ->
            val start = Vec3d(x, y, z)
            calculateForTick()
            val end = Vec3d(x, y, z)

            for (offset in offsets) {
                rayTrace(start + offset, end)?.let { return CollisionResult(it, i) }
            }
        }
        return null
    }

    private fun rayTrace(start: Vec3d, end: Vec3d): BlockPos? {
        val result = mc.world.rayTrace(start, end, true) ?: return null

        return if (result.typeOfHit == BLOCK && result.sideHit == Direction.UP) result.blockPos
        else null
    }

    private val offsets = listOf(
        Vec3d(0.0, 0.0, 0.0),
        Vec3d(0.3, 0.0, 0.3),
        Vec3d(-0.3, 0.0, 0.3),
        Vec3d(0.3, 0.0, -0.3),
        Vec3d(-0.3, 0.0, -0.3),
        Vec3d(0.3, 0.0, 0.15),
        Vec3d(-0.3, 0.0, 0.15),
        Vec3d(0.15, 0.0, 0.3),
        Vec3d(0.15, 0.0, -0.3)
    )

    class CollisionResult(val pos: BlockPos, val tick: Int)
}