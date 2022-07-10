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
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.minecraft.client.settings.GameSettings
import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos
import kotlin.math.ceil

@ModuleInfo(name = "Eagle", description = "Makes you eagle (a.k.a. FastBridge).", category = ModuleCategory.PLAYER)
class Eagle : Module()
{
    @EventTarget
    fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent)
    {
        val theWorld = mc.theWorld ?: return
        val thePlayer = mc.thePlayer ?: return

        mc.gameSettings.keyBindSneak.pressed = theWorld.getBlockState(BlockPos(thePlayer.posX, ceil(thePlayer.posY) - 1.0, thePlayer.posZ)).block == Blocks.air
    }

    override fun onDisable()
    {
        mc.thePlayer ?: return
        val gameSettings = mc.gameSettings

        if (GameSettings.isKeyDown(gameSettings.keyBindSneak)) gameSettings.keyBindSneak.pressed = false
    }
}
