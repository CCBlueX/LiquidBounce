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
package net.ccbluex.liquidbounce.injection.mixins.minecraft.network;

import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.api.protocol.packet.PacketType;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocols.v1_9_1to1_9_3.packet.ServerboundPackets1_9_3;
import net.ccbluex.liquidbounce.utils.client.ClientChat;
import net.ccbluex.liquidbounce.utils.client.ClientUtilsKt;
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
    public abstract <T> T get(Type<T> type, int index) throws Exception;

    /**
     * An injection that prevents ViaFabricPlus from sending an inventory packet twice.
     * <p>
     * This can be caused when inventory-managing modules silently open inventory and the user tries to manually open
     * their inventory, which results in the same packet being sent twice.
     */
    @Inject(method = "scheduleSendToServer", at = @At("HEAD"), cancellable = true)
    private void preventInventoryPacketDuplication(Class<? extends Protocol> protocol, boolean skipCurrentPipeline, CallbackInfo ci) {
        try {
            if (this.getPacketType() == ServerboundPackets1_9_3.CLIENT_COMMAND && this.get(Types.VAR_INT, 0) == 2 &&
                    InventoryManager.INSTANCE.isInventoryOpenServerSide()) {
                ci.cancel();
            }
        } catch (Exception e) {
            ClientChat.chat("§cInventory packet duplication prevention check failed, report to developers!");
            ClientUtilsKt.getLogger().error("Inventory packet duplication prevention check failed", e);
        }
    }
}
