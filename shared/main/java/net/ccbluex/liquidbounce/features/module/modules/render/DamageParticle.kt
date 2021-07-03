package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.WorldEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.misc.StringUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import org.lwjgl.opengl.GL11
import java.util.*

// Ported from FDPClient (https://github.com/Project-EZ4H/FDPClient)
// Original code is available in https://github.com/Project-EZ4H/FDPClient/blob/master/src/main/java/net/ccbluex/liquidbounce/features/module/modules/render/DamageParticle.kt
@ModuleInfo(name = "DamageParticle", description = "Allows you to see targets damage.", category = ModuleCategory.RENDER)
class DamageParticle : Module()
{
	private val healthData = HashMap<Int, Float>()
	private val particles = ArrayList<SingleParticle>()

	private val aliveTicks = IntegerValue("AliveTicks", 20, 10, 50)
	private val sizeValue = IntegerValue("Size", 3, 1, 7)
	private val particleAnimationSpeedValue = FloatValue("AnimationSpeed", 0.1F, -0.5F, 0.5F)

	private val damageAnimationColorRedValue = IntegerValue("DamageAnimationColorRed", 252, 0, 255)
	private val damageAnimationColorGreenValue = IntegerValue("DamageAnimationColorGreen", 185, 0, 255)
	private val damageAnimationColorBlueValue = IntegerValue("DamageAnimationColorBlue", 65, 0, 255)

	private val healAnimationColorRedValue = IntegerValue("HealAnimationColorRed", 44, 0, 255)
	private val healAnimationColorGreenValue = IntegerValue("HealAnimationColorGreen", 201, 0, 255)
	private val healAnimationColorBlueValue = IntegerValue("HealAnimationColorBlue", 144, 0, 255)

	@EventTarget
	fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent)
	{
		val theWorld = mc.theWorld ?: return

		val provider = classProvider
		synchronized(particles) {
			theWorld.loadedEntityList.filter(provider::isEntityLivingBase).map(IEntity::asEntityLivingBase).filter { EntityUtils.isSelected(it, true) }.mapNotNull { entity ->
				val lastHealth = healthData[entity.entityId] ?: entity.maxHealth
				healthData[entity.entityId] = entity.health

				val delta = lastHealth - entity.health
				if (delta == 0.0F) null else entity to delta
			}.forEach { (entity, delta) -> particles.add(SingleParticle(StringUtils.DECIMALFORMAT_1.format(delta), entity.posX - 0.5 + Random(System.currentTimeMillis()).nextInt(5).toDouble() * 0.1, entity.entityBoundingBox.minY + (entity.entityBoundingBox.maxY - entity.entityBoundingBox.minY) / 2.0, entity.posZ - 0.5 + Random(System.currentTimeMillis() + 1L).nextInt(5).toDouble() * 0.1, if (delta > 0) ColorUtils.createRGB(damageAnimationColorRedValue.get(), damageAnimationColorGreenValue.get(), damageAnimationColorBlueValue.get()) else ColorUtils.createRGB(healAnimationColorRedValue.get(), healAnimationColorGreenValue.get(), healAnimationColorBlueValue.get()))) }

			val itr = particles.iterator()
			while (itr.hasNext()) if (itr.next().ticks++ > aliveTicks.get()) itr.remove()
		}
	}

	@EventTarget
	fun onRender3d(@Suppress("UNUSED_PARAMETER") event: Render3DEvent)
	{
		synchronized(particles) {
			val renderManager = mc.renderManager
			val glStateManager = classProvider.glStateManager
			val font = mc.fontRendererObj
			val xRotate = if (mc.gameSettings.thirdPersonView == 2) -1.0f else 1.0f

			val size = sizeValue.get() * 0.01F
			val particleAnimationSpeed = particleAnimationSpeedValue.get()

			for (particle in particles)
			{
				glStateManager.pushMatrix()
				glStateManager.enablePolygonOffset()
				glStateManager.doPolygonOffset(1.0f, -1500000.0f)
				GL11.glTranslated(particle.posX - renderManager.renderPosX, (particle.posY + particle.ticks * particleAnimationSpeed) - renderManager.renderPosY, particle.posZ - renderManager.renderPosZ)
				GL11.glRotatef(-renderManager.playerViewY, 0.0f, 1.0f, 0.0f)
				GL11.glRotatef(renderManager.playerViewX, xRotate, 0.0f, 0.0f)
				GL11.glScalef(-size, -size, size)
				GL11.glDepthMask(false)
				font.drawStringWithShadow(particle.str, -(font.getStringWidth(particle.str) / 2), -(font.fontHeight - 1), particle.color)
				GL11.glColor4f(187.0f, 255.0f, 255.0f, 1.0f)
				GL11.glDepthMask(true)
				glStateManager.doPolygonOffset(1.0f, 1500000.0f)
				glStateManager.disablePolygonOffset()
				glStateManager.popMatrix()
			}
		}
	}

	@EventTarget
	fun onWorld(@Suppress("UNUSED_PARAMETER") event: WorldEvent)
	{
		particles.clear()
		healthData.clear()
	}
}

class SingleParticle(val str: String, val posX: Double, val posY: Double, val posZ: Double, val color: Int)
{
	var ticks = 0
}
