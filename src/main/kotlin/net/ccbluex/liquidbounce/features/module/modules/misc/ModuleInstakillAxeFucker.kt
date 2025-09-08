
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.events.ChatReceiveEvent
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.ScheduleInventoryActionEvent
import net.ccbluex.liquidbounce.event.events.ScreenEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.player.cheststealer.features.FeatureSilentScreen
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.inventory.ContainerItemSlot
import net.ccbluex.liquidbounce.utils.inventory.InventoryAction
import net.ccbluex.liquidbounce.utils.inventory.InventoryConstraints
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.client.option.KeyBinding
import net.minecraft.item.Items
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket
import net.minecraft.screen.slot.Slot

object ModuleInstakillAxeFucker : ClientModule("InstakillAxeFucker", Category.MISC) {

    private val inventoryConstraints = tree(InventoryConstraints())
    private var paused = false
    private var hasPerformedActions = false
    private var tasking = false

    private fun Slot.toContainerItemSlot(): ContainerItemSlot = ContainerItemSlot(this.id)


    @Suppress("unused")
    private val scheduleInventoryAction = handler<ScheduleInventoryActionEvent> { event ->
        if (paused) return@handler

        val paperSlot = Slots.OffhandWithHotbar.findSlot(Items.MAP)?.hotbarSlotForServer
        if (paperSlot != null && player.inventory.main.any { it.item == Items.BOW }) {
            SilentHotbar.selectSlotSilently(this,paperSlot, 0)
        } else {
            return@handler
        }

        KeyBinding.setKeyPressed(mc.options.useKey.boundKey, true)
        tasking = true
        val screen = mc.currentScreen as? GenericContainerScreen ?: return@handler

        val chestSlot = screen.screenHandler.slots.firstOrNull { it.stack.item == Items.CHEST }
        if (chestSlot != null) {
            event.schedule(
                inventoryConstraints,
                InventoryAction.Click.performPickup(screen, chestSlot.toContainerItemSlot())
            )
        }

        val helmetSlot = screen.screenHandler.slots.firstOrNull { it.stack.item == Items.LEATHER_HELMET }
        if (helmetSlot != null) {
            event.schedule(
                inventoryConstraints,
                InventoryAction.Click.performPickup(screen, helmetSlot.toContainerItemSlot())
            )
        if (hasPerformedActions) {
                KeyBinding.setKeyPressed(mc.options.useKey.boundKey, false)
                event.schedule(inventoryConstraints, InventoryAction.CloseScreen(screen), Priority.IMPORTANT_FOR_PLAYER_LIFE)
                notification(
                    "InstakillAxeFucker", "InstakillAxe has been auto selected with no probability.",
                    NotificationEvent.Severity.INFO
                )
                tasking = false
                paused = true
            }
        }
    }

    @Suppress("unused")
    private val screenHandler = handler<ScreenEvent> { event ->
        FeatureSilentScreen.setHide("InstakillAxeFucker", tasking && event.screen is GenericContainerScreen)
    }


    @Suppress("unused")
    private val chatMonitor = sequenceHandler<ChatReceiveEvent> { event ->
        val localName = player.name.string
        val message = event.textData.string

        if (message.startsWith(localName) && message.contains("已经为", ignoreCase = true)) {
            hasPerformedActions = true
        }
    }

    @Suppress("unused")
    private val respawnListener = handler<PacketEvent> { event ->
        if (event.packet is PlayerRespawnS2CPacket) {
            paused = false
            tasking = false
            hasPerformedActions = false
        }
    }

    @Suppress("unused")
    private val worldChangeListener = handler<WorldChangeEvent> {
        paused = false
        tasking = false
        hasPerformedActions = false
    }
}
