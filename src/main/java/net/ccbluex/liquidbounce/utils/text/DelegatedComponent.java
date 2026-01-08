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

import kotlin.LazyKt;
import kotlin.jvm.functions.Function0;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A component that is delegated to another component.
 * <p>
 * The implementation should provide delegation with {@link java.util.function.Supplier#get()}.
 * This method should never return {@code null}.
 */
@FunctionalInterface
public interface DelegatedComponent extends Component, Supplier<Component> {

    /**
     * Creates a lazy delegated component based on {@link kotlin.Lazy}.
     */
    static DelegatedComponent lazy(Function0<Component> initializer) {
        return LazyKt.lazy(initializer)::getValue;
    }

    @Override
    default String getString() {
        return get().getString();
    }

    @Override
    default String getString(int maxLength) {
        return get().getString(maxLength);
    }

    @Override
    default @Nullable String tryCollapseToString() {
        return get().tryCollapseToString();
    }

    @Override
    default MutableComponent plainCopy() {
        return get().plainCopy();
    }

    @Override
    default <T> Optional<T> visit(StyledContentConsumer<T> acceptor, Style style) {
        return get().visit(acceptor, style);
    }

    @Override
    default MutableComponent copy() {
        return get().copy();
    }

    @Override
    default <T> Optional<T> visit(ContentConsumer<T> acceptor) {
        return get().visit(acceptor);
    }

    @Override
    default List<Component> toFlatList() {
        return get().toFlatList();
    }

    @Override
    default List<Component> toFlatList(Style style) {
        return get().toFlatList(style);
    }

    @Override
    default boolean contains(Component other) {
        return get().contains(other);
    }

    @Override
    default Style getStyle() {
        return get().getStyle();
    }

    @Override
    default ComponentContents getContents() {
        return get().getContents();
    }

    @Override
    default List<Component> getSiblings() {
        return get().getSiblings();
    }

    @Override
    default FormattedCharSequence getVisualOrderText() {
        return get().getVisualOrderText();
    }

}
