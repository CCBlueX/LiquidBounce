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

package net.ccbluex.liquidbounce.utils.text

import kotlinx.coroutines.CompletableDeferred
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import kotlin.test.Test
import kotlin.test.assertEquals

class AsyncLoadingTextTest {

    @Test
    fun `test normal completion`() {
        val deferred = CompletableDeferred<Component>()
        val onLoading = PlainText.of("Loading now...", Style.EMPTY)
        val text = AsyncLoadingText(deferred, { onLoading }, AsyncLoadingText.DEFAULT_ON_EXCEPTION)
        assertEquals(onLoading, text.get())

        deferred.complete(PlainText.NEW_LINE)
        assertEquals(PlainText.NEW_LINE, text.get())
    }

    @Test
    fun `test exceptional completion`() {
        val deferred = CompletableDeferred<Component>()
        val text = AsyncLoadingText(deferred)
        assertEquals(AsyncLoadingText.DEFAULT_ON_LOADING.get(), text.get())

        val exception = Exception()
        deferred.completeExceptionally(exception)
        assertEquals(AsyncLoadingText.DEFAULT_ON_EXCEPTION.apply(exception), text.get())
    }

}
