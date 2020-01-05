package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.InventoryUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.init.Items
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.client.C09PacketHeldItemChange
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
@ModuleInfo(name = "AutoSoup", description = "Makes you automatically eat soup whenever your health is low.", category = ModuleCategory.COMBAT)
class AutoSoup : Module() {
    private val healthValue = FloatValue("Health", 15f, 0f, 20f)
    private val delayValue = IntegerValue("Delay", 150, 0, 500)
    private val openInventoryValue = BoolValue("OpenInv", false)
    private val msTimer = MSTimer()

    @EventTarget
    fun onUpdate(event: UpdateEvent?) {
        if (!msTimer.hasTimePassed(delayValue.get().toLong()))
            return

        val soupInHotbar = InventoryUtils.findItem(36, 45, Items.mushroom_stew)

        if (mc.thePlayer.health <= healthValue.get() && soupInHotbar != -1) {
            mc.netHandler.addToSendQueue(C09PacketHeldItemChange(soupInHotbar - 36))
            mc.netHandler.addToSendQueue(C08PacketPlayerBlockPlacement(mc.thePlayer.inventoryContainer.getSlot(soupInHotbar).stack))
            mc.netHandler.addToSendQueue(C07PacketPlayerDigging(C07PacketPlayerDigging.Action.DROP_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
            mc.netHandler.addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))

            msTimer.reset()

            return
        }

        val soupInInventory = InventoryUtils.findItem(9, 36, Items.mushroom_stew)

        if (soupInInventory != -1 && InventoryUtils.hasSpaceHotbar()) {
            if (openInventoryValue.get() && mc.currentScreen !is GuiInventory)
                return

            mc.playerController.windowClick(0, soupInInventory, 0, 1, mc.thePlayer)

            msTimer.reset()
        }
    }

    override val tag: String
        get() = healthValue.get().toString()
}