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
 *
 */

package net.ccbluex.liquidbounce.event.events

import io.netty.channel.ChannelPipeline
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.CancellableEvent
import net.ccbluex.liquidbounce.event.Event
import net.ccbluex.liquidbounce.utils.client.Nameable
import net.ccbluex.liquidbounce.utils.client.PacketQueueManager
import net.minecraft.network.packet.Packet

@Nameable("pipeline")
class PipelineEvent(val channelPipeline: ChannelPipeline, val local: Boolean) : Event()

@Nameable("packet")
class PacketEvent(val origin: TransferOrigin, val packet: Packet<*>, val original: Boolean = true) : CancellableEvent()

@Nameable("queuePacket")
class QueuePacketEvent(
    val packet: Packet<*>?,
    val origin: TransferOrigin,
    var action: PacketQueueManager.Action = PacketQueueManager.Action.FLUSH
) : Event()

enum class TransferOrigin(override val choiceName: String) : NamedChoice {
    INCOMING("Incoming"),
    OUTGOING("Outgoing");
}
