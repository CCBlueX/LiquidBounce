/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import co.uk.hexeption.utils.OutlineUtils
import net.ccbluex.liquidbounce.api.MinecraftVersion
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.world.ChestAura.clickedBlocks
import net.ccbluex.liquidbounce.injection.backend.Backend
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.shader.shaders.GlowShader
import net.ccbluex.liquidbounce.utils.render.shader.shaders.OutlineShader
import net.ccbluex.liquidbounce.value.*
import org.lwjgl.opengl.GL11.*
import java.awt.Color

@ModuleInfo(name = "StorageESP", description = "Allows you to see chests, dispensers, etc. through walls.", category = ModuleCategory.RENDER)
class StorageESP : Module()
{
	private val modeValue = ListValue("Mode", arrayOf("Box", "OtherBox", "Hydra", "Outline", "ShaderOutline", "ShaderGlow", "2D", "WireFrame"), "Outline")

	private val chestGroup = ValueGroup("Chest")
	private val chestEnabledValue = BoolValue("Enabled", true, "Chest")
	private val chestRainbowValue = BoolValue("Rainbow", false, "Rainbow")
	private val chestColorValue = object : RGBColorValue("Color", 0, 66, 255, Triple("Red", "Green", "Blue"))
	{
		override fun showCondition() = !chestRainbowValue.get()
	}

	private val trappedChestGroup = ValueGroup("TrappedChest")
	private val trappedChestRainbowValue = BoolValue("Rainbow", false, "TrappedChest-Rainbow")
	private val trappedChestColorValue = object : RGBColorValue("Color", 0, 66, 255, Triple("TrappedChest-R", "TrappedChest-G", "TrappedChest-B"))
	{
		override fun showCondition() = !trappedChestRainbowValue.get()
	}

	private val enderChestGroup = ValueGroup("EnderChest")
	private val enderChestEnabledValue = BoolValue("Enabled", true, "EnderChest")
	private val enderChestRainbowValue = BoolValue("Rainbow", false, "EnderChest-Rainbow")
	private val enderChestColorValue = object : RGBColorValue("Color", 255, 0, 255, Triple("EnderChest-R", "EnderChest-G", "EnderChest-B"))
	{
		override fun showCondition() = !enderChestRainbowValue.get()
	}

	private val furnaceGroup = ValueGroup("Furnace")
	private val furnaceEnabledValue = BoolValue("Enabled", true, "Furnace")
	private val furnaceRainbowValue = BoolValue("Rainbow", false, "Furnace-Rainbow")
	private val furnaceColorValue = object : RGBColorValue("Color", 0, 0, 0, Triple("Furnace-R", "Furnace-G", "Furnace-B"))
	{
		override fun showCondition() = !furnaceRainbowValue.get()
	}

	private val dispenserGroup = ValueGroup("Dispenser")
	private val dispenserEnabledValue = BoolValue("Enabled", true, "Dispenser")
	private val dispenserRainbowValue = BoolValue("Rainbow", false, "Dispenser-Rainbow")
	private val dispenserColorValue = object : RGBColorValue("Color", 0, 0, 0, Triple("Dispenser-R", "Dispenser-G", "Dispenser-B"))
	{
		override fun showCondition() = !dispenserRainbowValue.get()
	}

	private val hopperGroup = ValueGroup("Hopper")
	private val hopperEnabledValue = BoolValue("Enabled", true, "Hopper")
	private val hopperRainbowValue = BoolValue("Rainbow", false, "Hopper-Rainbow")
	private val hopperColorValue = object : RGBColorValue("Color", 0, 0, 0, Triple("Hopper-R", "Hopper-G", "Hopper-B"))
	{
		override fun showCondition() = !hopperRainbowValue.get()
	}

