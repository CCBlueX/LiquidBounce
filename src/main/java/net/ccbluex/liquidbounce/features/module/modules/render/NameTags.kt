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
import net.ccbluex.liquidbounce.value.TextValue
import net.minecraft.client.renderer.GlStateManager.*
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.potion.Potion
import net.minecraft.potion.PotionEffect
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import kotlin.math.roundToInt

object NameTags : Module("NameTags", ModuleCategory.RENDER) {
    private val decimalFormat = DecimalFormat("##0.00", DecimalFormatSymbols(Locale.ENGLISH))

    private val health by BoolValue("Health", true)
    private val absorption by BoolValue("Absorption", false) { health || healthBar }
    private val healthInInt by BoolValue("HealthInIntegers", true) { health }
    private val healthPrefix by BoolValue("HealthPrefix", false) { health }
    private val healthPrefixText by TextValue("HealthPrefixText", "") { health && healthPrefix }
    private val healthSuffix by BoolValue("HealthSuffix", true) { health }
    private val healthSuffixText by TextValue("HealthSuffifText", " HP") { health && healthSuffix }
    private val ping by BoolValue("Ping", false)
    private val healthBar by BoolValue("Bar", false)
    private val distance by BoolValue("Distance", false)
    private val armor by BoolValue("Armor", true)
    private val bot by BoolValue("Bots", true)
    private val potion by BoolValue("Potions", true)
    private val clearNames by BoolValue("ClearNames", false)
    private val font by FontValue("Font", Fonts.font40)
    private val scale by FloatValue("Scale", 1F, 1F..4F)
    private val fontShadow by BoolValue("Shadow", true)

    private val background by BoolValue("Background", true)
    private val backgroundColorRed by IntegerValue("Background-R", 255, 0..255) { background }
    private val backgroundColorGreen by IntegerValue("Background-G", 179, 0..255) { background }
    private val backgroundColorBlue by IntegerValue("Background-B", 72, 0..255) { background }
    private val backgroundColorAlpha by IntegerValue("Background-Alpha", 100, 0..255) { background }

    private val border by BoolValue("Border", true)
    private val borderColorRed by IntegerValue("Border-R", 255, 0..255) { border }
    private val borderColorGreen by IntegerValue("Border-G", 179, 0..255) { border }
    private val borderColorBlue by IntegerValue("Border-B", 72, 0..255) { border }
    private val borderColorAlpha by IntegerValue("Border-Alpha", 100, 0..255) { border }

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
            if (isBot(entity) && !bot) continue

            renderNameTag(entity,
                    if (clearNames)
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
        val fontRenderer = font

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

        // Disable lightning and depth test
        disableGlCap(GL_LIGHTING, GL_DEPTH_TEST)

        // Enable blend
        enableGlCap(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        // Modify tag
        val bot = isBot(entity)
        val nameColor = if (bot) "§3" else if (entity.isInvisible) "§6" else if (entity.isSneaking) "§4" else "§7"
        val playerPing = if (entity is EntityPlayer) entity.getPing() else 0
        val playerDistance = thePlayer.getDistanceToEntity(entity)

        val distanceText = if (distance) "§7${playerDistance.roundToInt()}m " else ""
        val pingText = if (ping && entity is EntityPlayer) " §7[" + (if (playerPing > 200) "§c" else if (playerPing > 100) "§e" else "§a") + playerPing + "ms§7]" else ""
        val healthText = if (health) getHealthString(entity) else ""
        val botText = if (bot) " §c§lBot" else ""

        val text = "$distanceText$pingText$nameColor$tag$healthText$botText"

        // Scale
        val scale = ((playerDistance / 4F).coerceAtLeast(1F) / 150F) * scale

        glScalef(-scale, -scale, scale)

        val width = fontRenderer.getStringWidth(text) * 0.5f
        fontRenderer.drawString(text, 1F + -width, if (fontRenderer == Fonts.minecraftFont) 1F else 1.5F, 0xFFFFFF, true)

        val dist = width + 4F - (-width - 2F)

        glDisable(GL_TEXTURE_2D)
        glEnable(GL_BLEND)

        val bgColor = if (background) {
            // Background
            Color(backgroundColorRed, backgroundColorGreen, backgroundColorBlue, backgroundColorAlpha)
        } else {
            // Transparent
            Color(0, 0, 0, 0)
        }

        val borderColor = Color(borderColorRed, borderColorGreen, borderColorBlue, borderColorAlpha)

        if (border)
            quickDrawBorderedRect(-width - 2F, -2F, width + 4F, fontRenderer.FONT_HEIGHT + 2F + if (healthBar) 2F else 0F, 2F, borderColor.rgb, bgColor.rgb)
        else
            quickDrawRect(-width - 2F, -2F, width + 4F, fontRenderer.FONT_HEIGHT + 2F + if (healthBar) 2F else 0F, bgColor.rgb)

        if (healthBar) {
            quickDrawRect(-width - 2F, fontRenderer.FONT_HEIGHT + 3F, -width - 2F + dist, fontRenderer.FONT_HEIGHT + 4F, Color(10, 155, 10).rgb)
            val currHealth = entity.health + if (absorption) entity.absorptionAmount else 0f
            quickDrawRect(-width - 2F, fontRenderer.FONT_HEIGHT + 3F, -width - 2F + (dist * (currHealth / entity.maxHealth).coerceIn(0F, 1F)), fontRenderer.FONT_HEIGHT + 4F, Color(10, 255, 10).rgb)
        }

        glEnable(GL_TEXTURE_2D)

        fontRenderer.drawString(text, 1F + -width, if (fontRenderer == Fonts.minecraftFont) 1F else 1.5F,
                0xFFFFFF, fontShadow)

        var foundPotion = false
        if (potion && entity is EntityPlayer) {
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

        if (armor && entity is EntityPlayer) {
            for (index in 0..4) {
                if (entity.getEquipmentInSlot(index) == null) {
                    continue
                }

                mc.renderItem.zLevel = -147F
                mc.renderItem.renderItemAndEffectIntoGUI(entity.getEquipmentInSlot(index), -50 + index * 20, if (potion && foundPotion) -42 else -22)
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

    private fun getHealthString(entity : EntityLivingBase) : String {
        val result = entity.health + if (absorption) entity.absorptionAmount else 0f
        val prefix = if (healthPrefix) healthPrefixText else ""
        val suffix = if (healthSuffix) healthSuffixText else ""
        return prefix + "§c " + ( if (healthInInt) result.toInt() else decimalFormat.format(result) ) + suffix
    }
}
