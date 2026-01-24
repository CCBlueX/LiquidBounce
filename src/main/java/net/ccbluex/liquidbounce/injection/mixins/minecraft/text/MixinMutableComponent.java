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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.text;

import net.ccbluex.liquidbounce.utils.text.PlainText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.PlainTextContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(MutableComponent.class)
public abstract class MixinMutableComponent {

    @Shadow
    public abstract MutableComponent append(Component text);

    /**
     * @author MukjepScarlet
     * @reason avoid {@link Component#literal(String)} because it creates {@link MutableComponent}
     */
    @Overwrite
    public MutableComponent append(String text) {
        return switch (text) {
            case "" -> (MutableComponent) (Object) this;
            case " " -> this.append(PlainText.SPACE);
            case "\n" -> this.append(PlainText.NEW_LINE);
            default -> this.append(new PlainText(PlainTextContents.create(text)));
        };
    }

}