	private val shulkerBoxGroup = ValueGroup("ShulkerBox")
	private val shulkerBoxEnabledValue = BoolValue("Enabled", true, "ShulkerBox")
	private val shulkerBoxRainbowValue = BoolValue("Rainbow", false, "ShulkerBox-Rainbow")
	private val shulkerBoxColorValue = object : RGBColorValue("Color", 0, 0, 0, Triple("ShulkerBox-R", "ShulkerBox-G", "ShulkerBox-B"))
	{
		override fun showCondition() = !shulkerBoxRainbowValue.get()
	}

	private val alphaValue = IntegerValue("Alpha", 60, 0, 255)

	private val boxOutlineAlphaValue = object : IntegerValue("Box-Outline-Alpha", 90, 0, 255)
	{
		override fun showCondition(): Boolean = modeValue.get().equals("Box", ignoreCase = true)
	}

	private val outlineWidthValue = object : FloatValue("Outline-Width", 3F, 0.5F, 3F)
	{
		override fun showCondition(): Boolean = modeValue.get().equals("Outline", ignoreCase = true)
	}

	private val wireFrameWidthValue = object : FloatValue("WireFrame-Width", 1.5F, 0.5F, 3F)
	{
		override fun showCondition(): Boolean = modeValue.get().equals("WireFrame", ignoreCase = true)
	}

	private val rainbowGroup = ValueGroup("Rainbow")
	private val rainbowSpeedValue = IntegerValue("Speed", 10, 1, 10, "Rainbow-Speed")
	private val rainbowSaturationValue = FloatValue("Saturation", 1.0f, 0.0f, 1.0f, "HSB-Saturation")
	private val rainbowBrightnessValue = FloatValue("Brightness", 1.0f, 0.0f, 1.0f, "HSB-Brightness")

	init
	{
		trappedChestGroup.addAll(trappedChestColorValue, trappedChestRainbowValue)
		chestGroup.addAll(chestEnabledValue, chestColorValue, chestRainbowValue, trappedChestGroup)

		enderChestGroup.addAll(enderChestEnabledValue, enderChestColorValue, enderChestRainbowValue)
		furnaceGroup.addAll(furnaceEnabledValue, furnaceColorValue, furnaceRainbowValue)
		dispenserGroup.addAll(dispenserEnabledValue, dispenserColorValue, dispenserRainbowValue)
		hopperGroup.addAll(hopperEnabledValue, hopperColorValue, hopperRainbowValue)
		shulkerBoxGroup.addAll(shulkerBoxEnabledValue, shulkerBoxColorValue, shulkerBoxRainbowValue)

		rainbowGroup.addAll(rainbowSpeedValue, rainbowSaturationValue, rainbowBrightnessValue)

		shulkerBoxGroup.isSupported = Backend.REPRESENTED_BACKEND_VERSION != MinecraftVersion.MC_1_8
	}

