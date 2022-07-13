/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.extensions.getBlock
import net.ccbluex.liquidbounce.utils.extensions.getMaterial
import net.ccbluex.liquidbounce.utils.extensions.multiply
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.block.BlockAir
import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos

@ModuleInfo(name = "IceSpeed", description = "Allows you to walk faster on ice.", category = ModuleCategory.MOVEMENT)
class IceSpeed : Module()
{
    private val modeValue = ListValue("Mode", arrayOf("NCP", "AAC3.2.0", "Spartan146"), "NCP")
    override fun onEnable()
    {
        if (modeValue.get().equals("NCP", ignoreCase = true))
        {
            Blocks.ice.slipperiness = 0.39f
            Blocks.packed_ice.slipperiness = 0.39f
        }
        super.onEnable()
    }

    @EventTarget
    fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent)
    {
        val mode = modeValue.get()

        if (mode.equals("NCP", ignoreCase = true))
        {
            Blocks.ice.slipperiness = 0.39f
            Blocks.packed_ice.slipperiness = 0.39f
        }
        else
        {
            Blocks.ice.slipperiness = 0.98f
            Blocks.packed_ice.slipperiness = 0.98f
        }

        val theWorld = mc.theWorld ?: return
        val thePlayer = mc.thePlayer ?: return

        if (thePlayer.onGround && !thePlayer.isOnLadder && !thePlayer.isSneaking && thePlayer.isSprinting && thePlayer.movementInput.moveForward > 0.0)
        {
            when (mode.lowercase())
            {
                "aac3.2.0" -> theWorld.getMaterial(thePlayer.position.down()).let {
                    if (it == Blocks.ice || it == Blocks.packed_ice)
                    {
                        thePlayer.multiply(1.342)

                        Blocks.ice.slipperiness = 0.6f
                        Blocks.packed_ice.slipperiness = 0.6f
                    }
                }

                "spartan146" -> theWorld.getMaterial(thePlayer.position.down()).let {
                    if (it == Blocks.ice || it == Blocks.packed_ice)
                    {
                        thePlayer.multiply(if (theWorld.getBlock(BlockPos(thePlayer.posX, thePlayer.posY + 2.0, thePlayer.posZ)) is BlockAir) 1.18 else 1.342)

                        Blocks.ice.slipperiness = 0.6f
                        Blocks.packed_ice.slipperiness = 0.6f
                    }
                }
            }
        }
    }

    override fun onDisable()
    {
        Blocks.ice.slipperiness = 0.98f
        Blocks.packed_ice.slipperiness = 0.98f
    }
}
