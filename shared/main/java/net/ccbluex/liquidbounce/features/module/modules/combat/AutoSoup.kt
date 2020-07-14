/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.api.enums.EnumFacingType
import net.ccbluex.liquidbounce.api.enums.ItemType
import net.ccbluex.liquidbounce.api.enums.WEnumHand
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketPlayerDigging
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.InventoryUtils
import net.ccbluex.liquidbounce.utils.createOpenInventoryPacket
import net.ccbluex.liquidbounce.utils.createUseItemPacket
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue

@ModuleInfo(name = "AutoSoup", description = "Makes you automatically eat soup whenever your health is low.", category = ModuleCategory.COMBAT)
class AutoSoup : Module() {

    private val healthValue = FloatValue("Health", 15f, 0f, 20f)
    private val delayValue = IntegerValue("Delay", 150, 0, 500)
    private val openInventoryValue = BoolValue("OpenInv", false)
    private val simulateInventoryValue = BoolValue("SimulateInventory", true)
    private val bowlValue = ListValue("Bowl", arrayOf("Drop", "Move", "Stay"), "Drop")

    private val timer = MSTimer()

    override val tag: String
        get() = healthValue.get().toString()

    @EventTarget
    fun onUpdate(event: UpdateEvent?) {
        if (!timer.hasTimePassed(delayValue.get().toLong()))
            return

        val thePlayer = mc.thePlayer ?: return

        val soupInHotbar = InventoryUtils.findItem(36, 45, classProvider.getItemEnum(ItemType.MUSHROOM_STEW))

        if (thePlayer.health <= healthValue.get() && soupInHotbar != -1) {
            mc.netHandler.addToSendQueue(classProvider.createCPacketHeldItemChange(soupInHotbar - 36))
            mc.netHandler.addToSendQueue(createUseItemPacket(thePlayer.inventory.getStackInSlot(soupInHotbar), WEnumHand.MAIN_HAND))

            if (bowlValue.get().equals("Drop", true))
                mc.netHandler.addToSendQueue(classProvider.createCPacketPlayerDigging(ICPacketPlayerDigging.WAction.DROP_ITEM, WBlockPos.ORIGIN, classProvider.getEnumFacing(EnumFacingType.DOWN)))

            mc.netHandler.addToSendQueue(classProvider.createCPacketHeldItemChange(thePlayer.inventory.currentItem))
            timer.reset()
            return
        }

        val bowlInHotbar = InventoryUtils.findItem(36, 45, classProvider.getItemEnum(ItemType.BOWL))
        if (bowlValue.get().equals("Move", true) && bowlInHotbar != -1) {
            if (openInventoryValue.get() && !classProvider.isGuiInventory(mc.currentScreen))
                return

            var bowlMovable = false

            for (i in 9..36) {
                val itemStack = thePlayer.inventory.getStackInSlot(i)

                if (itemStack == null) {
                    bowlMovable = true
                    break
                } else if (itemStack.item == classProvider.getItemEnum(ItemType.BOWL) && itemStack.stackSize < 64) {
                    bowlMovable = true
                    break
                }
            }

            if (bowlMovable) {
                val openInventory = !classProvider.isGuiInventory(mc.currentScreen) && simulateInventoryValue.get()

                if (openInventory)
                    mc.netHandler.addToSendQueue(createOpenInventoryPacket())

                mc.playerController.windowClick(0, bowlInHotbar, 0, 1, thePlayer)
            }
        }

        val soupInInventory = InventoryUtils.findItem(9, 36, classProvider.getItemEnum(ItemType.MUSHROOM_STEW))

        if (soupInInventory != -1 && InventoryUtils.hasSpaceHotbar()) {
            if (openInventoryValue.get() && !classProvider.isGuiInventory(mc.currentScreen))
                return

            val openInventory = !classProvider.isGuiInventory(mc.currentScreen) && simulateInventoryValue.get()
            if (openInventory)
                mc.netHandler.addToSendQueue(createOpenInventoryPacket())

            mc.playerController.windowClick(0, soupInInventory, 0, 1, thePlayer)

            if (openInventory)
                mc.netHandler.addToSendQueue(classProvider.createCPacketCloseWindow())

            timer.reset()
        }
    }

}