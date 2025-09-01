package net.ccbluex.liquidbounce.features.module.modules.`fun`

import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.CRITICAL_MODIFICATION
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.math.toVec3i
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.session.GameWins.OnGlass
import net.minecraft.block.Blocks
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

object ModuleSuicide : ClientModule("Suicide", Category.FUN, aliases = arrayOf("AutoVoid"),disableOnQuit = true) {

    private val pathStepThreshold by float("PathStepThreshold", 0.5f, 0.2f..1.0f)
    private val allowJump by boolean("AllowJump", true)
    private var targetPos: Vec3d? = null


    @Suppress("unused")
    private val moveInputHandler = handler<MovementInputEvent>(priority = CRITICAL_MODIFICATION) { event ->
        if (shouldSkipMovement()) return@handler

        if (targetPos == null || player.pos.distanceTo(targetPos!!) < pathStepThreshold) {
            targetPos = findDangerousTarget()
            ModuleDebug.debugParameter(this, "TargetPos", targetPos ?: "None")
        }

        val target = targetPos ?: return@handler
        val dir = Vec3d(target.x - player.pos.x, 0.0, target.z - player.pos.z)
        val distance = dir.length()

        ModuleDebug.debugParameter(this, "DistanceToTarget", distance)
        ModuleDebug.debugParameter(this, "PlayerPos", player.pos)

        if (distance < pathStepThreshold) {
            targetPos = null
            ModuleDebug.debugParameter(this, "TargetReached", true)
            return@handler
        }

        val yaw = atan2(dir.z, dir.x) * 180.0 / Math.PI - 90.0
        player.yaw = yaw.toFloat()

        val desiredX = dir.x
        val desiredZ = dir.z
        val desiredLen = kotlin.math.sqrt(desiredX * desiredX + desiredZ * desiredZ)
        val nx = if (desiredLen > 1e-6) desiredX / desiredLen else 0.0
        val nz = if (desiredLen > 1e-6) desiredZ / desiredLen else 0.0

        val yawRad = Math.toRadians(player.yaw.toDouble())
        val forwardVecX = -sin(yawRad)
        val forwardVecZ = cos(yawRad)
        val rightVecX = cos(yawRad)
        val rightVecZ = sin(yawRad)

        val forwardAmt = nx * forwardVecX + nz * forwardVecZ
        val rightAmt = nx * rightVecX + nz * rightVecZ

        val thresh = 0.15

        event.directionalInput = DirectionalInput(
            forwards = forwardAmt > thresh,
            backwards = forwardAmt < -thresh,
            left = rightAmt < -thresh,
            right = rightAmt > thresh
        )

        val shouldJump = allowJump && player.isOnGround && (isObstacleInPath(target) || isJumpNeeded(target))
        event.jump = shouldJump
        ModuleDebug.debugParameter(this, "ShouldJump", shouldJump)
    }
    private fun shouldSkipMovement(): Boolean {
        if (player.isCreative || player.isSpectator) return true
        if (OnGlass) return true
        if (!player.isAlive || player.abilities.flying || player.hasStatusEffect(StatusEffects.LEVITATION)) return true

        val yawRad = Math.toRadians(player.yaw.toDouble())
        val forwardX = -sin(yawRad)
        val forwardZ = cos(yawRad)
        val checkPos = player.pos + Vec3d(forwardX, 0.0, forwardZ)
        val block = world.getBlockState(BlockPos(checkPos.toVec3i())).block
        return block != Blocks.AIR && block != Blocks.LAVA && block != Blocks.MAGMA_BLOCK
    }

    private fun isJumpNeeded(target: Vec3d): Boolean {
        val playerPos = player.pos
        val direction = target.subtract(playerPos).normalize()
        val distance = playerPos.distanceTo(target)
        val steps = (distance / 0.3).toInt()

        for (i in 1..steps) {
            val checkPos = playerPos + direction.multiply(i * 0.3)
            val blockPos = BlockPos(checkPos.toVec3i())
            val blockBelow = world.getBlockState(blockPos.down()).block

            if (blockBelow == Blocks.AIR) {
                return true
            }
        }
        return false
    }
    private fun findDangerousTarget(): Vec3d? {
        val maxRadius = 64
        val playerPos = player.pos
        val yLevel = floor(playerPos.y).toInt()

        val dangerousPos = (-maxRadius..maxRadius).flatMap { x ->
            (-maxRadius..maxRadius).mapNotNull { z ->
                val blockPos = BlockPos(playerPos.x.toInt() + x, yLevel, playerPos.z.toInt() + z)
                if (isDangerousPosition(blockPos)) Vec3d.ofCenter(blockPos) else null
            }
        }


        return dangerousPos.minByOrNull { it.distanceTo(playerPos) }
    }

    private fun isDangerousPosition(blockPos: BlockPos): Boolean {
        val blockBelow = world.getBlockState(blockPos.down()).block
        val blockAtPos = world.getBlockState(blockPos).block

        val isVoid = blockAtPos == Blocks.AIR && isOverVoid(Vec3d.ofCenter(blockPos))

        val isLava = blockAtPos == Blocks.LAVA || blockAtPos == Blocks.MAGMA_BLOCK ||
                blockBelow == Blocks.LAVA || blockBelow == Blocks.MAGMA_BLOCK

        return isVoid || isLava
    }

    private fun isOverVoid(pos: Vec3d): Boolean {
        for (y in pos.y.toInt() downTo -64) {
            val blockPos = BlockPos(pos.x.toInt(), y, pos.z.toInt())
            if (!world.getBlockState(blockPos).isAir) {
                return false
            }
        }
        return true
    }
    private fun isObstacleInPath(target: Vec3d): Boolean {
        val playerPos = player.pos
        val direction = target.subtract(playerPos).normalize()
        val steps = (playerPos.distanceTo(target) / 0.3).toInt()

        for (i in 1..steps) {
            val checkPos = playerPos + direction.multiply(i * 0.3)
            val blockPos = BlockPos(checkPos.toVec3i())
            val block = world.getBlockState(blockPos).block
            if (block != Blocks.AIR && block != Blocks.LAVA && block != Blocks.MAGMA_BLOCK) {
                return true
            }
        }
        return false
    }
}
