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

import net.ccbluex.liquidbounce.interfaces.EntitiesDestroyS2CPacketAddition;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntitiesDestroyS2CPacket.class)
public class MixinEntitiesDestroyS2CPacket implements EntitiesDestroyS2CPacketAddition {

    @Unique
    private boolean liquid_bounce$containsCrystal;

    @Unique
    @Override
    public void liquid_bounce$setContainsCrystal() {
        this.liquid_bounce$containsCrystal = true;
    }

    @Unique
    @Override
    public boolean liquid_bounce$containsCrystal() {
        return this.liquid_bounce$containsCrystal;
    }

}
