/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.ClickWindowEvent
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.event.StrafeEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.ui.client.clickgui.ClickGui
import net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager.canClickInventory
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils.serverOpenInventory
import net.ccbluex.liquidbounce.value.BoolValue
import net.minecraft.client.gui.screen.ChatScreen
import net.minecraft.client.gui.screen.GameMenuScreen
import net.minecraft.client.gui.screen.ingame.ChestScreen
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import org.lwjgl.input.Mouse

object InventoryMove : Module("InventoryMove", Category.MOVEMENT, gameDetecting = false, hideModule = false) {

    private val notInChests by BoolValue("NotInChests", false)
    val aacAdditionPro by BoolValue("AACAdditionPro", false)
    private val intave by BoolValue("Intave", false)

    private val isIntave = (mc.currentScreen is InventoryScreen || mc.currentScreen is ChestScreen) && intave

    private val noMove by InventoryManager.noMoveValue
    private val noMoveAir by InventoryManager.noMoveAirValue
    private val noMoveGround by InventoryManager.noMoveGroundValue
    private val undetectable by InventoryManager.undetectableValue

        // If player violates nomove check and inventory is open, close inventory and reopen it when still
        private val silentlyCloseAndReopen by BoolValue("SilentlyCloseAndReopen", false)
            { noMove && (noMoveAir || noMoveGround) }
            // Reopen closed inventory just before a click (could flag for clicking too fast after opening inventory)
            private val reopenOnClick by BoolValue("ReopenOnClick", false)
                { silentlyCloseAndReopen && noMove && (noMoveAir || noMoveGround) }

    private val affectedBindings = arrayOf(
        mc.options.forwardKey,
        mc.options.backKey,
        mc.options.rightKey,
        mc.options.leftKey,
        mc.options.jumpKey,
        mc.options.sprintKey
    )

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val screen = mc.currentScreen

        // Don't make player move when chat or ESC menu are open
        if (screen is ChatScreen || screen is GameMenuScreen)
            return

        if (undetectable && (screen != null && screen !is GuiHudDesigner && screen !is ClickGui))
            return

        if (notInChests && screen is ChestScreen)
            return

        if (silentlyCloseAndReopen && screen is InventoryScreen) {
            if (canClickInventory(closeWhenViolating = true) && !reopenOnClick)
                serverOpenInventory = true
        }

        for (affectedBinding in affectedBindings)
            affectedBinding.pressed = isButtonPressed(affectedBinding)
                || (affectedBinding == mc.options.sprintKey && Sprint.handleEvents() && Sprint.mode == "Legit" && (!Sprint.onlyOnSprintPress || mc.player.isSprinting))
    }

    @EventTarget
    fun onStrafe(event: StrafeEvent) {
        if (isIntave) {
            mc.options.keyBindSneak.pressed = true
        }
    }

    @EventTarget
    fun onJump(event: JumpEvent) {
        if (isIntave) event.cancelEvent()
    }
    
    @EventTarget
    fun onClick(event: ClickWindowEvent) {
        if (!canClickInventory()) event.cancelEvent()
        else if (reopenOnClick) serverOpenInventory = true
    }

    override fun onDisable() {
        for (affectedBinding in affectedBindings)
            affectedBinding.pressed = isButtonPressed(affectedBinding)
    }

    private fun isButtonPressed(keyBinding: KeyBinding): Boolean {
        return if (keyBinding.keyCode < 0) {
            Mouse.isButtonDown(keyBinding.keyCode + 100)
        } else {
            GameSettings.isKeyDown(keyBinding)
        }
    }

    override val tag
        get() = if (aacAdditionPro) "AACAdditionPro" else null
}
