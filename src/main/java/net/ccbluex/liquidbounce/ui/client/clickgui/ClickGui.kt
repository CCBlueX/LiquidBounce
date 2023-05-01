/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.clickgui

import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_NAME
import net.ccbluex.liquidbounce.LiquidBounce.moduleManager
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI
import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI.guiColor
import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI.scale
import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI.scrolls
import net.ccbluex.liquidbounce.file.FileManager.clickGuiConfig
import net.ccbluex.liquidbounce.file.FileManager.saveConfig
import net.ccbluex.liquidbounce.ui.client.GuiClientSettings
import net.ccbluex.liquidbounce.ui.client.clickgui.elements.ButtonElement
import net.ccbluex.liquidbounce.ui.client.clickgui.elements.ModuleElement
import net.ccbluex.liquidbounce.ui.client.clickgui.style.Style
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.BlackStyle
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.LiquidBounceStyle
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.SlowlyStyle
import net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner
import net.ccbluex.liquidbounce.ui.font.AWTFontRenderer.Companion.assumeNonVolatile
import net.ccbluex.liquidbounce.utils.EntityUtils.targetAnimals
import net.ccbluex.liquidbounce.utils.EntityUtils.targetDead
import net.ccbluex.liquidbounce.utils.EntityUtils.targetInvisible
import net.ccbluex.liquidbounce.utils.EntityUtils.targetMobs
import net.ccbluex.liquidbounce.utils.EntityUtils.targetPlayer
import net.ccbluex.liquidbounce.utils.render.RenderUtils.deltaTime
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawImage
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.renderer.GlStateManager.disableLighting
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.util.ResourceLocation
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11.glScaled
import kotlin.math.roundToInt

object ClickGui : GuiScreen() {
    val panels = mutableListOf<Panel>()
    private val hudIcon = ResourceLocation("${CLIENT_NAME.lowercase()}/custom_hud_icon.png")
    private val settingsIcon = ResourceLocation("${CLIENT_NAME.lowercase()}/settings_icon.png")
    var style: Style = LiquidBounceStyle
    private var mouseX = 0
    private var mouseY = 0

    // Used when closing ClickGui using its key bind, prevents it from getting closed instantly.
    // Caused by keyTyped being called along with onKey that opens the ClickGui.
    private var ignoreClosing = false

