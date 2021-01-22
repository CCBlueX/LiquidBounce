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
import java.util.*

@ModuleInfo(name = "BlockESP", description = "Allows you to see a selected block through walls.", category = ModuleCategory.RENDER)
class BlockESP : Module()
{
	private val modeValue = ListValue("Mode", arrayOf("Box", "2D"), "Box")

	private val blockValue = BlockValue("Block", 168)
	private val radiusValue = IntegerValue("Radius", 40, 5, 120)
	private val blockLimitValue = IntegerValue("BlockLimit", 256, 0, 2056)

	private val colorRedValue = IntegerValue("R", 255, 0, 255)
	private val colorGreenValue = IntegerValue("G", 179, 0, 255)
	private val colorBlueValue = IntegerValue("B", 72, 0, 255)
	private val colorAlphaValue = IntegerValue("Alpha", 72, 0, 255)

	private val colorRainbow = BoolValue("Rainbow", false)
	private val rainbowSpeedValue = IntegerValue("Rainbow-Speed", 10, 1, 10)
	private val saturationValue = FloatValue("HSB-Saturation", 1.0f, 0.0f, 1.0f)
	private val brightnessValue = FloatValue("HSB-Brightness", 1.0f, 0.0f, 1.0f)

	private val searchTimer = MSTimer()
	private val posList: MutableList<WBlockPos> = ArrayList()
	private var thread: Thread? = null

	@EventTarget
	fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent?)
	{
		if (searchTimer.hasTimePassed(1000L) && (thread == null || !thread!!.isAlive))
		{
			val radius = radiusValue.get()
			val selectedBlock = functions.getBlockById(blockValue.get())

			if (selectedBlock == null || selectedBlock == classProvider.getBlockEnum(BlockType.AIR)) return

			thread = Thread({
				val blockList: MutableList<WBlockPos> = ArrayList()

				for (x in -radius until radius)
				{
					for (y in radius downTo -radius + 1)
					{
						for (z in -radius until radius)
						{
							val thePlayer = mc.thePlayer!!

							val xPos = thePlayer.posX.toInt() + x
							val yPos = thePlayer.posY.toInt() + y
							val zPos = thePlayer.posZ.toInt() + z

							val blockPos = WBlockPos(xPos, yPos, zPos)
							val block = getBlock(blockPos)

							if (block == selectedBlock && blockList.size < blockLimitValue.get()) blockList.add(blockPos)
						}
					}
				}
				searchTimer.reset()

				synchronized(posList) {
					posList.clear()
					posList.addAll(blockList)
				}
			}, "BlockESP-BlockFinder")

			thread!!.start()
		}
	}

	@EventTarget
	fun onRender3D(@Suppress("UNUSED_PARAMETER") event: Render3DEvent?)
	{
		val alpha = colorAlphaValue.get()
		synchronized(posList) {
			val color =
				if (colorRainbow.get()) rainbow(alpha = alpha, speed = rainbowSpeedValue.get(), saturation = saturationValue.get(), brightness = brightnessValue.get()) else Color(colorRedValue.get(), colorGreenValue.get(), colorBlueValue.get(), alpha)

			for (blockPos in posList)
			{
				when (modeValue.get().toLowerCase())
				{
					"box" -> RenderUtils.drawBlockBox(blockPos, color, true)
					"2d" -> RenderUtils.draw2D(blockPos, color.rgb, Color.BLACK.rgb)
				}
			}
		}
	}

	override val tag: String
		get() = getBlockName(blockValue.get())
}
