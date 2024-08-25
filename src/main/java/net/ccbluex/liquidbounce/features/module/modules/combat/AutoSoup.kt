/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.extensions.sendUseItem
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils.isFirstInventoryClick
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils.serverOpenInventory
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils.serverSlot
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction

object AutoSoup : Module("AutoSoup", Category.COMBAT, hideModule = false) {

    private val health by FloatValue("Health", 15f, 0f..20f)
    private val delay by IntegerValue("Delay", 150, 0..500)

    private val openInventory by BoolValue("OpenInv", true)
    private val startDelay by IntegerValue("StartDelay", 100, 0..1000) { openInventory }
    private val autoClose by BoolValue("AutoClose", false) { openInventory }
    private val autoCloseDelay by IntegerValue("CloseDelay", 500, 0..1000) { openInventory && autoClose }

    private val simulateInventory by BoolValue("SimulateInventory", false) { !openInventory }

    private val bowl by ListValue("Bowl", arrayOf("Drop", "Move", "Stay"), "Drop")

    private val timer = MSTimer()
    private val startTimer = MSTimer()
    private val closeTimer = MSTimer()

    private var canCloseInventory = false

    override val tag
        get() = health.toString()

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val thePlayer = mc.player ?: return

        if (!timer.hasTimePassed(delay))
            return

        val soupInHotbar = InventoryUtils.findItem(36, 44, Items.MUSHROOM_STEW)

        if (thePlayer.health <= health && soupInHotbar != null) {
            sendPacket(UpdateSelectedSlotC2SPacket(soupInHotbar - 36))

            thePlayer.sendUseItem(thePlayer.inventory.main[serverSlot])

            // Schedule slot switch the next tick as we violate vanilla logic if we do it now.
            TickScheduler += {
                if (bowl == "Drop")
                    sendPacket(PlayerActionC2SPacket(PlayerActionC2SPacket.Action.DROP_ITEM, BlockPos.ORIGIN, Direction.DOWN))

                TickScheduler += {
                    serverSlot = thePlayer.inventory.selectedSlot
                }
            }

            timer.reset()
            return
        }

        val bowlInHotbar = InventoryUtils.findItem(36, 44, Items.BOWL)

        if (bowl == "Move" && bowlInHotbar != null) {
            if (openInventory && mc.currentScreen !is InventoryScreen)
                return

            var bowlMovable = false

            for (i in 9..36) {
                val itemStack = thePlayer.inventory.getInvStack(i)

                if (itemStack == null || (itemStack.item == Items.BOWL && itemStack.count < 64)) {
                    bowlMovable = true
                    break
                }
            }

            if (bowlMovable) {
                if (simulateInventory)
                    serverOpenInventory = true

                mc.interactionManager.clickSlot(0, bowlInHotbar, 0, 1, thePlayer)
            }
        }

        val soupInInventory = InventoryUtils.findItem(9, 35, Items.MUSHROOM_STEW)

        if (soupInInventory != null && InventoryUtils.hasSpaceInHotbar()) {
            if (isFirstInventoryClick && !startTimer.hasTimePassed(startDelay)) {
                // InventoryScreen checks, have to be put separately due to problem with reseting timer.
                if (mc.currentScreen is InventoryScreen)
                    return
            } else {
                // InventoryScreen checks, have to be put separately due to problem with reseting timer.
                if (mc.currentScreen is InventoryScreen)
                    isFirstInventoryClick = false

                startTimer.reset()
            }

            if (openInventory && mc.currentScreen !is InventoryScreen)
                return

            canCloseInventory = false

            if (simulateInventory)
                serverOpenInventory = true

            mc.interactionManager.clickSlot(0, soupInInventory, 0, 1, thePlayer)

            if (simulateInventory && mc.currentScreen !is InventoryScreen)
                serverOpenInventory = false

            timer.reset()
            closeTimer.reset()
        } else {
            canCloseInventory = true
        }

        if (autoClose && canCloseInventory && closeTimer.hasTimePassed(autoCloseDelay)) {
            if (mc.currentScreen is InventoryScreen) {
                mc.player?.closeScreen()
            }
            closeTimer.reset()
            canCloseInventory = false
        }
    }
}