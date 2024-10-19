/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_NAME
import net.ccbluex.liquidbounce.LiquidBounce.hud
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner
import net.ccbluex.liquidbounce.ui.client.hud.element.Element.Companion.MAX_GRADIENT_COLORS
import net.ccbluex.liquidbounce.utils.render.ColorSettingsFloat
import net.ccbluex.liquidbounce.utils.render.ColorSettingsInteger
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.client.gui.GuiChat
import net.minecraft.util.ResourceLocation

object HUD : Module("HUD", Category.RENDER, defaultInArray = false, gameDetecting = false, hideModule = true) {
    val customHotbar by BoolValue("CustomHotbar", true)

    val roundedHotbarRadius by FloatValue("RoundedHotbar-Radius", 0F, 0F..5F) { customHotbar }

    val hotbarMode by ListValue("Hotbar-Color", arrayOf("Custom", "Rainbow", "Gradient"), "Custom") { customHotbar }

    val hbHighlightColors = ColorSettingsInteger(this, "Hotbar-Highlight-Colors", applyMax = true)
    { customHotbar }.with(a = 100)
    val hbBackgroundColors = ColorSettingsInteger(this, "Hotbar-Background-Colors")
    { customHotbar && hotbarMode == "Custom" }.with(a = 100)

    val gradientHotbarSpeed by FloatValue("Hotbar-Gradient-Speed", 1f, 0.5f..10f)
    { customHotbar && hotbarMode == "Gradient" }

    val maxHotbarGradientColors by IntegerValue("Max-Hotbar-Gradient-Colors", 4, 1..MAX_GRADIENT_COLORS)
    { customHotbar && hotbarMode == "Gradient" }
    val bgGradColors = ColorSettingsFloat.create(this, "Hotbar-Gradient")
    { customHotbar && hotbarMode == "Gradient" && it <= maxHotbarGradientColors }

    val hotbarBorder by FloatValue("HotbarBorder-Width", 0.5F, 0.5F..5F) { customHotbar }
    val hbBorderColors = ColorSettingsInteger(this, "HotbarBorder", zeroAlphaCheck = true) { customHotbar }.with(a = 0)

    val rainbowX by FloatValue("Rainbow-X", -1000F, -2000F..2000F) { customHotbar && hotbarMode == "Rainbow" }
    val rainbowY by FloatValue("Rainbow-Y", -1000F, -2000F..2000F) { customHotbar && hotbarMode == "Rainbow" }
    val gradientX by FloatValue("Gradient-X", -1000F, -2000F..2000F) { customHotbar && hotbarMode == "Gradient" }
    val gradientY by FloatValue("Gradient-Y", -1000F, -2000F..2000F) { customHotbar && hotbarMode == "Gradient" }

    val inventoryParticle by BoolValue("InventoryParticle", false)
    private val blur by BoolValue("Blur", false)
    val fontChat by BoolValue("FontChat", false)

    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        if (mc.currentScreen is GuiHudDesigner)
            return

        hud.render(false)
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) = hud.update()

    @EventTarget
    fun onKey(event: KeyEvent) = hud.handleKey('a', event.key)

    @EventTarget(ignoreCondition = true)
    fun onScreen(event: ScreenEvent) {
        if (mc.theWorld == null || mc.thePlayer == null) return
        if (state && blur && !mc.entityRenderer.isShaderActive && event.guiScreen != null &&
                !(event.guiScreen is GuiChat || event.guiScreen is GuiHudDesigner)) mc.entityRenderer.loadShader(
            ResourceLocation(CLIENT_NAME.lowercase() + "/blur.json")
        ) else if (mc.entityRenderer.shaderGroup != null &&
            "liquidbounce/blur.json" in mc.entityRenderer.shaderGroup.shaderGroupName) mc.entityRenderer.stopUseShader()
    }

    init {
        state = true
    }
}