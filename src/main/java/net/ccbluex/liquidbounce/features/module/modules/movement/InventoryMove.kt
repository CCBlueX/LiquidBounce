/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.ClickWindowEvent
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.extensions.createOpenInventoryPacket
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.isOpenInventoryPacket
import net.ccbluex.liquidbounce.utils.extensions.sendPacketWithoutEvent
import net.ccbluex.liquidbounce.value.BoolValue
import net.minecraft.client.gui.GuiChat
import net.minecraft.client.gui.GuiIngameMenu
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.client.settings.GameSettings
import net.minecraft.network.play.client.C0DPacketCloseWindow

@ModuleInfo(name = "InventoryMove", description = "Allows you to walk while an inventory is opened.", category = ModuleCategory.MOVEMENT)
class InventoryMove : Module()
{
    private val undetectable = BoolValue("Undetectable", false)
    val aacAdditionProValue = BoolValue("AACAdditionPro", false)
    private val blockPacketsValue = BoolValue("BlockPackets", true)
    private val noMoveClicksValue = BoolValue("NoMoveClicks", false)
    private val onlyInventoryValue = BoolValue("OnlyInventory", false)

    private val affectedBindings = run {
        val gameSettings = mc.gameSettings
        arrayOf(gameSettings.keyBindForward, gameSettings.keyBindBack, gameSettings.keyBindRight, gameSettings.keyBindLeft, gameSettings.keyBindJump, gameSettings.keyBindSprint)
    }

    @EventTarget
    fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent)
    {
        val currentScreen = mc.currentScreen
        if (currentScreen !is GuiChat && currentScreen !is GuiIngameMenu && (!undetectable.get() || currentScreen !is GuiContainer) && (!onlyInventoryValue.get() || currentScreen !is GuiContainer || currentScreen is GuiInventory)) for (affectedBinding in affectedBindings) affectedBinding.pressed = GameSettings.isKeyDown(affectedBinding)
    }

    @EventTarget
    fun onPacket(event: PacketEvent)
    {
        if (!blockPacketsValue.get()) return

        val packet = event.packet

        if (isOpenInventoryPacket(packet)) event.cancelEvent()
        else if (packet is C0DPacketCloseWindow && packet.windowId == 0) event.cancelEvent()
    }

    @EventTarget
    fun onClick(event: ClickWindowEvent)
    {
        val cancel = noMoveClicksValue.get() && (mc.thePlayer ?: return).isMoving
        val simulateInventory = blockPacketsValue.get() && !cancel

        // Open inventory
        if (simulateInventory) mc.netHandler.networkManager.sendPacketWithoutEvent(createOpenInventoryPacket())

        if (cancel) event.cancelEvent()

        // Close inventory
        if (simulateInventory) mc.netHandler.networkManager.sendPacketWithoutEvent(C0DPacketCloseWindow(0))
    }

    override fun onDisable()
    {
        val isIngame = mc.currentScreen != null

        affectedBindings.filter { !GameSettings.isKeyDown(it) || isIngame }.forEach { it.pressed = false }
    }

    override val tag: String?
        get() = if (aacAdditionProValue.get()) "AACAdditionPro" else null
}
