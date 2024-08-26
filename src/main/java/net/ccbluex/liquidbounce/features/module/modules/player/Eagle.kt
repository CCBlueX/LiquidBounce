/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.client.option.GameOptions
import net.minecraft.init.Blocks.air
import net.minecraft.util.math.BlockPos

object Eagle : Module("Eagle", Category.PLAYER, hideModule = false) {

    private val sneakDelay by IntegerValue("SneakDelay", 0, 0..100)
    private val onlyWhenLookingDown by BoolValue("OnlyWhenLookingDown", false)
    private val lookDownThreshold by FloatValue("LookDownThreshold", 45f, 0f..90f) { onlyWhenLookingDown }

    private val sneakTimer = MSTimer()

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val thePlayer = mc.player ?: return

        if (thePlayer.onGround && getBlock(BlockPos(thePlayer).down()) == air) {
            if (!onlyWhenLookingDown || (onlyWhenLookingDown && theplayer.pitch >= lookDownThreshold)) {
                if (sneakTimer.hasTimePassed(sneakDelay)) {
                    mc.gameSettings.keyBindSneak.pressed = true
                    sneakTimer.reset()
                } else {
                    mc.gameSettings.keyBindSneak.pressed = false
                }
            } else {
                mc.gameSettings.keyBindSneak.pressed = false
            }
        } else {
            mc.gameSettings.keyBindSneak.pressed = false
        }
    }

    override fun onDisable() {
        if (mc.player == null)
            return

        if (!GameOptions.isKeyDown(mc.gameSettings.keyBindSneak))
            mc.gameSettings.keyBindSneak.pressed = false
    }
}
