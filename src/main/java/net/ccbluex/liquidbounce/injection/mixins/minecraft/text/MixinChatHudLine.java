/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.injection.mixins.minecraft.text;

import net.ccbluex.liquidbounce.interfaces.ChatHudLineAddition;
import net.ccbluex.liquidbounce.interfaces.ChatMessageAddition;
import net.minecraft.client.gui.hud.ChatHudLine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ChatHudLine.class)
public abstract class MixinChatHudLine implements ChatMessageAddition, ChatHudLineAddition {

    @Unique
    private String liquid_bounce$id = null;

    @Unique
    int liquid_bounce$count = 1;

    @Unique
    @Override
    public void liquid_bounce$setId(String id) {
        this.liquid_bounce$id = id;
    }

    @Unique
    @Override
    public String liquid_bounce$getId() {
        return liquid_bounce$id;
    }

    @Unique
    @Override
    public void liquid_bounce$setCount(int count) {
        this.liquid_bounce$count = count;
    }

    @Unique
    @Override
    public int liquid_bounce$getCount() {
        return liquid_bounce$count;
    }

}