    fun setDefault() {
        panels.clear()

        val width = 100
        val height = 18
        var yPos = 5

        for (category in ModuleCategory.values()) {
            panels.add(object : Panel(category.displayName, 100, yPos, width, height, false) {
                override val elements = moduleManager.modules.filter { it.category == category }.map { ModuleElement(it) }
            })

            yPos += 20
        }

        yPos += 20
        panels.add(object : Panel("Targets", 100, yPos, width, height, false) {
            override val elements = listOf(
                object : ButtonElement("Players") {
                    override val color
                        get() = if (targetPlayer) guiColor else Int.MAX_VALUE

                    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
                        if (mouseButton == 0 && isHovered(mouseX, mouseY)) {
                            targetPlayer = !targetPlayer
                            style.clickSound()
                            return true
                        }
                        return false
                    }
                },
                object : ButtonElement("Mobs") {
                    override val color
                        get() = if (targetMobs) guiColor else Int.MAX_VALUE

                    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
                        if (mouseButton == 0 && isHovered(mouseX, mouseY)) {
                            targetMobs = !targetMobs
                            style.clickSound()
                            return true
                        }
                        return false
                    }
                },
                object : ButtonElement("Animals") {
                    override val color
                        get() = if (targetAnimals) guiColor else Int.MAX_VALUE

                    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
                        if (mouseButton == 0 && isHovered(mouseX, mouseY)) {
                            targetAnimals = !targetAnimals
                            style.clickSound()
                            return true
                        }
                        return false
                    }
                },
                object : ButtonElement("Invisible") {
                    override val color
                        get() = if (targetInvisible) guiColor else Int.MAX_VALUE

                    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
                        if (mouseButton == 0 && isHovered(mouseX, mouseY)) {
                            targetInvisible = !targetInvisible
                            style.clickSound()
                            return true
                        }
                        return false
                    }
                },
                object : ButtonElement("Dead") {
                    override val color
                        get() = if (targetDead) guiColor else Int.MAX_VALUE

                    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
                        if (mouseButton == 0 && isHovered(mouseX, mouseY)) {
                            targetDead = !targetDead
                            style.clickSound()
                            return true
                        }
                        return false
                    }
                }
            )
        })
    }

    override fun drawScreen(x: Int, y: Int, partialTicks: Float) {
        // Enable DisplayList optimization
        assumeNonVolatile = true

        mouseX = (x / scale).roundToInt()
        mouseY = (y / scale).roundToInt()

        drawDefaultBackground()
        drawImage(hudIcon, 9, height - 41, 32, 32)
        drawImage(settingsIcon, 46, height - 41, 32, 32)

        val scale = scale.toDouble()
        glScaled(scale, scale, scale)

        for (panel in panels) {
            panel.updateFade(deltaTime)
            panel.drawScreenAndClick(mouseX, mouseY)
        }

        descriptions@ for (panel in panels.reversed()) {
            // Don't draw descriptions when hovering over a panel header.
            if (panel.isHovered(mouseX, mouseY))
                break

            for (element in panel.elements) {
                if (element is ModuleElement) {
                    if (element.isVisible && element.isHovered(mouseX, mouseY) && element.y <= panel.y + panel.fade) {
                        style.drawDescription(mouseX, mouseY, element.module.description)
                        // Don't draw descriptions for any module elements below.
                        break@descriptions
                    }
                }
            }
        }

        if (Mouse.hasWheel()) {
            val wheel = Mouse.getDWheel()
            if (wheel != 0) {
                var handledScroll = false

                // Handle foremost panel.
                for (panel in panels.reversed()) {
                    if (panel.handleScroll(mouseX, mouseY, wheel)) {
                        handledScroll = true
                        break
                    }
                }

                if (!handledScroll) handleScroll(wheel)
            }
        }

        disableLighting()
        RenderHelper.disableStandardItemLighting()
        glScaled(1.0, 1.0, 1.0)

        assumeNonVolatile = false

        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    private fun handleScroll(wheel: Int) {
        if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)) {
            scale += wheel * 0.0001f
        }
        else if (scrolls) {
            for (panel in panels) panel.y = panel.parseY(panel.y + wheel / 10)
        }
    }

    public override fun mouseClicked(x: Int, y: Int, mouseButton: Int) {
        if (mouseButton == 0 && x in 5..50 && y in height - 50..height - 5) {
            mc.displayGuiScreen(GuiHudDesigner())
            return
        } else if (mouseButton == 0 && x in 42..87 && y in height - 50..height - 5) {
            mc.displayGuiScreen(GuiClientSettings())
            return
        }

        mouseX = (x / scale).roundToInt()
        mouseY = (y / scale).roundToInt()

        // Handle foremost panel.
        panels.reversed().forEachIndexed { index, panel ->
            if (panel.mouseClicked(mouseX, mouseY, mouseButton))
                return

            panel.drag = false

            if (mouseButton == 0 && panel.isHovered(mouseX, mouseY)) {
                panel.x2 = panel.x - mouseX
                panel.y2 = panel.y - mouseY
                panel.drag = true

                // Move dragged panel to top.
                panels.removeAt(panels.lastIndex - index)
                panels.add(panel)
                return
            }
        }
    }

    public override fun mouseReleased(x: Int, y: Int, state: Int) {
        mouseX = (x / scale).roundToInt()
        mouseY = (y / scale).roundToInt()

        for (panel in panels) panel.mouseReleased(mouseX, mouseY, state)
    }

    override fun updateScreen() {
        if (style is SlowlyStyle || style is BlackStyle) {
            for (panel in panels) {
                for (element in panel.elements) {
                    if (element is ButtonElement)
                        element.hoverTime += if (element.isHovered(mouseX, mouseY)) 1 else -1

                    if (element is ModuleElement)
                        element.slowlyFade += if (element.module.state) 20 else -20
                }
            }
        }

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

    override fun doesGuiPauseGame() = false
}