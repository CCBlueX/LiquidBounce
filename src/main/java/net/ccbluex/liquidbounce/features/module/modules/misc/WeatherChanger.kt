package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.value.FloatValue

/**
 * LiquidBounce Hacked Client A minecraft forge injection client using Mixin
 *
 * @author CCBlueX
 * @game   Minecraft
 */
@ModuleInfo(name = "WeatherChanger", description = "", category = ModuleCategory.MISC)
class WeatherChanger : Module()
{
    private val rainStrength = FloatValue("RainStrength", 0.0F, 0.0F, 1.0F)
    private val thunderingStrength = FloatValue("ThunderingStrength", 0.0F, 0.0F, 1.0F)

    @EventTarget
    fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent)
    {
        val theWorld = mc.theWorld ?: return

        theWorld.setRainStrength(rainStrength.get())
        theWorld.setThunderingStrength(thunderingStrength.get())
    }

    @EventTarget
    fun onPacket(event: PacketEvent)
    {
        if (event.packet is SPacketChangeGameState && event.packet.asSPacketChangeGameState().gameState in arrayOf(1, 2, 7, 8)) event.cancelEvent()
    }
}
