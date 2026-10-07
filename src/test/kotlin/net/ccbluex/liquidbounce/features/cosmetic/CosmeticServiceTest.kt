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
package net.ccbluex.liquidbounce.features.cosmetic

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import net.ccbluex.liquidbounce.test.MinecraftBootstrap
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CosmeticServiceTest {

    companion object {
        init {
            MinecraftBootstrap.ensureInitialized()
        }
    }

    private val done = AtomicInteger()
    private val failed = AtomicInteger()

    @BeforeTest
    fun `no refresh has happened yet`() {
        CosmeticService.lastUpdate.reset(0)
    }

    private fun refresh(force: Boolean, load: suspend () -> Set<String>): Job? =
        CosmeticService.refreshCarriersWith(
            force,
            load,
            Consumer { it.run() },
            onFailure = { failed.incrementAndGet() },
            done = { done.incrementAndGet() },
        )

    @Test
    fun `a failed refresh is reported and does not block the next one`() = runBlocking {
        assertNotNull(refresh(force = true) { throw IOException("offline") }).join()
        assertEquals(1, failed.get())
        assertEquals(0, done.get())

        val retry = assertNotNull(refresh(force = true) { setOf("carrier") })
        retry.join()
        assertEquals(setOf("carrier"), CosmeticService.carriers)
        assertEquals(1, failed.get())
        assertEquals(1, done.get())
    }

    @Test
    fun `a failed refresh is not repeated before the refresh delay has passed`() = runBlocking {
        assertNotNull(refresh(force = true) { throw IOException("offline") }).join()

        // Nothing is asked from the API again, the lookups carry on with what they have
        assertNull(refresh(force = false) { throw AssertionError("The API was asked again") })
        assertEquals(1, failed.get())
        assertEquals(1, done.get())
    }

    @Test
    fun `a refresh is not started while another one is running`() = runBlocking {
        val release = CompletableDeferred<Unit>()
        val running = assertNotNull(refresh(force = true) { release.await(); setOf("carrier") })

        assertNull(refresh(force = true) { throw AssertionError("A second refresh was started") })

        release.complete(Unit)
        running.join()
        assertEquals(1, done.get())
    }

    @Test
    fun `a cancelled refresh does not block the next one`() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val cancelled = assertNotNull(refresh(force = true) {
            started.complete(Unit)
            awaitCancellation()
        })
        started.await()
        cancelled.cancelAndJoin()
        assertEquals(0, failed.get())
        assertEquals(0, done.get())

        assertNotNull(refresh(force = true) { setOf("carrier") }).join()
        assertEquals(1, done.get())
    }

}
