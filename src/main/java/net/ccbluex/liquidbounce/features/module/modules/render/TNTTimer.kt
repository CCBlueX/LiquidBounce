/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.EntityUtils.isLookingOnEntities
import net.ccbluex.liquidbounce.utils.render.RenderUtils.disableGlCap
import net.ccbluex.liquidbounce.utils.render.RenderUtils.enableGlCap
import net.ccbluex.liquidbounce.utils.render.RenderUtils.resetCaps
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.FontValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.entity.item.EntityTNTPrimed
import org.lwjgl.opengl.GL11.*
import kotlin.math.pow

object TNTTimer : Module("TNTTimer", Category.RENDER, spacedName = "TNT Timer", hideModule = false) {

    private val scale by FloatValue("Scale", 3F, 1F..4F)
    private val font by FontValue("Font", Fonts.font40)
    private val fontShadow by BoolValue("Shadow", true)

    private val colorRed by IntegerValue("R", 255, 0..255)
    private val colorGreen by IntegerValue("G", 255, 0..255)
    private val colorBlue by IntegerValue("B", 255, 0..255)

    private val maxRenderDistance by object : IntegerValue("MaxRenderDistance", 100, 1..200) {
        override fun onInit(value: Int) {
            maxRenderDistanceSq = value.toDouble().pow(2.0)
        }
        override fun onUpdate(value: Int) {
            maxRenderDistanceSq = value.toDouble().pow(2.0)
        }
    }

    private val onLook by BoolValue("OnLook", false)
    private val maxAngleDifference by FloatValue("MaxAngleDifference", 5.0f, 5.0f..90f) { onLook }

    private var maxRenderDistanceSq = 0.0

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        val player = mc.thePlayer ?: return
        val world = mc.theWorld ?: return

        for (entity in world.loadedEntityList.filterNotNull()) {
            if (entity is EntityTNTPrimed && player.getDistanceSqToEntity(entity) <= maxRenderDistanceSq) {
                val explosionTime = entity.fuse / 5

                if (explosionTime > 0 && (isLookingOnEntities(entity, maxAngleDifference.toDouble()) || !onLook)) {
                    renderTNTTimer(entity, explosionTime)
                }
            }
        }
    }

    private fun renderTNTTimer(tnt: EntityTNTPrimed, timeRemaining: Int) {
        val player = mc.thePlayer ?: return
        val renderManager = mc.renderManager

        glPushAttrib(GL_ENABLE_BIT)
        glPushMatrix()

        // Translate to TNT position
        glTranslated(
            tnt.lastTickPosX + (tnt.posX - tnt.lastTickPosX) - renderManager.renderPosX,
            tnt.lastTickPosY + (tnt.posY - tnt.lastTickPosY) - renderManager.renderPosY + 1.5,
            tnt.lastTickPosZ + (tnt.posZ - tnt.lastTickPosZ) - renderManager.renderPosZ
        )

        glRotatef(-renderManager.playerViewY, 0F, 1F, 0F)
        glRotatef(renderManager.playerViewX, 1F, 0F, 0F)

        disableGlCap(GL_LIGHTING, GL_DEPTH_TEST)

        enableGlCap(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        val color = ((colorRed and 0xFF) shl 16) or ((colorGreen and 0xFF) shl 8) or (colorBlue and 0xFF)

        val text = "TNT Explodes in: $timeRemaining"

        val fontRenderer = font

        // Scale
        val scale = (player.getDistanceToEntity(tnt) / 4F).coerceAtLeast(1F) / 150F * scale
        glScalef(-scale, -scale, scale)

        // Draw text
        val width = fontRenderer.getStringWidth(text) * 0.5f
        fontRenderer.drawString(
            text, 1F + -width, if (fontRenderer == Fonts.minecraftFont) 1F else 1.5F, color, fontShadow
        )

        resetCaps()
        glPopMatrix()
        glPopAttrib()
    }

}