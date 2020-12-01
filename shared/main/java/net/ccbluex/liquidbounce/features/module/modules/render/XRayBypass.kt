package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.minecraft.block.Block
import net.minecraft.client.Minecraft
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos

@ModuleInfo(name = "XRayBypass", description = "Allows you to bypass AntiXRay plugin.", category = ModuleCategory.RENDER)
class XRayBypass : Module() {

    override fun onEnable() {
        var i = 16
        var x = 0
        var y = 0
        var z = 0

        for (posX in -i until i) {
            for (posY in i downTo -i + 1) {
                for (posZ in -i until i) {
                    z = mc.thePlayer!!.posX.toInt() + posX
                    y = mc.thePlayer!!.posY.toInt() + posY
                    x = mc.thePlayer!!.posZ.toInt() + posZ
                    val blockPos = BlockPos(x, y, z)
                    val block = Minecraft.getMinecraft().world.getBlockState(blockPos).block
                    if (Block.getIdFromBlock(block) == 56) {
                        Minecraft.getMinecraft().playerController.clickBlock(blockPos, EnumFacing.DOWN)
                    }
                }
            }
        }
       mc.renderGlobal.loadRenderers()
    }

    override fun onDisable() {
        mc.renderGlobal.loadRenderers()
    }

}