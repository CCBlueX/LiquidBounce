/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.features.module.modules.world.packetmine.tool

import it.unimi.dsi.fastutil.ints.IntObjectImmutablePair
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleAutoTool
import net.ccbluex.liquidbounce.features.module.modules.world.packetmine.MineTarget
import net.ccbluex.liquidbounce.features.module.modules.world.packetmine.ModulePacketMine
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.utils.inventory.ClickInventoryAction
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.network.PickFromInventoryPacket
import net.ccbluex.liquidbounce.utils.network.sendPacket
import net.minecraft.item.ItemStack

enum class SwitchMethod(override val choiceName: String, val shouldSync: Boolean) : NamedChoice, MinecraftShortcuts {

    NORMAL("Normal", true) {

        override fun switch(slot: IntObjectImmutablePair<ItemStack>, mineTarget: MineTarget) {
            if (ModuleAutoTool.running) {
                ModuleAutoTool.switchToBreakBlock(mineTarget.targetPos)
                return
            }

            SilentHotbar.selectSlotSilently(ModulePacketMine, slot.firstInt(), 1)
        }

        override fun switchBack() {
            // nothing, handled by SilentHotbar
        }

    },

    @Suppress("unused")
    SWAP("Swap", false) {

        override fun switch(slot: IntObjectImmutablePair<ItemStack>, mineTarget: MineTarget) {
            val selectedSlot = SilentHotbar.serversideSlot
            val desiredSlot = slot.firstInt()
            if (selectedSlot == desiredSlot) {
                return
            }


            exchanged = desiredSlot
            ClickInventoryAction.performSwap(
                from = Slots.Hotbar.slots[desiredSlot],
                to = Slots.Hotbar.slots[selectedSlot]
            ).performAction()
        }

        override fun switchBack() {
            val desiredSlot = exchanged ?: return
            val selectedSlot = SilentHotbar.serversideSlot
            exchanged = null
            ClickInventoryAction.performSwap(
                from = Slots.Hotbar.slots[desiredSlot],
                to = Slots.Hotbar.slots[selectedSlot]
            ).performAction()
        }

    },

    /**
     * Only works before 1.21.3.
     */
    @Suppress("unused")
    PICK("Pick", false) {

        override fun switch(slot: IntObjectImmutablePair<ItemStack>, mineTarget: MineTarget) {
            if (!usesViaFabricPlus) {
                chat(warning(ModulePacketMine.message("noVfp")))
                ModulePacketMine.disable()
                return
            }

            exchanged = slot.firstInt()
            network.sendPacket(
                PickFromInventoryPacket(slot.firstInt() - 1),
                onFailure = {
                    chat(
                        markAsError(
                            "Failed to pick an item from your inventory using ViaFabricPlus, report to developers!"
                        )
                    )
                    exchanged = null
                }
            )
        }

        override fun switchBack() {
            if (!usesViaFabricPlus) {
                return
            }

            // TODO make it not mess up the hotbar
            exchanged?.let { network.sendPacket(PickFromInventoryPacket(it)) }
            exchanged = null
        }

    };

    var exchanged: Int? = null

    abstract fun switch(slot: IntObjectImmutablePair<ItemStack>, mineTarget: MineTarget)

    abstract fun switchBack()

    fun reset() {
        exchanged = null
    }

}
