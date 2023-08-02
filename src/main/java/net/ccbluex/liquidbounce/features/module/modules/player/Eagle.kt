/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.minecraft.client.settings.GameSettings
import net.minecraft.init.Blocks.air
import net.minecraft.util.BlockPos

object Eagle : Module("Eagle", ModuleCategory.PLAYER) {

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val thePlayer = mc.thePlayer ?: return

        mc.gameSettings.keyBindSneak.pressed = mc.thePlayer.onGround && getBlock(BlockPos(thePlayer).down()) == air
    }

    override fun onDisable() {
        if (mc.thePlayer == null)
            return

        if (!GameSettings.isKeyDown(mc.gameSettings.keyBindSneak))
            mc.gameSettings.keyBindSneak.pressed = false
    }
}
