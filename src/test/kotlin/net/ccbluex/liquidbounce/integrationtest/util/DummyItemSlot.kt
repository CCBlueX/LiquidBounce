package net.ccbluex.liquidbounce.integrationtest.util

import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemSlot
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemSlotType
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.item.ItemStack

internal class DummyItemSlot(
    override val itemStack: ItemStack,
    override val slotType: ItemSlotType,
    val name: String
) :
    ItemSlot() {
    private val id = Object()

    override fun getIdForServer(screen: GenericContainerScreen?) = throw UnsupportedOperationException()

    override fun hashCode(): Int = id.hashCode()

    override fun equals(other: Any?): Boolean = this === other

    override fun toString(): String {
        return "DummySlot('${this.name}')"
    }
}