	@EventTarget
	fun onRender3D(event: Render3DEvent)
	{
		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return
		val gameSettings = mc.gameSettings
		val renderManager = mc.renderManager

		try
		{
			val mode = modeValue.get().toLowerCase()
			val chest = chestEnabledValue.get()
			val enderChest = enderChestEnabledValue.get()
			val furnace = furnaceEnabledValue.get()
			val dispenser = dispenserEnabledValue.get()
			val hopper = hopperEnabledValue.get()
			val shulkerBox = shulkerBoxEnabledValue.get()

			val alpha = alphaValue.get()

			val rainbow = ColorUtils.rainbowRGB(alpha = alpha, speed = rainbowSpeedValue.get(), saturation = rainbowSaturationValue.get(), brightness = rainbowBrightnessValue.get())

			val chestColor = if (chestRainbowValue.get()) rainbow else chestColorValue.get(alpha)
			val trappedChestColor = if (trappedChestRainbowValue.get()) rainbow else trappedChestColorValue.get(alpha)
			val enderChestColor = if (enderChestRainbowValue.get()) rainbow else enderChestColorValue.get(alpha)
			val furnaceColor = if (furnaceRainbowValue.get()) rainbow else furnaceColorValue.get(alpha)
			val dispenserColor = if (dispenserRainbowValue.get()) rainbow else dispenserColorValue.get(alpha)
			val hopperColor = if (hopperRainbowValue.get()) rainbow else hopperColorValue.get(alpha)
			val shulkerBoxColor = if (shulkerBoxRainbowValue.get()) rainbow else shulkerBoxColorValue.get(alpha)

			val outlineAlpha = boxOutlineAlphaValue.get()

			val partialTicks = event.partialTicks

			if (mode == "outline")
			{
				ClientUtils.disableFastRender()
				OutlineUtils.checkSetupFBO()
			}

			val gamma = gameSettings.gammaSetting

			gameSettings.gammaSetting = 100000.0f

			val provider = classProvider

			val outlineWidth = outlineWidthValue.get()
			val wireFrameWidth = wireFrameWidthValue.get()

			val hydraESP = mode == "hydra"

			theWorld.loadedTileEntityList.mapNotNull {
				val type = when
				{
					chest && provider.isTileEntityChest(it) && !clickedBlocks.contains(it.pos) -> if (it.asTileEntityChest().chestType == 1) 2 else 1
					enderChest && provider.isTileEntityEnderChest(it) && !clickedBlocks.contains(it.pos) -> 3
					furnace && provider.isTileEntityFurnace(it) -> 4
					dispenser && provider.isTileEntityDispenser(it) -> 5
					hopper && provider.isTileEntityHopper(it) -> 6
					shulkerBox && provider.isTileEntityShulkerBox(it) -> 7
					else -> null
				} ?: return@mapNotNull null

				Triple(it, type, when (type)
				{
					1 -> chestColor
					2 -> trappedChestColor
					3 -> enderChestColor
					4 -> furnaceColor
					5 -> dispenserColor
					6 -> hopperColor
					7 -> shulkerBoxColor
					else -> null
				} ?: return@mapNotNull null)
			}.forEach { (tileEntity, type, color) ->
				val outlineColor = ColorUtils.applyAlphaChannel(color, outlineAlpha)

				if (type > 3) // Not chest or enderchest
				{
					RenderUtils.drawBlockBox(theWorld, thePlayer, tileEntity.pos, color, outlineColor, hydraESP)
					return@forEach
				}

				val func = functions

				when (mode)
				{
					"otherbox", "box", "hydra" -> RenderUtils.drawBlockBox(theWorld, thePlayer, tileEntity.pos, color, outlineColor, hydraESP)

					"2d" -> RenderUtils.draw2D(tileEntity.pos, color, -16777216)

					"outline" ->
					{
						RenderUtils.glColor(color)
						OutlineUtils.renderOne(outlineWidth)
						func.renderTileEntity(tileEntity, partialTicks, -1)
						OutlineUtils.renderTwo()
						func.renderTileEntity(tileEntity, partialTicks, -1)
						OutlineUtils.renderThree()
						func.renderTileEntity(tileEntity, partialTicks, -1)
						OutlineUtils.renderFour(color)
						func.renderTileEntity(tileEntity, partialTicks, -1)
						OutlineUtils.renderFive()
						RenderUtils.glColor(Color.WHITE)
					}

					"wireframe" ->
					{
						glPushMatrix()
						glPushAttrib(GL_ALL_ATTRIB_BITS)

						glPolygonMode(GL_FRONT_AND_BACK, GL_LINE)
						glDisable(GL_TEXTURE_2D)
						glDisable(GL_LIGHTING)
						glDisable(GL_DEPTH_TEST)
						glEnable(GL_LINE_SMOOTH)
						glEnable(GL_BLEND)
						glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

						glLineWidth(0.5F)

						func.renderTileEntity(tileEntity, partialTicks, -1)

						RenderUtils.glColor(color)
						glLineWidth(wireFrameWidth)

						func.renderTileEntity(tileEntity, partialTicks, -1)

						glPopAttrib()
						glPopMatrix()
					}
				}
			}

			theWorld.loadedEntityList.mapNotNull {
				it to (when
				{
					chest && provider.isEntityMinecartChest(it) -> chestColor
					furnace && provider.isEntityMinecartFurnace(it) -> furnaceColor
					hopper && provider.isEntityMinecartHopper(it) -> hopperColor
					else -> null
				} ?: return@mapNotNull null)
			}.forEach { (entity, color) ->
				when (mode)
				{
					"otherbox", "box", "hydra" -> RenderUtils.drawEntityBox(entity, color, ColorUtils.applyAlphaChannel(color, outlineAlpha), hydraESP)

					"2d" -> RenderUtils.draw2D(entity.position, color, -16777216)

					"outline" ->
					{
						val entityShadow: Boolean = gameSettings.entityShadows
						gameSettings.entityShadows = false

						RenderUtils.glColor(color)
						OutlineUtils.renderOne(outlineWidth)
						renderManager.renderEntityStatic(entity, partialTicks, true)
						OutlineUtils.renderTwo()
						renderManager.renderEntityStatic(entity, partialTicks, true)
						OutlineUtils.renderThree()
						renderManager.renderEntityStatic(entity, partialTicks, true)
						OutlineUtils.renderFour(color)
						renderManager.renderEntityStatic(entity, partialTicks, true)
						OutlineUtils.renderFive()
						RenderUtils.glColor(Color.WHITE)

						gameSettings.entityShadows = entityShadow
					}

					"wireframe" ->
					{
						val entityShadow: Boolean = gameSettings.entityShadows
						gameSettings.entityShadows = false

						glPushMatrix()
						glPushAttrib(GL_ALL_ATTRIB_BITS)
						glPolygonMode(GL_FRONT_AND_BACK, GL_LINE)
						glDisable(GL_TEXTURE_2D)
						glDisable(GL_LIGHTING)
						glDisable(GL_DEPTH_TEST)
						glEnable(GL_LINE_SMOOTH)
						glEnable(GL_BLEND)
						glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

						RenderUtils.glColor(color)

						renderManager.renderEntityStatic(entity, partialTicks, true)

						RenderUtils.glColor(color)

						glLineWidth(wireFrameWidth)

						renderManager.renderEntityStatic(entity, partialTicks, true)

						glPopAttrib()
						glPopMatrix()

						gameSettings.entityShadows = entityShadow
					}
				}
			}

			RenderUtils.glColor(Color(255, 255, 255, 255))

			gameSettings.gammaSetting = gamma
		}
		catch (ignored: Exception)
		{
		}
	}

