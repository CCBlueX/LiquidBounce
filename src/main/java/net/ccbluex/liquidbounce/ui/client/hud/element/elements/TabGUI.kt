/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.LiquidBounce.moduleManager
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.client.hud.element.Side
import net.ccbluex.liquidbounce.ui.font.AWTFontRenderer
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.RenderUtils.deltaTime
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedBorder
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect2
import net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowShader
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.FontValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.client.font.TextRenderer
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11.glColor4f
import java.awt.Color

@ElementInfo(name = "TabGUI")
class TabGUI(x: Double = 5.0, y: Double = 25.0) : Element(x = x, y = y) {

    private val rectRainbow by BoolValue("Rectangle Rainbow", false)
    private val rectRed by IntegerValue("Rectangle Red", 0, 0..255) { !rectRainbow }
    private val rectGreen by IntegerValue("Rectangle Green", 148, 0..255) { !rectRainbow }
    private val rectBlue by IntegerValue("Rectangle Blue", 255, 0..255) { !rectRainbow }
    private val rectAlpha by IntegerValue("Rectangle Alpha", 140, 0..255) { !rectRainbow }

    private val roundedRectRadius by FloatValue("Rounded-Radius", 3F, 0F..5F)

    private val backgroundRed by IntegerValue("Background Red", 0, 0..255)
    private val backgroundGreen by IntegerValue("Background Green", 0, 0..255)
    private val backgroundBlue by IntegerValue("Background Blue", 0, 0..255)
    private val backgroundAlpha by IntegerValue("Background Alpha", 150, 0..255)

    private val borderValue by BoolValue("Border", false)
        private val borderStrength by FloatValue("Border Strength", 2F, 1F..5F) { borderValue }
        private val borderRainbow by BoolValue("Border Rainbow", false) { borderValue }
            private val borderRed by IntegerValue("Border Red", 0, 0..255) { borderValue && !borderRainbow }
            private val borderGreen by IntegerValue("Border Green", 0, 0..255) { borderValue && !borderRainbow }
            private val borderBlue by IntegerValue("Border Blue", 0, 0..255) { borderValue && !borderRainbow }
            private val borderAlpha by IntegerValue("Border Alpha", 150, 0..255) { borderValue && !borderRainbow }

    private val rainbowX by FloatValue("Rainbow-X", -1000F, -2000F..2000F)
        { rectRainbow || (borderValue && borderRainbow) }
    private val rainbowY by FloatValue("Rainbow-Y", -1000F, -2000F..2000F)
        { rectRainbow || (borderValue && borderRainbow) }

    private val arrows by BoolValue("Arrows", true)
    private val font by FontValue("Font", Fonts.font35)
    private val textShadow by BoolValue("TextShadow", false)
    private val textFade by BoolValue("TextFade", true)
    private val textPositionY by FloatValue("TextPosition-Y", 2F, 0F..5F)
    private val width by FloatValue("Width", 60F, 55F..100F)
    private val tabHeight by FloatValue("TabHeight", 12F, 10F..15F)
    private val upperCase by BoolValue("UpperCase", false)

    private val tabs = mutableListOf<Tab>()

    private var categoryMenu = true
    private var selectedCategory = 0
        set(value) {
            field = when {
                value < 0 -> tabs.lastIndex
                value > tabs.lastIndex -> 0
                else -> value
            }
        }
    private var selectedModule = 0
        set(value) {
            field = when {
                value < 0 -> tabs[selectedCategory].modules.lastIndex
                value > tabs[selectedCategory].modules.lastIndex -> 0
                else -> value
            }
        }

    private var tabY = 0F
    private var itemY = 0F

    init {
        for (category in Category.values()) {
            val tab = Tab(category.displayName)

            moduleManager.modules
                    .filter { module -> category == module.category }
                    .forEach { e -> tab.modules += e }

            tabs += tab
        }
    }

