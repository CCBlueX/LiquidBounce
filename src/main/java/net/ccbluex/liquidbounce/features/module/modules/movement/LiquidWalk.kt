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
import net.minecraft.util.BlockPos
import org.lwjgl.input.Keyboard

object LiquidWalk : Module("LiquidWalk", Category.MOVEMENT, Keyboard.KEY_J) {

    val mode by ListValue("Mode", arrayOf("Vanilla", "NCP", "AAC", "AAC3.3.11", "AACFly", "Spartan", "Dolphin"), "NCP")
        private val aacFly by FloatValue("AACFlyMotion", 0.5f, 0.1f..1f) { mode == "AACFly" }

    private val noJump by BoolValue("NoJump", false)

    private var nextTick = false

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val player = mc.thePlayer

        if (player == null || player.isSneaking) return

        when (mode.lowercase()) {
            "ncp", "vanilla" -> if (collideBlock(player.entityBoundingBox) { it is BlockLiquid } && player.isInsideOfMaterial(Material.air) && !thePlayer.isSneaking) player.motionY = 0.08
            "aac" -> {
                val blockPos = player.position.down()
                if (!thePlayer.onGround && getBlock(blockPos) == Blocks.water || player.isInWater) {
                    if (!thePlayer.isSprinting) {
                        player.motionX *= 0.99999
                        player.motionY *= 0.0
                        player.motionZ *= 0.99999
                        if (player.isCollidedHorizontally) player.motionY = ((player.posY - (player.posY - 1).toInt()).toInt() / 8f).toDouble()
                    } else {
                        player.motionX *= 0.99999
                        player.motionY *= 0.0
                        player.motionZ *= 0.99999
                        if (player.isCollidedHorizontally) player.motionY = ((player.posY - (player.posY - 1).toInt()).toInt() / 8f).toDouble()
                    }
                    if (player.fallDistance >= 4) player.motionY = -0.004 else if (player.isInWater) player.motionY = 0.09
                }
                if (player.hurtTime != 0) player.onGround = false
            }
            "spartan" -> if (player.isInWater) {
                if (player.isCollidedHorizontally) {
                    player.motionY += 0.15
                    return
                }
                val block = getBlock(BlockPos(player).up())
                val blockUp = getBlock(BlockPos(player.posX, player.posY + 1.1, player.posZ))

                if (blockUp is BlockLiquid) {
                    player.motionY = 0.1
                } else if (block is BlockLiquid) {
                    player.motionY = 0.0
                }

                player.onGround = true
                player.motionX *= 1.085
                player.motionZ *= 1.085
            }
            "aac3.3.11" -> if (player.isInWater) {
                player.motionX *= 1.17
                player.motionZ *= 1.17
                if (player.isCollidedHorizontally)
                    player.motionY = 0.24
                else if (getBlock(BlockPos(player).up()) != Blocks.air)
                    player.motionY += 0.04
            }
            "dolphin" -> if (player.isInWater) player.motionY += 0.03999999910593033
        }
    }

    @EventTarget
    fun onMove(event: MoveEvent) {
        if ("aacfly" == mode.lowercase() && mc.thePlayer.isInWater) {
            event.y = aacFly.toDouble()
            mc.thePlayer.motionY = aacFly.toDouble()
        }
    }

    @EventTarget
    fun onBlockBB(event: BlockBBEvent) {
        if (mc.thePlayer == null)
            return

        if (event.block is BlockLiquid && !collideBlock(mc.thePlayer.entityBoundingBox) { it is BlockLiquid } && !mc.thePlayer.isSneaking) {
            when (mode.lowercase()) {
                "ncp", "vanilla" -> event.boundingBox = AxisAlignedBB.fromBounds(event.x.toDouble(), event.y.toDouble(), event.z.toDouble(), event.x + 1.toDouble(), event.y + 1.toDouble(), event.z + 1.toDouble())
            }
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val player = mc.thePlayer

        if (player == null || mode != "NCP")
            return

        if (event.packet is C03PacketPlayer) {
            val packetPlayer = event.packet

            if (collideBlock(AxisAlignedBB.fromBounds(player.entityBoundingBox.maxX, player.entityBoundingBox.maxY, player.entityBoundingBox.maxZ, player.entityBoundingBox.minX, player.entityBoundingBox.minY - 0.01, player.entityBoundingBox.minZ)) { it is BlockLiquid }) {
                nextTick = !nextTick
                if (nextTick) packetPlayer.y -= 0.001
            }
        }
    }

    @EventTarget
    fun onJump(event: JumpEvent) {
        val player = mc.thePlayer ?: return

        val block = getBlock(BlockPos(player.posX, player.posY - 0.01, player.posZ))

        if (noJump && block is BlockLiquid)
            event.cancelEvent()
    }

    override val tag
        get() = mode
}