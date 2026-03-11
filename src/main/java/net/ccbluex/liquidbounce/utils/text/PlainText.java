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

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.util.StringDecomposer;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * A plain and immutable {@link Component}, {@link FormattedCharSequence} and {@link CharSequence}.
 */
public record PlainText(
    PlainTextContents content,
    Style style
) implements Component, FormattedCharSequence, CharSequence {

    public static final PlainText EMPTY = new PlainText(PlainTextContents.EMPTY, Style.EMPTY);
    public static final PlainText SPACE = new PlainText(new PlainTextContents.LiteralContents(" "), Style.EMPTY);
    public static final PlainText NEW_LINE = new PlainText(new PlainTextContents.LiteralContents("\n"), Style.EMPTY);

    public static PlainText empty() {
        return EMPTY;
    }

    public PlainText(PlainTextContents content) {
        this(content, Style.EMPTY);
    }

    public static PlainText of(PlainTextContents content, Style style) {
        return content.text().isEmpty() && style.isEmpty()
                ? EMPTY
                : new PlainText(content, style);
    }

    public static PlainText of(String content) {
        return of(content, Style.EMPTY);
    }

    public static PlainText of(String content, Style style) {
        return content.isEmpty() && style.isEmpty()
                ? EMPTY
                : new PlainText(PlainTextContents.create(content), style);
    }

    public static PlainText of(String content, ChatFormatting formatting) {
        return of(content, Style.EMPTY.applyFormat(formatting));
    }

    public String string() {
        return content.text();
    }

    @Override
    public boolean contains(@Nullable Component text) {
        if (text == null) return false;
        if (text.equals(this)) return true;
        List<Component> sameStyle = style.isEmpty() ? text.toFlatList() : toFlatList(this.getStyle());
        return sameStyle.isEmpty() || sameStyle.size() == 1 && sameStyle.getFirst().equals(this);
    }

    @Override
    public List<Component> toFlatList(Style style) {
        return singletonList(this.style.equals(style) ? this : of(this.content, style));
    }

    @Override
    public List<Component> toFlatList() {
        return toFlatList(Style.EMPTY);
    }

    @Override
    public MutableComponent copy() {
        return plainCopy().setStyle(this.style);
    }

    @Override
    public MutableComponent plainCopy() {
        return MutableComponent.create(content);
    }

    @Override
    public String getString(int length) {
        final String string = string();
        return string.length() <= length ? string : string.substring(0, length);
    }

    @Override
    public @Nullable String tryCollapseToString() {
        return this.style.isEmpty() ? this.string() : null;
    }

    @Override
    public Style getStyle() {
        return this.style;
    }

    @Override
    public ComponentContents getContents() {
        return this.content;
    }

    @Override
    public String getString() {
        return string();
    }

    @Override
    public List<Component> getSiblings() {
        return emptyList();
    }

    @Override
    public FormattedCharSequence getVisualOrderText() {
        return this;
    }

    @Override
    public boolean accept(FormattedCharSink visitor) {
        return StringDecomposer.iterateFormatted(string(), this.style, visitor);
    }

    @Override
    public <T> Optional<T> visit(StyledContentConsumer<T> styledVisitor, Style style) {
        return styledVisitor.accept(this.style.applyTo(style), string());
    }

    @Override
    public <T> Optional<T> visit(ContentConsumer<T> visitor) {
        return visitor.accept(string());
    }

    @Override
    public int length() {
        return string().length();
    }

    @Override
    public char charAt(int index) {
        return string().charAt(index);
    }

    @Override
    public boolean isEmpty() {
        return string().isEmpty();
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return string().subSequence(start, end);
    }

    @Override
    public String toString() {
        return string();
    }

    @Override
    public IntStream chars() {
        return string().chars();
    }

    @Override
    public IntStream codePoints() {
        return string().codePoints();
    }
}
