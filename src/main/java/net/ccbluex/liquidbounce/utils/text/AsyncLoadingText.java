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

import com.google.common.base.Suppliers;
import kotlinx.coroutines.Deferred;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A lazy text component with async loading.
 *
 * @param delegate The lazy component delegate.
 * @param onLoading The component to display while loading.
 * @param onException The component to display when an exception occurs.
 */
public record AsyncLoadingText(
    Deferred<Component> delegate,
    Supplier<Component> onLoading,
    Function<Throwable, Component> onException
) implements DelegatedComponent {

    public static final Supplier<Component> DEFAULT_ON_LOADING =
        Suppliers.ofInstance(PlainText.of("Loading...", ChatFormatting.GRAY));
    public static final Function<Throwable, Component> DEFAULT_ON_EXCEPTION =
        throwable -> {
            String message = throwable.getMessage();

            return PlainText.of(message == null ?
                "Unknown error (" + throwable.getClass().getName() + ")" :
                message, ChatFormatting.RED);
        };

    public AsyncLoadingText(Deferred<Component> delegate) {
        this(delegate, DEFAULT_ON_LOADING, DEFAULT_ON_EXCEPTION);
    }

    @Override
    public Component get() {
        if (delegate().isActive()) {
            return onLoading().get();
        }

        Throwable completionExceptionOrNull = delegate().getCompletionExceptionOrNull();
        if (completionExceptionOrNull != null) {
            return onException().apply(completionExceptionOrNull);
        } else {
            return delegate().getCompleted();
        }
    }
}
