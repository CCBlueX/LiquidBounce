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
package net.ccbluex.liquidbounce.integration.backend.backends.cef

import net.ccbluex.liquidbounce.mcef.MCEF
import net.ccbluex.liquidbounce.utils.client.mc
import org.cef.CefSettings
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefDisplayHandlerAdapter
import java.util.UUID

/**
 * Connects the system clipboard to the clipboard of the embedded Chromium on Linux.
 *
 * The browser is rendered off-screen, so it owns no window and never takes ownership of the X11
 * CLIPBOARD selection. Copy and paste inside the interface keep working, but they run against an
 * in-process clipboard that no other application - and no compositor - can read or write.
 *
 * Pasting takes one step: the system clipboard is read and inserted at the caret. Copying needs a
 * round trip, because CEF offers no way to read back what it copied. The injected script reports
 * the selection through the console, where [install] picks it up.
 *
 * @see <a href="https://github.com/CCBlueX/LiquidBounce/issues/9218">#9218</a>
 */
object CefClipboardBridge {

    /**
     * Marks a console message as a clipboard payload. The control characters keep it apart from
     * anything a page would log on its own.
     */
    private const val MARKER = "\u0001LiquidBounceClipboard\u0001"

    /**
     * How long a copy may take to report back. Anything later is treated as unsolicited.
     */
    private const val REPLY_TIMEOUT_MS = 2_000L

    /**
     * The copy we are waiting for a reply to.
     *
     * A page can log whatever it likes, so a payload is only accepted while it answers a copy the
     * user just triggered: same browser, matching token, inside [REPLY_TIMEOUT_MS], once. That
     * still trusts the page the shortcut was sent to - scripts running there see the token we
     * injected - but it keeps every other page, and every other moment, out of the clipboard.
     */
    private var pendingCopy: PendingCopy? = null

    private var installed = false

    private class PendingCopy(val browser: CefBrowser, val token: String) {
        private val issuedAt = System.currentTimeMillis()

        fun matches(browser: CefBrowser?, token: String) = this.browser === browser
            && this.token == token
            && System.currentTimeMillis() - issuedAt <= REPLY_TIMEOUT_MS
    }

    fun install() {
        if (installed) {
            return
        }
        installed = true

        MCEF.INSTANCE.client.addDisplayHandler(object : CefDisplayHandlerAdapter() {
            override fun onConsoleMessage(
                browser: CefBrowser?, level: CefSettings.LogSeverity?,
                message: String?, source: String?, line: Int
            ): Boolean {
                if (message == null || !message.startsWith(MARKER)) {
                    return false
                }

                val payload = message.substring(MARKER.length)
                val separator = payload.indexOf('\u0001')
                if (separator < 0) {
                    return true
                }

                val pending = pendingCopy
                if (pending == null || !pending.matches(browser, payload.substring(0, separator))) {
                    // Swallow it anyway - it was addressed to us, whoever sent it.
                    return true
                }
                pendingCopy = null

                val selection = payload.substring(separator + 1)
                mc.execute { mc.keyboardHandler.clipboard = selection }

                // Swallow it, so the payload never reaches the log.
                return true
            }
        })
    }

    /**
     * Inserts the system clipboard at the caret of [frame].
     *
     * @return whether anything was inserted
     */
    fun paste(frame: CefFrame): Boolean {
        val text = mc.keyboardHandler.clipboard
        if (text.isEmpty()) {
            return false
        }

        frame.executeJavaScript("document.execCommand('insertText', false, ${quote(text)})", frame.url, 0)
        return true
    }

    /**
     * Reports the selection of [frame] back through the console, from where [install] copies it to
     * the system clipboard. The token ties the reply to this call.
     */
    fun copy(browser: CefBrowser, frame: CefFrame) {
        val token = UUID.randomUUID().toString()
        pendingCopy = PendingCopy(browser, token)

        frame.executeJavaScript(
            """
            (function() {
                var selection = '';
                var focused = document.activeElement;
                if (focused && (focused.tagName === 'INPUT' || focused.tagName === 'TEXTAREA')
                    && focused.selectionStart !== focused.selectionEnd) {
                    selection = focused.value.substring(focused.selectionStart, focused.selectionEnd);
                } else {
                    var range = window.getSelection();
                    selection = range ? range.toString() : '';
                }
                if (selection) {
                    console.log(${quote(MARKER + token + "\u0001")} + selection);
                }
            })()
            """.trimIndent(),
            frame.url,
            0
        )
    }

    /** Turns [text] into a JavaScript string literal. */
    private fun quote(text: String) = buildString(text.length + 2) {
        append('"')
        for (char in text) {
            when {
                char == '"' -> append("\\\"")
                char == '\\' -> append("\\\\")
                char == '\n' -> append("\\n")
                char == '\r' -> append("\\r")
                char == '\t' -> append("\\t")
                char.code < 0x20 || char.code > 0x7E -> append("\\u%04x".format(char.code))
                else -> append(char)
            }
        }
        append('"')
    }

}
