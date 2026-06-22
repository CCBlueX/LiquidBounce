/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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

package net.ccbluex.liquidbounce.utils.network

import it.unimi.dsi.fastutil.objects.ObjectRBTreeSet
import net.ccbluex.fastutil.enumMapOf
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.resources.Identifier

/**
 * A registry for packet types, allowing registration of packet identifiers
 * for both clientbound and serverbound packets.
 * This is used to keep track of which packets are registered for each side of the network.
 *
 * Be aware that serverbound means packets sent from the client to the server (C2S),
 * and clientbound means packets sent from the server to the client (S2C).
 */
val packetRegistry = enumMapOf<PacketFlow, MutableSet<Identifier>> { _ -> ObjectRBTreeSet() }
