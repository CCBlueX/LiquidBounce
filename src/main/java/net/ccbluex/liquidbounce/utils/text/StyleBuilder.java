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

package net.ccbluex.liquidbounce.utils.text;

import net.ccbluex.liquidbounce.injection.mixins.minecraft.text.MixinStyleAccessor;
import net.minecraft.network.chat.*;
import org.jspecify.annotations.Nullable;

public final class StyleBuilder {

    private final Style base;

    public StyleBuilder(Style base) {
        this.base = base;
    }

    public StyleBuilder() {
        this(Style.EMPTY);
    }

    @Nullable
    public TextColor color;
    @Nullable
    public Integer shadowColor;
    @Nullable
    public Boolean bold;
    @Nullable
    public Boolean italic;
    @Nullable
    public Boolean underlined;
    @Nullable
    public Boolean strikethrough;
    @Nullable
    public Boolean obfuscated;
    @Nullable
    public ClickEvent clickEvent;
    @Nullable
    public HoverEvent hoverEvent;
    @Nullable
    public String insertion;
    @Nullable
    public FontDescription font;

    public Style build() {
        Style style = MixinStyleAccessor.create(
            this.color == null ? this.base.getColor() : this.color,
            this.shadowColor == null ? this.base.getShadowColor() : this.shadowColor,
            this.bold == null ? this.base.isBold() : this.bold,
            this.italic == null ? this.base.isItalic() : this.italic,
            this.underlined == null ? this.base.isUnderlined() : this.underlined ,
            this.strikethrough == null ? this.base.isStrikethrough() : this.strikethrough,
            this.obfuscated == null ? this.base.isObfuscated() : this.obfuscated,
            this.clickEvent == null ? this.base.getClickEvent() : this.clickEvent,
            this.hoverEvent == null ? this.base.getHoverEvent() : this.hoverEvent,
            this.insertion == null ? this.base.getInsertion() : this.insertion,
            this.font == null ? this.base.getFont() : this.font
        );
        return style.equals(Style.EMPTY) ? Style.EMPTY : style;
    }

}
