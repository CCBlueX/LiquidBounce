/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.features.module.modules.render.BlockOverlay
import net.ccbluex.liquidbounce.features.module.modules.world.scaffolds.Scaffold
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.shader.shaders.GradientFontShader
import net.ccbluex.liquidbounce.utils.render.shader.shaders.GradientShader
import net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowFontShader
import net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowShader
import net.ccbluex.liquidbounce.value.*
import net.minecraft.block.BlockBush
import net.minecraft.item.ItemBlock
import org.lwjgl.opengl.GL11
import java.awt.Color

@ElementInfo(name = "BlockCounter")
class BlockCounter(x: Double = 520.0, y: Double = 245.0) : Element(x = x, y = y) {

    private val onScaffold by BoolValue("ScaffoldOnly", true)

    private val textColorMode by ListValue("Text-Color", arrayOf("Custom", "Rainbow", "Gradient"), "Custom")
    private val textRed by IntegerValue("Text-R", 255, 0..255) { textColorMode == "Custom" }
    private val textGreen by IntegerValue("Text-G", 255, 0..255) { textColorMode == "Custom" }
    private val textBlue by IntegerValue("Text-B", 255, 0..255) { textColorMode == "Custom" }

    private val gradientTextSpeed by FloatValue("Text-Gradient-Speed", 1f, 0.5f..10f) { textColorMode == "Gradient" }

    // TODO: Make Color picker to fix this mess :/
    private val gradientTextRed1 by FloatValue("Text-Gradient-R1", 255f, 0f..255f) { textColorMode == "Gradient" }
    private val gradientTextGreen1 by FloatValue("Text-Gradient-G1", 0f, 0f..255f) { textColorMode == "Gradient" }
    private val gradientTextBlue1 by FloatValue("Text-Gradient-B1", 0f, 0f..255f) { textColorMode == "Gradient" }

    private val gradientTextRed2 by FloatValue("Text-Gradient-R2", 0f, 0f..255f) { textColorMode == "Gradient" }
    private val gradientTextGreen2 by FloatValue("Text-Gradient-G2", 255f, 0f..255f) { textColorMode == "Gradient" }
    private val gradientTextBlue2 by FloatValue("Text-Gradient-B2", 0f, 0f..255f) { textColorMode == "Gradient" }

    private val gradientTextRed3 by FloatValue("Text-Gradient-R3", 0f, 0f..255f) { textColorMode == "Gradient" }
    private val gradientTextGreen3 by FloatValue("Text-Gradient-G3", 0f, 0f..255f) { textColorMode == "Gradient" }
    private val gradientTextBlue3 by FloatValue("Text-Gradient-B3", 255f, 0f..255f) { textColorMode == "Gradient" }

    private val gradientTextRed4 by FloatValue("Text-Gradient-R4", 0f, 0f..255f) { textColorMode == "Gradient" }
    private val gradientTextGreen4 by FloatValue("Text-Gradient-G4", 0f, 0f..255f) { textColorMode == "Gradient" }
    private val gradientTextBlue4 by FloatValue("Text-Gradient-B4", 0f, 0f..255f) { textColorMode == "Gradient" }

    private val roundedRectRadius by FloatValue("Rounded-Radius", 2F, 0F..5F)

    private val backgroundMode by ListValue("Background-Color", arrayOf("Custom", "Rainbow", "Gradient"), "Custom")
    private val backgroundRed by IntegerValue("Background-R", 0, 0..255) { backgroundMode == "Custom" }
    private val backgroundGreen by IntegerValue("Background-G", 0, 0..255) { backgroundMode == "Custom" }
    private val backgroundBlue by IntegerValue("Background-B", 0, 0..255) { backgroundMode == "Custom" }
    private val backgroundAlpha by IntegerValue("Background-Alpha", 255, 0..255) { backgroundMode == "Custom" }