    override fun drawElement(): Border {
        updateAnimation()

        AWTFontRenderer.assumeNonVolatile = true

        val backgroundColor = Color(backgroundRed, backgroundGreen, backgroundBlue, backgroundAlpha)

        val borderColor = if (borderRainbow) Color.black else Color(borderRed, borderGreen, borderBlue, borderAlpha)

        // Draw
        val guiHeight = tabs.size * tabHeight

        drawRoundedRect(1F, 0F, width, guiHeight, backgroundColor.rgb, roundedRectRadius)

        if (borderValue) {
            RainbowShader.begin(borderRainbow, if (rainbowX == 0f) 0f else 1f / rainbowX, if (rainbowY == 0f) 0f else 1f / rainbowY, System.currentTimeMillis() % 10000 / 10000F).use {
                drawRoundedBorder(1F, 0F, width, guiHeight, borderStrength, borderColor.rgb, roundedRectRadius)
            }
        }

        // Color
        val rectColor = if (rectRainbow) Color.black else Color(rectRed, rectGreen, rectBlue, rectAlpha)

        RainbowShader.begin(rectRainbow, if (rainbowX == 0f) 0f else 1f / rainbowX, if (rainbowY == 0f) 0f else 1f / rainbowY, System.currentTimeMillis() % 10000 / 10000F).use {
            if (!borderValue) {
                drawRoundedRect2(1F, 1 + tabY - 1, width, tabY + tabHeight, rectColor, roundedRectRadius)
            } else {
                drawRoundedRect2(2.5F, 5 + tabY - 3.5F, width - 1.5F, tabY + tabHeight - 1.5F, rectColor, roundedRectRadius)
            }
        }

        glColor4f(1f, 1f, 1f, 1f)

        var y = 1F
        tabs.forEachIndexed { index, tab ->
            val tabName = if (upperCase)
                tab.tabName.uppercase()
            else
                tab.tabName

            val textX = if (side.horizontal == Side.Horizontal.RIGHT)
                width - font.getStringWidth(tabName) - tab.textFade - 3
            else
                tab.textFade + 5
            val textY = y + textPositionY

            val textColor = if (selectedCategory == index) 0xffffff else Color(210, 210, 210).rgb

            font.drawString(tabName, textX, textY, textColor, textShadow)

            if (arrows) {
                if (side.horizontal == Side.Horizontal.RIGHT)
                    font.drawString(if (!categoryMenu && selectedCategory == index) ">" else "<", 3F, y + 2F,
                            0xffffff, textShadow)
                else
                    font.drawString(if (!categoryMenu && selectedCategory == index) "<" else ">",
                            width - 8F, y + 2F, 0xffffff, textShadow)
            }

            if (index == selectedCategory && !categoryMenu) {
                val tabX = if (side.horizontal == Side.Horizontal.RIGHT)
                    1F - tab.menuWidth
                else
                    width + 5

                tab.drawTab(tabX, y, rectColor.rgb, backgroundColor.rgb, borderColor.rgb, borderStrength, font, borderRainbow, rectRainbow)
            }
            y += tabHeight
        }

        AWTFontRenderer.assumeNonVolatile = false

        return Border(1F, 0F, width, guiHeight)
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
        val xPos = tabHeight * selectedCategory
        if (tabY.toInt() != xPos.toInt()) {
            if (xPos > tabY)
                tabY += 0.1F * deltaTime
            else
                tabY -= 0.1F * deltaTime
        } else
            tabY = xPos
        val xPos2 = tabHeight * selectedModule

        if (itemY.toInt() != xPos2.toInt()) {
            if (xPos2 > itemY)
                itemY += 0.1F * deltaTime
            else
                itemY -= 0.1F * deltaTime
        } else
            itemY = xPos2

        if (categoryMenu)
            itemY = 0F

        if (textFade) {
            tabs.forEachIndexed { index, tab ->
                if (index == selectedCategory) tab.textFade += 0.05F * deltaTime
                else tab.textFade -= 0.05F * deltaTime
            }
        } else {
            for (tab in tabs)
                tab.textFade -= 0.05F * deltaTime
        }
    }

    private fun parseAction(action: Action) {
        var toggle = false

        when (action) {
            Action.UP ->
                if (categoryMenu) {
                    --selectedCategory
                    tabY = tabHeight * selectedCategory
                } else {
                    --selectedModule
                    itemY = tabHeight * selectedModule
                }

            Action.DOWN ->
                if (categoryMenu) {
                    ++selectedCategory
                    tabY = tabHeight * selectedCategory
                } else {
                    ++selectedModule
                    itemY = tabHeight * selectedModule
                }

            Action.LEFT ->
                if (!categoryMenu)
                    categoryMenu = true

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

    fun getDisplayName(module: Module) = if (upperCase) module.getName().uppercase() else module.getName()

    /**
     * TabGUI Tab
     */
    private inner class Tab(val tabName: String) {

        val modules = mutableListOf<Module>()
        var menuWidth = 0
        var textFade = 0F
            set(value) {
                field = value.coerceIn(0f, 4f)
            }

        fun drawTab(
            x: Float, y: Float, color: Int, backgroundColor: Int, borderColor: Int, borderStrength: Float,
            fontRenderer: TextRenderer, borderRainbow: Boolean, rectRainbow: Boolean
        ) {
            var maxWidth = 0

            for (module in modules) {
                val width = fontRenderer.getStringWidth(getDisplayName(module))
                if (width + 4 > maxWidth)
                    maxWidth = width + 7
            }

            menuWidth = maxWidth

            val menuHeight = modules.size * tabHeight

            if (borderValue) {
                RainbowShader.begin(borderRainbow, if (rainbowX == 0f) 0f else 1f / rainbowX, if (rainbowY == 0f) 0f else 1f / rainbowY, System.currentTimeMillis() % 10000 / 10000F).use {
                    drawRoundedBorder(x - 1F, y - 1F, x + menuWidth - 2F, y + menuHeight - 1F, borderStrength, borderColor, roundedRectRadius)
                }
            }
            drawRoundedRect(x - 1F, y - 1F, x + menuWidth - 2F, y + menuHeight - 1F, backgroundColor, roundedRectRadius)


            RainbowShader.begin(rectRainbow, if (rainbowX == 0f) 0f else 1f / rainbowX, if (rainbowY == 0f) 0f else 1f / rainbowY, System.currentTimeMillis() % 10000 / 10000F).use {
                drawRoundedRect(x - 1f, y + itemY - 1, x + menuWidth - 2F, y + itemY + tabHeight - 1, color, roundedRectRadius)
            }

            glColor4f(1f, 1f, 1f, 1f)

            modules.forEachIndexed { index, module ->
                val moduleColor = if (module.state) 0xffffff else Color(205, 205, 205).rgb

                fontRenderer.draw(getDisplayName(module), x + 2F,
                        y + tabHeight * index + textPositionY, moduleColor, textShadow)
            }
        }

    }

    /**
     * TabGUI Action
     */
    enum class Action { UP, DOWN, LEFT, RIGHT, TOGGLE }
}