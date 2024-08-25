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
import net.minecraft.block.BlockLiquid
import net.minecraft.block.material.Material
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.util.AxisAlignedBB
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
            "ncp", "vanilla" -> if (collideBlock(thePlayer.entityBoundingBox) { it is BlockLiquid } && thePlayer.isInsideOfMaterial(Material.air) && !thePlayer.isSneaking) thePlayer.velocityY = 0.08
            "aac" -> {
                val blockPos = thePlayer.position.down()
                if (!thePlayer.onGround && getBlock(blockPos) == Blocks.water || thePlayer.isInWater) {
                    if (!thePlayer.isSprinting) {
                        thePlayer.velocityX *= 0.99999
                        thePlayer.velocityY *= 0.0
                        thePlayer.velocityZ *= 0.99999
                        if (thePlayer.isCollidedHorizontally) thePlayer.velocityY = ((thePlayer.posY - (thePlayer.posY - 1).toInt()).toInt() / 8f).toDouble()
                    } else {
                        thePlayer.velocityX *= 0.99999
                        thePlayer.velocityY *= 0.0
                        thePlayer.velocityZ *= 0.99999
                        if (thePlayer.isCollidedHorizontally) thePlayer.velocityY = ((thePlayer.posY - (thePlayer.posY - 1).toInt()).toInt() / 8f).toDouble()
                    }
                    if (thePlayer.fallDistance >= 4) thePlayer.velocityY = -0.004 else if (thePlayer.isInWater) thePlayer.velocityY = 0.09
                }
                if (thePlayer.hurtTime != 0) thePlayer.onGround = false
            }
            "spartan" -> if (thePlayer.isInWater) {
                if (thePlayer.isCollidedHorizontally) {
                    thePlayer.velocityY += 0.15
                    return
                }
                val block = getBlock(BlockPos(thePlayer).up())
                val blockUp = getBlock(BlockPos(thePlayer.posX, thePlayer.posY + 1.1, thePlayer.posZ))

                if (blockUp is BlockLiquid) {
                    thePlayer.velocityY = 0.1
                } else if (block is BlockLiquid) {
                    thePlayer.velocityY = 0.0
                }

                thePlayer.onGround = true
                thePlayer.velocityX *= 1.085
                thePlayer.velocityZ *= 1.085
            }
            "aac3.3.11" -> if (thePlayer.isInWater) {
                thePlayer.velocityX *= 1.17
                thePlayer.velocityZ *= 1.17
                if (thePlayer.isCollidedHorizontally)
                    thePlayer.velocityY = 0.24
                else if (getBlock(BlockPos(thePlayer).up()) != Blocks.air)
                    thePlayer.velocityY += 0.04
            }
            "dolphin" -> if (thePlayer.isInWater) thePlayer.velocityY += 0.03999999910593033
        }
    }

    @EventTarget
    fun onMove(event: MoveEvent) {
        if ("aacfly" == mode.lowercase() && mc.player.isInWater) {
            event.y = aacFly.toDouble()
            mc.player.velocityY = aacFly.toDouble()
        }
    }

    @EventTarget
    fun onBlockBB(event: BlockBBEvent) {
        if (mc.player == null)
            return

        if (event.block is BlockLiquid && !collideBlock(mc.player.entityBoundingBox) { it is BlockLiquid } && !mc.player.isSneaking) {
            when (mode.lowercase()) {
                "ncp", "vanilla" -> event.boundingBox = AxisAlignedBB.fromBounds(event.x.toDouble(), event.y.toDouble(), event.z.toDouble(), event.x + 1.toDouble(), event.y + 1.toDouble(), event.z + 1.toDouble())
            }
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val thePlayer = mc.player

        if (thePlayer == null || mode != "NCP")
            return

        if (event.packet is C03PacketPlayer) {
            val packetPlayer = event.packet

            if (collideBlock(AxisAlignedBB.fromBounds(thePlayer.entityBoundingBox.maxX, thePlayer.entityBoundingBox.maxY, thePlayer.entityBoundingBox.maxZ, thePlayer.entityBoundingBox.minX, thePlayer.entityBoundingBox.minY - 0.01, thePlayer.entityBoundingBox.minZ)) { it is BlockLiquid }) {
                nextTick = !nextTick
                if (nextTick) packetPlayer.y -= 0.001
            }
        }
    }

    @EventTarget
    fun onJump(event: JumpEvent) {
        val thePlayer = mc.player ?: return

        val block = getBlock(BlockPos(thePlayer.posX, thePlayer.posY - 0.01, thePlayer.posZ))

        if (noJump && block is BlockLiquid)
            event.cancelEvent()
    }

    override val tag
        get() = mode
}