    private val gradientBackgroundSpeed by FloatValue("Background-Gradient-Speed", 1f, 0.5f..10f) { backgroundMode == "Gradient" }

    // TODO: Make Color picker to fix this mess :/
    private val gradientBackgroundRed1 by FloatValue("Background-Gradient-R1", 255f, 0f..255f) { backgroundMode == "Gradient" }
    private val gradientBackgroundGreen1 by FloatValue("Background-Gradient-G1", 0f, 0f..255f) { backgroundMode == "Gradient" }
    private val gradientBackgroundBlue1 by FloatValue("Background-Gradient-B1", 0f, 0f..255f) { backgroundMode == "Gradient" }

    private val gradientBackgroundRed2 by FloatValue("Background-Gradient-R2", 0f, 0f..255f) { backgroundMode == "Gradient" }
    private val gradientBackgroundGreen2 by FloatValue("Background-Gradient-G2", 255f, 0f..255f) { backgroundMode == "Gradient" }
    private val gradientBackgroundBlue2 by FloatValue("Background-Gradient-B2", 0f, 0f..255f) { backgroundMode == "Gradient" }

    private val gradientBackgroundRed3 by FloatValue("Background-Gradient-R3", 0f, 0f..255f) { backgroundMode == "Gradient" }
    private val gradientBackgroundGreen3 by FloatValue("Background-Gradient-G3", 0f, 0f..255f) { backgroundMode == "Gradient" }
    private val gradientBackgroundBlue3 by FloatValue("Background-Gradient-B3", 255f, 0f..255f) { backgroundMode == "Gradient" }

    private val gradientBackgroundRed4 by FloatValue("Background-Gradient-R4", 0f, 0f..255f) { backgroundMode == "Gradient" }
    private val gradientBackgroundGreen4 by FloatValue("Background-Gradient-G4", 0f, 0f..255f) { backgroundMode == "Gradient" }
    private val gradientBackgroundBlue4 by FloatValue("Background-Gradient-B4", 0f, 0f..255f) { backgroundMode == "Gradient" }

    private val borderRed by IntegerValue("Border-R", 0, 0..255)
    private val borderGreen by IntegerValue("Border-G", 0, 0..255)
    private val borderBlue by IntegerValue("Border-B", 0, 0..255)
    private val borderAlpha by IntegerValue("Border-Alpha", 255, 0..255)

    private val font by FontValue("Font", Fonts.font40)
    private val textShadow by BoolValue("ShadowText", true)

    private val rainbowX by FloatValue("Rainbow-X", -1000F, -2000F..2000F) { backgroundMode == "Rainbow" }
    private val rainbowY by FloatValue("Rainbow-Y", -1000F, -2000F..2000F) { backgroundMode == "Rainbow" }
    private val gradientX by FloatValue("Gradient-X", -1000F, -2000F..2000F) { textColorMode == "Gradient" || backgroundMode == "Gradient" }
    private val gradientY by FloatValue("Gradient-Y", -1000F, -2000F..2000F) { textColorMode == "Gradient" || backgroundMode == "Gradient" }

