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
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot.isBot
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.EntityUtils.isSelected
import net.ccbluex.liquidbounce.utils.extensions.getPing
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.disableGlCap
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawTexturedModalRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.enableGlCap
import net.ccbluex.liquidbounce.utils.render.RenderUtils.quickDrawBorderedRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.quickDrawRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.resetCaps
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.FontValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.client.renderer.GlStateManager.*
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.potion.Potion
import net.minecraft.potion.PotionEffect
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import kotlin.math.roundToInt

object NameTags : Module("NameTags", ModuleCategory.RENDER) {
    private val healthValue = BoolValue("Health", true)
    private val pingValue = BoolValue("Ping", false)
    private val healthBarValue = BoolValue("Bar", false)
    private val distanceValue = BoolValue("Distance", false)
    private val armorValue = BoolValue("Armor", true)
    private val botValue = BoolValue("Bots", true)
    private val potionValue = BoolValue("Potions", true)
    private val clearNamesValue = BoolValue("ClearNames", false)
    private val fontValue = FontValue("Font", Fonts.font40)
    private val scaleValue = FloatValue("Scale", 1F, 1F, 4F)
    private val fontShadowValue = BoolValue("Shadow", true)

    private val backgroundValue = BoolValue("Background", true)
    private val backgroundColorRedValue = object : IntegerValue("Background-R", 255, 0, 255) {
        override fun isSupported() = backgroundValue.get()
    }

    private val backgroundColorGreenValue = object : IntegerValue("Background-G", 179, 0, 255) {
        override fun isSupported() = backgroundValue.get()
    }
    private val backgroundColorBlueValue = object : IntegerValue("Background-B", 72, 0, 255) {
        override fun isSupported() = backgroundValue.get()
    }
    private val backgroundColorAlphaValue = object : IntegerValue("Background-Alpha", 100, 0, 255) {
        override fun isSupported() = backgroundValue.get()
    }

    private val borderValue = BoolValue("Border", true)
    private val borderColorRedValue = object : IntegerValue("Border-R", 255, 0, 255) {
        override fun isSupported() = borderValue.get()
    }
    private val borderColorGreenValue = object : IntegerValue("Border-G", 179, 0, 255) {
        override fun isSupported() = borderValue.get()
    }
    private val borderColorBlueValue = object : IntegerValue("Border-B", 72, 0, 255) {
        override fun isSupported() = borderValue.get()
    }
    private val borderColorAlphaValue = object : IntegerValue("Border-Alpha", 100, 0, 255) {
        override fun isSupported() = borderValue.get()
    }


    private val inventoryBackground = ResourceLocation("textures/gui/container/inventory.png")

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

