/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.block.BlockPane
import net.minecraft.util.BlockPos

@ModuleInfo(name = "HighJump", description = "Allows you to jump higher.", category = ModuleCategory.MOVEMENT)
class HighJump : Module() {
    private val modeValue = ListValue("Mode", arrayOf("Vanilla", "Damage", "AACv3", "DAC", "Mineplex"), "Vanilla")
    private val heightValue = object : FloatValue("Height", 2f, 1.1f, 5f) {
        override fun isSupported() = modeValue.get() in setOf("Vanilla", "Damage")
    }
    private val glassValue = BoolValue("OnlyGlassPane", false)

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val thePlayer = mc.thePlayer

        if (glassValue.get() && getBlock(BlockPos(thePlayer)) !is BlockPane)
            return

        when (modeValue.get().lowercase()) {
            "damage" -> if (thePlayer.hurtTime > 0 && thePlayer.onGround) thePlayer.motionY += 0.42f * heightValue.get()
            "aacv3" -> if (!thePlayer.onGround) thePlayer.motionY += 0.059
            "dac" -> if (!thePlayer.onGround) thePlayer.motionY += 0.049999
            "mineplex" -> if (!thePlayer.onGround) strafe(0.35f)
        }
    }

    @EventTarget
    fun onMove(event: MoveEvent) {
        val thePlayer = mc.thePlayer ?: return

        if (glassValue.get() && getBlock(BlockPos(thePlayer)) !is BlockPane)
            return
        if (!thePlayer.onGround) {
            if ("mineplex" == modeValue.get().lowercase()) {
                thePlayer.motionY += if (thePlayer.fallDistance == 0f) 0.0499 else 0.05
            }
        }
    }

    @EventTarget
    fun onJump(event: JumpEvent) {
        val thePlayer = mc.thePlayer ?: return

        if (glassValue.get() && getBlock(BlockPos(thePlayer)) !is BlockPane)
            return
        when (modeValue.get().lowercase()) {
            "vanilla" -> event.motion = event.motion * heightValue.get()
            "mineplex" -> event.motion = 0.47f
        }
    }

    override val tag
        get() = modeValue.get()
}