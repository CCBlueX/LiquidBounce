package net.ccbluex.liquidbounce.features.module.modules.player.autoqueue.presets

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.ScheduleInventoryActionEvent
import net.ccbluex.liquidbounce.event.events.ScreenEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.player.autoqueue.ModuleAutoQueue
import net.ccbluex.liquidbounce.features.module.modules.player.cheststealer.features.FeatureSilentScreen
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.inventory.*
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.client.option.KeyBinding
import net.minecraft.item.Items
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket
import net.minecraft.screen.slot.SlotActionType

object AutoQueueKKCraftSW : Choice("KKCraftSW") {

    override val parent: ChoiceConfigurable<*>
        get() = ModuleAutoQueue.presets

    private val hasPaper: Boolean
        get() = Slots.Hotbar.findSlot(Items.PAPER) != null

    private var tasking = false

    @Suppress("unused")
    private val repeatable = tickHandler {
        if (!player.isSpectator && !hasPaper) {
            return@tickHandler
        }
        val paperSlot = Slots.OffhandWithHotbar.findSlot(Items.PAPER)
        if (paperSlot == null) {
            return@tickHandler
        }
        SilentHotbar.selectSlotSilently(ModuleAutoQueue, paperSlot, 0)
        KeyBinding.setKeyPressed(mc.options.useKey.boundKey, true)
        tasking = true
    }

    @Suppress("unused")
    private val screenHandler = handler<ScreenEvent> { event ->
        FeatureSilentScreen.setHide("AutoQueue", tasking && event.screen is GenericContainerScreen)
    }

    @Suppress("unused")
    private val scheduleInventoryHandler = handler<ScheduleInventoryActionEvent> { event ->
        if (!player.isSpectator && !hasPaper) {
            return@handler
        }

        val screen = mc.currentScreen as? GenericContainerScreen ?: return@handler
        val fireworkSlot = screen.screenHandler.slots.firstOrNull { it.stack.item == Items.FIREWORK_ROCKET } ?: return@handler

        event.schedule(
            InventoryConstraints(),
            ClickInventoryAction.click(screen, ContainerItemSlot(fireworkSlot.id), 0, SlotActionType.PICKUP)
        )

        KeyBinding.setKeyPressed(mc.options.useKey.boundKey, false)
        event.schedule(
            InventoryConstraints(),
            CloseContainerAction(screen).also {
                tasking = false
            }
        )
    }

    @Suppress("unused")
    private val respawnListener = handler<PacketEvent> { event ->
        if (event.packet is PlayerRespawnS2CPacket) {
         tasking = false
        }
    }

    @Suppress("unused")
    private val worldChangeListener = handler<WorldChangeEvent> {
         tasking = false
    }
}
