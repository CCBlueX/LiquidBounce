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
package net.ccbluex.liquidbounce.injection.mixins.viaversion;

import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.api.protocol.packet.PacketType;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.exception.InformativeException;
import com.viaversion.viaversion.protocols.v1_16_1to1_16_2.packet.ServerboundPackets1_16_2;
import com.viaversion.viaversion.protocols.v1_21_2to1_21_4.packet.ServerboundPackets1_21_4;
import com.viaversion.viaversion.protocols.v1_9_1to1_9_3.packet.ServerboundPackets1_9_3;
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.viaversion.viaversion.protocol.packet.PacketWrapperImpl", remap = false)
public abstract class MixinPacketWrapper {

    @Shadow
    public abstract @Nullable PacketType getPacketType();

    @Shadow
    public abstract <T> T get(Type<T> type, int index) throws InformativeException;

    @Inject(method = "scheduleSendToServer", at = @At("HEAD"), cancellable = true)
    private void vfpSendPacketHandler(Class<? extends Protocol> protocol, boolean skipCurrentPipeline, CallbackInfo ci) {
        PacketType packetType = this.getPacketType();

        /*
          An injection that prevents ViaFabricPlus from sending an inventory packet twice.
          <p>
          This can be caused when inventory-managing modules silently open inventory and the user tries to manually open
          their inventory, which results in the same packet being sent twice.
         */
        if (packetType == ServerboundPackets1_9_3.CLIENT_COMMAND && this.get(Types.VAR_INT, 0) == 2 &&
                InventoryManager.INSTANCE.isInventoryOpenServerSide()) {
            ci.cancel();
        }

        /*
          Handles old protocol version of {@link ServerboundContainerClickPacket} (containerId = 0)

          https://github.com/ViaVersion/ViaFabricPlus/blob/e7a6c9d193e278120942a805f14201048ceb43ff/src/main/java/com/viaversion/viafabricplus/injection/mixin/features/interaction/container_clicking/MixinMultiPlayerGameMode.java#L79
         */
        if (packetType == ServerboundPackets1_16_2.CONTAINER_CLICK && this.get(Types.BYTE, 0) == 0
            || packetType == ServerboundPackets1_21_4.CONTAINER_CLICK && this.get(Types.VAR_INT, 0) == 0) {
            // Note: this doesn't cover all handlers of PacketEvent
            InventoryManager.onClickOccurs();
            InventoryManager.INSTANCE.setInventoryOpenServerSide(true);
        }
    }

}
