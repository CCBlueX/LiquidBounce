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

import net.ccbluex.fastutil.Pool;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;

public record AppenderCharSink(StringBuilder builder) implements FormattedCharSink {

    private static final Pool<AppenderCharSink> POOL = Pool.create(AppenderCharSink::new, AppenderCharSink::clear).sync();

    public static String codePointsToString(final FormattedCharSequence input) {
        var sink = POOL.borrow();
        try {
            input.accept(sink);
            return sink.builder.toString();
        } finally {
            POOL.recycle(sink);
        }
    }

    public AppenderCharSink() {
        this(new StringBuilder(128));
    }

    @Override
    public boolean accept(int position, Style style, int codepoint) {
        this.builder.appendCodePoint(codepoint);
        return true;
    }

    public void clear() {
        this.builder.setLength(0);
    }

}
