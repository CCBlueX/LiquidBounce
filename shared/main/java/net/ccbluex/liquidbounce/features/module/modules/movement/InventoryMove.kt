/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.ClickWindowEvent
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.value.BoolValue

@ModuleInfo(name = "InventoryMove", description = "Allows you to walk while an inventory is opened.", category = ModuleCategory.MOVEMENT)
class InventoryMove : Module() {

    private val undetectable = BoolValue("Undetectable", false)
    val aacAdditionProValue = BoolValue("AACAdditionPro", false)
    private val noMoveClicksValue = BoolValue("NoMoveClicks", false)

    private val affectedBindings = arrayOf(
            mc.gameSettings.keyBindForward,
            mc.gameSettings.keyBindBack,
            mc.gameSettings.keyBindRight,
            mc.gameSettings.keyBindLeft,
            mc.gameSettings.keyBindJump,
            mc.gameSettings.keyBindSprint
    )

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        tick()
    }

    fun tick() {
        if (!classProvider.isGuiChat(mc.currentScreen) && !classProvider.isGuiIngameMenu(mc.currentScreen) && (!undetectable.get() || !classProvider.isGuiContainer(mc.currentScreen))) {
            for (affectedBinding in affectedBindings) {
                affectedBinding.pressed = mc.gameSettings.isKeyDown(affectedBinding)
            }
        }
    }

    @EventTarget
    fun onClick(event: ClickWindowEvent) {
        if (noMoveClicksValue.get() && MovementUtils.isMoving)
            event.cancelEvent()
    }

    override fun onDisable() {
        val isIngame = mc.currentScreen != null

        for (affectedBinding in affectedBindings) {
            if (!mc.gameSettings.isKeyDown(affectedBinding) || isIngame)
                affectedBinding.pressed = false
        }
    }

    override val tag: String?
        get() = if (aacAdditionProValue.get()) "AACAdditionPro" else null
}
