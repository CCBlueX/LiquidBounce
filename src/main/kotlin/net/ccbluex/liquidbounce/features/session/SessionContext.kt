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
package net.ccbluex.liquidbounce.features.session

import java.util.function.Supplier

/**
 * Tells which [ClientSession] the running code belongs to.
 *
 * Minecraft keeps one player, level and screen per client. On the game thread a session is entered by moving
 * its [GameState] into those fields, so vanilla code runs unchanged. Other threads only carry the reference.
 */
object SessionContext {

    private var gameThread: Thread? = null

    /**
     * The session whose game state sits in the Minecraft fields. Only the game thread moves it.
     */
    @JvmStatic
    var loaded: ClientSession = SessionManager.main
        private set

    /**
     * [loaded] as other threads may see it: null while the fields are being moved.
     */
    @Volatile
    private var settled: ClientSession? = loaded

    private val bound = ThreadLocal<ClientSession>()

    @JvmStatic
    val isGameThread: Boolean
        get() {
            val thread = gameThread
            return thread == null || thread === Thread.currentThread()
        }

    /**
     * Threads that never entered a session act for the active one.
     */
    @JvmStatic
    val current: ClientSession
        get() = if (isGameThread) loaded else bound.get() ?: SessionManager.active

    /**
     * True while the game thread runs a session the window does not show.
     */
    @JvmStatic
    val isBackground: Boolean
        get() = isGameThread && loaded !== SessionManager.active

    @JvmStatic
    fun attachGameThread() {
        gameThread = Thread.currentThread()
    }

    internal fun holds(session: ClientSession) =
        if (isGameThread) loaded === session else settled === session

    /**
     * @return what [exit] needs to leave [session] again
     */
    @JvmStatic
    fun enter(session: ClientSession): ClientSession? {
        if (isGameThread) {
            val previous = loaded
            load(session)
            return previous
        }

        val previous = bound.get()
        bound.set(session)
        return previous
    }

    @JvmStatic
    fun exit(previous: ClientSession?) {
        when {
            isGameThread -> load(previous!!)
            previous == null -> bound.remove()
            else -> bound.set(previous)
        }
    }

    /**
     * Makes [session] the one that stays loaded between frames.
     */
    internal fun rest(session: ClientSession) {
        check(isGameThread) { "Only the game thread moves sessions" }
        load(session)
    }

    inline fun <T> within(session: ClientSession, block: () -> T): T {
        val previous = enter(session)
        try {
            return block()
        } finally {
            exit(previous)
        }
    }

    /**
     * [task], run for the session of the thread that schedules it. The active session needs no wrapping
     * on the game thread: its tasks run before the window can change hands.
     */
    @JvmStatic
    fun pinned(task: Runnable): Runnable {
        val session = if (isGameThread) loaded.takeUnless { it.isActive } else bound.get()
        session ?: return task

        return Runnable { within(session) { task.run() } }
    }

    /**
     * [supplier], run for the session that asks for it. For work that is handed to a worker thread.
     */
    @JvmStatic
    fun <T> pinned(supplier: Supplier<T>): Supplier<T> {
        val session = current
        return Supplier { within(session) { supplier.get() } }
    }

    private fun load(session: ClientSession) {
        val previous = loaded
        if (previous === session) {
            return
        }

        // A thread still sees the fields as the previous session's until they are saved, never half moved.
        previous.game.capture()
        settled = null
        session.game.install()
        loaded = session
        settled = session
    }

}
