/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.LiquidBounceStyle
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.NullStyle
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.SlowlyStyle
import net.ccbluex.liquidbounce.utils.render.ColorUtils.rainbow
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import org.lwjgl.input.Keyboard
import java.awt.Color

@ModuleInfo(name = "ClickGUI", description = "Opens the ClickGUI.", category = ModuleCategory.RENDER, defaultKeyBinds = [Keyboard.KEY_RSHIFT], canEnable = false)
class ClickGUI : Module()
{
	/**
	 * Options
	 */
	private val styleValue: ListValue = object : ListValue("Style", arrayOf("LiquidBounce", "Null", "Slowly"), "Slowly")
	{
		override fun onChanged(oldValue: String, newValue: String)
		{
			updateStyle()
		}
	}
	val scaleValue = FloatValue("Scale", 1.0f, 0.7f, 2.0f)
	val maxElementsValue = IntegerValue("MaxElements", 15, 1, 20)

	override fun onEnable()
	{
		updateStyle()
		mc.displayGuiScreen(classProvider.wrapGuiScreen(LiquidBounce.clickGui))
	}

	fun updateStyle()
	{
		LiquidBounce.clickGui.style = when (styleValue.get().toLowerCase())
		{
			"liquidbounce" -> LiquidBounceStyle()
			"slowly" -> SlowlyStyle()
			else -> NullStyle() // null style
		}
	}

	@EventTarget(ignoreCondition = true)
	fun onPacket(event: PacketEvent)
	{
		val packet = event.packet

		val provider = classProvider

		if (provider.isSPacketCloseWindow(packet) && provider.isClickGui(mc.currentScreen)) event.cancelEvent()
	}

	companion object
	{
		private val colorRedValue = IntegerValue("R", 0, 0, 255)
		private val colorGreenValue = IntegerValue("G", 160, 0, 255)
		private val colorBlueValue = IntegerValue("B", 255, 0, 255)
		private val colorRainbowValue = BoolValue("Rainbow", false)
		private val rainbowSpeedValue = IntegerValue("Rainbow-Speed", 10, 1, 10)
		private val saturationValue = FloatValue("HSB-Saturation", 1.0f, 0.0f, 1.0f)
		private val brightnessValue = FloatValue("HSB-Brightness", 1.0f, 0.0f, 1.0f)

		@JvmStatic
		fun generateColor(): Color = if (colorRainbowValue.get()) rainbow(speed = rainbowSpeedValue.get(), saturation = saturationValue.get(), brightness = brightnessValue.get()) else Color(colorRedValue.get(), colorGreenValue.get(), colorBlueValue.get())
	}
}
