/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getMaterial
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos

object IceSpeed : Module("IceSpeed", Category.MOVEMENT) {
    private val mode by ListValue("Mode", arrayOf("NCP", "AAC", "Spartan"), "NCP")
    override fun onEnable() {
        if (mode == "NCP") {
            Blocks.ice.slipperiness = 0.39f
            Blocks.packed_ice.slipperiness = 0.39f
        }
        super.onEnable()
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val mode = mode
        if (mode == "NCP") {
            Blocks.ice.slipperiness = 0.39f
            Blocks.packed_ice.slipperiness = 0.39f
        } else {
            Blocks.ice.slipperiness = 0.98f
            Blocks.packed_ice.slipperiness = 0.98f
        }

        val thePlayer = mc.thePlayer ?: return

        if (thePlayer.onGround && !thePlayer.isOnLadder && !thePlayer.isSneaking && thePlayer.isSprinting && isMoving) {
            if (mode == "AAC") {
                getMaterial(thePlayer.position.down()).let {
                    if (it == Blocks.ice || it == Blocks.packed_ice) {
                        thePlayer.motionX *= 1.342
                        thePlayer.motionZ *= 1.342
                        Blocks.ice.slipperiness = 0.6f
                        Blocks.packed_ice.slipperiness = 0.6f
                    }
                }
            }
            if (mode == "Spartan") {
                getMaterial(thePlayer.position.down()).let {
                    if (it == Blocks.ice || it == Blocks.packed_ice) {
                        val upBlock = getBlock(BlockPos(thePlayer).up(2))

                        if (upBlock != Blocks.air) {
                            thePlayer.motionX *= 1.342
                            thePlayer.motionZ *= 1.342
                        } else {
                            thePlayer.motionX *= 1.18
                            thePlayer.motionZ *= 1.18
                        }

                        Blocks.ice.slipperiness = 0.6f
                        Blocks.packed_ice.slipperiness = 0.6f
                    }
                }
            }
        }
    }

    override fun onDisable() {
        Blocks.ice.slipperiness = 0.98f
        Blocks.packed_ice.slipperiness = 0.98f
        super.onDisable()
    }
}