/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import co.uk.hexeption.utils.OutlineUtils
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.tileentity.ITileEntity
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.world.ChestAura.clickedBlocks
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.shader.shaders.GlowShader
import net.ccbluex.liquidbounce.utils.render.shader.shaders.OutlineShader
import net.ccbluex.liquidbounce.value.*
import org.lwjgl.opengl.GL11
import java.awt.Color

@ModuleInfo(name = "StorageESP", description = "Allows you to see chests, dispensers, etc. through walls.", category = ModuleCategory.RENDER)
class StorageESP : Module()
{
	private val modeValue = ListValue("Mode", arrayOf("Box", "OtherBox", "Outline", "ShaderOutline", "ShaderGlow", "2D", "WireFrame"), "Outline")

	private val chestValue = BoolValue("Chest", true)
	private val chestRedValue = IntegerValue("Chest-R", 0, 0, 255)
	private val chestGreenValue = IntegerValue("Chest-G", 66, 0, 255)
	private val chestBlueValue = IntegerValue("Chest-B", 255, 0, 255)
	private val chestRainbowValue = BoolValue("Chest-Rainbow", false)

	private val enderChestValue = BoolValue("EnderChest", true)
	private val enderChestRedValue = IntegerValue("EnderChest-R", 255, 0, 255)
	private val enderChestGreenValue = IntegerValue("EnderChest-G", 0, 0, 255)
	private val enderChestBlueValue = IntegerValue("EnderChest-B", 255, 0, 255)
	private val enderChestRainbowValue = BoolValue("EnderChest-Rainbow", false)

	private val furnaceValue = BoolValue("Furnace", true)
	private val furnaceRedValue = IntegerValue("Furnace-R", 0, 0, 255)
	private val furnaceGreenValue = IntegerValue("Furnace-G", 0, 0, 255)
	private val furnaceBlueValue = IntegerValue("Furnace-B", 0, 0, 255)
	private val furnaceRainbowValue = BoolValue("Furnace-Rainbow", false)

	private val dispenserValue = BoolValue("Dispenser", true)
	private val dispenserRedValue = IntegerValue("Dispenser-R", 0, 0, 255)
	private val dispenserGreenValue = IntegerValue("Dispenser-G", 0, 0, 255)
	private val dispenserBlueValue = IntegerValue("Dispenser-B", 0, 0, 255)
	private val dispenserRainbowValue = BoolValue("Dispenser-Rainbow", false)

	private val hopperValue = BoolValue("Hopper", true)
	private val hopperRedValue = IntegerValue("Hopper-R", 128, 0, 255)
	private val hopperGreenValue = IntegerValue("Hopper-G", 128, 0, 255)
	private val hopperBlueValue = IntegerValue("Hopper-B", 128, 0, 255)
	private val hopperRainbowValue = BoolValue("Hopper-Rainbow", false)

	private val shulkerBoxValue = BoolValue("ShulkerBox", true)
	private val shulkerBoxRedValue = IntegerValue("ShulkerBox-R", 110, 0, 255)
	private val shulkerBoxGreenValue = IntegerValue("ShulkerBox-G", 77, 0, 255)
	private val shulkerBoxBlueValue = IntegerValue("ShulkerBox-B", 110, 0, 255)
	private val shulkerBoxRainbowValue = BoolValue("ShulkerBox-Rainbow", false)

	private val rainbowSpeedValue = IntegerValue("Rainbow-Speed", 10, 1, 10)

	private val saturationValue = FloatValue("HSB-Saturation", 1.0f, 0.0f, 1.0f)
	private val brightnessValue = FloatValue("HSB-Brightness", 1.0f, 0.0f, 1.0f)

