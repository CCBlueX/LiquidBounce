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
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * A plain and immutable {@link Text}, {@link OrderedText} and {@link CharSequence}.
 */
public record ImmutableText(@NotNull PlainTextContent content) implements Text, OrderedText, CharSequence {

    public static final ImmutableText EMPTY = new ImmutableText(PlainTextContent.EMPTY);

    public ImmutableText(@NotNull String content) {
        this(PlainTextContent.of(content));
    }

    public static @NotNull ImmutableText of(@NotNull String content) {
        return content.isEmpty() ? EMPTY : new ImmutableText(content);
    }

    public @NotNull String string() {
        return content.string();
    }

    @Override
    public boolean contains(Text text) {
        if (text == null) return false;
        if (text.equals(this)) return true;
        final List<Text> sameStyle = text.withoutStyle();
        return sameStyle.size() == 1 && sameStyle.getFirst().equals(this);
    }

    @Override
    public List<Text> getWithStyle(Style style) {
        return Collections.singletonList(copy().setStyle(style));
    }

    @Override
    public List<Text> withoutStyle() {
        return Collections.singletonList(this);
    }

    @Override
    public MutableText copy() {
        return MutableText.of(content);
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
    public @NotNull String getLiteralString() {
        return string();
    }

    @Override
    public Style getStyle() {
        return Style.EMPTY;
    }

    @Override
    public TextContent getContent() {
        return content;
    }

    @Override
    public String getString() {
        return string();
    }

    @Override
    public List<Text> getSiblings() {
        return Collections.emptyList();
    }

    @Override
    public OrderedText asOrderedText() {
        return this;
    }

    @Override
    public boolean accept(CharacterVisitor visitor) {
        return TextVisitFactory.visitFormatted(string(), Style.EMPTY, visitor);
    }

    @Override
    public <T> Optional<T> visit(StyledVisitor<T> styledVisitor, Style style) {
        return styledVisitor.accept(style, string());
    }

    @Override
    public <T> Optional<T> visit(Visitor<T> visitor) {
        return visitor.accept(string());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;

        return switch (obj) {
            case PlainTextContent plainTextContent -> plainTextContent.string().equals(string());
            case CharSequence charSequence -> string().contentEquals(charSequence);
            case null, default -> false;
        };
    }

    @Override
    public int hashCode() {
        return string().hashCode();
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
