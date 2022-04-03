/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.ui.font.AWTFontRenderer
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.extensions.getPing
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.quickDrawBorderedRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.quickDrawRect
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.FontValue
import net.minecraft.client.renderer.GlStateManager.*
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import kotlin.math.roundToInt

@ModuleInfo(name = "NameTags", description = "Changes the scale of the nametags so you can always read them.", category = ModuleCategory.RENDER)
class NameTags : Module() {
    private val healthValue = BoolValue("Health", true)
    private val pingValue = BoolValue("Ping", true)
    private val distanceValue = BoolValue("Distance", false)
    private val armorValue = BoolValue("Armor", true)
    private val clearNamesValue = BoolValue("ClearNames", false)
    private val fontValue = FontValue("Font", Fonts.font40)
    private val borderValue = BoolValue("Border", true)
    private val scaleValue = FloatValue("Scale", 1F, 1F, 4F)
    private val botValue = BoolValue("Bots", true)

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        glPushAttrib(GL_ENABLE_BIT)
        glPushMatrix()

        // Disable lightning and depth test
        glDisable(GL_LIGHTING)
        glDisable(GL_DEPTH_TEST)

        glEnable(GL_LINE_SMOOTH)

        // Enable blend
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        for (entity in mc.theWorld!!.loadedEntityList) {
            if (entity !is EntityLivingBase) continue
            if (!EntityUtils.isSelected(entity, false)) continue
            if (AntiBot.isBot(entity) && !botValue.get()) continue

            renderNameTag(entity,
                    if (clearNamesValue.get())
                        ColorUtils.stripColor(entity.displayName?.unformattedText) ?: continue
                    else
                        (entity.displayName ?: continue).unformattedText
            )
        }

        glPopMatrix()
        glPopAttrib()

        // Reset color
        glColor4f(1F, 1F, 1F, 1F)
    }

    private fun renderNameTag(entity: EntityLivingBase, tag: String) {
        val thePlayer = mc.thePlayer ?: return

        val fontRenderer = fontValue.get()

        // Modify tag
        val bot = AntiBot.isBot(entity)
        val nameColor = if (bot) "§3" else if (entity.isInvisible) "§6" else if (entity.isSneaking) "§4" else "§7"
        val ping = if (entity is EntityPlayer) entity.getPing() else 0

        val distanceText = if (distanceValue.get()) "§7${thePlayer.getDistanceToEntity(entity).roundToInt()}m " else ""
        val pingText = if (pingValue.get() && entity is EntityPlayer) (if (ping > 200) "§c" else if (ping > 100) "§e" else "§a") + ping + "ms §7" else ""
        val healthText = if (healthValue.get()) "§7§c " + entity.health.toInt() + " HP" else ""
        val botText = if (bot) " §c§lBot" else ""

        val text = "$distanceText$pingText$nameColor$tag$healthText$botText"

        // Push
        glPushMatrix()

        // Translate to player position
        val timer = mc.timer
        val renderManager = mc.renderManager


        glTranslated( // Translate to player position with render pos and interpolate it
                entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * timer.renderPartialTicks - renderManager.renderPosX,
                entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * timer.renderPartialTicks - renderManager.renderPosY + entity.eyeHeight.toDouble() + 0.55,
                entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * timer.renderPartialTicks - renderManager.renderPosZ
        )

        glRotatef(-mc.renderManager.playerViewY, 0F, 1F, 0F)
        glRotatef(mc.renderManager.playerViewX, 1F, 0F, 0F)


        // Scale
        var distance = thePlayer.getDistanceToEntity(entity) * 0.25f

        if (distance < 1F)
            distance = 1F

        val scale = distance / 100f * scaleValue.get()

        glScalef(-scale, -scale, scale)

        AWTFontRenderer.assumeNonVolatile = true

        // Draw NameTag
        val width = fontRenderer.getStringWidth(text) * 0.5f

        glDisable(GL_TEXTURE_2D)
        glEnable(GL_BLEND)

        if (borderValue.get())
            quickDrawBorderedRect(-width - 2F, -2F, width + 4F, fontRenderer.FONT_HEIGHT + 2F, 2F, Color(255, 255, 255, 90).rgb, Integer.MIN_VALUE)
        else
            quickDrawRect(-width - 2F, -2F, width + 4F, fontRenderer.FONT_HEIGHT + 2F, Integer.MIN_VALUE)

        glEnable(GL_TEXTURE_2D)

        fontRenderer.drawString(text, 1F + -width, if (fontRenderer == Fonts.minecraftFont) 1F else 1.5F,
                0xFFFFFF, true)

        AWTFontRenderer.assumeNonVolatile = false

        if (armorValue.get() && entity is EntityPlayer) {
            mc.renderItem.zLevel = -147F

            val indices = (0..4).toList().toIntArray()

            for (index in indices) {
                val equipmentInSlot = entity.getEquipmentInSlot(index) ?: continue

                mc.renderItem.renderItemAndEffectIntoGUI(equipmentInSlot, -50 + index * 20, -22)
            }

            enableAlpha()
            disableBlend()
            enableTexture2D()
        }

        // Pop
        glPopMatrix()
    }
}
