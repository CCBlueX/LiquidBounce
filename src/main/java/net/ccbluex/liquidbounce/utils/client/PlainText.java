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

import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * A plain and immutable {@link Text}, {@link OrderedText} and {@link CharSequence}.
 */
public record PlainText(
        @NotNull PlainTextContent content,
        @NotNull Style style
) implements Text, OrderedText, CharSequence {

    public static final PlainText EMPTY = new PlainText(PlainTextContent.EMPTY, Style.EMPTY);
    public static final PlainText SPACE = new PlainText(PlainTextContent.of(" "), Style.EMPTY);
    public static final PlainText NEW_LINE = new PlainText(PlainTextContent.of("\n"), Style.EMPTY);

    public PlainText(@NotNull PlainTextContent content) {
        this(content, Style.EMPTY);
    }

    public static @NotNull PlainText of(@NotNull PlainTextContent content, @NotNull Style style) {
        return content.string().isEmpty() && style.isEmpty()
                ? EMPTY
                : new PlainText(content, style);
    }

    public static @NotNull PlainText of(@NotNull String content, @NotNull Style style) {
        return content.isEmpty() && style.isEmpty()
                ? EMPTY
                : new PlainText(PlainTextContent.of(content), style);
    }

    public static @NotNull PlainText of(@NotNull String content, @NotNull Formatting formatting) {
        return of(content, Style.EMPTY.withFormatting(formatting));
    }

    public @NotNull String string() {
        return content.string();
    }

    @Override
    public boolean contains(Text text) {
        if (text == null) return false;
        if (text.equals(this)) return true;
        List<Text> sameStyle = style.isEmpty() ? text.withoutStyle() : getWithStyle(this.getStyle());
        return sameStyle.isEmpty() || sameStyle.size() == 1 && sameStyle.getFirst().equals(this);
    }

    @Override
    public List<Text> getWithStyle(Style style) {
        return singletonList(this.style.equals(style) ? this : of(this.content, style));
    }

    @Override
    public List<Text> withoutStyle() {
        return getWithStyle(Style.EMPTY);
    }

    @Override
    public MutableText copy() {
        return copyContentOnly().setStyle(this.style);
    }

    @Override
    public MutableText copyContentOnly() {
        return MutableText.of(content);
    }

    @Override
    public String asTruncatedString(int length) {
        final String string = string();
        return string.length() <= length ? string : string.substring(0, length);
    }

    @Override
    public @Nullable String getLiteralString() {
        return this.style.isEmpty() ? this.string() : null;
    }

    @Override
    public Style getStyle() {
        return this.style;
    }

    @Override
    public TextContent getContent() {
        return this.content;
    }

    @Override
    public String getString() {
        return string();
    }

    @Override
    public List<Text> getSiblings() {
        return emptyList();
    }

    @Override
    public OrderedText asOrderedText() {
        return this;
    }

    @Override
    public boolean accept(CharacterVisitor visitor) {
        return TextVisitFactory.visitFormatted(string(), this.style, visitor);
    }

    @Override
    public <T> Optional<T> visit(StyledVisitor<T> styledVisitor, Style style) {
        return styledVisitor.accept(this.style.withParent(style), string());
    }

    @Override
    public <T> Optional<T> visit(Visitor<T> visitor) {
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
    public @NotNull CharSequence subSequence(int start, int end) {
        return string().subSequence(start, end);
    }

    @Override
    public @NotNull String toString() {
        return string();
    }

    @Override
    public @NotNull IntStream chars() {
        return string().chars();
    }

    @Override
    public @NotNull IntStream codePoints() {
        return string().codePoints();
    }
}
