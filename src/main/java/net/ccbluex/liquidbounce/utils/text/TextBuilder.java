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

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public final class TextBuilder {

    private @Nullable Object inner;
    private int size;

    public TextBuilder() {
        this.inner = null;
        this.size = 0;
    }

    public TextBuilder(Component component) {
        this.inner = Objects.requireNonNull(component);
        this.size = 1;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public TextBuilder append(@Nullable Component component) {
        if (component == null) return this;

        switch (this.size) {
            case 0 -> this.inner = component;
            case 1 -> this.inner = List.of((Component) this.inner, component);
            case 2 -> {
                List prev = (List) this.inner; // immutable
                assert prev != null;
                assert prev.size() == 2;

                ObjectArrayList<Object> list = new ObjectArrayList<>();
                this.inner = list;
                list.add(prev.get(0));
                list.add(prev.get(1));
                list.add(component);
            }
            default -> {
                List prev = (List) this.inner; // mutable
                assert prev != null;

                prev.add(component);
            }
        }
        this.size++;

        return this;
    }

    @SuppressWarnings("unchecked")
    public Component build() {
        Component result = switch (this.size) {
            case 0 -> PlainText.EMPTY;
            case 1 -> (Component) this.inner;
            default -> new TextList((List<Component>) this.inner);
        };
        assert result != null;

        this.inner = null;
        this.size = 0;
        return result;
    }

}
