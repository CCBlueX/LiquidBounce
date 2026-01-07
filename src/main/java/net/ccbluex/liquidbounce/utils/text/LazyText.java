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

import kotlin.Lazy;
import kotlin.LazyKt;
import kotlin.jvm.functions.Function0;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * A lazy text component.
 * @param delegate The lazy component delegate.
 */
@NullMarked
public record LazyText(
    Lazy<Component> delegate
) implements Component {

    public static LazyText of(Function0<Component> initializer) {
        return new LazyText(LazyKt.lazy(initializer));
    }

    @Override
    public String getString() {
        return delegate.getValue().getString();
    }

    @Override
    public String getString(int maxLength) {
        return delegate.getValue().getString(maxLength);
    }

    @Override
    public @Nullable String tryCollapseToString() {
        return delegate.getValue().tryCollapseToString();
    }

    @Override
    public MutableComponent plainCopy() {
        return delegate.getValue().plainCopy();
    }

    @Override
    public <T> Optional<T> visit(StyledContentConsumer<T> acceptor, Style style) {
        return delegate.getValue().visit(acceptor, style);
    }

    @Override
    public MutableComponent copy() {
        return delegate.getValue().copy();
    }

    @Override
    public <T> Optional<T> visit(ContentConsumer<T> acceptor) {
        return delegate.getValue().visit(acceptor);
    }

    @Override
    public List<Component> toFlatList() {
        return delegate.getValue().toFlatList();
    }

    @Override
    public List<Component> toFlatList(Style style) {
        return delegate.getValue().toFlatList(style);
    }

    @Override
    public boolean contains(Component other) {
        return delegate.getValue().contains(other);
    }

    @Override
    public Style getStyle() {
        return delegate.getValue().getStyle();
    }

    @Override
    public ComponentContents getContents() {
        return delegate.getValue().getContents();
    }

    @Override
    public List<Component> getSiblings() {
        return delegate.getValue().getSiblings();
    }

    @Override
    public FormattedCharSequence getVisualOrderText() {
        return delegate.getValue().getVisualOrderText();
    }
}
