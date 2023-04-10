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
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner
import net.ccbluex.liquidbounce.value.BoolValue
import net.minecraft.client.gui.GuiChat
import net.minecraft.util.ResourceLocation

object HUD : Module("HUD", "Toggles visibility of the HUD.", ModuleCategory.RENDER, defaultInArray = false) {
    val blackHotbarValue = BoolValue("BlackHotbar", true)
    val inventoryParticle = BoolValue("InventoryParticle", false)
    private val blurValue = BoolValue("Blur", false)
    val fontChatValue = BoolValue("FontChat", false)

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
        if (state && blurValue.get() && !mc.entityRenderer.isShaderActive && event.guiScreen != null &&
                !(event.guiScreen is GuiChat || event.guiScreen is GuiHudDesigner)) mc.entityRenderer.loadShader(
            ResourceLocation(CLIENT_NAME.lowercase() + "/blur.json")
        ) else if (mc.entityRenderer.shaderGroup != null &&
            "liquidbounce/blur.json" in mc.entityRenderer.shaderGroup.shaderGroupName) mc.entityRenderer.stopUseShader()
    }

    init {
        state = true
    }
}