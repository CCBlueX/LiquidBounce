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
 *
 */

package net.ccbluex.liquidbounce.utils.client.vfp;

import com.viaversion.viafabricplus.ViaFabricPlus;
import com.viaversion.viaversion.api.minecraft.BlockPosition;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.packet.ServerboundPacketType;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocols.v1_8to1_9.packet.ServerboundPackets1_8;
import net.minecraft.util.math.BlockPos;

import java.util.function.Consumer;

/**
 * Compatibility layer for ViaFabricPlus on protocol 1.8
 * <p>
 * DO NOT CALL ANY OF THESE METHODS WITHOUT CHECKING IF VIAFABRICPLUS IS LOADED AND YOU ARE NOT
 * ON 1.8 PROTOCOL
 */
public enum VfpCompatibility1_8 {

    INSTANCE;

    public void sendSignUpdate(final BlockPos blockPos, final String[] lines) throws IllegalArgumentException {
        if (lines.length != 4) {
            throw new IllegalArgumentException("Lines length does not match 4");
        }

        writePacket(ServerboundPackets1_8.SIGN_UPDATE, packet -> {
            packet.write(Types.BLOCK_POSITION1_8, new BlockPosition(blockPos.getX(), blockPos.getY(), blockPos.getZ()));

            for (var line : lines) {
                packet.write(Types.STRING, line);
            }
        });
    }

    public void sendPlayerInput(Float sideways, Float forwards, boolean jumping, boolean sneaking) {
        writePacket(ServerboundPackets1_8.PLAYER_INPUT, packet -> {
            packet.write(Types.FLOAT, sideways);
            packet.write(Types.FLOAT, forwards);
            byte b = 0;
            if (jumping) {
                b = (byte)(b | 1);
            }

            if (sneaking) {
                b = (byte)(b | 2);
            }
            packet.write(Types.BYTE, b);
        });
    }

    private void writePacket(ServerboundPacketType packetType, Consumer<PacketWrapper> writer) {
        if (!VfpCompatibility.INSTANCE.isEqual1_8()) {
            throw new IllegalStateException("Not on 1.8 protocol");
        }

        var packet = PacketWrapper.create(packetType, ViaFabricPlus.getImpl().getPlayNetworkUserConnection());
        writer.accept(packet);
        packet.sendToServerRaw();
    }

}
