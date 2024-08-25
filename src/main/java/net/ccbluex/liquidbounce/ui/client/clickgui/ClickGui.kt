/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.clickgui

import kotlinx.coroutines.*
import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_NAME
import net.ccbluex.liquidbounce.LiquidBounce.moduleManager
import net.ccbluex.liquidbounce.api.ClientApi
import net.ccbluex.liquidbounce.api.autoSettingsList
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI
import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI.guiColor
import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI.scale
import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI.scrolls
import net.ccbluex.liquidbounce.file.FileManager.clickGuiConfig
import net.ccbluex.liquidbounce.file.FileManager.saveConfig
import net.ccbluex.liquidbounce.ui.client.clickgui.elements.ButtonElement
import net.ccbluex.liquidbounce.ui.client.clickgui.elements.ModuleElement
import net.ccbluex.liquidbounce.ui.client.clickgui.style.Style
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.BlackStyle
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.LiquidBounceStyle
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.SlowlyStyle
import net.ccbluex.liquidbounce.ui.client.hud.HUD
import net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification
import net.ccbluex.liquidbounce.ui.font.AWTFontRenderer.Companion.assumeNonVolatile
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.ClientUtils.displayChatMessage
import net.ccbluex.liquidbounce.utils.EntityUtils.targetAnimals
import net.ccbluex.liquidbounce.utils.EntityUtils.targetDead
import net.ccbluex.liquidbounce.utils.EntityUtils.targetInvisible
import net.ccbluex.liquidbounce.utils.EntityUtils.targetMobs
import net.ccbluex.liquidbounce.utils.EntityUtils.targetPlayer
import net.ccbluex.liquidbounce.utils.MinecraftInstance.Companion.mc
import net.ccbluex.liquidbounce.utils.SettingsUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.deltaTime
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawImage
import net.minecraft.client.audio.PositionedSoundRecord
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.render.GlStateManager.disableLighting
import net.minecraft.client.render.RenderHelper
import net.minecraft.util.Identifier
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11.glScaled
import kotlin.math.roundToInt

object ClickGui : Screen() {

    val panels = mutableListOf<Panel>()
    private val hudIcon = Identifier("${CLIENT_NAME.lowercase()}/custom_hud_icon.png")
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

    fun setDefault() {
        panels.clear()

        val width = 100
        val height = 18
        var yPos = 5

        for (category in Category.entries) {
            panels += object : Panel(category.displayName, 100, yPos, width, height, false) {
                override val elements =
                    moduleManager.modules.filter { it.category == category }.map { ModuleElement(it) }
            }

            yPos += 20
        }

        yPos += 20
        panels += setupTargetsPanel(100, yPos, width, height)

        // Settings Panel
        yPos += 20
        panels += setupSettingsPanel(100, yPos, width, height)
    }

    private fun setupTargetsPanel(xPos: Int = 100, yPos: Int, width: Int, height: Int) =
        object : Panel("Targets", xPos, yPos, width, height, false) {

            override val elements = listOf(
                ButtonElement("Players", { if (targetPlayer) guiColor else Int.MAX_VALUE }) {
                    targetPlayer = !targetPlayer
                },
                ButtonElement("Mobs", { if (targetMobs) guiColor else Int.MAX_VALUE }) {
                    targetMobs = !targetMobs
                },
                ButtonElement("Animals", { if (targetAnimals) guiColor else Int.MAX_VALUE }) {
                    targetAnimals = !targetAnimals
                },
                ButtonElement("Invisible", { if (targetInvisible) guiColor else Int.MAX_VALUE }) {
                    targetInvisible = !targetInvisible
                },
                ButtonElement("Dead", { if (targetDead) guiColor else Int.MAX_VALUE }) {
                    targetDead = !targetDead
                },
            )

        }

    private fun setupSettingsPanel(xPos: Int = 100, yPos: Int, width: Int, height: Int) =
        object : Panel("Auto Settings", xPos, yPos, width, height, false) {

            /**
             * Auto settings list
             */
            override val elements = runBlocking {
                async(Dispatchers.IO) {
                    autoSettingsList?.map { setting ->
                        ButtonElement(setting.name, { Integer.MAX_VALUE }) {
                            GlobalScope.launch {
                                try {
                                    displayChatMessage("Loading settings...")

                                    // Load settings and apply them
                                    val settings = ClientApi.requestSettingsScript(setting.settingId)

                                    displayChatMessage("Applying settings...")
                                    SettingsUtils.applyScript(settings)

                                    displayChatMessage("§6Settings applied successfully")
                                    HUD.addNotification(Notification("Updated Settings"))
                                    mc.soundHandler.playSound(
                                        PositionedSoundRecord.create(
                                            Identifier("random.anvil_use"), 1F
                                        )
                                    )
                                } catch (e: Exception) {
                                    ClientUtils.LOGGER.error("Failed to load settings", e)
                                    displayChatMessage("Failed to load settings: ${e.message}")
                                }
                            }
                        }.apply {
                            this.hoverText = buildString {
                                appendLine("§7Description: §e${setting.description.ifBlank { "No description available" }}")
                                appendLine("§7Type: §e${setting.type.displayName}")
                                appendLine("§7Contributors: §e${setting.contributors}")
                                appendLine("§7Last updated: §e${setting.date}")
                                append("§7Status: §e${setting.statusType.displayName} §a(${setting.statusDate})")
                            }
                        }
                    } ?: emptyList()
                }.await()
            }
        }

    override fun drawScreen(x: Int, y: Int, partialTicks: Float) {
        // Enable DisplayList optimization
        assumeNonVolatile = true

        mouseX = (x / scale).roundToInt()
        mouseY = (y / scale).roundToInt()

        drawDefaultBackground()
        drawImage(hudIcon, 9, height - 41, 32, 32)

        val scale = scale.toDouble()
        glScaled(scale, scale, scale)

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

            for (panel in panels) {
                panel.x = panel.parseX()
                panel.y = panel.parseY()
            }

        } else if (scrolls) {
            for (panel in panels) panel.y = panel.parseY(panel.y + wheel / 10)
        }
    }

    public override fun mouseClicked(x: Int, y: Int, mouseButton: Int) {
        if (mouseButton == 0 && x in 5..50 && y in height - 50..height - 5) {
            mc.displayGuiScreen(GuiHudDesigner())
            return
        }

        mouseX = (x / scale).roundToInt()
        mouseY = (y / scale).roundToInt()

        // Handle foremost panel.
        panels.reversed().forEachIndexed { index, panel ->
            if (panel.mouseClicked(mouseX, mouseY, mouseButton)) return

            panel.drag = false

            if (mouseButton == 0 && panel.isHovered(mouseX, mouseY)) {
                panel.x2 = panel.x - mouseX
                panel.y2 = panel.y - mouseY
                panel.drag = true

                // Move dragged panel to top.
                panels.removeAt(panels.lastIndex - index)
                panels += panel
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
                    if (element is ButtonElement) element.hoverTime += if (element.isHovered(mouseX, mouseY)) 1 else -1

                    if (element is ModuleElement) element.slowlyFade += if (element.module.state) 20 else -20
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

    fun Int.clamp(min: Int, max: Int): Int = this.coerceIn(min, max.coerceAtLeast(0))

    override fun doesGuiPauseGame() = false
}