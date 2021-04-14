package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.extensions.*
import net.minecraft.block.Blocks
import net.minecraft.block.FallingBlock
import net.minecraft.item.BlockItem
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import net.minecraft.util.shape.VoxelShapes
import kotlin.math.abs

object ModuleScaffold : Module("Scaffold", Category.WORLD) {

    private val swing by boolean("Swing", true)
    private val search by boolean("Search", true)
    private val eagle by enumChoice("Eagle", EagleMode.OFF, EagleMode.values())
    private val blocksToEagle by int("BlocksToEagle", 0, 0..10)
    private val edgeDist by float("EagleEdgeDistance", 0f, 0f..0.5f)
    private val timer by float("Timer", 1f, 0.1f..3f)
    private var speedModifier by float("SpeedModifier", 1.5f, 0f..2f)
    private var sameY by boolean("SameY", true)
    private val rotations = RotationsConfigurable()

    private var slot = -1
    private var oldSlot = -1
    private var placedBlocksWithoutEagle = 0
    private var eagleSneaking = false
    private var yaw = 0f
    private var pitch = 0f
    private var bothRotations: Rotation? = null
    private var keepRotation = false
    private var facesBlock = false
    private var launchY = -1

    override fun enable() {
        launchY = player.y.toInt()
    }

    override fun disable() {
        facesBlock = false
        keepRotation = false
        mc.options.keySneak.isPressed = false
        if (eagleSneaking)
            network.sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY))
    }

    val repeatable = repeatable {
        mc.timer.timerSpeed = timer

        val blockPos = (if (sameY && launchY <= player.y) BlockPos(
            player.x,
            launchY - 1.0,
            player.z
        ) else BlockPos(player.pos).down())

        if (eagle != EagleMode.OFF) {
            var dif = 0.5
            if (edgeDist > 0) {
                for (direction in Direction.values()) {
                    if (direction != Direction.NORTH && direction != Direction.EAST && direction != Direction.SOUTH && direction != Direction.WEST)
                        continue
                    val blockPosition = BlockPos(
                        player.x,
                        player.y - 1.0,
                        player.z
                    )
                    val neighbor = blockPosition.offset(direction, 1)
                    if (world.getBlockState(neighbor).block == Blocks.AIR) {
                        val calcDif = (if (direction == Direction.NORTH || direction == Direction.SOUTH)
                            abs((neighbor.z + 0.5) - player.z) else
                            abs((neighbor.x + 0.5) - player.x)) - 0.5
                        if (calcDif < dif)
                            dif = calcDif
                    }
                }
            }
            if (placedBlocksWithoutEagle >= blocksToEagle) {
                val shouldEagle: Boolean = world.getBlockState(
                    BlockPos(
                        player.x,
                        player.y - 1.0,
                        player.z
                    )
                ).block == Blocks.AIR || dif < edgeDist
                if (eagle == EagleMode.SNEAK) {
                    if (eagleSneaking != shouldEagle) {
                        network.sendPacket(
                            ClientCommandC2SPacket(
                                player,
                                if (shouldEagle) ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY else ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY
                            )
                        )
                    }
                    eagleSneaking = shouldEagle
                } else {
                    mc.options.keySneak.isPressed = shouldEagle
                    placedBlocksWithoutEagle = 0
                }
            } else {
                placedBlocksWithoutEagle++
            }
        }

        // KeepRotation
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
        if (sameY && player.y.toInt() < launchY)
            return@repeatable

        // Search
        if (search)
            extraSearch()
        // Place block
        place(blockPos, !sameY)

        // Create some sort of autoblock change without the player noticing (aka slot change server-sided)
        player.inventory.selectedSlot = oldSlot
    }

    private fun extraSearch() {
        val blockPos = (if (sameY && launchY <= player.y) BlockPos(
            player.x,
            launchY - 1.0,
            player.z
        ) else BlockPos(player.pos).down())
        if ((!world.getBlockState(blockPos).material.isReplaceable || place(blockPos, !sameY)))
            return

        for (x in -1..1) {
            for (z in -1..1) {
                if (place(blockPos.add(x, 0, z), !sameY))
                    return
            }
        }
    }

    private fun place(blockPos: BlockPos, checks: Boolean): Boolean {
        facesBlock = false
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

            if (checks && eyes.squaredDistanceTo(hitVec) > 18)
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
            facesBlock = true
            interaction.interactBlock(
                player,
                world,
                Hand.MAIN_HAND,
                BlockHitResult(hitVec, direction, blockPos, false)
            )
            interaction.interactItem(player, world, Hand.MAIN_HAND)

            if (facesBlock) {
                if (player.isOnGround) {
                    player.velocity.x *= speedModifier
                    player.velocity.z *= speedModifier
                }
                if (swing)
                    player.swingHand(Hand.MAIN_HAND)
                else
                    network.sendPacket(HandSwingC2SPacket(Hand.MAIN_HAND))
                facesBlock = false
                return true
            }
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

    private enum class EagleMode(override val choiceName: String) : NamedChoice {
        OFF("Off"), SNEAK("On"), SILENT_SNEAK("Silent")
    }

}
