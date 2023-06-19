package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.entity.eyes

object ModuleTest : Module("Test", Category.WORLD) {
    override fun enable() {
        chat("your - ${RotationManager.serverRotation.toString()}")
        chat(player.blockPos.add(-1, -1, 0).toString())
        chat(RotationManager.aimToBlock(mc.player!!.eyes, player.blockPos.add(-1, -1, 0), 4.5).toString())
        super.enable()
    }
}
