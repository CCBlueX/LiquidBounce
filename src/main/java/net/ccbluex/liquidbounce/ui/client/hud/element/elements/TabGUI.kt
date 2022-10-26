/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.LiquidBounce

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.client.hud.element.Side
import net.ccbluex.liquidbounce.ui.font.AWTFontRenderer
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowShader
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.FontValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.client.gui.FontRenderer
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11
import java.awt.Color

@ElementInfo(name = "TabGUI")
class TabGUI(x: Double = 5.0, y: Double = 25.0) : Element(x = x, y = y) {

    private val rainbowX = FloatValue("Rainbow-X", -1000F, -2000F, 2000F)
    private val rainbowY = FloatValue("Rainbow-Y", -1000F, -2000F, 2000F)
    private val redValue = IntegerValue("Rectangle Red", 0, 0, 255)
    private val greenValue = IntegerValue("Rectangle Green", 148, 0, 255)
    private val blueValue = IntegerValue("Rectangle Blue", 255, 0, 255)
    private val alphaValue = IntegerValue("Rectangle Alpha", 140, 0, 255)
    private val rectangleRainbow = BoolValue("Rectangle Rainbow", false)
    private val backgroundRedValue = IntegerValue("Background Red", 0, 0, 255)
    private val backgroundGreenValue = IntegerValue("Background Green", 0, 0, 255)
    private val backgroundBlueValue = IntegerValue("Background Blue", 0, 0, 255)
    private val backgroundAlphaValue = IntegerValue("Background Alpha", 150, 0, 255)
    private val borderValue = BoolValue("Border", true)
    private val borderStrength = FloatValue("Border Strength", 2F, 1F, 5F)
    private val borderRedValue = IntegerValue("Border Red", 0, 0, 255)
    private val borderGreenValue = IntegerValue("Border Green", 0, 0, 255)
    private val borderBlueValue = IntegerValue("Border Blue", 0, 0, 255)
    private val borderAlphaValue = IntegerValue("Border Alpha", 150, 0, 255)
    private val borderRainbow = BoolValue("Border Rainbow", false)
    private val arrowsValue = BoolValue("Arrows", true)
    private val fontValue = FontValue("Font", Fonts.font35)
    private val textShadow = BoolValue("TextShadow", false)
    private val textFade = BoolValue("TextFade", true)
    private val textPositionY = FloatValue("TextPosition-Y", 2F, 0F, 5F)
    private val width = FloatValue("Width", 60F, 55F, 100F)
    private val tabHeight = FloatValue("TabHeight", 12F, 10F, 15F)
    private val upperCaseValue = BoolValue("UpperCase", false)

    private val tabs = mutableListOf<Tab>()

    private var categoryMenu = true
    private var selectedCategory = 0
    private var selectedModule = 0

    private var tabY = 0F
    private var itemY = 0F

    init {
        for (category in ModuleCategory.values()) {
            val tab = Tab(category.displayName)

            LiquidBounce.moduleManager.modules
                    .filter { module: Module -> category == module.category }
                    .forEach { e: Module -> tab.modules.add(e) }

            tabs.add(tab)
        }
    }

    override fun drawElement(): Border? {
        updateAnimation()

        AWTFontRenderer.assumeNonVolatile = true

        val fontRenderer = fontValue.get()

        val rectangleRainbowEnabled = rectangleRainbow.get()

        val backgroundColor = Color(backgroundRedValue.get(), backgroundGreenValue.get(), backgroundBlueValue.get(),
                backgroundAlphaValue.get())

        val borderColor = if (!borderRainbow.get())
            Color(borderRedValue.get(), borderGreenValue.get(), borderBlueValue.get(), borderAlphaValue.get())
        else
            Color.black

        // Draw
        val guiHeight = tabs.size * tabHeight.get()

        RenderUtils.drawRect(1F, 0F, width.get(), guiHeight, backgroundColor.rgb)

        if (borderValue.get()) {
            RainbowShader.begin(borderRainbow.get(), if (rainbowX.get() == 0.0F) 0.0F else 1.0F / rainbowX.get(), if (rainbowY.get() == 0.0F) 0.0F else 1.0F / rainbowY.get(), System.currentTimeMillis() % 10000 / 10000F).use {
                RenderUtils.drawBorder(1F, 0F, width.get(), guiHeight, borderStrength.get(), borderColor.rgb)
            }
        }

        // Color
        val rectColor = if (!rectangleRainbowEnabled)
            Color(redValue.get(), greenValue.get(), blueValue.get(), alphaValue.get())
        else {
            Color.black
        }

        RainbowShader.begin(rectangleRainbowEnabled, if (rainbowX.get() == 0.0F) 0.0F else 1.0F / rainbowX.get(), if (rainbowY.get() == 0.0F) 0.0F else 1.0F / rainbowY.get(), System.currentTimeMillis() % 10000 / 10000F).use {
            RenderUtils.drawRect(1F, 1 + tabY - 1, width.get(), tabY + tabHeight.get(), rectColor)
        }

        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f)

