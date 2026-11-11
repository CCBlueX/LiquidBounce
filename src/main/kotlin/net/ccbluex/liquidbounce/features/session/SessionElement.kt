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

import kotlinx.coroutines.ThreadContextElement
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * Keeps a coroutine in the session it was started in, on whichever thread it continues.
 */
class SessionElement(val session: ClientSession) :
    AbstractCoroutineContextElement(Key), ThreadContextElement<ClientSession?> {

    companion object Key : CoroutineContext.Key<SessionElement>

    override fun updateThreadContext(context: CoroutineContext): ClientSession? = SessionContext.enter(session)

    override fun restoreThreadContext(context: CoroutineContext, oldState: ClientSession?) =
        SessionContext.exit(oldState)

}
