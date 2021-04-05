package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.repeatable
import net.ccbluex.liquidbounce.utils.extensions.*
import net.minecraft.block.FallingBlock
import net.minecraft.block.GrassBlock
import net.minecraft.item.BlockItem
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import net.minecraft.util.shape.VoxelShapes
import kotlin.math.atan2
import kotlin.math.sqrt

object ModuleScaffold : Module("Scaffold", Category.WORLD) {

    private val swing by boolean("Swing", true)
    private val timer by float("Timer", 1f, 0.1f..3f)
    private val rotations = RotationsConfigurable()

    private var oldSlot = -1
    private var slot = -1

    private var yaw = 0f
    private var pitch = 0f
    private var bothRotations: Rotation? = null
    private var keepRotation = false

    val repeatable = repeatable {
        mc.timer.timerSpeed = timer

        val blockpos = BlockPos(player.x, player.y, player.z).down()

        // Gipsy KeepRotation
        if(keepRotation)
            RotationManager.aimAt(bothRotations!!, configurable = rotations)

        if (!mc.world!!.getBlockState(blockpos).material.isReplaceable)
            return@repeatable

        // If there isn't any block detected
        if (findBlock() == -1)
            return@repeatable

        // Change slot
        oldSlot = player.inventory.selectedSlot
        slot = findBlock()
        player.inventory.selectedSlot = slot
        mc.interactionManager!!.tick()

        // Place block
        place(blockpos)

        // Reset slot
        player.inventory.selectedSlot = oldSlot
    }

    private fun place(blockPos: BlockPos) {
        val eyes = player.eyesPos

        for (direction in Direction.values()) {
            val neighbor = blockPos.offset(direction)
            val opposite = direction.opposite

            if (world.getBlockState(neighbor).getOutlineShape(world, neighbor) == VoxelShapes.empty())
                continue

            // Could've done a for loop xyz 0.1..0.9 here, didn't want to, because basics first
            val hitVec = Vec3d.ofCenter(neighbor).add(Vec3d.of(opposite.vector).multiply(0.5))

            if (eyes.squaredDistanceTo(hitVec) > 18)
                continue

            // Make yaw + pitch based on hitvec and eyes
            val diffX = hitVec.x - eyes.x
            val diffY = hitVec.y - eyes.y
            val diffZ = hitVec.z - eyes.z

            val rotation = Rotation(
                (Math.toDegrees(atan2(diffZ, diffX)).toFloat() - 90f),
                ((-Math.toDegrees(atan2(diffY, sqrt(diffX * diffX + diffZ * diffZ)))).toFloat())
            )

            // Rotate server-sided
            keepRotation = true
            RotationManager.aimAt(rotation, configurable = rotations)

            // Save yaw + pitch to variables to make KeepRotation the gipsy way
            yaw = rotation.yaw
            pitch = rotation.pitch
            bothRotations = rotation

            // Send right click
            mc.interactionManager!!.interactBlock(
                player,
                world,
                Hand.MAIN_HAND,
                BlockHitResult(hitVec, direction, blockPos, false)
            )

            if (swing)
                player.swingHand(Hand.MAIN_HAND)
            else
                network.sendPacket(HandSwingC2SPacket(Hand.MAIN_HAND))
        }
    }

    // Simple hot bar block detection
    private fun findBlock(): Int {
        for (i in 0..8) {
            val itemStack = player.inventory.getStack(i).item
            if (itemStack is BlockItem) {
                val blockType = itemStack.block
                if (blockType !is FallingBlock || blockType !is GrassBlock) {
                    return i
                }
            }
        }
        return -1
    }
}
