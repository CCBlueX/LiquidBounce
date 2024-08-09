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
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.block.BlockPane
import net.minecraft.util.BlockPos

object HighJump : Module("HighJump", Category.MOVEMENT) {
    private val mode by ListValue("Mode", arrayOf("Vanilla", "Damage", "AACv3", "DAC", "Mineplex"), "Vanilla")
        private val height by FloatValue("Height", 2f, 1.1f..5f) { mode in arrayOf("Vanilla", "Damage") }

    private val glass by BoolValue("OnlyGlassPane", false)

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val player = mc.thePlayer

        if (glass && getBlock(BlockPos(player)) !is BlockPane)
            return

        when (mode.lowercase()) {
            "damage" -> if (player.hurtTime > 0 && player.onGround) player.motionY += 0.42f * height
            "aacv3" -> if (!player.onGround) player.motionY += 0.059
            "dac" -> if (!player.onGround) player.motionY += 0.049999
            "mineplex" -> if (!player.onGround) strafe(0.35f)
        }
    }

    @EventTarget
    fun onMove(event: MoveEvent) {
        val player = mc.thePlayer ?: return

        if (glass && getBlock(BlockPos(player)) !is BlockPane)
            return
        if (!player.onGround) {
            if ("mineplex" == mode.lowercase()) {
                player.motionY += if (player.fallDistance == 0f) 0.0499 else 0.05
            }
        }
    }

    @EventTarget
    fun onJump(event: JumpEvent) {
        val player = mc.thePlayer ?: return

        if (glass && getBlock(BlockPos(player)) !is BlockPane)
            return
        when (mode.lowercase()) {
            "vanilla" -> event.motion *= height
            "mineplex" -> event.motion = 0.47f
        }
    }

    override val tag
        get() = mode
}