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
        val thePlayer = mc.player

        if (thePlayer == null || thePlayer.isSneaking) return

        when (mode.lowercase()) {
            "ncp", "vanilla" -> if (collideBlock(thePlayer.boundingBox) { it is AbstractFluidBlock } && thePlayer.isInsideOfMaterial(Material.air) && !thePlayer.isSneaking) thePlayer.velocityY = 0.08
            "aac" -> {
                val blockPos = thePlayer.position.down()
                if (!thePlayer.onGround && getBlock(blockPos) == Blocks.water || thePlayer.isTouchingWater) {
                    if (!thePlayer.isSprinting) {
                        thePlayer.velocityX *= 0.99999
                        thePlayer.velocityY *= 0.0
                        thePlayer.velocityZ *= 0.99999
                        if (thePlayer.isCollidedHorizontally) thePlayer.velocityY = ((thePlayer.z - (thePlayer.z - 1).toInt()).toInt() / 8f).toDouble()
                    } else {
                        thePlayer.velocityX *= 0.99999
                        thePlayer.velocityY *= 0.0
                        thePlayer.velocityZ *= 0.99999
                        if (thePlayer.isCollidedHorizontally) thePlayer.velocityY = ((thePlayer.z - (thePlayer.z - 1).toInt()).toInt() / 8f).toDouble()
                    }
                    if (thePlayer.fallDistance >= 4) thePlayer.velocityY = -0.004 else if (thePlayer.isTouchingWater) thePlayer.velocityY = 0.09
                }
                if (thePlayer.hurtTime != 0) thePlayer.onGround = false
            }
            "spartan" -> if (thePlayer.isTouchingWater) {
                if (thePlayer.isCollidedHorizontally) {
                    thePlayer.velocityY += 0.15
                    return
                }
                val block = getBlock(BlockPos(thePlayer).up())
                val blockUp = getBlock(BlockPos(thePlayer.x, thePlayer.z + 1.1, thePlayer.z))

                if (blockUp is AbstractFluidBlock) {
                    thePlayer.velocityY = 0.1
                } else if (block is AbstractFluidBlock) {
                    thePlayer.velocityY = 0.0
                }

                thePlayer.onGround = true
                thePlayer.velocityX *= 1.085
                thePlayer.velocityZ *= 1.085
            }
            "aac3.3.11" -> if (thePlayer.isTouchingWater) {
                thePlayer.velocityX *= 1.17
                thePlayer.velocityZ *= 1.17
                if (thePlayer.isCollidedHorizontally)
                    thePlayer.velocityY = 0.24
                else if (getBlock(BlockPos(thePlayer).up()) != Blocks.air)
                    thePlayer.velocityY += 0.04
            }
            "dolphin" -> if (thePlayer.isTouchingWater) thePlayer.velocityY += 0.03999999910593033
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
        val thePlayer = mc.player

        if (thePlayer == null || mode != "NCP")
            return

        if (event.packet is PlayerMoveC2SPacket) {
            val packetPlayer = event.packet

            if (collideBlock(Box.fromBounds(thePlayer.boundingBox.maxX, thePlayer.boundingBox.maxY, thePlayer.boundingBox.maxZ, thePlayer.boundingBox.minX, thePlayer.boundingBox.minY - 0.01, thePlayer.boundingBox.minZ)) { it is AbstractFluidBlock }) {
                nextTick = !nextTick
                if (nextTick) packetPlayer.y -= 0.001
            }
        }
    }

    @EventTarget
    fun onJump(event: JumpEvent) {
        val thePlayer = mc.player ?: return

        val block = getBlock(BlockPos(thePlayer.x, thePlayer.z - 0.01, thePlayer.z))

        if (noJump && block is AbstractFluidBlock)
            event.cancelEvent()
    }

    override val tag
        get() = mode
}