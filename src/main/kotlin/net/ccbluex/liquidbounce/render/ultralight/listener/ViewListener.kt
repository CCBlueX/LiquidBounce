/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2022 CCBlueX
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
 *
 * and
 *
 * Ultralight Java - Java wrapper for the Ultralight web engine
 * Copyright (C) 2020 - 2021 LabyMedia and contributors
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.ccbluex.liquidbounce.render.ultralight.listener

import com.labymedia.ultralight.UltralightView
import com.labymedia.ultralight.input.UltralightCursor
import com.labymedia.ultralight.math.IntRect
import com.labymedia.ultralight.plugin.view.MessageLevel
import com.labymedia.ultralight.plugin.view.MessageSource
import com.labymedia.ultralight.plugin.view.UltralightViewListener
import net.ccbluex.liquidbounce.render.ultralight.UltralightEngine
import net.ccbluex.liquidbounce.utils.client.logger

class ViewListener : UltralightViewListener {

    /**
     * Called by Ultralight when the page title changes.
     *
     * @param title The new page title
     */
    override fun onChangeTitle(title: String) { }

    /**
     * Called by Ultralight when the view URL changes.
     *
     * @param url The new page url
     */
    override fun onChangeURL(url: String) {
        logger.debug("View url has changed: $url")
    }

    /**
     * Called by Ultralight when the displayed tooltip changes.
     *
     * @param tooltip The new page tooltip
     */
    override fun onChangeTooltip(tooltip: String) { }

    /**
     * Called by Ultralight when the cursor changes. Ultralight supports a lot of cursors, but currently not a custom
     * one.
     *
     * @param cursor The new page cursor
     */
    override fun onChangeCursor(cursor: UltralightCursor) {
        UltralightEngine.cursorAdapter.notifyCursorUpdated(cursor)
    }

    /**
     * Called when a message is added to the console. This includes, but is not limited to, `console.log` and
     * friends.
     *
     * @param source       The source the message originated from
     * @param level        The severity of the message
     * @param message      The message itself
     * @param lineNumber   The line the message originated from
     * @param columnNumber The column the message originated from
     * @param sourceId     The id of the source
     */
    override fun onAddConsoleMessage(
        source: MessageSource,
        level: MessageLevel,
        message: String,
        lineNumber: Long,
        columnNumber: Long,
        sourceId: String
    ) {
        logger.info("View message: [${source.name}/${level.name}] $sourceId:$lineNumber:$columnNumber: $message")
    }

    /**
     * Called by Ultralight when a new view is requested. This is your chance to either open the view in an external
     * browser, in a new application internal tab, or, if desired, to just ignore it entirely.
     *
     * @param openerUrl The URL of the page that initiated this request
     * @param targetUrl The URL that the new View will navigate to
     * @param isPopup   Whether or not this was triggered by window.open()
     * @param popupRect Popups can optionally request certain dimensions and coordinates via window.open(). You can
     * choose to respect these or not by resizing/moving the View to this rect.
     * @return The view to display the new URL in, or `null`, if the request should not be further handled by
     * Ultralight
     */
    override fun onCreateChildView(
        openerUrl: String,
        targetUrl: String,
        isPopup: Boolean,
        popupRect: IntRect
    ): UltralightView? {
        // Returning null will stop Ultralight from further handling the request, ignoring it altogether
        return null
    }

}
