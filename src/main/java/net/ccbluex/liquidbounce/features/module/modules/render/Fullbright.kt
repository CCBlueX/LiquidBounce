/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.minecraft.potion.PotionType
import net.ccbluex.liquidbounce.event.ClientShutdownEvent
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.value.ListValue

@ModuleInfo(name = "Fullbright", description = "Brightens up the world around you.", category = ModuleCategory.RENDER)
class Fullbright : Module() {
    private val modeValue = ListValue("Mode", arrayOf("Gamma", "NightVision"), "Gamma")
    private var prevGamma = -1f

    override fun onEnable() {
        prevGamma = mc.gameSettings.gammaSetting
    }

    override fun onDisable() {
        if (prevGamma == -1f)
            return

        mc.gameSettings.gammaSetting = prevGamma
        prevGamma = -1f

        mc.thePlayer?.removePotionEffectClient(classProvider.getPotionEnum(PotionType.NIGHT_VISION).id)
    }

    @EventTarget(ignoreCondition = true)
    fun onUpdate(event: UpdateEvent?) {
        if (state || LiquidBounce.moduleManager.getModule(XRay::class.java)!!.state) {
            when (modeValue.get().toLowerCase()) {
                "gamma" -> when {
                    mc.gameSettings.gammaSetting <= 100f -> mc.gameSettings.gammaSetting++
                }
                "nightvision" -> mc.thePlayer?.addPotionEffect(classProvider.createPotionEffect(classProvider.getPotionEnum(PotionType.NIGHT_VISION).id, 1337, 1))
            }
        } else if (prevGamma != -1f) {
            mc.gameSettings.gammaSetting = prevGamma
            prevGamma = -1f
        }
    }

    @EventTarget(ignoreCondition = true)
    fun onShutdown(event: ClientShutdownEvent?) {
        onDisable()
    }
}