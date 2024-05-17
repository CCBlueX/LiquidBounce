/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.block.BlockSlime

object SlimeJump : Module("SlimeJump", Category.MOVEMENT) {

    private val motion by FloatValue("Motion", 0.42f, 0.2f..1f)
    private val mode by ListValue("Mode", arrayOf("Set", "Add"), "Add")

    @EventTarget
    fun onJump(event: JumpEvent) {
        val thePlayer = mc.thePlayer ?: return

        if (mc.thePlayer != null && mc.theWorld != null && getBlock(thePlayer.position.down()) is BlockSlime) {
            event.cancelEvent()

            when (mode.lowercase()) {
                "set" -> thePlayer.motionY = motion.toDouble()
                "add" -> thePlayer.motionY += motion
            }
        }
    }
}