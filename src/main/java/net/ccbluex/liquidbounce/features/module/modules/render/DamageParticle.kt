package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.WorldEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.extensions.drawStringWithShadow
import net.ccbluex.liquidbounce.utils.extensions.isSelected
import net.ccbluex.liquidbounce.utils.misc.StringUtils
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.FontValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.RGBColorValue
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.EntityLivingBase
import org.lwjgl.opengl.GL11
import kotlin.random.Random.Default.nextDouble

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

    private val damageAnimationColorValue = RGBColorValue("DamageAnimationColor", 252, 185, 65, Triple("DamageAnimationColorRed", "DamageAnimationColorGreen", "DamageAnimationColorBlue"))
    private val healAnimationColorValue = RGBColorValue("DamageAnimationColor", 44, 201, 144, Triple("HealAnimationColorRed", "HealAnimationColorGreen", "HealAnimationColorBlue"))

    private val fontValue = FontValue("Font", Fonts.minecraftFont)

    @EventTarget
    fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent)
    {
        val theWorld = mc.theWorld ?: return
        synchronized(particles) {
            theWorld.loadedEntityList.filterIsInstance<EntityLivingBase>().filter { it.isSelected(true) }.mapNotNull { entity ->
                val lastHealth = healthData[entity.entityId] ?: entity.maxHealth
                healthData[entity.entityId] = entity.health

                val delta = entity.health - lastHealth
                if (delta == 0.0F) null else entity to delta
            }.forEach { (entity, delta) ->
                val width = entity.width * 0.5
                val height = entity.height * 0.5
                val pos = RotationUtils.getCenter(entity.entityBoundingBox)
                particles.add(SingleParticle(StringUtils.DECIMALFORMAT_1.format(delta), pos.xCoord + nextDouble(-width, width), pos.yCoord + nextDouble(-height, height), pos.zCoord + nextDouble(-width, width), if (delta > 0) healAnimationColorValue.get() else damageAnimationColorValue.get()))
            }

            val itr = particles.iterator()
            while (itr.hasNext()) if (itr.next().ticks++ > aliveTicks.get()) itr.remove()
        }
    }

    @EventTarget
    fun onRender3D(@Suppress("UNUSED_PARAMETER") event: Render3DEvent)
    {
        synchronized(particles) {            val xRotate = if (mc.gameSettings.thirdPersonView == 2) -1.0f else 1.0f

            val renderManager = mc.renderManager
            val renderPosX = renderManager.renderPosX
            val renderPosY = renderManager.renderPosY
            val renderPosZ = renderManager.renderPosZ
            val playerViewX = renderManager.playerViewX
            val playerViewY = renderManager.playerViewY

            val font = fontValue.get()
            val particleAnimationSpeed = particleAnimationSpeedValue.get()
            val size = sizeValue.get() * 0.01F

            for (particle in particles)
            {
                GlStateManager.pushMatrix()
                GlStateManager.enablePolygonOffset()
                GlStateManager.doPolygonOffset(1.0f, -1500000.0f)
                GL11.glTranslated(particle.posX - renderPosX, (particle.posY + particle.ticks * particleAnimationSpeed) - renderPosY, particle.posZ - renderPosZ)
                GL11.glRotatef(-playerViewY, 0.0f, 1.0f, 0.0f)
                GL11.glRotatef(playerViewX, xRotate, 0.0f, 0.0f)
                GL11.glScalef(-size, -size, size)
                GL11.glDepthMask(false)

                font.drawStringWithShadow(particle.str, -(font.getStringWidth(particle.str) / 2), -(font.FONT_HEIGHT - 1), particle.color)

                GL11.glColor4f(1f, 1f, 1f, 1.0f)
                GL11.glDepthMask(true)
                GlStateManager.doPolygonOffset(1.0f, 1500000.0f)
                GlStateManager.disablePolygonOffset()
                GlStateManager.popMatrix()
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
