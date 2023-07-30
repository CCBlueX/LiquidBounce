/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2023 CCBlueX
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

package net.ccbluex.liquidbounce.ultralight.listener;

import net.ccbluex.liquidbounce.ultralight.WebWindow;
import net.ccbluex.liquidbounce.ultralight.translators.CursorTranslator;
import net.janrupf.ujr.api.UltralightView;
import net.janrupf.ujr.api.cursor.UlCursor;
import net.janrupf.ujr.api.listener.UltralightViewListener;

public class WebViewListener implements UltralightViewListener {
    private final WebWindow window;

    public WebViewListener(WebWindow window) {
        this.window = window;
    }

    @Override
    public void onChangeCursor(UltralightView view, UlCursor cursor) {
        int cursorId = CursorTranslator.ultralightToGlfwCursor(cursor);
        window.getWindow().setCursor(cursorId);
    }
}