        var y = 1F
        tabs.forEachIndexed { index, tab ->
            val tabName = if (upperCaseValue.get())
                tab.tabName.uppercase()
            else
                tab.tabName

            val textX = if (side.horizontal == Side.Horizontal.RIGHT)
                width.get() - fontRenderer.getStringWidth(tabName) - tab.textFade - 3
            else
                tab.textFade + 5
            val textY = y + textPositionY.get()

            val textColor = if (selectedCategory == index) 0xffffff else Color(210, 210, 210).rgb

            fontRenderer.drawString(tabName, textX, textY, textColor, textShadow.get())

            if (arrowsValue.get()) {
                if (side.horizontal == Side.Horizontal.RIGHT)
                    fontRenderer.drawString(if (!categoryMenu && selectedCategory == index) ">" else "<", 3F, y + 2F,
                            0xffffff, textShadow.get())
                else
                    fontRenderer.drawString(if (!categoryMenu && selectedCategory == index) "<" else ">",
                            width.get() - 8F, y + 2F, 0xffffff, textShadow.get())
            }

            if (index == selectedCategory && !categoryMenu) {
                val tabX = if (side.horizontal == Side.Horizontal.RIGHT)
                    1F - tab.menuWidth
                else
                    width.get() + 5

                tab.drawTab(
                        tabX,
                        y,
                        rectColor.rgb,
                        backgroundColor.rgb,
                        borderColor.rgb,
                        borderStrength.get(),
                        upperCaseValue.get(),
                        fontRenderer,
                        borderRainbow.get(),
                        rectangleRainbowEnabled
                )
            }
            y += tabHeight.get()
        }

        AWTFontRenderer.assumeNonVolatile = false

