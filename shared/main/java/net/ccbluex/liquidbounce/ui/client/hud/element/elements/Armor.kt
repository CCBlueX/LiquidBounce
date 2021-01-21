/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.api.enums.MaterialType
import net.ccbluex.liquidbounce.ui.client.hud.element.*
import net.ccbluex.liquidbounce.value.ListValue
import org.lwjgl.opengl.GL11

/**
 * CustomHUD Armor element
 *
 * Shows a horizontal display of current armor
 */
@ElementInfo(name = "Armor")
class Armor(
	x: Double = -8.0, y: Double = 57.0, scale: Float = 1F, side: Side = Side(Side.Horizontal.MIDDLE, Side.Vertical.DOWN)
) : Element(x, y, scale, side)
{

	private val modeValue = ListValue("Alignment", arrayOf("Horizontal", "Vertical"), "Horizontal")

	/**
	 * Draw element
	 */
	override fun drawElement(): Border
	{
		if (mc.playerController.isNotCreative)
		{
			GL11.glPushMatrix()

			val renderItem = mc.renderItem

			// Prevents being conflicted with the bubbles
			val isInsideWater = mc.thePlayer!!.isInsideOfMaterial(classProvider.getMaterialEnum(MaterialType.WATER))

			var x = 1
			var y = if (isInsideWater) -10 else 0

			val mode = modeValue.get()

			for (index in 3 downTo 0)
			{
				val stack = mc.thePlayer!!.inventory.armorInventory[index] ?: continue

				renderItem.renderItemIntoGUI(stack, x, y)
				renderItem.renderItemOverlays(mc.fontRendererObj, stack, x, y)
				if (mode.equals("Horizontal", true)) x += 18
				else if (mode.equals("Vertical", true)) y += 18
			}

			classProvider.getGlStateManager().enableAlpha()
			classProvider.getGlStateManager().disableBlend()
			classProvider.getGlStateManager().disableLighting()
			classProvider.getGlStateManager().disableCull()
			GL11.glPopMatrix()
		}

		return if (modeValue.get().equals("Horizontal", true)) Border(0F, 0F, 72F, 17F)
		else Border(0F, 0F, 18F, 72F)
	}
}
