package net.ccbluex.liquidbounce.utils.item

import com.viaversion.viaversion.api.connection.UserConnection
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper
import com.viaversion.viaversion.api.type.Type
import com.viaversion.viaversion.protocols.protocol1_12to1_11_1.Protocol1_12To1_11_1
import com.viaversion.viaversion.protocols.protocol1_9_3to1_9_1_2.ServerboundPackets1_9_3
import io.netty.util.AttributeKey
import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.*
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.minecraft.block.Blocks
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket
import net.minecraft.registry.Registries
import net.minecraft.util.Hand

/**
 * Contains all container slots in inventory. (hotbar, offhand, inventory, armor)
 */
val ALL_SLOTS_IN_INVENTORY: List<ItemSlot> = run {
    val hotbarItems = (0 until 9).map { HotbarItemSlot(it) }
    val offHandItem = listOf(OffHandSlot)
    val inventoryItems = (0 until 27).map { InventoryItemSlot(it) }
    val armorItems = (0 until 4).map { ArmorItemSlot(it) }

    return@run hotbarItems + offHandItem + inventoryItems + armorItems
}

fun findNonEmptySlotsInInventory(): List<ItemSlot> {
    return ALL_SLOTS_IN_INVENTORY.filter { !it.itemStack.isEmpty }
}

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

fun convertServerSlotToClientSlot(slot: Int): Int {
    return when (slot) {
        in 36..44 -> slot - 36
        in 9..35 -> slot
        in 5..8 -> 39 - slot + 5
        45 -> 40
        else -> throw IllegalArgumentException("Invalid slot $slot")
    }
}

/**
 * Sends an open inventory packet using ViaFabricPlus code. This is only for older versions.
 */

// https://github.com/ViaVersion/ViaFabricPlus/blob/d6c8501fa908520f99676aefa46dcc20de2840a6/src/main/java/de/florianmichael/viafabricplus/injection/mixin/fixes/minecraft/MixinMinecraftClient.java#L128-L143
fun openInventorySilently() {
    if (InventoryTracker.isInventoryOpenServerSide) {
        return
    }

    runCatching {
        val isViaFabricPlusLoaded = AttributeKey.exists("viafabricplus-via-connection")

        if (!isViaFabricPlusLoaded) {
            return
        }

        val localViaConnection = AttributeKey.valueOf<UserConnection>("viafabricplus-via-connection")

        val viaConnection = mc.networkHandler?.connection?.channel?.attr(localViaConnection)?.get() ?: return

        if (viaConnection.protocolInfo.pipeline.contains(Protocol1_12To1_11_1::class.java)) {
            val clientStatus = PacketWrapper.create(ServerboundPackets1_9_3.CLIENT_STATUS, viaConnection)
            clientStatus.write(Type.VAR_INT, 2) // Open Inventory Achievement

            runCatching {
                clientStatus.sendToServer(Protocol1_12To1_11_1::class.java)
            }.onSuccess {
                InventoryTracker.isInventoryOpenServerSide = true
            }
        }
    }
}

inline fun runWithOpenedInventory(closeInventory: () -> Boolean = { true }) {
    val isInInventory = InventoryTracker.isInventoryOpenServerSide

    if (!isInInventory) {
        openInventorySilently()
    }

    val shouldClose = closeInventory()

    if (shouldClose) {
        mc.networkHandler?.sendPacket(CloseHandledScreenC2SPacket(0))
    }
}

fun useHotbarSlotOrOffhand(item: HotbarItemSlot) {
    // We assume whatever called this function passed the isHotBar check
    when (item) {
        OffHandSlot -> {
            interactItem(Hand.OFF_HAND)
        }

        else -> {
            interactItem(Hand.MAIN_HAND) {
                SilentHotbar.selectSlotSilently(null, item.hotbarSlotForServer, 1)
            }
        }
    }
}

fun interactItem(hand: Hand, preInteraction: () -> Unit = { }) {
    val player = mc.player ?: return
    val interaction = mc.interactionManager ?: return

    preInteraction()

    interaction.interactItem(player, hand).let {
        if (it.shouldSwingHand()) {
            player.swingHand(hand)
        }
    }
}

fun findBlocksEndingWith(vararg targets: String) =
    Registries.BLOCK.filter { block -> targets.any { Registries.BLOCK.getId(block).path.endsWith(it.lowercase()) } }

/**
 * A list of blocks, which are useless, so inv cleaner and scaffold won't count them as blocks
 */
var DISALLOWED_BLOCKS_TO_PLACE = hashSetOf(Blocks.CAKE, Blocks.TNT, Blocks.SAND, Blocks.CACTUS, Blocks.ANVIL)

/**
 * Configurable to configure the dynamic rotation engine
 */
class InventoryConstraintsConfigurable : Configurable("InventoryConstraints") {
    internal val delay by intRange("Delay", 2..4, 0..20)
    internal val invOpen by boolean("InvOpen", false)
    internal val noMove by boolean("NoMove", false)
    internal val noRotation by boolean("NoRotation", false) // This should be visible only when NoMove is enabled

    val violatesNoMove
        get() = noMove && (mc.player?.moving == true || noRotation
            && (RotationManager.currentRotation ?: mc.player.rotation) != RotationManager.serverRotation)
}

data class ItemStackWithSlot(val slot: Int, val itemStack: ItemStack)
