/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.misc

import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.utils.extensions.applyStrafe
import net.ccbluex.liquidbounce.utils.extensions.plus
import net.minecraft.entity.EntityLivingBase
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.MovingObjectPosition
import net.minecraft.util.Vec3
import net.minecraft.world.World

class FallingPlayer(private val theWorld: World, private val thePlayer: EntityLivingBase, private var x: Double, private var y: Double, private var z: Double, private var motionX: Double, private var motionY: Double, private var motionZ: Double, private val yaw: Float, private var strafe: Float, private var forward: Float) : MinecraftInstance()
{
    private fun calculateForTick()
    {
        strafe *= 0.98f
        forward *= 0.98f

        val (newX, newZ) = (motionX to motionZ).applyStrafe(forward, strafe, yaw, thePlayer.jumpMovementFactor)
        motionX = newX * 0.91
        motionZ = newZ * 0.91

        motionY -= 0.08
        motionY *= 0.9800000190734863
        motionY *= 0.91

        x += motionX
        y += motionY
        z += motionZ
    }

    fun findCollision(ticks: Int): CollisionResult?
    {
        repeat(ticks) { i ->

            val start = Vec3(x, y, z)

            calculateForTick()

            val end = Vec3(x, y, z)

            var result: CollisionResult? = null

            val w = thePlayer.width * 0.5

            val rayTrace = { xOffset: Double, zOffset: Double -> rayTrace(theWorld, start.plus(xOffset, 0.0, zOffset), end).also { result = it?.let { CollisionResult(it, i) } } != null }

            if (rayTrace(0.0, 0.0)) return@findCollision result

            if (rayTrace(w, w)) return@findCollision result
            if (rayTrace(-w, w)) return@findCollision result
            if (rayTrace(w, -w)) return@findCollision result
            if (rayTrace(-w, -w)) return@findCollision result

            if (rayTrace(w, w * 0.5)) return@findCollision result
            if (rayTrace(-w, w * 0.5)) return@findCollision result
            if (rayTrace(w * 0.5, w)) return@findCollision result
            if (rayTrace(w * 0.5, -w)) return@findCollision result
        }

        return null
    }

    class CollisionResult(val pos: BlockPos, val tick: Int)

    companion object
    {
        private fun rayTrace(theWorld: World, start: Vec3, end: Vec3): BlockPos?
        {
            val result = theWorld.rayTraceBlocks(start, end, true)
            return if (result != null && result.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && result.sideHit == EnumFacing.UP) result.blockPos else null
        }
    }
}
