package net.ccbluex.liquidbounce.features.module.modules.player


import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.utils.combat.findEnemy
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.OffHandSlot
import net.ccbluex.liquidbounce.utils.inventory.findClosestSlot
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction


object ModuleAutoSwap : ClientModule("AutoSwap", Category.PLAYER) {
    private val range by float("Range", 6.0f, 1.0f..20.0f)
    private val switchDelay by int("SwitchDelay", 2, 0..10, "ticks")

    internal val ignoreOpenInventory by boolean("IgnoreOpenInventory", true)

    private var lastSwitchTick = 0

    @Suppress("unused")
    private val tickHandler = tickHandler {

        if (ignoreOpenInventory && mc.currentScreen != null) {
            return@tickHandler
        }

        if (lastSwitchTick > 0) {
            lastSwitchTick--
            return@tickHandler
        }

        val enemy = world.findEnemy(0f..range)
        if (enemy != null) {

            val snowballSlot = Slots.OffhandWithHotbar.findClosestSlot(Items.SNOWBALL)
            val eggSlot = Slots.OffhandWithHotbar.findClosestSlot(Items.EGG)

            when {
                snowballSlot != null && snowballSlot != OffHandSlot ->
                    performSmartSwitch(snowballSlot)
                eggSlot != null && eggSlot != OffHandSlot ->
                    performSmartSwitch(eggSlot)
            }

        } else {

            val gappleSlot = Slots.OffhandWithHotbar.findClosestSlot(Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE)
            if (gappleSlot != null && gappleSlot != OffHandSlot) {
                performSmartSwitch(gappleSlot)
            }
        }
    }
    
    private fun performSmartSwitch(targetSlot: HotbarItemSlot) {
        val selectedSlot = player.inventory.selectedSlot
        val targetHotbarSlot = targetSlot.hotbarSlot

        if (selectedSlot != targetHotbarSlot) {
            network.sendPacket(UpdateSelectedSlotC2SPacket(targetHotbarSlot))
        }

        network.sendPacket(
            PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND,
                BlockPos.ORIGIN,
                Direction.DOWN
            )
        )

        if (selectedSlot != targetHotbarSlot) {
            network.sendPacket(UpdateSelectedSlotC2SPacket(selectedSlot))
        }

        lastSwitchTick = switchDelay
    }
}
