/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.clickgui

import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_NAME
import net.ccbluex.liquidbounce.LiquidBounce.moduleManager
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI
//import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI
//import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI.scale
//import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI.scrolls
import net.ccbluex.liquidbounce.file.FileManager.clickGuiConfig
import net.ccbluex.liquidbounce.file.FileManager.saveConfig
import net.ccbluex.liquidbounce.ui.client.clickgui.elements.ButtonElement
import net.ccbluex.liquidbounce.ui.client.clickgui.style.Style
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.LiquidBounceStyle
import net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner
import net.ccbluex.liquidbounce.ui.font.AWTFontRenderer.Companion.assumeNonVolatile
import net.ccbluex.liquidbounce.ui.font.Fonts
//import net.ccbluex.liquidbounce.ui.font.Fonts.getFont
import net.ccbluex.liquidbounce.utils.render.RenderUtils.deltaTime
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.renderer.GlStateManager.disableLighting
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.util.ResourceLocation
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11.glScaled
import java.awt.Color
import kotlin.math.roundToInt

object RiceGui : GuiScreen() {

    private val mainColor = Color(21,20,29).rgb
    private val mainColor2 = Color(28,27,34).rgb
    val highlightColor = Color(238,150,208).rgb
    val accentColor = Color(109,114,175).rgb
    val referenceColor = Color(82,81,92).rgb
    val panels = mutableListOf<Panel>()
    private val hudIcon = ResourceLocation("${CLIENT_NAME.lowercase()}/custom_hud_icon.png")
    var style: Style = LiquidBounceStyle
    private var mouseX = 0
        set(value) {
            field = value.coerceAtLeast(0)
        }
    private var mouseY = 0
        set(value) {
            field = value.coerceAtLeast(0)
        }

    // Used when closing ClickGui using its key bind, prevents it from getting closed instantly after getting opened.
    // Caused by keyTyped being called along with onKey that opens the ClickGui.
    private var ignoreClosing = false



    override fun drawScreen(x: Int, y: Int, partialTicks: Float) {
        // Enable DisplayList optimization
        assumeNonVolatile = true

//        val scale = scale.toDouble()
//        glScaled(scale, scale, scale)
        mouseX = x
        mouseY = y

        val initX = 130f
        val contentXOffset = 100
        val initY = 60f
        val widthBg = 400f
        val heightBg = 260f
        val marginLeft = 10f



        drawBackground(initX,initY,widthBg,heightBg, mainColor)
        drawBackground(initX+contentXOffset,initY,widthBg-contentXOffset,heightBg,mainColor2)
//        var font60  = Fonts.font40

//        val font60 = GameFontRenderer(getFont("Roboto-Medium.ttf", 40))

        Fonts.font60.drawString("Liquidbounce",initX+marginLeft-2,initY+9,Color.WHITE.rgb)
        Category.values().forEachIndexed { index, category ->
            run {
                Fonts.font35.drawString(category.displayName, initX + marginLeft, initY +40+(Fonts.font35.fontHeight + 10 )*index, Color.WHITE.rgb)
            }
        }


//        drawRoundedRect(
//            startX + contentXOffset + marginLeft,
//            initY + 20,
//            startX + widthBg - marginLeft,
//            initY + 20 + 50,
//            mainColor,
//            3f
//        )

        moduleManager.modules.take(3).forEachIndexed{index, module->run {
         drawElement(initX + contentXOffset, initY+20 + (45)*index, 40f, widthBg-contentXOffset, 5f, mainColor, module)
        }}

//        drawImage(hudIcon, 9, height - 41, 32, 32)


        for (panel in panels) {
            panel.updateFade(deltaTime)
            panel.drawScreenAndClick(mouseX, mouseY)
        }

        descriptions@ for (panel in panels.reversed()) {
            // Don't draw hover text when hovering over a panel header.
            if (panel.isHovered(mouseX, mouseY)) break

            for (element in panel.elements) {
                if (element is ButtonElement) {
                    if (element.isVisible && element.hoverText.isNotBlank() && element.isHovered(
                            mouseX, mouseY
                        ) && element.y <= panel.y + panel.fade
                    ) {
                        style.drawHoverText(mouseX, mouseY, element.hoverText)
                        // Don't draw hover text for any elements below.
                        break@descriptions
                    }
                }
            }
        }

//        if (Mouse.hasWheel()) {
//            val wheel = Mouse.getDWheel()
//            if (wheel != 0) {
//                var handledScroll = false
//
//                // Handle foremost panel.
//                for (panel in panels.reversed()) {
//                    if (panel.handleScroll(mouseX, mouseY, wheel)) {
//                        handledScroll = true
//                        break
//                    }
//                }
//
//                if (!handledScroll) handleScroll(wheel)
//            }
//        }

        disableLighting()
        RenderHelper.disableStandardItemLighting()
        glScaled(1.0, 1.0, 1.0)

        assumeNonVolatile = false

        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    private fun drawElement(
        startX: Float,
        startY: Float,
        height: Float,
        width: Float,
        margin:Float,
        mainColor: Int,
        module:Module
    ) {
        drawRoundedRect(
            startX + margin,
            startY,
            startX + width - margin,
            startY + height,
            mainColor,
            3f
        )
//        val module = moduleManager.modules.first()
        Fonts.font40.drawString( module.name,startX+margin+10,startY+10, if(module.state)accentColor else Color.WHITE.rgb)
        Fonts.font35.drawString( "(${module.category.displayName})",startX+margin+15+Fonts.font40.getStringWidth(module.name),startY+10, referenceColor)
        Fonts.font35.drawString(module.description,startX+margin+10,startY+Fonts.font40.fontHeight+15, referenceColor)
    }

    fun drawBackground(x:Float,y:Float,width:Float,height:Float,color:Int){
        drawRoundedRect(x,y,x+width,y+height, color,3f)
    }
//    private fun handleScroll(wheel: Int) {
//        if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)) {
//            scale += wheel * 0.0001f
//
//            for (panel in panels) {
//                panel.x = panel.parseX()
//                panel.y = panel.parseY()
//            }
//
//        } else if (scrolls) {
//            for (panel in panels) panel.y = panel.parseY(panel.y + wheel / 10)
//        }
//    }

    public override fun mouseClicked(x: Int, y: Int, mouseButton: Int) {
        if (mouseButton == 0 && x in 5..50 && y in height - 50..height - 5) {
            mc.displayGuiScreen(GuiHudDesigner())
            return
        }

        mouseX = x
        mouseY = y

    }

    public override fun mouseReleased(x: Int, y: Int, state: Int) {
        mouseX = x
        mouseY = y

        for (panel in panels) panel.mouseReleased(mouseX, mouseY, state)
    }

    override fun updateScreen() {
        super.updateScreen()
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        // Close ClickGUI by using its key bind.
        if (keyCode == ClickGUI.keyBind) {
            if (ignoreClosing) ignoreClosing = false
            else mc.displayGuiScreen(null)

            return
        }

        super.keyTyped(typedChar, keyCode)
    }

    override fun onGuiClosed() {
        saveConfig(clickGuiConfig)
        for (panel in panels) panel.fade = 0
    }

    override fun initGui() {
        ignoreClosing = true
    }

    fun Int.clamp(min: Int, max: Int): Int = this.coerceIn(min, max.coerceAtLeast(0))

    override fun doesGuiPauseGame() = false
}