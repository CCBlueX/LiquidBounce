/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.value.BoolValue

@ModuleInfo(name = "TNTESP", description = "Allows you to see ignited TNT blocks through walls.", category = ModuleCategory.RENDER)
class TNTESP : Module()
{
    private val interpolateValue = BoolValue("Interpolate", true, description = "Interpolate tnt positions")

    @EventTarget
    fun onRender3D(@Suppress("UNUSED_PARAMETER") event: Render3DEvent)
    {
        val theWorld = mc.theWorld ?: return
        val partialTicks = if (interpolateValue.get()) event.partialTicks else 1f
        theWorld.loadedEntityList.filter(classProvider::isEntityTNTPrimed).forEach { RenderUtils.drawEntityBox(it, 0x40FF0000, 0, false, partialTicks) }
    }
}
