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
package net.ccbluex.liquidbounce.injection.mixins.minecraft.network;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.ccbluex.liquidbounce.features.session.SessionContext;
import net.minecraft.network.PacketProcessor;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PacketProcessor.class)
public abstract class MixinPacketProcessor {

    /**
     * Each packet enters the session of its connection, see {@link MixinPacketProcessorListenerAndPacket}.
     * The session that was loaded before returns once the queue is empty.
     */
    @WrapMethod(method = "processQueuedPackets")
    private void returnToSessionAfterPackets(Operation<Void> original) {
        if (!SessionContext.isGameThread()) {
            original.call();
            return;
        }

        var loaded = SessionContext.getLoaded();
        try {
            original.call();
        } finally {
            SessionContext.exit(loaded);
        }
    }

}
