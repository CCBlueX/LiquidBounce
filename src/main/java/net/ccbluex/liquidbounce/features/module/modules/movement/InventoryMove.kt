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
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.value.BoolValue
import net.minecraft.client.gui.GuiChat
import net.minecraft.client.gui.GuiIngameMenu
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.settings.GameSettings

object InventoryMove : Module("InventoryMove", ModuleCategory.MOVEMENT) {

    private val undetectable by BoolValue("Undetectable", false)
    val aacAdditionPro by BoolValue("AACAdditionPro", false)

    private val noMoveClicks by BoolValue("NoMoveClicks", false)
    private val noClicksAir by BoolValue("NoClicksInAir", false) { noMoveClicks }
    private val noClicksGround by BoolValue("NoClicksOnGround", true) { noMoveClicks }

    private val affectedBindings = arrayOf(
        mc.gameSettings.keyBindForward,
        mc.gameSettings.keyBindBack,
        mc.gameSettings.keyBindRight,
        mc.gameSettings.keyBindLeft,
        mc.gameSettings.keyBindJump,
        mc.gameSettings.keyBindSprint
    )

    fun canClickInventory() =
        !state || !isMoving || !noMoveClicks || (if (mc.thePlayer.onGround) !noClicksGround else !noClicksAir)

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (mc.currentScreen !is GuiChat && mc.currentScreen !is GuiIngameMenu && (!undetectable || mc.currentScreen !is GuiContainer)) {
            for (affectedBinding in affectedBindings) {
                val shouldExcept = Sprint.state && Sprint.mode == "Legit"
                affectedBinding.pressed = if (affectedBinding == affectedBindings.last() && shouldExcept) true else GameSettings.isKeyDown(affectedBinding)
            }
        }
    }

    @EventTarget
    fun onClick(event: ClickWindowEvent) {
        if (!canClickInventory())
            event.cancelEvent()
    }

    override fun onDisable() {
        val isIngame = mc.currentScreen != null

        for (affectedBinding in affectedBindings) {
            if (!GameSettings.isKeyDown(affectedBinding) || isIngame)
                affectedBinding.pressed = false
        }
    }

    override val tag
        get() = if (aacAdditionPro) "AACAdditionPro" else null
}
