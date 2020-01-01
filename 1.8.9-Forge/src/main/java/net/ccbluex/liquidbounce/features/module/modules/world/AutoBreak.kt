package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.minecraft.client.settings.GameSettings
import net.minecraft.init.Blocks

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
@ModuleInfo(name = "AutoBrechen", description = "Automatically breaks the block you are looking at.", category = ModuleCategory.WORLD)
class AutoBreak : Module() {

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (mc.objectMouseOver == null || mc.objectMouseOver.blockPos == null)
            return

        mc.gameSettings.keyBindAttack.pressed = mc.theWorld.getBlockState(mc.objectMouseOver.blockPos).block != Blocks.air
    }

    override fun onDisable() {
        if (!GameSettings.isKeyDown(mc.gameSettings.keyBindAttack))
            mc.gameSettings.keyBindAttack.pressed = false
    }
}
