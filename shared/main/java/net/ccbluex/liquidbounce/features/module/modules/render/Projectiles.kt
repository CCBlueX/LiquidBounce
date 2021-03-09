/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.enums.MaterialType
import net.ccbluex.liquidbounce.api.enums.WDefaultVertexFormats
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.util.*
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.combat.FastBow
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import org.lwjgl.opengl.GL11
import org.lwjgl.util.glu.Cylinder
import org.lwjgl.util.glu.GLU
import java.awt.Color
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.sqrt

@ModuleInfo(name = "Projectiles", description = "Allows you to see where arrows will land.", category = ModuleCategory.RENDER)
class Projectiles : Module()
{
	private val colorMode = ListValue("Color", arrayOf("Custom", "BowPower", "Rainbow"), "Custom")

	private val colorRedValue = IntegerValue("R", 0, 0, 255)
	private val colorGreenValue = IntegerValue("G", 160, 0, 255)
	private val colorBlueValue = IntegerValue("B", 255, 0, 255)

	private val lineWidthValue = FloatValue("LineWidth", 2.0f, 1.0f, 3.0f)

	private val saturationValue = FloatValue("HSB-Saturation", 1.0f, 0.0f, 1.0f)
	private val brightnessValue = FloatValue("HSB-Brightness", 1.0f, 0.0f, 1.0f)

	private var lastBowChargeDuration: Int = 0

	@EventTarget
	fun onRender3D(event: Render3DEvent)
	{
		val thePlayer = mc.thePlayer ?: return
		val theWorld = mc.theWorld ?: return

		val heldItem = thePlayer.heldItem ?: return

		val item = heldItem.item
		val renderManager = mc.renderManager
		val renderPosX = renderManager.renderPosX
		val renderPosY = renderManager.renderPosY
		val renderPosZ = renderManager.renderPosZ

		var isSplash = false

		var motionMultiplier = 0.4
		var motionFactor = 1.5F
		var motionSlowdown = 0.99F

		val gravity: Float
		val size: Float

		val partialTicks = event.partialTicks

		val provider = classProvider

		// Check items
		when
		{
			provider.isItemBow(item) -> // Bow
			{
				val fastBow = LiquidBounce.moduleManager[FastBow::class.java] as FastBow
				val fastBowEnabled = fastBow.state

				if (!fastBowEnabled && !thePlayer.isUsingItem)
				{
					lastBowChargeDuration = 0 // Reset bow charge duration
					return
				}

				motionMultiplier = 1.0
				gravity = 0.05F
				size = 0.3F

				// Interpolate and calculate power of bow
				val bowChargeDuration = if (fastBowEnabled) fastBow.packetsValue.get() else thePlayer.itemInUseDuration
				var power = (lastBowChargeDuration + (bowChargeDuration - lastBowChargeDuration) * partialTicks) * 0.05f
				lastBowChargeDuration = bowChargeDuration

				power = (power * power + power * 2F) / 3F

				if (power < 0.1F) return

				if (power > 1F) power = 1F

				motionFactor = power * 3F
			}

			provider.isItemFishingRod(item) -> // Fishing Rod
			{
				gravity = 0.04F
				size = 0.25F
				motionSlowdown = 0.92F
			}

			provider.isItemPotion(item) && heldItem.isSplash() -> // Splash potion
			{
				isSplash = true
				gravity = 0.05F
				size = 0.25F
				motionFactor = 0.5F
			}

			else -> // Snowball, Ender Pearl, Egg
			{
				if (!provider.isItemSnowball(item) && !provider.isItemEnderPearl(item) && !provider.isItemEgg(item)) return

				gravity = 0.03F
				size = 0.25F
			}
		}

		// Interpolated yaw and pitch of player
		val (serverYaw, serverPitch) = RotationUtils.serverRotation
		val (lastServerYaw, lastServerPitch) = RotationUtils.lastServerRotation

		val yaw = lastServerYaw + (serverYaw - lastServerYaw) * partialTicks
		val pitch = lastServerPitch + (serverPitch - lastServerPitch) * partialTicks

		val yawRadians = WMathHelper.toRadians(yaw)
		val pitchRadians = WMathHelper.toRadians(pitch)

		val func = functions

		val yawSin = func.sin(yawRadians)
		val yawCos = func.cos(yawRadians)
		val pitchCos = func.cos(pitchRadians)

		// Positions
		var posX = renderPosX - yawCos * 0.16F
		var posY = renderPosY + thePlayer.eyeHeight - 0.10000000149011612
		var posZ = renderPosZ - yawSin * 0.16F

		// Motions
		var motionX = -yawSin * pitchCos * motionMultiplier
		var motionY = -func.sin(WMathHelper.toRadians(pitch + if (isSplash) -20 else 0)) * motionMultiplier
		var motionZ = yawCos * pitchCos * motionMultiplier

		val distance = sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ)

		motionX /= distance
		motionY /= distance
		motionZ /= distance

		motionX *= motionFactor
		motionY *= motionFactor
		motionZ *= motionFactor

		// Landing
		var landingPosition: IMovingObjectPosition? = null
		var hasLanded = false
		var hitEntity = false

		val tessellator = provider.tessellatorInstance
		val worldRenderer = tessellator.worldRenderer

