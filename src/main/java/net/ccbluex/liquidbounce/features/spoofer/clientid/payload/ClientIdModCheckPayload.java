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
package net.ccbluex.liquidbounce.features.spoofer.clientid.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ClientIdModCheckPayload(String uuid) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ClientIdModCheckPayload> ID = ClientIdPayloads.type("modcheck");
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientIdModCheckPayload> CODEC =
        ClientIdPayloads.stringCodec(ClientIdModCheckPayload::uuid, ClientIdModCheckPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

}
