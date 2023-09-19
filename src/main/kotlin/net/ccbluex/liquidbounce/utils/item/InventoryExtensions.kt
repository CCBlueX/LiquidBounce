/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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

package net.ccbluex.liquidbounce.utils.item

import com.viaversion.viaversion.api.connection.UserConnection
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper
import com.viaversion.viaversion.api.type.Type
import com.viaversion.viaversion.protocols.protocol1_12to1_11_1.Protocol1_12To1_11_1
import com.viaversion.viaversion.protocols.protocol1_9_3to1_9_1_2.ServerboundPackets1_9_3
import io.netty.util.AttributeKey
import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleAutoArmor
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.block.Blocks
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket
import net.minecraft.screen.ScreenHandler
import net.minecraft.util.Hand

val ScreenHandler.isPlayerInventory: Boolean
    get() = this.syncId == 0