	@EventTarget
	fun onRender3D(event: Render3DEvent)
	{
		val theWorld = mc.theWorld ?: return

		try
		{
			val mode = modeValue.get()
			val saturation = saturationValue.get()
			val brightness = brightnessValue.get()
			val rainbowSpeed = rainbowSpeedValue.get()
			val chest = chestValue.get()
			val enderChest = enderChestValue.get()
			val furnace = furnaceValue.get()
			val dispenser = dispenserValue.get()
			val hopper = hopperValue.get()
			val shulkerBox = shulkerBoxValue.get()

			val rainbow = ColorUtils.rainbow(alpha = 1.0f, speed = rainbowSpeed, saturation = saturation, brightness = brightness)

			val chestColor = if (chestRainbowValue.get()) rainbow else Color(chestRedValue.get(), chestGreenValue.get(), chestBlueValue.get())
			val enderChestColor = if (enderChestRainbowValue.get()) rainbow else Color(enderChestRedValue.get(), enderChestGreenValue.get(), enderChestBlueValue.get())
			val furnaceColor = if (furnaceRainbowValue.get()) rainbow else Color(furnaceRedValue.get(), furnaceGreenValue.get(), furnaceBlueValue.get())
			val dispenserColor = if (dispenserRainbowValue.get()) rainbow else Color(dispenserRedValue.get(), dispenserGreenValue.get(), dispenserBlueValue.get())
			val hopperColor = if (hopperRainbowValue.get()) rainbow else Color(hopperRedValue.get(), hopperGreenValue.get(), hopperBlueValue.get())
			val shulkerBoxColor = if (shulkerBoxRainbowValue.get()) rainbow else Color(shulkerBoxRedValue.get(), shulkerBoxGreenValue.get(), shulkerBoxBlueValue.get())

			val partialTicks = event.partialTicks

			if (mode.equals("outline", ignoreCase = true))
			{
				ClientUtils.disableFastRender()
				OutlineUtils.checkSetupFBO()
			}

			val gamma = mc.gameSettings.gammaSetting

			mc.gameSettings.gammaSetting = 100000.0f


			for (tileEntity in theWorld.loadedTileEntityList)
			{
				val color: Color = when
				{
					chest && classProvider.isTileEntityChest(tileEntity) && !clickedBlocks.contains(tileEntity.pos) -> chestColor
					enderChest && classProvider.isTileEntityEnderChest(tileEntity) && !clickedBlocks.contains(tileEntity.pos) -> enderChestColor
					furnace && classProvider.isTileEntityFurnace(tileEntity) -> furnaceColor
					dispenser && classProvider.isTileEntityDispenser(tileEntity) -> dispenserColor
					hopper && classProvider.isTileEntityHopper(tileEntity) -> hopperColor
					shulkerBox && classProvider.isTileEntityShulkerBox(tileEntity) -> shulkerBoxColor.brighter()
					else -> null
				} ?: continue

				if (!(classProvider.isTileEntityChest(tileEntity) || classProvider.isTileEntityEnderChest(tileEntity)))
				{
					RenderUtils.drawBlockBox(tileEntity.pos, color, mode.equals("Box", ignoreCase = true))
					continue
				}

				when (mode.toLowerCase())
				{
					"otherbox", "box" -> RenderUtils.drawBlockBox(tileEntity.pos, color, mode.equals("Box", ignoreCase = true))

					"2d" -> RenderUtils.draw2D(tileEntity.pos, color.rgb, Color.BLACK.rgb)

					"outline" ->
					{
						RenderUtils.glColor(color)

						OutlineUtils.renderOne(3f)

						functions.renderTileEntity(tileEntity, partialTicks, -1)

						OutlineUtils.renderTwo()

						functions.renderTileEntity(tileEntity, partialTicks, -1)

						OutlineUtils.renderThree()

						functions.renderTileEntity(tileEntity, partialTicks, -1)

						OutlineUtils.renderFour(color)

						functions.renderTileEntity(tileEntity, partialTicks, -1)

						OutlineUtils.renderFive()
						OutlineUtils.setColor(Color.WHITE)
					}

					"wireframe" ->
					{
						GL11.glPushMatrix()
						GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)
						GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE)
						GL11.glDisable(GL11.GL_TEXTURE_2D)
						GL11.glDisable(GL11.GL_LIGHTING)
						GL11.glDisable(GL11.GL_DEPTH_TEST)
						GL11.glEnable(GL11.GL_LINE_SMOOTH)
						GL11.glEnable(GL11.GL_BLEND)
						GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)

						functions.renderTileEntity(tileEntity, partialTicks, -1)

						RenderUtils.glColor(color)

						GL11.glLineWidth(1.5f)

						functions.renderTileEntity(tileEntity, partialTicks, -1)

						GL11.glPopAttrib()
						GL11.glPopMatrix()
					}
				}
			}

			for (it in theWorld.loadedEntityList)
			{
				val color = when
				{
					classProvider.isEntityMinecartChest(it) -> chestColor
					classProvider.isEntityMinecartFurnace(it) -> furnaceColor
					classProvider.isEntityMinecartHopper(it) -> hopperColor
					else -> null
				} ?: continue


				when (mode.toLowerCase())
				{
					"otherbox", "box" -> RenderUtils.drawEntityBox(it, color, mode.equals("Box", ignoreCase = true))

					"2d" -> RenderUtils.draw2D(it.position, color.rgb, Color.BLACK.rgb)

					"outline" ->
					{
						val entityShadow: Boolean = mc.gameSettings.entityShadows
						mc.gameSettings.entityShadows = false

						RenderUtils.glColor(color)

						OutlineUtils.renderOne(3f)

						mc.renderManager.renderEntityStatic(it, partialTicks, true)

						OutlineUtils.renderTwo()

						mc.renderManager.renderEntityStatic(it, partialTicks, true)

						OutlineUtils.renderThree()

						mc.renderManager.renderEntityStatic(it, partialTicks, true)

						OutlineUtils.renderFour(color)

						mc.renderManager.renderEntityStatic(it, partialTicks, true)

						OutlineUtils.renderFive()
						OutlineUtils.setColor(Color.WHITE)

						mc.gameSettings.entityShadows = entityShadow
					}

					"wireframe" ->
					{
						val entityShadow: Boolean = mc.gameSettings.entityShadows
						mc.gameSettings.entityShadows = false

						GL11.glPushMatrix()
						GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)
						GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE)
						GL11.glDisable(GL11.GL_TEXTURE_2D)
						GL11.glDisable(GL11.GL_LIGHTING)
						GL11.glDisable(GL11.GL_DEPTH_TEST)
						GL11.glEnable(GL11.GL_LINE_SMOOTH)
						GL11.glEnable(GL11.GL_BLEND)
						GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)

						RenderUtils.glColor(color)

						mc.renderManager.renderEntityStatic(it, partialTicks, true)

						RenderUtils.glColor(color)

						GL11.glLineWidth(1.5f)

						mc.renderManager.renderEntityStatic(it, partialTicks, true)

						GL11.glPopAttrib()
						GL11.glPopMatrix()

						mc.gameSettings.entityShadows = entityShadow
					}
				}
			}

			RenderUtils.glColor(Color(255, 255, 255, 255))

			mc.gameSettings.gammaSetting = gamma
		} catch (ignored: Exception)
		{
		}
	}

	@EventTarget
	fun onRender2D(event: Render2DEvent)
	{
		val mode = modeValue.get()

		val chest = chestValue.get()
		val enderChest = enderChestValue.get()
		val furnace = furnaceValue.get()
		val dispenser = dispenserValue.get()
		val hopper = hopperValue.get()
		val shulkerBox = shulkerBoxValue.get()

		val saturation = saturationValue.get()
		val brightness = brightnessValue.get()
		val rainbowSpeed = rainbowSpeedValue.get()
		val rainbow = ColorUtils.rainbow(alpha = 1.0f, speed = rainbowSpeed, saturation = saturation, brightness = brightness)
		val chestColor = if (chestRainbowValue.get()) rainbow else Color(chestRedValue.get(), chestGreenValue.get(), chestBlueValue.get())
		val enderChestColor = if (enderChestRainbowValue.get()) rainbow else Color(enderChestRedValue.get(), enderChestGreenValue.get(), enderChestBlueValue.get())
		val furnaceColor = if (furnaceRainbowValue.get()) rainbow else Color(furnaceRedValue.get(), furnaceGreenValue.get(), furnaceBlueValue.get())
		val dispenserColor = if (dispenserRainbowValue.get()) rainbow else Color(dispenserRedValue.get(), dispenserGreenValue.get(), dispenserBlueValue.get())
		val hopperColor = if (hopperRainbowValue.get()) rainbow else Color(hopperRedValue.get(), hopperGreenValue.get(), hopperBlueValue.get())
		val shulkerBoxColor = if (shulkerBoxRainbowValue.get()) rainbow else Color(shulkerBoxRedValue.get(), shulkerBoxGreenValue.get(), shulkerBoxBlueValue.get())

		val shader = (if (mode.equals("shaderoutline", ignoreCase = true)) OutlineShader.OUTLINE_SHADER else if (mode.equals("shaderglow", ignoreCase = true)) GlowShader.GLOW_SHADER else null) ?: return
		val radius = if (mode.equals("shaderglow", ignoreCase = true)) 2.5f else 1.5f

		val renderManager = mc.renderManager

		val renderPosX = renderManager.renderPosX
		val renderPosY = renderManager.renderPosY
		val renderPosZ = renderManager.renderPosZ

		val partialTicks = event.partialTicks

		try
		{
			val startDraw = { shader.startDraw(partialTicks) }
			val renderTileEntity = { predicate: (ITileEntity) -> Boolean ->
				mc.theWorld!!.loadedTileEntityList.asSequence().filter(predicate).forEach {
					mc.renderManager.renderEntityAt(it, it.pos.x - renderPosX, it.pos.y - renderPosY, it.pos.z - renderPosZ, partialTicks)
				}
			}

			val renderMinecart = { predicate: (IEntity) -> Boolean ->
				mc.theWorld!!.loadedEntityList.asSequence().filter(predicate).forEach {
					renderManager.renderEntityStatic(it, partialTicks, true)
				}
			}

			val stopDraw = { color: Color -> shader.stopDraw(color, radius, 1f) }

			val renderTileEntityOnly = { predicate: ((ITileEntity) -> Boolean), color: Color ->
				startDraw()
				renderTileEntity(predicate)
				stopDraw(color)
			}

			// Draw Chest and MinecartChest
			if (chest)
			{
				startDraw()
				renderTileEntity(classProvider::isTileEntityChest)
				renderMinecart(classProvider::isEntityMinecartChest)
				stopDraw(chestColor)
			}

			// Draw EnderChest
			if (enderChest) renderTileEntityOnly(classProvider::isTileEntityEnderChest, enderChestColor)

			// Draw Furnace and MinecartFurnace
			if (furnace)
			{
				startDraw()
				renderTileEntity(classProvider::isTileEntityFurnace)
				renderMinecart(classProvider::isEntityMinecartFurnace)
				stopDraw(furnaceColor)
			}

			// Draw Dispenser
			if (dispenser) renderTileEntityOnly(classProvider::isTileEntityDispenser, dispenserColor)

			// Draw Hopper and MinecartHopper
			if (hopper)
			{
				startDraw()
				renderTileEntity(classProvider::isTileEntityHopper)
				renderMinecart(classProvider::isEntityMinecartHopper)
				stopDraw(hopperColor)
			}

			// Draw Shulker box
			if (shulkerBox) renderTileEntityOnly(classProvider::isTileEntityShulkerBox, shulkerBoxColor)
		} catch (ex: Exception)
		{
			ClientUtils.logger.error("An error occurred while rendering all storages for shader esp", ex)
		}
	}

	override val tag: String
		get() = modeValue.get()
}
