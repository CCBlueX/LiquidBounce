/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.api.enums.BlockType
import net.ccbluex.liquidbounce.api.enums.MaterialType
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.block.BlockUtils.collideBlock
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.ListValue
import org.lwjgl.input.Keyboard

@ModuleInfo(name = "LiquidWalk", description = "Allows you to walk on water.", category = ModuleCategory.MOVEMENT, keyBind = Keyboard.KEY_J)
class LiquidWalk : Module() {
    val modeValue = ListValue("Mode", arrayOf("Vanilla", "NCP", "AAC", "AAC3.3.11", "AACFly", "Spartan", "Dolphin"), "NCP")
    private val noJumpValue = BoolValue("NoJump", false)
    private val aacFlyValue = FloatValue("AACFlyMotion", 0.5f, 0.1f, 1f)

    private var nextTick = false

    @EventTarget
    fun onUpdate(event: UpdateEvent?) {
        val thePlayer = mc.thePlayer

        if (thePlayer == null || thePlayer.sneaking) return

        when (modeValue.get().toLowerCase()) {
            "ncp", "vanilla" -> if (collideBlock(thePlayer.entityBoundingBox, classProvider::isBlockLiquid) && thePlayer.isInsideOfMaterial(classProvider.getMaterialEnum(MaterialType.AIR)) && !thePlayer.sneaking) thePlayer.motionY = 0.08
            "aac" -> {
                val blockPos = thePlayer.position.down()
                if (!thePlayer.onGround && getBlock(blockPos) == classProvider.getBlockEnum(BlockType.WATER) || thePlayer.isInWater) {
                    if (!thePlayer.sprinting) {
                        thePlayer.motionX *= 0.99999
                        thePlayer.motionY *= 0.0
                        thePlayer.motionZ *= 0.99999
                        if (thePlayer.isCollidedHorizontally) thePlayer.motionY = ((thePlayer.posY - (thePlayer.posY - 1).toInt()).toInt() / 8f).toDouble()
                    } else {
                        thePlayer.motionX *= 0.99999
                        thePlayer.motionY *= 0.0
                        thePlayer.motionZ *= 0.99999
                        if (thePlayer.isCollidedHorizontally) thePlayer.motionY = ((thePlayer.posY - (thePlayer.posY - 1).toInt()).toInt() / 8f).toDouble()
                    }
                    if (thePlayer.fallDistance >= 4) thePlayer.motionY = -0.004 else if (thePlayer.isInWater) thePlayer.motionY = 0.09
                }
                if (thePlayer.hurtTime != 0) thePlayer.onGround = false
            }
            "spartan" -> if (thePlayer.isInWater) {
                if (thePlayer.isCollidedHorizontally) {
                    thePlayer.motionY += 0.15
                    return
                }
                val block = getBlock(WBlockPos(thePlayer.posX, thePlayer.posY + 1, thePlayer.posZ))
                val blockUp = getBlock(WBlockPos(thePlayer.posX, thePlayer.posY + 1.1, thePlayer.posZ))

                if (classProvider.isBlockLiquid(blockUp)) {
                    thePlayer.motionY = 0.1
                } else if (classProvider.isBlockLiquid(block)) {
                    thePlayer.motionY = 0.0
                }

                thePlayer.onGround = true
                thePlayer.motionX *= 1.085
                thePlayer.motionZ *= 1.085
            }
            "aac3.3.11" -> if (thePlayer.isInWater) {
                thePlayer.motionX *= 1.17
                thePlayer.motionZ *= 1.17
                if (thePlayer.isCollidedHorizontally) thePlayer.motionY = 0.24 else if (mc.theWorld!!.getBlockState(WBlockPos(thePlayer.posX, thePlayer.posY + 1.0, thePlayer.posZ)).block != classProvider.getBlockEnum(BlockType.AIR)) thePlayer.motionY += 0.04
            }
            "dolphin" -> if (thePlayer.isInWater) thePlayer.motionY += 0.03999999910593033
        }
    }

    @EventTarget
    fun onMove(event: MoveEvent) {
        if ("aacfly" == modeValue.get().toLowerCase() && mc.thePlayer!!.isInWater) {
            event.y = aacFlyValue.get().toDouble()
            mc.thePlayer!!.motionY = aacFlyValue.get().toDouble()
        }
    }

    @EventTarget
    fun onBlockBB(event: BlockBBEvent) {
        if (mc.thePlayer == null)
            return

        if (classProvider.isBlockLiquid(event.block) && !collideBlock(mc.thePlayer!!.entityBoundingBox, classProvider::isBlockLiquid) && !mc.thePlayer!!.sneaking) {
            when (modeValue.get().toLowerCase()) {
                "ncp", "vanilla" -> event.boundingBox = classProvider.createAxisAlignedBB(event.x.toDouble(), event.y.toDouble(), event.z.toDouble(), event.x + 1.toDouble(), event.y + 1.toDouble(), event.z + 1.toDouble())
            }
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val thePlayer = mc.thePlayer

        if (thePlayer == null || !modeValue.get().equals("NCP", ignoreCase = true))
            return

        if (classProvider.isCPacketPlayer(event.packet)) {
            val packetPlayer = event.packet.asCPacketPlayer()

            if (collideBlock(classProvider.createAxisAlignedBB(thePlayer.entityBoundingBox.maxX, thePlayer.entityBoundingBox.maxY, thePlayer.entityBoundingBox.maxZ, thePlayer.entityBoundingBox.minX, thePlayer.entityBoundingBox.minY - 0.01, thePlayer.entityBoundingBox.minZ), classProvider::isBlockLiquid)) {
                nextTick = !nextTick
                if (nextTick) packetPlayer.y -= 0.001
            }
        }
    }

    @EventTarget
    fun onJump(event: JumpEvent) {
        val thePlayer = mc.thePlayer ?: return

        val block = getBlock(WBlockPos(thePlayer.posX, thePlayer.posY - 0.01, thePlayer.posZ))

        if (noJumpValue.get() && classProvider.isBlockLiquid(block))
            event.cancelEvent()
    }

    override val tag: String
        get() = modeValue.get()
}