        for (entity in mc.theWorld.loadedEntityList) {
            if (entity !is EntityLivingBase) continue
            if (!isSelected(entity, false)) continue
            if (isBot(entity) && !botValue.get()) continue

            renderNameTag(entity,
                    if (clearNamesValue.get())
                        ColorUtils.stripColor(entity.displayName?.unformattedText ?: continue)
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

        // Set fontrenderer local
        val fontRenderer = fontValue.get()

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
        var distance = mc.thePlayer.getDistanceToEntity(entity) / 4F

        if (distance < 1F) {
            distance = 1F
        }

        val scale = (distance / 150F) * scaleValue.get()

        // Disable lightning and depth test
        disableGlCap(GL_LIGHTING, GL_DEPTH_TEST)

        // Enable blend
        enableGlCap(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        // Modify tag
        val bot = isBot(entity)
        val nameColor = if (bot) "§3" else if (entity.isInvisible) "§6" else if (entity.isSneaking) "§4" else "§7"
        val ping = if (entity is EntityPlayer) entity.getPing() else 0

        val distanceText = if (distanceValue.get()) "§7${thePlayer.getDistanceToEntity(entity).roundToInt()}m " else ""
        val pingText = if (pingValue.get() && entity is EntityPlayer) " §7[" + (if (ping > 200) "§c" else if (ping > 100) "§e" else "§a") + ping + "ms§7]" else ""
        val healthText = if (healthValue.get()) "§7§c " + entity.health.toInt() + " HP" else ""
        val botText = if (bot) " §c§lBot" else ""

        val text = "$distanceText$pingText$nameColor$tag$healthText$botText"

        glScalef(-scale, -scale, scale)

        val width = fontRenderer.getStringWidth(text) * 0.5f
        fontRenderer.drawString(
                text,
                1F + -width,
                if (fontRenderer == Fonts.minecraftFont) 1F else 1.5F,
                0xFFFFFF,
                true
        )

        val dist = width + 4F - (-width - 2F)

        glDisable(GL_TEXTURE_2D)
        glEnable(GL_BLEND)

        val bgColor = if (backgroundValue.get()) {
            // Background
            Color(backgroundColorRedValue.get(), backgroundColorGreenValue.get(), backgroundColorBlueValue.get(), backgroundColorAlphaValue.get())
        } else {
            // Transparent
            Color(0, 0, 0, 0)
        }

        val borderColor = Color(borderColorRedValue.get(), borderColorGreenValue.get(), borderColorBlueValue.get(), borderColorAlphaValue.get())

        if (borderValue.get())
            quickDrawBorderedRect(-width - 2F, -2F, width + 4F, fontRenderer.FONT_HEIGHT + 2F + if (healthBarValue.get()) 2F else 0F, 2F, borderColor.rgb, bgColor.rgb)
        else
            quickDrawRect(-width - 2F, -2F, width + 4F, fontRenderer.FONT_HEIGHT + 2F + if (healthBarValue.get()) 2F else 0F, bgColor.rgb)

        if (healthBarValue.get()) {
            quickDrawRect(-width - 2F, fontRenderer.FONT_HEIGHT + 3F, -width - 2F + dist, fontRenderer.FONT_HEIGHT + 4F, Color(10, 155, 10).rgb)
            quickDrawRect(-width - 2F, fontRenderer.FONT_HEIGHT + 3F, -width - 2F + (dist * (entity.health.toFloat() / entity.maxHealth.toFloat()).coerceIn(0F, 1F)), fontRenderer.FONT_HEIGHT + 4F, Color(10, 255, 10).rgb)
        }

        glEnable(GL_TEXTURE_2D)

        fontRenderer.drawString(text, 1F + -width, if (fontRenderer == Fonts.minecraftFont) 1F else 1.5F,
                0xFFFFFF, fontShadowValue.get())

        var foundPotion = false
        if (potionValue.get() && entity is EntityPlayer) {
            val potions = (entity.getActivePotionEffects() as Collection<PotionEffect>).map { Potion.potionTypes[it.getPotionID()] }.filter { it.hasStatusIcon() }
            if (!potions.isEmpty()) {
                foundPotion = true

                color(1.0F, 1.0F, 1.0F, 1.0F)
                disableLighting()
                enableTexture2D()

                val minX = (potions.size * -20) / 2

                var index = 0

                glPushMatrix()
                enableRescaleNormal()
                for (potion in potions) {
                    color(1.0F, 1.0F, 1.0F, 1.0F)
                    mc.getTextureManager().bindTexture(inventoryBackground)
                    val i1 = potion.getStatusIconIndex()
                    drawTexturedModalRect(minX + index * 20, -22, 0 + i1 % 8 * 18, 198 + i1 / 8 * 18, 18, 18, 0F)
                    index++
                }
                disableRescaleNormal()
                glPopMatrix()

                enableAlpha()
                disableBlend()
                enableTexture2D()
            }
        }

        if (armorValue.get() && entity is EntityPlayer) {
            for (index in 0..4) {
                if (entity.getEquipmentInSlot(index) == null) {
                    continue
                }

                mc.renderItem.zLevel = -147F
                mc.renderItem.renderItemAndEffectIntoGUI(entity.getEquipmentInSlot(index), -50 + index * 20, if (potionValue.get() && foundPotion) -42 else -22)
            }

            enableAlpha()
            disableBlend()
            enableTexture2D()
        }

        // Reset caps
        resetCaps()

        // Reset color
        resetColor()
        glColor4f(1F, 1F, 1F, 1F)

        // Pop
        glPopMatrix()
    }
}
