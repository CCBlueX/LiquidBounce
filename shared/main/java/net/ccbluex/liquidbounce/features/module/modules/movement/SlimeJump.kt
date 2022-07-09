/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.extensions.getBlock
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.ListValue

@ModuleInfo(name = "SlimeJump", description = "Allows you to to jump higher on slime blocks.", category = ModuleCategory.MOVEMENT)
class SlimeJump : Module()
{
    private val modeValue = ListValue("Mode", arrayOf("Set", "Add"), "Add")
    private val motionValue = FloatValue("Motion", 0.42f, 0.2f, 5f)

    @EventTarget
    fun onJump(event: JumpEvent)
    {
        val theWorld = mc.theWorld ?: return
        val thePlayer = mc.thePlayer ?: return

        if (classProvider.isBlockSlime(theWorld.getBlock(thePlayer.position.down())))
        {
            event.cancelEvent()

            when (modeValue.get().toLowerCase())
            {
                "set" -> thePlayer.motionY = motionValue.get().toDouble()
                "add" -> thePlayer.motionY += motionValue.get()
            }
        }
    }

    override val tag: String
        get() = "${if (modeValue.get().equals("Add", ignoreCase = true)) "+" else ""}${motionValue.get()}"
}
