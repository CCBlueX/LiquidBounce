/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FontValue
import net.ccbluex.liquidbounce.value.ValueGroup

/**
 * TODO
 * * Add more notification options
 */
@ModuleInfo(name = "HUD", description = "Toggles visibility of the HUD.", category = ModuleCategory.RENDER, array = false)
class HUD : Module()
{
    val blackHotbarValue = BoolValue("BlackHotbar", true)
    val inventoryParticle = BoolValue("InventoryParticle", false)
    private val blurValue = BoolValue("Blur", false)
    private val notificationGroup = ValueGroup("Notification")
    val notificationAlertsValue = BoolValue("Alerts", true, "Alerts")
    val notificationWorldChangeValue = BoolValue("WorldChange", true, "WorldChangeAlerts")
    val notificationModuleManagerValue = BoolValue("ModuleToggle", true) // TODO: ModuleToggleNotificationKeepTime
    val fontChatValue = BoolValue("FontChat", false)
    val chatFontValue = FontValue("FontChatFont", Fonts.font40)

    init
    {
        notificationGroup.addAll(notificationAlertsValue, notificationWorldChangeValue, notificationModuleManagerValue)
    }

    @EventTarget
    fun onRender2D(@Suppress("UNUSED_PARAMETER") event: Render2DEvent)
    {
        if (classProvider.isGuiHudDesigner(mc.currentScreen)) return

        LiquidBounce.hud.render(false)
    }

    @EventTarget
    fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent)
    {
        LiquidBounce.hud.update()
    }

    @EventTarget
    fun onKey(event: KeyEvent)
    {
        LiquidBounce.hud.handleKey('a', event.key)
    }

    @EventTarget(ignoreCondition = true)
    fun onScreen(event: ScreenEvent)
    {
        mc.theWorld ?: return
        mc.thePlayer ?: return

        val entityRenderer = mc.entityRenderer

        val provider = classProvider

        val screen = event.guiScreen

        if (state && blurValue.get() && !entityRenderer.isShaderActive() && screen != null && !provider.isGuiChat(screen) && !provider.isGuiHudDesigner(screen)) entityRenderer.loadShader(provider.createResourceLocation(LiquidBounce.CLIENT_NAME.toLowerCase() + "/blur.json"))
        else
        {
            val shaderGroup = entityRenderer.shaderGroup

            if (shaderGroup != null && shaderGroup.shaderGroupName.contains("liquidbounce/blur.json")) entityRenderer.stopUseShader()
        }
    }

    init
    {
        state = true
    }
}
