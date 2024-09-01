/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.ClientShutdownEvent
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.potion.Potion
import net.minecraft.potion.PotionEffect

object Fullbright : Module("Fullbright", Category.RENDER, gameDetecting = false, hideModule = false) {
    private val mode by ListValue("Mode", arrayOf("Gamma", "NightVision"), "Gamma")
    private var prevGamma = -1f

    override fun onEnable() {
        prevGamma = mc.options.gammaSetting
    }

    override fun onDisable() {
        if (prevGamma == -1f)
            return

        mc.options.gammaSetting = prevGamma
        prevGamma = -1f

        mc.player?.removePotionEffectClient(Potion.nightVision.id)
    }

    @EventTarget(ignoreCondition = true)
    fun onUpdate(event: UpdateEvent) {
        if (state || XRay.handleEvents()) {
            when (mode.lowercase()) {
                "gamma" -> when {
                    mc.options.gammaSetting <= 100f -> mc.options.gammaSetting++
                }
                "nightvision" -> mc.player?.addPotionEffect(PotionEffect(Potion.nightVision.id, 1337, 1))
            }
        } else if (prevGamma != -1f) {
            mc.options.gammaSetting = prevGamma
            prevGamma = -1f
        }
    }

    @EventTarget(ignoreCondition = true)
    fun onShutdown(event: ClientShutdownEvent) {
        onDisable()
    }
}