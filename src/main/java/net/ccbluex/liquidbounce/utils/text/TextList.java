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

import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Text, but only siblings
 */
public final class TextList implements Component {

    public static final TextList EMPTY = new TextList(Collections.emptyList());

    private final List<Component> siblings;

    private FormattedCharSequence ordered = FormattedCharSequence.EMPTY;
    @Nullable
    private Language language;

    private TextList(@Nullable List<Component> siblings) {
        this.siblings = siblings == null ? Collections.emptyList() : siblings;
    }

    public static TextList of(Component... texts) {
        return of(ObjectList.of(texts));
    }

    public static TextList of(@Nullable List<Component> siblings) {
        return siblings == null || siblings.isEmpty() ? EMPTY : new TextList(siblings);
    }

    @Override
    public Style getStyle() {
        return Style.EMPTY;
    }

    @Override
    public ComponentContents getContents() {
        return PlainText.EMPTY.content();
    }

    @Override
    public List<Component> getSiblings() {
        return siblings;
    }

    @Override
    public FormattedCharSequence getVisualOrderText() {
        Language language = Language.getInstance();
        if (this.language != language) {
            this.ordered = language.getVisualOrder(this);
            this.language = language;
        }

        return this.ordered;
    }
}