        return Border(1F, 0F, width.get(), guiHeight)
    }

    override fun handleKey(c: Char, keyCode: Int) {
        when (keyCode) {
            Keyboard.KEY_UP -> parseAction(Action.UP)
            Keyboard.KEY_DOWN -> parseAction(Action.DOWN)
            Keyboard.KEY_RIGHT -> parseAction(if (side.horizontal == Side.Horizontal.RIGHT) Action.LEFT else Action.RIGHT)
            Keyboard.KEY_LEFT -> parseAction(if (side.horizontal == Side.Horizontal.RIGHT) Action.RIGHT else Action.LEFT)
            Keyboard.KEY_RETURN -> parseAction(Action.TOGGLE)
        }
    }

    private fun updateAnimation() {
        val delta = RenderUtils.deltaTime

        val xPos = tabHeight.get() * selectedCategory
        if (tabY.toInt() != xPos.toInt()) {
            if (xPos > tabY)
                tabY += 0.1F * delta
            else
                tabY -= 0.1F * delta
        } else
            tabY = xPos
        val xPos2 = tabHeight.get() * selectedModule

        if (itemY.toInt() != xPos2.toInt()) {
            if (xPos2 > itemY)
                itemY += 0.1F * delta
            else
                itemY -= 0.1F * delta
        } else
            itemY = xPos2

        if (categoryMenu)
            itemY = 0F

        if (textFade.get()) {
            tabs.forEachIndexed { index, tab ->
                if (index == selectedCategory) {
                    if (tab.textFade < 4)
                        tab.textFade += 0.05F * delta

                    if (tab.textFade > 4)
                        tab.textFade = 4F
                } else {
                    if (tab.textFade > 0)
                        tab.textFade -= 0.05F * delta

                    if (tab.textFade < 0)
                        tab.textFade = 0F
                }
            }
        } else {
            for (tab in tabs) {
                if (tab.textFade > 0)
                    tab.textFade -= 0.05F * delta

                if (tab.textFade < 0)
                    tab.textFade = 0F
            }
        }
    }

    private fun parseAction(action: Action) {
        var toggle = false

        when (action) {
            Action.UP -> if (categoryMenu) {
                --selectedCategory
                if (selectedCategory < 0) {
                    selectedCategory = tabs.size - 1
                    tabY = tabHeight.get() * selectedCategory.toFloat()
                }
            } else {
                --selectedModule
                if (selectedModule < 0) {
                    selectedModule = tabs[selectedCategory].modules.size - 1
                    itemY = tabHeight.get() * selectedModule.toFloat()
                }
            }

            Action.DOWN -> if (categoryMenu) {
                ++selectedCategory
                if (selectedCategory > tabs.size - 1) {
                    selectedCategory = 0
                    tabY = tabHeight.get() * selectedCategory.toFloat()
                }
            } else {
                ++selectedModule
                if (selectedModule > tabs[selectedCategory].modules.size - 1) {
                    selectedModule = 0
                    itemY = tabHeight.get() * selectedModule.toFloat()
                }
            }

            Action.LEFT -> {
                if (!categoryMenu)
                    categoryMenu = true
            }

            Action.RIGHT ->
                if (!categoryMenu) {
                    toggle = true
                } else {
                    categoryMenu = false
                    selectedModule = 0
                }


            Action.TOGGLE -> if (!categoryMenu) toggle = true
        }

        if (toggle) {
            val sel = selectedModule
            tabs[selectedCategory].modules[sel].toggle()
        }
    }

    /**
     * TabGUI Tab
     */
    private inner class Tab(val tabName: String) {

        val modules = mutableListOf<Module>()
        var menuWidth = 0
        var textFade = 0F

        fun drawTab(x: Float, y: Float, color: Int, backgroundColor: Int, borderColor: Int, borderStrength: Float,
                    upperCase: Boolean, fontRenderer: FontRenderer, borderRainbow: Boolean, rectRainbow: Boolean) {
            var maxWidth = 0

            for (module in modules)
                if (fontRenderer.getStringWidth(if (upperCase) module.name.uppercase() else module.name) + 4 > maxWidth)
                    maxWidth = (fontRenderer.getStringWidth(if (upperCase) module.name.uppercase() else module.name) + 7F).toInt()

            menuWidth = maxWidth

            val menuHeight = modules.size * tabHeight.get()

            if (borderValue.get()) {
                RainbowShader.begin(borderRainbow, if (rainbowX.get() == 0.0F) 0.0F else 1.0F / rainbowX.get(), if (rainbowY.get() == 0.0F) 0.0F else 1.0F / rainbowY.get(), System.currentTimeMillis() % 10000 / 10000F).use {
                    RenderUtils.drawBorder(x - 1F, y - 1F, x + menuWidth - 2F, y + menuHeight - 1F, borderStrength, borderColor)
                }
            }
            RenderUtils.drawRect(x - 1F, y - 1F, x + menuWidth - 2F, y + menuHeight - 1F, backgroundColor)


            RainbowShader.begin(rectRainbow, if (rainbowX.get() == 0.0F) 0.0F else 1.0F / rainbowX.get(), if (rainbowY.get() == 0.0F) 0.0F else 1.0F / rainbowY.get(), System.currentTimeMillis() % 10000 / 10000F).use {
                RenderUtils.drawRect(x - 1.toFloat(), y + itemY - 1, x + menuWidth - 2F, y + itemY + tabHeight.get() - 1, color)
            }

            GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f)

            modules.forEachIndexed { index, module ->
                val moduleColor = if (module.state) 0xffffff else Color(205, 205, 205).rgb

                fontRenderer.drawString(if (upperCase) module.name.uppercase() else module.name, x + 2F,
                        y + tabHeight.get() * index + textPositionY.get(), moduleColor, textShadow.get())
            }
        }

    }

    /**
     * TabGUI Action
     */
    enum class Action { UP, DOWN, LEFT, RIGHT, TOGGLE }
}