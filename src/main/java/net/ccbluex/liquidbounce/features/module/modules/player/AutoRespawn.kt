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
import net.ccbluex.liquidbounce.features.module.modules.exploit.Ghost
import net.ccbluex.liquidbounce.value.BoolValue
import net.minecraft.client.gui.GuiGameOver

object AutoRespawn : Module("AutoRespawn", ModuleCategory.PLAYER) {

    private val instant by BoolValue("Instant", true)

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val thePlayer = mc.thePlayer

        if (thePlayer == null || Ghost.state)
            return

        if (if (instant) mc.thePlayer.health == 0F || mc.thePlayer.isDead else mc.currentScreen is GuiGameOver
                    && (mc.currentScreen as GuiGameOver).enableButtonsTimer >= 20) {
            thePlayer.respawnPlayer()
            mc.displayGuiScreen(null)
        }
    }
}