package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.events.HeypixelSWKillEvent
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.features.misc.FriendManager
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.client.notification
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.item.Items
import net.minecraft.screen.slot.SlotActionType

object ModuleAutoReport : ClientModule("AutoReport", Category.MISC) {

    private val reportedPlayers = mutableSetOf<String>()

    @Suppress("unused")
    private val autoReportHandler = sequenceHandler<HeypixelSWKillEvent> { event ->
        val victim = event.victim
        val killer = event.killer

        if (victim != player.name.string || FriendManager.isFriend(killer) || !reportedPlayers.add(killer)) return@sequenceHandler

        player.networkHandler?.sendChatCommand("report $killer")

        repeat(4) {
            waitTicks(5)

            val screen = mc.currentScreen as? GenericContainerScreen ?: return@repeat
            val swordSlot = screen.screenHandler.slots.find { it.stack.item == Items.DIAMOND_SWORD } ?: return@repeat

            interaction.clickSlot(
                screen.screenHandler.syncId,
                swordSlot.id,
                0,
                SlotActionType.PICKUP,
                player
            )

            notification(
                "AutoReport",
                "Reported $killer",
                NotificationEvent.Severity.INFO
            )
            mc.currentScreen = null
            return@sequenceHandler
        }
    }
}