    override fun drawElement(): Border {
        if ((Scaffold.handleEvents() && onScaffold) || !onScaffold) {

            GL11.glPushMatrix()

            if (BlockOverlay.handleEvents() && BlockOverlay.info && BlockOverlay.currentBlock != null)
                GL11.glTranslatef(0f, 15f, 0f)

            val info = "Blocks: §7$blocksAmount"

            val textCustomColor = Color(textRed, textGreen, textBlue, 1).rgb
            val backgroundCustomColor = Color(backgroundRed, backgroundGreen, backgroundBlue, backgroundAlpha).rgb
            val borderCustomColor = Color(borderRed, borderGreen, borderBlue, borderAlpha).rgb

            val rainbowOffset = System.currentTimeMillis() % 10000 / 10000F
            val rainbowX = if (rainbowX == 0f) 0f else 1f / rainbowX
            val rainbowY = if (rainbowY == 0f) 0f else 1f / rainbowY

            val gradientOffset = System.currentTimeMillis() % 10000 / 10000F
            val gradientX = if (gradientX == 0f) 0f else 1f / gradientX
            val gradientY = if (gradientY == 0f) 0f else 1f / gradientY

            GradientShader.begin(
                backgroundMode == "Gradient",
                gradientX,
                gradientY,
                floatArrayOf(
                    gradientBackgroundRed1 / 255.0f,
                    gradientBackgroundGreen1 / 255.0f,
                    gradientBackgroundBlue1 / 255.0f,
                    1.0f
                ),
                floatArrayOf(
                    gradientBackgroundRed2 / 255.0f,
                    gradientBackgroundGreen2 / 255.0f,
                    gradientBackgroundBlue2 / 255.0f,
                    1.0f
                ),
                floatArrayOf(
                    gradientBackgroundRed3 / 255.0f,
                    gradientBackgroundGreen3 / 255.0f,
                    gradientBackgroundBlue3 / 255.0f,
                    1.0f
                ),
                floatArrayOf(
                    gradientBackgroundRed4 / 255.0f,
                    gradientBackgroundGreen4 / 255.0f,
                    gradientBackgroundBlue4 / 255.0f,
                    1.0f
                ),
                gradientBackgroundSpeed,
                gradientOffset
            ).use {
                RainbowShader.begin(
                    backgroundMode == "Rainbow",
                    rainbowX,
                    rainbowY,
                    rainbowOffset
                ).use {
                    RenderUtils.drawRoundedBorderRect(
                        0F,
                        0F,
                        font.getStringWidth(info) + 8F,
                        18f,
                        3F,
                        when (backgroundMode) {
                            "Gradient" -> 0
                            "Rainbow" -> 0
                            else -> backgroundCustomColor
                        },
                        borderCustomColor,
                        roundedRectRadius
                    )
                }
            }

            GradientFontShader.begin(
                textColorMode == "Gradient",
                gradientX,
                gradientY,
                floatArrayOf(
                    gradientTextRed1 / 255.0f,
                    gradientTextGreen1 / 255.0f,
                    gradientTextBlue1 / 255.0f,
                    1.0f
                ),
                floatArrayOf(
                    gradientTextRed2 / 255.0f,
                    gradientTextGreen2 / 255.0f,
                    gradientTextBlue2 / 255.0f,
                    1.0f
                ),
                floatArrayOf(
                    gradientTextRed3 / 255.0f,
                    gradientTextGreen3 / 255.0f,
                    gradientTextBlue3 / 255.0f,
                    1.0f
                ),
                floatArrayOf(
                    gradientTextRed4 / 255.0f,
                    gradientTextGreen4 / 255.0f,
                    gradientTextBlue4 / 255.0f,
                    1.0f
                ),
                gradientTextSpeed,
                gradientOffset
            ).use {
                RainbowFontShader.begin(
                    textColorMode == "Rainbow",
                    rainbowX,
                    rainbowY,
                    rainbowOffset
                ).use {
                    font.drawString(
                        info, 5F, 6F,
                        when (textColorMode) {
                            "Gradient" -> 0
                            "Rainbow" -> 0
                            else -> textCustomColor
                        },
                        textShadow
                    )

                    GL11.glPopMatrix()
                }
            }
        }

        return Border(0F, 0F, 55F, 18F)
    }

    private val blocksAmount: Int
        get() {
            var amount = 0
            for (i in 36..44) {
                val stack = mc.thePlayer.inventoryContainer.getSlot(i).stack ?: continue
                val item = stack.item
                if (item is ItemBlock) {
                    val block = item.block
                    val heldItem = mc.thePlayer?.heldItem
                    if (heldItem != null && heldItem == stack || block !in InventoryUtils.BLOCK_BLACKLIST && block !is BlockBush) {
                        amount += stack.stackSize
                    }
                }
            }
            return amount
        }
}