package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.minecraft.client.Minecraft
import net.minecraft.potion.Potion

@ModuleInfo(name = "AntiLevitation", description = "Removes Levitation potion effect.", category = ModuleCategory.MOVEMENT)
class AntiLevitation : Module() {
    @EventTarget
    fun onMotion(event: MotionEvent) {
        if(event.eventState == EventState.PRE) {
            if (Minecraft.getMinecraft().player.isPotionActive(Potion.getPotionById(25)))
                Minecraft.getMinecraft().player.removeActivePotionEffect(Potion.getPotionById(25))
        }
    }
}