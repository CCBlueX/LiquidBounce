/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.utils.block.BlockUtils.collideBlock
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.block.AbstractFluidBlock
import net.minecraft.block.material.Material
import net.minecraft.init.Blocks
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.util.Box
import net.minecraft.util.math.BlockPos
import org.lwjgl.input.Keyboard

object LiquidWalk : Module("LiquidWalk", Category.MOVEMENT, Keyboard.KEY_J) {

    val mode by ListValue("Mode", arrayOf("Vanilla", "NCP", "AAC", "AAC3.3.11", "AACFly", "Spartan", "Dolphin"), "NCP")
        private val aacFly by FloatValue("AACFlyMotion", 0.5f, 0.1f..1f) { mode == "AACFly" }

    private val noJump by BoolValue("NoJump", false)

    private var nextTick = false

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val player = mc.player

        if (player == null || player.isSneaking) return

        when (mode.lowercase()) {
            "ncp", "vanilla" -> if (collideBlock(player.boundingBox) { it is AbstractFluidBlock } && player.isInsideOfMaterial(Material.Blocks.AIR) && !player.isSneaking) player.velocityY = 0.08
            "aac" -> {
                val blockPos = player.position.down()
                if (!player.onGround && getBlock(blockPos) == Blocks.water || player.isTouchingWater) {
                    if (!player.isSprinting) {
                        player.velocityX *= 0.99999
                        player.velocityY *= 0.0
                        player.velocityZ *= 0.99999
                        if (player.isCollidedHorizontally) player.velocityY = ((player.z - (player.z - 1).toInt()).toInt() / 8f).toDouble()
                    } else {
                        player.velocityX *= 0.99999
                        player.velocityY *= 0.0
                        player.velocityZ *= 0.99999
                        if (player.isCollidedHorizontally) player.velocityY = ((player.z - (player.z - 1).toInt()).toInt() / 8f).toDouble()
                    }
                    if (player.fallDistance >= 4) player.velocityY = -0.004 else if (player.isTouchingWater) player.velocityY = 0.09
                }
                if (player.hurtTime != 0) player.onGround = false
            }
            "spartan" -> if (player.isTouchingWater) {
                if (player.isCollidedHorizontally) {
                    player.velocityY += 0.15
                    return
                }
                val block = getBlock(BlockPos(player).up())
                val blockUp = getBlock(BlockPos(player.x, player.z + 1.1, player.z))

                if (blockUp is AbstractFluidBlock) {
                    player.velocityY = 0.1
                } else if (block is AbstractFluidBlock) {
                    player.velocityY = 0.0
                }

                player.onGround = true
                player.velocityX *= 1.085
                player.velocityZ *= 1.085
            }
            "aac3.3.11" -> if (player.isTouchingWater) {
                player.velocityX *= 1.17
                player.velocityZ *= 1.17
                if (player.isCollidedHorizontally)
                    player.velocityY = 0.24
                else if (getBlock(BlockPos(player).up()) != Blocks.Blocks.AIR)
                    player.velocityY += 0.04
            }
            "dolphin" -> if (player.isTouchingWater) player.velocityY += 0.03999999910593033
        }
    }

    @EventTarget
    fun onMove(event: MoveEvent) {
        if ("aacfly" == mode.lowercase() && mc.player.isTouchingWater) {
            event.y = aacFly.toDouble()
            mc.player.velocityY = aacFly.toDouble()
        }
    }

    @EventTarget
    fun onBlockBB(event: BlockBBEvent) {
        if (mc.player == null)
            return

        if (event.block is AbstractFluidBlock && !collideBlock(mc.player.boundingBox) { it is AbstractFluidBlock } && !mc.player.isSneaking) {
            when (mode.lowercase()) {
                "ncp", "vanilla" -> event.boundingBox = Box.fromBounds(event.x.toDouble(), event.y.toDouble(), event.z.toDouble(), event.x + 1.toDouble(), event.y + 1.toDouble(), event.z + 1.toDouble())
            }
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val player = mc.player

        if (player == null || mode != "NCP")
            return

        if (event.packet is PlayerMoveC2SPacket) {
            val packetPlayer = event.packet

            if (collideBlock(Box.fromBounds(player.boundingBox.maxX, player.boundingBox.maxY, player.boundingBox.maxZ, player.boundingBox.minX, player.boundingBox.minY - 0.01, player.boundingBox.minZ)) { it is AbstractFluidBlock }) {
                nextTick = !nextTick
                if (nextTick) packetPlayer.y -= 0.001
            }
        }
    }

    @EventTarget
    fun onJump(event: JumpEvent) {
        val player = mc.player ?: return

        val block = getBlock(BlockPos(player.x, player.z - 0.01, player.z))

        if (noJump && block is AbstractFluidBlock)
            event.cancelEvent()
    }

    override val tag
        get() = mode
}