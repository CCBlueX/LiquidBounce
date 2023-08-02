/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlockName
import net.ccbluex.liquidbounce.utils.render.ColorUtils.rainbow
import net.ccbluex.liquidbounce.utils.render.RenderUtils.draw2D
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBlockBox
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.BlockValue
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.block.Block
import net.minecraft.init.Blocks.air
import net.minecraft.util.BlockPos
import java.awt.Color

object BlockESP : Module("BlockESP", ModuleCategory.RENDER) {
    private val mode by ListValue("Mode", arrayOf("Box", "2D"), "Box")
    private val block by BlockValue("Block", 168)
    private val radius by IntegerValue("Radius", 40, 5..120)
    private val blockLimit by IntegerValue("BlockLimit", 256, 0..2056)

    private val colorRainbow by BoolValue("Rainbow", false)
    private val colorRed by IntegerValue("R", 255, 0..255) { !colorRainbow }
    private val colorGreen by IntegerValue("G", 179, 0..255) { !colorRainbow }
    private val colorBlue by IntegerValue("B", 72, 0..255) { !colorRainbow }

    private val searchTimer = MSTimer()
    private val posList = mutableListOf<BlockPos>()
    private var thread: Thread? = null

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (searchTimer.hasTimePassed(1000) && (thread?.isAlive != true)) {
            val radius = radius
            val selectedBlock = Block.getBlockById(block)

            if (selectedBlock == null || selectedBlock == air)
                return

            thread = Thread({
                val blockList = mutableListOf<BlockPos>()

                for (x in -radius until radius) {
                    for (y in radius downTo -radius + 1) {
                        for (z in -radius until radius) {
                            val thePlayer = mc.thePlayer

                            val xPos = thePlayer.posX.toInt() + x
                            val yPos = thePlayer.posY.toInt() + y
                            val zPos = thePlayer.posZ.toInt() + z

                            val blockPos = BlockPos(xPos, yPos, zPos)
                            val block = getBlock(blockPos)

                            if (block == selectedBlock && blockList.size < blockLimit) blockList += blockPos
                        }
                    }
                }
                searchTimer.reset()

                synchronized(posList) {
                    posList.clear()
                    posList += blockList
                }
            }, "BlockESP-BlockFinder")

            thread!!.start()
        }
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        synchronized(posList) {
            val color = if (colorRainbow) rainbow() else Color(colorRed, colorGreen, colorBlue)
            for (blockPos in posList) {
                when (mode.lowercase()) {
                    "box" -> drawBlockBox(blockPos, color, true)
                    "2d" -> draw2D(blockPos, color.rgb, Color.BLACK.rgb)
                }
            }
        }
    }

    override val tag
        get() = getBlockName(block)
}