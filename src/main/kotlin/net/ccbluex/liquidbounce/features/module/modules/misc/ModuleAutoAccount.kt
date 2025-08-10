/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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

import net.ccbluex.liquidbounce.event.Sequence
import net.ccbluex.liquidbounce.event.events.ChatReceiveEvent
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.features.command.commands.module.CommandAutoAccount
import net.ccbluex.liquidbounce.features.misc.HideAppearance
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.client.chat


/**
 * Auto account module
 *
 * Automatically handles logins or registrations on servers when requested.
 *
 * Command: [CommandAutoAccount]
 */
object ModuleAutoAccount : ClientModule("AutoAccount", Category.MISC, aliases = arrayOf("AutoLogin", "AutoRegister")) {

    private val password by text("Password", "a1b2c3d4")
        .doNotIncludeAlways()
    private val delay by intRange("Delay", 3..5, 0..50, "ticks")

    private val registerCommand by text("RegisterCommand", "register")
    private val loginCommand by text("LoginCommand", "login")

    private val registerRegexString: String by text("RegisterRegex", "/register").onChanged {
        registerRegex = Regex(it)
    }
    private val loginRegexString: String by text("LoginRegex", "/login").onChanged {
        loginRegex = Regex(it)
    }

    private var registerRegex = Regex(registerRegexString)
    private var loginRegex = Regex(loginRegexString)

    // We can receive chat messages before the world is initialized,
    // so we have to handle events even before that
    override val running
        get() = !HideAppearance.isDestructed && enabled

    private var sending = false

    override fun onDisabled() {
        sending = false
    }

    private suspend inline fun Sequence.action(operation: () -> Unit) {
        sending = true
        waitUntil { mc.networkHandler != null }
        waitTicks(delay.random())
        operation()
        sending = false
    }

    fun login() {
        chat("login")
        network.sendCommand("$loginCommand $password")
    }

    fun register() {
        chat("register")
        network.sendCommand("$registerCommand $password $password")
    }

    @Suppress("unused")
    val onChat = sequenceHandler<ChatReceiveEvent> { event ->
        if (sending) {
            return@sequenceHandler
        }

        val msg = event.message

        when {
            registerRegex.containsMatchIn(msg) -> {
                action(::register)
            }
            loginRegex.containsMatchIn(msg) -> {
                action(::login)
            }
        }
    }

}
