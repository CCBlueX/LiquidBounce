package net.ccbluex.liquidbounce.utils.item

import com.viaversion.viaversion.api.connection.UserConnection
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper
import com.viaversion.viaversion.api.type.Type
import com.viaversion.viaversion.protocols.protocol1_12to1_11_1.Protocol1_12To1_11_1
import com.viaversion.viaversion.protocols.protocol1_9_3to1_9_1_2.ServerboundPackets1_9_3
import io.netty.util.AttributeKey
import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.block.Blocks
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket
import net.minecraft.util.Hand


fun convertClientSlotToServerSlot(slot: Int, screen: GenericContainerScreen? = null): Int {
    if (screen == null) {
        return when (slot) {
            in 0..8 -> 36 + slot
            in 9..35 -> slot
            in 36..39 -> 39 - slot + 5
            40 -> 45
            else -> throw IllegalArgumentException("Invalid slot $slot")
        }
    } else {
        val stacks = screen.screenHandler.rows * 9

        return when (slot) {
            in 0..8 -> stacks + 27 + slot
            in 9..35 -> stacks + slot - 9
            else -> throw IllegalArgumentException("Invalid slot $slot")
        }
    }
}

/**
 * Sends an open inventory packet using ViaFabricPlus code. This is only for older versions.
 */

// https://github.com/FlorianMichael/ViaFabricPlus/blob/602d723945d011d7cd9ca6f4ed7312d85f9bdf36/src/main/java/de/florianmichael/viafabricplus/injection/mixin/fixes/minecraft/MixinMinecraftClient.java#L118-L130
fun openInventorySilently() {
    runCatching {
        val isViaFabricPlusLoaded = AttributeKey.exists("viafabricplus-via-connection")

        if (!isViaFabricPlusLoaded) {
            return
        }

        val localViaConnection = AttributeKey.valueOf<UserConnection>("viafabricplus-via-connection")

        val viaConnection = mc.networkHandler?.connection?.channel?.attr(localViaConnection)?.get() ?: return

        if (viaConnection.protocolInfo.pipeline.contains(Protocol1_12To1_11_1::class.java)) {
            viaConnection.channel?.eventLoop()?.submit {
                val clientStatus = PacketWrapper.create(ServerboundPackets1_9_3.CLIENT_STATUS, viaConnection)
                clientStatus.write(Type.VAR_INT, 2) // Open Inventory Achievement

                runCatching {
                    clientStatus.sendToServer(Protocol1_12To1_11_1::class.java)
                }
            }
        }
    }
}

inline fun runWithOpenedInventory(f: () -> Unit) {
    val isInInventory = mc.currentScreen is InventoryScreen

    if (!isInInventory) {
        openInventorySilently()
    }

    f()

    if (!isInInventory) {
        mc.networkHandler!!.sendPacket(CloseHandledScreenC2SPacket(0))
    }

}

fun clickHotbarOrOffhand(item: Int) {
    when (item) {
        40 -> clickOffHand()
        else -> clickHotbar(item)
    }
}

private fun clickHotbar(item: Int) {
    val player = mc.player!!
    val network = mc.networkHandler!!

    if (item != player.inventory.selectedSlot) {
        network.sendPacket(UpdateSelectedSlotC2SPacket(item))
    }

    val interact = mc.interactionManager!!.interactItem(player, Hand.MAIN_HAND)

    if (interact.shouldSwingHand()) {
        player.swingHand(Hand.MAIN_HAND)
    }

    if (item != player.inventory.selectedSlot) {
        network.sendPacket(UpdateSelectedSlotC2SPacket(player.inventory.selectedSlot))
    }
}


fun clickOffHand() {
    val interact = mc.interactionManager!!.interactItem(mc.player!!, Hand.OFF_HAND)

    if (interact.shouldSwingHand()) {
        mc.player!!.swingHand(Hand.OFF_HAND)
    }
}

/**
 * A list of blocks, which are useless, so inv cleaner and scaffold won't count them as blocks
 */
var DISALLOWED_BLOCKS_TO_PLACE = hashSetOf(Blocks.CAKE, Blocks.TNT, Blocks.SAND, Blocks.CACTUS, Blocks.ANVIL)

/**
 * Configurable to configure the dynamic rotation engine
 */
class InventoryConstraintsConfigurable : Configurable("InventoryConstraints") {
    internal var delay by intRange("Delay", 2..4, 0..20)
    internal val invOpen by boolean("InvOpen", false)
    internal val noMove by boolean("NoMove", false)
}

data class ItemStackWithSlot(val slot: Int, val itemStack: ItemStack)