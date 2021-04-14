package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.client.gui.screen.ChatScreen
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.options.KeyBinding

object ModuleInventoryMove : Module("InventoryMove", Category.MOVEMENT) {

    val undetectable by boolean("Undetectable", true)
    val passthroughSneak by boolean("PassthroughSneak", false)

    fun handleInputs(keyBinding: KeyBinding) = enabled && mc.currentScreen !is ChatScreen &&
        (!undetectable || mc.currentScreen !is HandledScreen<*>) && (passthroughSneak || keyBinding != mc.options.keySneak)

}
