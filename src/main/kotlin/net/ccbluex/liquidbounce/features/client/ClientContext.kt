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
package net.ccbluex.liquidbounce.features.client

import java.util.function.Supplier

/**
 * Tells which [Client] the running code belongs to.
 *
 * Minecraft keeps one player, level and screen per client. On the game thread a client is entered by moving
 * its [GameState] into those fields, so vanilla code runs unchanged. Other threads only carry the reference.
 * A simulated client moves no field at all: it reads the ones of the client it simulates for.
 */
object ClientContext {

    private var gameThread: Thread? = null

    /**
     * The client whose game state sits in the Minecraft fields. Only the game thread moves it.
     */
    @JvmStatic
    var loaded: Client = ClientManager.main
        private set

    /**
     * [loaded] as other threads may see it: null while the fields are being moved.
     */
    @Volatile
    private var settled: Client? = loaded

    /**
     * The simulated client the game thread runs in. It does not move any field.
     */
    private var simulated: Client? = null

    private val bound = ThreadLocal<Client>()

    @JvmStatic
    val isGameThread: Boolean
        get() {
            val thread = gameThread
            return thread == null || thread === Thread.currentThread()
        }

    /**
     * Threads that never entered a client act for the active one.
     */
    @JvmStatic
    val current: Client
        get() = if (isGameThread) simulated ?: loaded else bound.get() ?: ClientManager.active

    /**
     * True while the game thread runs a client the window does not show.
     */
    @JvmStatic
    val isBackground: Boolean
        get() = isGameThread && loaded !== ClientManager.active

    @JvmStatic
    fun attachGameThread() {
        gameThread = Thread.currentThread()
    }

    internal fun holds(client: Client) =
        if (isGameThread) loaded === client else settled === client

    /**
     * @return what [exit] needs to leave [client] again
     */
    @JvmStatic
    fun enter(client: Client): Client? {
        if (isGameThread) {
            val previous = simulated ?: loaded
            if (!client.isSimulated) {
                simulated?.host = loaded
            }
            switchTo(client)
            return previous
        }

        val previous = bound.get()
        bound.set(client)
        return previous
    }

    @JvmStatic
    fun exit(previous: Client?) {
        when {
            isGameThread -> switchTo(previous!!)
            previous == null -> bound.remove()
            else -> bound.set(previous)
        }
    }

    private fun switchTo(client: Client) {
        if (!client.isSimulated) {
            simulated = null
            load(client)
            return
        }

        // Back from a real client that was entered while simulating
        client.host?.let { host ->
            client.host = null
            load(host)
        }
        simulated = client
    }

    /**
     * Makes [client] the one that stays loaded between frames.
     */
    internal fun rest(client: Client) {
        check(isGameThread) { "Only the game thread moves clients" }
        load(client)
    }

    inline fun <T> within(client: Client, block: () -> T): T {
        val previous = enter(client)
        try {
            return block()
        } finally {
            exit(previous)
        }
    }

    /**
     * [task], run for the client of the thread that schedules it. The active client needs no wrapping
     * on the game thread: its tasks run before the window can change hands.
     */
    @JvmStatic
    fun pinned(task: Runnable): Runnable {
        val client = if (isGameThread) simulated ?: loaded.takeUnless { it.isActive } else bound.get()
        client ?: return task

        return Runnable { within(client) { task.run() } }
    }

    /**
     * [supplier], run for the client that asks for it. For work that is handed to a worker thread.
     */
    @JvmStatic
    fun <T> pinned(supplier: Supplier<T>): Supplier<T> {
        val client = current
        return Supplier { within(client) { supplier.get() } }
    }

    private fun load(client: Client) {
        val previous = loaded
        if (previous === client) {
            return
        }

        // A thread still sees the fields as the previous client's until they are saved, never half moved.
        previous.game.capture()
        settled = null
        client.game.install()
        loaded = client
        settled = client
    }

}
