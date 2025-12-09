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

package net.ccbluex.liquidbounce.utils.client;

import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.util.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Text, but only siblings
 */
public final class TextList implements Text {

    public static final TextList EMPTY = new TextList(Collections.emptyList());

    private final @NotNull List<@NotNull Text> siblings;

    private OrderedText ordered = OrderedText.EMPTY;
    @Nullable
    private Language language;

    private TextList(@NotNull List<@NotNull Text> siblings) {
        this.siblings = siblings;
    }

    public static TextList of(@NotNull Text... texts) {
        return of(ObjectList.of(texts));
    }

    public static TextList of(@NotNull List<@NotNull Text> siblings) {
        return siblings.isEmpty() ? EMPTY : new TextList(siblings);
    }

    @Override
    public Style getStyle() {
        return Style.EMPTY;
    }

    @Override
    public TextContent getContent() {
        return PlainText.EMPTY.content();
    }

    @Override
    public @NotNull List<Text> getSiblings() {
        return siblings;
    }

    @Override
    public OrderedText asOrderedText() {
        Language language = Language.getInstance();
        if (this.language != language) {
            this.ordered = language.reorder(this);
            this.language = language;
        }

        return this.ordered;
    }
}
