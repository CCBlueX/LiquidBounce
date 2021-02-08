/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.api.enums.BlockType
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlockName
import net.ccbluex.liquidbounce.utils.render.ColorUtils.rainbow
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.*
import java.awt.Color
import java.util.concurrent.*

// TODO: Improve performance
@ModuleInfo(name = "BlockESP", description = "Allows you to see a selected block through walls.", category = ModuleCategory.RENDER)
class BlockESP : Module()
{
	companion object
	{
		private val workerPool: ThreadPoolExecutor = ThreadPoolExecutor(1, 1, 1L, TimeUnit.MINUTES, LinkedBlockingQueue()) // To re-use worker thread
	}

	private val modeValue = ListValue("Mode", arrayOf("Box", "OtherBox", "2D"), "Box")

	private val blockValue = BlockValue("Block", 168)
	private val radiusValue = IntegerValue("Radius", 40, 5, 120)
	private val blockLimitValue = IntegerValue("BlockLimit", 256, 0, 2056)

	private val updateDelayValue = IntegerValue("UpdateDelay", 1000, 500, 2000)

	private val colorRedValue = IntegerValue("R", 255, 0, 255)
	private val colorGreenValue = IntegerValue("G", 179, 0, 255)
	private val colorBlueValue = IntegerValue("B", 72, 0, 255)
	private val colorAlphaValue = IntegerValue("Alpha", 72, 0, 255)

	private val colorRainbow = BoolValue("Rainbow", false)
	private val rainbowSpeedValue = IntegerValue("Rainbow-Speed", 10, 1, 10)
	private val saturationValue = FloatValue("HSB-Saturation", 1.0f, 0.0f, 1.0f)
	private val brightnessValue = FloatValue("HSB-Brightness", 1.0f, 0.0f, 1.0f)

	private val searchTimer = MSTimer()
	private val posList: MutableCollection<WBlockPos> = ConcurrentLinkedQueue()

	@Volatile
	private var task: Runnable? = null

	@Volatile
	private var moduleState = false

	override fun onEnable()
	{
		moduleState = true

		if (workerPool.queue.isEmpty())
		{
			workerPool.submit {
				while (moduleState)
				{
					if (task != null)
					{
						task!!.run()
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
	fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent?)
	{
		val thePlayer = mc.thePlayer ?: return

		if (task == null && searchTimer.hasTimePassed(updateDelayValue.get().toLong()))
		{
			val radius = radiusValue.get()
			val selectedBlock = functions.getBlockById(blockValue.get())

			if (selectedBlock == null || selectedBlock == classProvider.getBlockEnum(BlockType.AIR)) return

			val playerX = thePlayer.posX.toInt()
			val playerY = thePlayer.posY.toInt()
			val playerZ = thePlayer.posZ.toInt()

			task = Runnable {
				val blockList: MutableList<WBlockPos> = ArrayList()

				(-radius until radius).forEach { x -> (-radius until radius).forEach { y -> (-radius until radius).asSequence().map { z -> WBlockPos(playerX + x, playerY + y, playerZ + z) }.filter { getBlock(it) == selectedBlock && blockList.size < blockLimitValue.get() }.forEach { blockList.add(it) } } }

				searchTimer.reset()

				posList.clear()
				posList.addAll(blockList)
			}
		}
	}

	@EventTarget
	fun onRender3D(@Suppress("UNUSED_PARAMETER") event: Render3DEvent?)
	{
		val alpha = colorAlphaValue.get()
		val color = if (colorRainbow.get()) rainbow(alpha = alpha, speed = rainbowSpeedValue.get(), saturation = saturationValue.get(), brightness = brightnessValue.get()) else Color(colorRedValue.get(), colorGreenValue.get(), colorBlueValue.get(), alpha)

		for (blockPos in posList)
		{
			when (val mode = modeValue.get().toLowerCase())
			{
				"box", "otherbox" -> RenderUtils.drawBlockBox(blockPos, color, mode == "box")
				"2d" -> RenderUtils.draw2D(blockPos, color.rgb, Color.BLACK.rgb)
			}
		}
	}

	override val tag: String
		get() = getBlockName(blockValue.get())
}
