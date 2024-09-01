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
import net.ccbluex.liquidbounce.features.module.modules.exploit.Ghost
import net.ccbluex.liquidbounce.value.BoolValue
import net.minecraft.client.gui.screen.DeathScreen

object AutoRespawn : Module("AutoRespawn", Category.PLAYER, gameDetecting = false, hideModule = false) {

    private val instant by BoolValue("Instant", true)

    private var enableButtonsTimer = 0

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val player = mc.player

        if (player == null || Ghost.handleEvents())
            return

        if (mc.currentScreen is DeathScreen) {
            enableButtonsTimer++
        }

        if (if (instant) mc.player.health == 0F || !mc.player.isAlive else mc.currentScreen is DeathScreen
                    && enableButtonsTimer >= 20) {
            player.requestRespawn()
            mc.setScreen(null)
        }
    }

    override fun onEnable() {
        enableButtonsTimer = 0
    }
}