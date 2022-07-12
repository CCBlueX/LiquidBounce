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
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlockName
import net.ccbluex.liquidbounce.utils.extensions.getBlock
import net.ccbluex.liquidbounce.utils.render.ColorUtils.rainbowRGB
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.*
import net.minecraft.block.Block
import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

// TODO: Improve performance
@ModuleInfo(name = "BlockESP", description = "Allows you to see a selected block through walls.", category = ModuleCategory.RENDER)
class BlockESP : Module()
{
    companion object
    {
        private val workerPool: ThreadPoolExecutor = ThreadPoolExecutor(1, 1, 1L, TimeUnit.MINUTES, LinkedBlockingQueue()) // To re-use worker thread
    }

    private val modeGroup = ValueGroup("Mode")
    private val modeValue = ListValue("Mode", arrayOf("Box", "OtherBox", "Hydra", "2D"), "Box")
    private val modeBoxOutlineColorValue = object : RGBAColorValue("BoxOutlineColor", 255, 255, 255, 90, listOf(null, null, null, "Outline-Alpha"))
    {
        override fun showCondition() = modeValue.get().equals("Box", ignoreCase = true)
    }

    private val blockValue = BlockValue("Block", 168)
    private val radiusValue = IntegerValue("Radius", 40, 5, 120)
    private val blockLimitValue = IntegerValue("BlockLimit", 256, 0, 2056)

    private val updateDelayValue = IntegerValue("UpdateDelay", 1000, 500, 2000)

    private val colorGroup = ValueGroup("Color")
    private val colorValue = RGBAColorValue("Color", 255, 255, 255, 30, listOf("R", "G", "B", "Alpha"))

    private val colorRainbowGroup = ValueGroup("Rainbow")
    private val colorRainbowEnabledValue = BoolValue("Enabled", true, "Rainbow")
    private val colorRainbowSpeedValue = IntegerValue("Speed", 10, 1, 10, "Rainbow-Speed")
    private val colorRainbowSaturationValue = FloatValue("Saturation", 1.0f, 0.0f, 1.0f, "HSB-Saturation")
    private val colorRainbowBrightnessValue = FloatValue("Brightness", 1.0f, 0.0f, 1.0f, "HSB-Brightness")

    private val searchTimer = MSTimer()
    private val posList: MutableCollection<BlockPos> = ConcurrentLinkedQueue()

    @Volatile
    private var task: Runnable? = null

    @Volatile
    private var moduleState = false

    init
    {
        modeGroup.addAll(modeValue, modeBoxOutlineColorValue)

        colorRainbowGroup.addAll(colorRainbowEnabledValue, colorRainbowSpeedValue, colorRainbowSaturationValue, colorRainbowBrightnessValue)
        colorGroup.addAll(colorValue, colorRainbowGroup)
    }

    override fun onEnable()
    {
        moduleState = true

        if (workerPool.queue.isEmpty())
        {
            workerPool.execute {
                while (moduleState)
                {
                    val currentTask = task

                    if (currentTask != null)
                    {
                        currentTask.run()
                        task = null
                    }

                    if (!moduleState) break
                }
            }
        }
    }

    override fun onDisable()
    {
        moduleState = false
    }

    @EventTarget
    fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent)
    {
        val theWorld = mc.theWorld ?: return
        val thePlayer = mc.thePlayer ?: return

        if (task == null && searchTimer.hasTimePassed(updateDelayValue.get().toLong()))
        {
            val radius = radiusValue.get()
            val selectedBlock = Block.getBlockById(blockValue.get())

            if (selectedBlock == null || selectedBlock == Blocks.air) return

            val playerX = thePlayer.posX.toInt()
            val playerY = thePlayer.posY.toInt()
            val playerZ = thePlayer.posZ.toInt()

            val blockLimit = blockLimitValue.get()
            task = Runnable {
                val blockList: MutableList<BlockPos> = ArrayList()

                (-radius until radius).forEach { x -> (-radius until radius).forEach { y -> (-radius until radius).map { z -> BlockPos(playerX + x, playerY + y, playerZ + z) }.filter { blockList.size < blockLimit }.filter { theWorld.getBlock(it) == selectedBlock }.forEach { blockList.add(it) } } }

                searchTimer.reset()

                posList.clear()
                posList.addAll(blockList)
            }
        }
    }

    @EventTarget
    fun onRender3D(@Suppress("UNUSED_PARAMETER") event: Render3DEvent)
    {
        val theWorld = mc.theWorld ?: return
        val thePlayer = mc.thePlayer ?: return

        val mode = modeValue.get().lowercase(Locale.getDefault())
        val hydraESP = mode == "hydra"
        val color = if (colorRainbowEnabledValue.get()) rainbowRGB(colorValue.getAlpha(), speed = colorRainbowSpeedValue.get(), saturation = colorRainbowSaturationValue.get(), brightness = colorRainbowBrightnessValue.get()) else colorValue.get()
        val outlineColor = modeBoxOutlineColorValue.get()

        for (blockPos in posList)
        {
            when (mode)
            {
                "box", "otherbox", "hydra" -> RenderUtils.drawBlockBox(theWorld, thePlayer, blockPos, color, outlineColor, hydraESP, 1f)
                "2d" -> RenderUtils.draw2D(blockPos, color, -16777216)
            }
        }
    }

    override val tag: String
        get() = getBlockName(blockValue.get())
}