		// Start drawing of path
		GL11.glDepthMask(false)
		RenderUtils.enableGlCap(GL11.GL_BLEND, GL11.GL_LINE_SMOOTH)
		RenderUtils.disableGlCap(GL11.GL_DEPTH_TEST, GL11.GL_ALPHA_TEST, GL11.GL_TEXTURE_2D)
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
		GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST)

		when (colorMode.get().toLowerCase())
		{
			"custom" -> RenderUtils.glColor(Color(colorRedValue.get(), colorGreenValue.get(), colorBlueValue.get(), 255))
			"bowpower" -> RenderUtils.glColor(ColorUtils.blendColors(floatArrayOf(0f, 0.5f, 1f), arrayOf(Color.RED, Color.YELLOW, Color.GREEN), (motionFactor / 30) * 10))
			"rainbow" -> RenderUtils.glColor(ColorUtils.rainbow(saturation = saturationValue.get(), brightness = brightnessValue.get()))
		}

		GL11.glLineWidth(lineWidthValue.get())

		worldRenderer.begin(GL11.GL_LINE_STRIP, provider.getVertexFormatEnum(WDefaultVertexFormats.POSITION))

		while (!hasLanded && posY > 0.0)
		{
			// Set pos before and after
			var posBefore = WVec3(posX, posY, posZ)
			var posAfter = WVec3(posX + motionX, posY + motionY, posZ + motionZ)

			// Get landing position
			landingPosition = theWorld.rayTraceBlocks(posBefore, posAfter, stopOnLiquid = false, ignoreBlockWithoutBoundingBox = true, returnLastUncollidableBlock = false)

			// Set pos before and after
			posBefore = WVec3(posX, posY, posZ)
			posAfter = WVec3(posX + motionX, posY + motionY, posZ + motionZ)

			// Check if arrow is landing
			if (landingPosition != null)
			{
				hasLanded = true
				posAfter = WVec3(landingPosition.hitVec.xCoord, landingPosition.hitVec.yCoord, landingPosition.hitVec.zCoord)
			}

			// Set arrow box
			val arrowBox = provider.createAxisAlignedBB(posX - size, posY - size, posZ - size, posX + size, posY + size, posZ + size).addCoord(motionX, motionY, motionZ).expand(1.0, 1.0, 1.0)

			val chunkMinX = floor((arrowBox.minX - 2) * 0.0625).toInt()
			val chunkMaxX = ceil((arrowBox.maxX + 2) * 0.0625).toInt()

			val chunkMinZ = floor((arrowBox.minZ - 2) * 0.0625).toInt()
			val chunkMaxZ = ceil((arrowBox.maxZ + 2) * 0.0625).toInt()

			// Check which entities colliding with the arrow
			val collidedEntities = mutableListOf<IEntity>()

			for (x in chunkMinX..chunkMaxX) for (z in chunkMinZ..chunkMaxZ) theWorld.getChunkFromChunkCoords(x, z).getEntitiesWithinAABBForEntity(thePlayer, arrowBox, collidedEntities, null)

			val sizeDouble = size.toDouble()

			// Check all possible entities
			collidedEntities.filter(IEntity::canBeCollidedWith).filter { it != thePlayer }.forEach { possibleEntity ->
				val possibleEntityBoundingBox = possibleEntity.entityBoundingBox.expand(sizeDouble, sizeDouble, sizeDouble)

				val possibleEntityLanding = possibleEntityBoundingBox.calculateIntercept(posBefore, posAfter) ?: return@forEach

				hitEntity = true
				hasLanded = true
				landingPosition = possibleEntityLanding
			}

			// Affect motions of arrow
			posX += motionX
			posY += motionY
			posZ += motionZ

			val blockState = theWorld.getBlockState(WBlockPos(posX, posY, posZ))

			// Check is next position water
			if (blockState.block.getMaterial(blockState) == provider.getMaterialEnum(MaterialType.WATER))
			{
				// Update motion in water
				motionX *= 0.6
				motionY *= 0.6
				motionZ *= 0.6
			}
			else
			{
				// Update motion
				val motionSlowdownDouble = motionSlowdown.toDouble()

				motionX *= motionSlowdownDouble
				motionY *= motionSlowdownDouble
				motionZ *= motionSlowdownDouble
			}

			motionY -= gravity.toDouble()

			// Draw path
			worldRenderer.pos(posX - renderPosX, posY - renderPosY, posZ - renderPosZ).endVertex()
		}

		// End the rendering of the path
		tessellator.draw()
		GL11.glPushMatrix()
		GL11.glTranslated(posX - renderPosX, posY - renderPosY, posZ - renderPosZ)

		if (landingPosition != null)
		{
			// Switch rotation of hit cylinder of the hit axis
			when (landingPosition?.sideHit?.axisOrdinal ?: -1)
			{
				0 -> GL11.glRotatef(90F, 0F, 0F, 1F)
				2 -> GL11.glRotatef(90F, 1F, 0F, 0F)
			}

			// Check if hitting a entity
			if (hitEntity) RenderUtils.glColor(Color(255, 0, 0, 150))
		}

		// Rendering hit cylinder
		GL11.glRotatef(-90F, 1F, 0F, 0F)

		val cylinder = object : Cylinder()
		{
			override fun sin(r: Float): Float = func.sin(r)
			override fun cos(r: Float): Float = func.cos(r)
		}
		cylinder.drawStyle = GLU.GLU_LINE
		cylinder.draw(0.2F, 0F, 0F, 60, 1)

		GL11.glPopMatrix()
		GL11.glDepthMask(true)
		RenderUtils.resetCaps()
		RenderUtils.resetColor()
	}
}
