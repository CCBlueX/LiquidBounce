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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.text;

import net.ccbluex.liquidbounce.utils.client.PlainText;
import net.minecraft.text.MutableText;
import net.minecraft.text.PlainTextContent;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(MutableText.class)
public abstract class MixinMutableText {

    @Shadow
    public abstract MutableText append(Text text);

    /**
     * @author MukjepScarlet
     * @reason avoid {@link Text#literal(String)} because it creates {@link MutableText}
     */
    @Overwrite
    public MutableText append(String text) {
        return switch (text) {
            case "" -> (MutableText) (Object) this;
            case " " -> this.append(PlainText.SPACE);
            case "\n" -> this.append(PlainText.NEW_LINE);
            default -> this.append(new PlainText(PlainTextContent.of(text)));
        };
    }

}