	@EventTarget
	fun onRender2D(event: Render2DEvent)
	{
		val theWorld = mc.theWorld ?: return
		val renderManager = mc.renderManager
		val renderPosX = renderManager.renderPosX
		val renderPosY = renderManager.renderPosY
		val renderPosZ = renderManager.renderPosZ

		val mode = modeValue.get()

		val chest = chestEnabledValue.get()
		val enderChest = enderChestEnabledValue.get()
		val furnace = furnaceEnabledValue.get()
		val dispenser = dispenserEnabledValue.get()
		val hopper = hopperEnabledValue.get()
		val shulkerBox = shulkerBoxEnabledValue.get()

		val rainbow = ColorUtils.rainbowRGB(speed = rainbowSpeedValue.get(), saturation = rainbowSaturationValue.get(), brightness = rainbowBrightnessValue.get())
		val chestColor = if (chestRainbowValue.get()) rainbow else chestColorValue.get()
		val trappedChestColor = if (trappedChestRainbowValue.get()) rainbow else trappedChestColorValue.get()
		val enderChestColor = if (enderChestRainbowValue.get()) rainbow else enderChestColorValue.get()
		val furnaceColor = if (furnaceRainbowValue.get()) rainbow else furnaceColorValue.get()
		val dispenserColor = if (dispenserRainbowValue.get()) rainbow else dispenserColorValue.get()
		val hopperColor = if (hopperRainbowValue.get()) rainbow else hopperColorValue.get()
		val shulkerBoxColor = if (shulkerBoxRainbowValue.get()) rainbow else shulkerBoxColorValue.get()

		val shader = (if (mode.equals("shaderoutline", ignoreCase = true)) OutlineShader.INSTANCE else if (mode.equals("shaderglow", ignoreCase = true)) GlowShader.INSTANCE else null) ?: return
		val radius = if (mode.equals("shaderglow", ignoreCase = true)) 2.5f else 1.5f

		val partialTicks = event.partialTicks

		try
		{
			val startDraw = { shader.startDraw(partialTicks) }
			val stopDraw = { color: Int -> shader.stopDraw(color, radius, 1f) }

			val provider = classProvider

			val tileEntityGroup = theWorld.loadedTileEntityList.groupBy {
				when
				{
					provider.isTileEntityChest(it) -> if (it.asTileEntityChest().chestType == 1) 2 else 1 // Chest or Trapped Chest
					provider.isTileEntityEnderChest(it) -> 3 // Ender Chest
					provider.isTileEntityFurnace(it) -> 4 // Furnace
					provider.isTileEntityDispenser(it) -> 5 // Dispenser (and Dropper)
					provider.isTileEntityHopper(it) -> 6 // Hopper
					provider.isTileEntityShulkerBox(it) -> 7 // Shulker box
					else -> 0
				}
			}.filterNot { it.key == 0 }

			val entityGroup = theWorld.loadedEntityList.groupBy {
				when
				{
					provider.isEntityMinecartChest(it) -> 1 // Minecart Chest
					provider.isEntityMinecartFurnace(it) -> 2 // Minecart Furnace
					provider.isEntityMinecartHopper(it) -> 3 // Minecart Hopper
					else -> 0
				}
			}.filterNot { it.key == 0 }

			val renderTileEntity = { type: Int ->
				tileEntityGroup[type]?.forEach {
					renderManager.renderEntityAt(it, it.pos.x - renderPosX, it.pos.y - renderPosY, it.pos.z - renderPosZ, partialTicks)
				}
			}

			val renderMinecart = { type: Int ->
				entityGroup[type]?.forEach {
					renderManager.renderEntityStatic(it, partialTicks, true)
				}
			}

			val renderTileEntityOnly = { type: Int, color: Int ->
				startDraw()
				renderTileEntity(type)
				stopDraw(color)
			}

			// Draw Chest and MinecartChest and Trapped Chest
			if (chest)
			{
				startDraw()
				renderTileEntity(1) // TileEntityChest (Regular)
				renderMinecart(1) // EntityMinecartChest
				stopDraw(chestColor)

				renderTileEntityOnly(2, trappedChestColor) // TileEntityChest (Trapped)
			}

			// Draw EnderChest
			if (enderChest) renderTileEntityOnly(3, enderChestColor) // TileEntityEnderChest

			// Draw Furnace and MinecartFurnace
			if (furnace)
			{
				startDraw()
				renderTileEntity(4) // TileEntityFurnace
				renderMinecart(2) // EntityMinecartFurnace
				stopDraw(furnaceColor)
			}

			// Draw Dispenser
			if (dispenser) renderTileEntityOnly(5, dispenserColor) // TileEntityDispenser

			// Draw Hopper and MinecartHopper
			if (hopper)
			{
				startDraw()
				renderTileEntity(6) // TileEntityHopper
				renderMinecart(3) // EntityMinecartHopper
				stopDraw(hopperColor)
			}

			// Draw Shulker box
			if (shulkerBox) renderTileEntityOnly(7, shulkerBoxColor) // TileEntityShulkerBox
		}
		catch (ex: Exception)
		{
			ClientUtils.logger.error("An error occurred while rendering all storages for shader esp", ex)
		}
	}

	override val tag: String
		get() = modeValue.get()
}
