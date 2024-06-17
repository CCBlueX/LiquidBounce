package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.DummyEvent
import net.ccbluex.liquidbounce.event.Sequence
import net.ccbluex.liquidbounce.event.events.ChatReceiveEvent
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.event.SuspendableHandler
import net.ccbluex.liquidbounce.event.events.ScheduleInventoryActionEvent
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ContainerItemSlot
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.utils.inventory.*
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.screen.slot.SlotActionType

object ModuleAutoKit : Module("AutoKit", Category.MISC) {

    var sequence: Sequence<DummyEvent>? = null

    // We can receive chat messages before the world is initialized,
    // so we have to handel events even before the that
    override fun handleEvents() = enabled

    private fun removeFormatting(message: String): String {
        return message.replace("§[0-9a-fk-or]", "")
    }

    private fun startDelayedAction(action: SuspendableHandler<DummyEvent>) {
        // cancel the previous sequence
        ModuleAutoAccount.sequence?.cancel()

        //start the new sequence
        ModuleAutoAccount.sequence = Sequence(this, {
            waitUntil { mc.networkHandler != null }
            sync()
            waitTicks(1)

            action(it)
        }, DummyEvent())
    }

    private fun getKitDaddy(){
        SilentHotbar.selectSlotSilently(this, 1, 5)

    }

    @Suppress("unused")
    val onChat = handler<ChatReceiveEvent> { event ->
        val msg = removeFormatting(event.message)
        if (msg.contains("Cages open in: 10 seconds")){
            notification("Auto Kit", "Open msg found.",
                NotificationEvent.Severity.INFO)
            startDelayedAction { getKitDaddy() }
        }
    }





    /**
     * @return the chest screen if it is open and the title matches the chest title
     */
    private fun getChestScreen(): GenericContainerScreen? {
        val screen = mc.currentScreen

        return if (screen is GenericContainerScreen && screen.title.string == "Kits") {
            screen
        }else{null}
    }

    @Suppress("unused")
    val scheduleInventoryAction = handler<ScheduleInventoryActionEvent> { event ->
        // Check if we are in a chest screen
        val screen = getChestScreen() ?: return@handler

        //ClickInventoryAction.click(screen, ContainerItemSlot(1),0, SlotActionType.PICKUP)
        //ClickInventoryAction.click(screen, ContainerItemSlot(2),0, SlotActionType.PICKUP)
    }

}
