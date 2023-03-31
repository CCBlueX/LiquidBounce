/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.clickgui

import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_NAME
import net.ccbluex.liquidbounce.LiquidBounce.moduleManager
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI.guiColor
import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI.scaleValue
import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI.scrollsValue
import net.ccbluex.liquidbounce.file.FileManager.clickGuiConfig
import net.ccbluex.liquidbounce.file.FileManager.saveConfig
import net.ccbluex.liquidbounce.ui.client.clickgui.elements.ButtonElement
import net.ccbluex.liquidbounce.ui.client.clickgui.elements.ModuleElement
import net.ccbluex.liquidbounce.ui.client.clickgui.style.Style
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
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class ClickGui : GuiScreen() {
    val panels = mutableListOf<Panel>()
    private val hudIcon = ResourceLocation("${CLIENT_NAME.lowercase()}/custom_hud_icon.png")
    var style: Style = LiquidBounceStyle
    var mouseX = 0
    var mouseY = 0

    init {
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

        mouseX = (x / scaleValue.get()).roundToInt()
        mouseY = (y / scaleValue.get()).roundToInt()

        drawDefaultBackground()
        drawImage(hudIcon, 9, height - 41, 32, 32)

        val scale = scaleValue.get().toDouble()
        glScaled(scale, scale, scale)

        for (panel in panels) {
            panel.updateFade(deltaTime)
            panel.drawScreenAndClick(mouseX, mouseY)
        }

        for (panel in panels) {
            for (element in panel.elements) {
                if (element is ModuleElement) {
                    if (element.isVisible && element.isHovered(mouseX, mouseY) && element.y <= panel.y + panel.fade)
                        style.drawDescription(mouseX, mouseY, element.module.description)
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
            scaleValue.set(scaleValue.get() + wheel * 0.0001)
        }
        else if (scrollsValue.get()) {
            for (panel in panels) panel.y += wheel / 10
        }
    }

    public override fun mouseClicked(x: Int, y: Int, mouseButton: Int) {
        if (mouseButton == 0 && x in 5..50 && y in height - 50..height - 5) {
            mc.displayGuiScreen(GuiHudDesigner())
            return
        }

        mouseX = (x / scaleValue.get()).roundToInt()
        mouseY = (y / scaleValue.get()).roundToInt()

        // Handle foremost panel.
        for (panel in panels.reversed()) {
            if (panel.mouseClicked(mouseX, mouseY, mouseButton))
                return

            panel.drag = false

            if (mouseButton == 0 && panel.isHovered(mouseX, mouseY)) {
                panel.x2 = panel.x - mouseX
                panel.y2 = panel.y - mouseY
                panel.drag = true
                return
            }
        }
    }

    public override fun mouseReleased(x: Int, y: Int, state: Int) {
        mouseX = (x / scaleValue.get()).roundToInt()
        mouseY = (y / scaleValue.get()).roundToInt()

        for (panel in panels) panel.mouseReleased(mouseX, mouseY, state)
    }

    override fun updateScreen() {
        if (style is SlowlyStyle) {
            for (panel in panels) {
                for (element in panel.elements) {
                    if (element is ButtonElement) {
                        if (element.isHovered(mouseX, mouseY)) {
                            if (element.hoverTime < 7) element.hoverTime++
                        } else if (element.hoverTime > 0) element.hoverTime--
                    }

                    if (element is ModuleElement) {
                        if (element.module.state) {
                            if (element.slowlyFade < 255)
                                element.slowlyFade = min(element.slowlyFade + 20, 255)
                        } else if (element.slowlyFade > 0)
                            element.slowlyFade = max(0, element.slowlyFade - 20)
                    }
                }
            }
        }

        super.updateScreen()
    }

    override fun onGuiClosed() {
        saveConfig(clickGuiConfig)
        for (panel in panels) panel.fade = 0
    }

    override fun doesGuiPauseGame() = false
}