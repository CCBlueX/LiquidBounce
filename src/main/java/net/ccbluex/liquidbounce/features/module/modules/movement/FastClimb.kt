/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.BlockBBEvent
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.block.BlockUtils.collideBlockIntersects
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.block.BlockLadder
import net.minecraft.block.BlockVine
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing

@ModuleInfo(name = "FastClimb", description = "Allows you to climb up ladders and vines faster.", category = ModuleCategory.MOVEMENT)
class FastClimb : Module() {

    val modeValue = ListValue("Mode",
            arrayOf("Vanilla", "Clip", "AAC3.0.0", "AAC3.0.5", "SAAC3.1.2", "AAC3.1.2"), "Vanilla")
    private val speedValue = FloatValue("Speed", 0.2872F, 0.01F, 5F)

    @EventTarget
    fun onMove(event: MoveEvent) {
        val mode = modeValue.get()

        val thePlayer = mc.thePlayer ?: return

        when {
            mode.equals("Vanilla", ignoreCase = true) && thePlayer.isCollidedHorizontally &&
                    thePlayer.isOnLadder -> {
                event.y = speedValue.get().toDouble()
                thePlayer.motionY = 0.0
            }

            mode.equals("AAC3.0.0", ignoreCase = true) && thePlayer.isCollidedHorizontally -> {
                var x = 0.0
                var z = 0.0

                val horizontalFacing = thePlayer.horizontalFacing

                when(horizontalFacing) {
                    EnumFacing.NORTH -> z = -0.99
                    EnumFacing.EAST -> x = +0.99
                    EnumFacing.SOUTH -> z = +0.99
                    EnumFacing.WEST -> x = -0.99
                    else -> { }
                }

                val block = getBlock(BlockPos(thePlayer.posX + x, thePlayer.posY, thePlayer.posZ + z))

                if (block is BlockLadder || block is BlockVine) {
                    event.y = 0.5
                    thePlayer.motionY = 0.0
                }
            }

            mode.equals("AAC3.0.5", ignoreCase = true) && mc.gameSettings.keyBindForward.isKeyDown &&
                    collideBlockIntersects(thePlayer.entityBoundingBox) {
                        it is BlockLadder || it is BlockVine
                    } -> {
                event.x = 0.0
                event.y = 0.5
                event.z = 0.0

                thePlayer.motionX = 0.0
                thePlayer.motionY = 0.0
                thePlayer.motionZ = 0.0
            }

            mode.equals("SAAC3.1.2", ignoreCase = true) && thePlayer.isCollidedHorizontally &&
                    thePlayer.isOnLadder -> {
                event.y = 0.1649
                thePlayer.motionY = 0.0
            }

            mode.equals("AAC3.1.2", ignoreCase = true) && thePlayer.isCollidedHorizontally &&
                    thePlayer.isOnLadder -> {
                event.y = 0.1699
                thePlayer.motionY = 0.0
            }

            mode.equals("Clip", ignoreCase = true) && thePlayer.isOnLadder && mc.gameSettings.keyBindForward.isKeyDown -> {
                for (i in thePlayer.posY.toInt()..thePlayer.posY.toInt() + 8) {
                    val block = getBlock(BlockPos(thePlayer.posX, i.toDouble(), thePlayer.posZ))

                    if (block !is BlockLadder) {
                        var x = 0.0
                        var z = 0.0
                        val horizontalFacing = thePlayer.horizontalFacing

                        when(horizontalFacing) {
                            EnumFacing.NORTH -> z = -1.0
                            EnumFacing.EAST -> x = +1.0
                            EnumFacing.SOUTH -> z = +1.0
                            EnumFacing.WEST -> x = -1.0
                            else -> { }
                        }

                        thePlayer.setPosition(thePlayer.posX + x, i.toDouble(), thePlayer.posZ + z)
                        break
                    } else {
                        thePlayer.setPosition(thePlayer.posX, i.toDouble(), thePlayer.posZ)
                    }
                }
            }
        }
    }

    @EventTarget
    fun onBlockBB(event: BlockBBEvent) {
        if (mc.thePlayer != null && (event.block is BlockLadder|| event.block is BlockVine) &&
                modeValue.get().equals("AAC3.0.5", ignoreCase = true) && mc.thePlayer!!.isOnLadder)
            event.boundingBox = null
    }

    override val tag: String
        get() = modeValue.get()
}