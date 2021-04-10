package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.repeatable
import net.ccbluex.liquidbounce.utils.extensions.*
import net.minecraft.block.FallingBlock
import net.minecraft.item.BlockItem
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import net.minecraft.util.shape.VoxelShapes

object ModuleScaffold : Module("Scaffold", Category.WORLD) {

    private val swing by boolean("Swing", true)
    private val timer by float("Timer", 1f, 0.1f..3f)
    private var sameY by boolean("SameY", false)
    private val rotations = RotationsConfigurable()

    private var slot = -1
    private var oldSlot = -1

    private var yaw = 0f
    private var pitch = 0f
    private var bothRotations: Rotation? = null
    private var keepRotation = false
    private var launchY = -1

    override fun enable() {
        launchY = player.y.toInt()
    }

    val repeatable = repeatable {
        mc.timer.timerSpeed = timer

        val blockPos = (if (sameY && launchY <= player.y) BlockPos(
            player.x,
            launchY - 1.0,
            player.z
        ) else BlockPos(player.pos).down())

        // Gipsy KeepRotation
        if (keepRotation)
            RotationManager.aimAt(bothRotations!!, configurable = rotations)

        if (!world.getBlockState(blockPos).material.isReplaceable)
            return@repeatable

        // If there isn't any block detected
        if (findBlock() == -1)
            return@repeatable

        // Change slot
        slot = findBlock()
        oldSlot = player.inventory.selectedSlot
        player.inventory.selectedSlot = slot

        // If sameY is not in the same posY it was set to be
        if(sameY && player.y.toInt() < launchY)
            return@repeatable

        // Place block
        place(blockPos)

        // Create some sort of autoblock change without the player noticing (aka slot change server-sided)
        player.inventory.selectedSlot = oldSlot
    }

    private fun place(blockPos: BlockPos): Boolean {
        if (!world.getBlockState(blockPos).material.isReplaceable)
            return false

        val eyes = player.eyesPos

        for (direction in Direction.values()) {
            val neighbor = blockPos.offset(direction)
            val opposite = direction.opposite

            // If block can be clicked
            if (world.getBlockState(neighbor).getOutlineShape(world, neighbor) == VoxelShapes.empty())
                continue

            val hitVec = Vec3d.ofCenter(neighbor).add(Vec3d.of(opposite.vector).multiply(0.5))

            if (eyes.squaredDistanceTo(hitVec) > 18)
                continue

            // Make yaw + pitch based on hitVec and eyes
            val rotation = RotationManager.makeRotation(hitVec, eyes)

            // Rotate server-sided
            keepRotation = true
            RotationManager.aimAt(rotation, configurable = rotations)

            // Save yaw + pitch to variables to make KeepRotation the gipsy way
            yaw = rotation.yaw
            pitch = rotation.pitch
            bothRotations = rotation

            // Send right click which for some reason doesn't fucking work
            interaction.interactBlock(
                player,
                world,
                Hand.MAIN_HAND,
                BlockHitResult(hitVec, direction, blockPos, false)
            )
            interaction.interactItem(player, world, Hand.MAIN_HAND)

            if (swing)
                player.swingHand(Hand.MAIN_HAND)
            else
                network.sendPacket(HandSwingC2SPacket(Hand.MAIN_HAND))
            return true
        }
        return false
    }

    // Simple hot bar block detection
    private fun findBlock(): Int {
        return (0..8)
            .map { Pair(it, player.inventory.getStack(it).item) }
            .find {
                val second = it.second
                second is BlockItem && second.block !is FallingBlock
            }?.first ?: -1
    }
}
