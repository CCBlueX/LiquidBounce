/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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

package net.ccbluex.liquidbounce.features.module.modules.misc

import kotlinx.coroutines.awaitCancellation
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.event.Sequence
import net.ccbluex.liquidbounce.event.events.ChatReceiveEvent
import net.ccbluex.liquidbounce.event.events.ChatSendEvent
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.chat
import kotlin.math.log


/**
 * Spammer module
 *
 * Spams the chat with a given message.
 */
object ModuleAutoLogin : Module("AutoLogin", Category.MISC) {

    private val password by text("Password", "pass123")
    private val delay by intRange("Delay", 3..5, 0..20)

    private val registerCommand by text("RegisterCommand", "register")
    private val loginCommand by text("LoginCommand", "login")

    private val registerRegexString by text("RegisterRegex", "/register")
    private val loginRegexString by text("LoginRegex", "/login")

    var repeatable = repeatable {
        repeat(50) {
//            chat(it.toString())
            waitTicks(1)
        }
    };

    var sequence: Sequence<DummyEvent>? = null



    fun login() {
        network.sendCommand("$loginCommand $password")
    }

    fun register() {
        network.sendCommand("$registerCommand $password $password")
    }

    val onChat = handler<ChatReceiveEvent> { event ->
        val msg = event.message

        val registerRegex = Regex(registerRegexString)

        if (registerRegex.containsMatchIn(msg)) {
            startAction { register() }

            return@handler
        }

        val loginRegex = Regex(loginRegexString)

        if(loginRegex.containsMatchIn(msg)) {
            startAction { login() }
        }
    }

    private fun startAction(action: SuspendableHandler<DummyEvent>) {
        // cancel the previous sequence
        sequence?.cancel()

        //start the new
        sequence = Sequence<DummyEvent>({
            waitTicks(delay.random())
            action(it)
        }, DummyEvent())
    }

}
