/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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

import net.ccbluex.liquidbounce.event.DummyEvent
import net.ccbluex.liquidbounce.event.Sequence
import net.ccbluex.liquidbounce.event.SuspendableHandler
import net.ccbluex.liquidbounce.event.events.ChatReceiveEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.chat
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket


/**
 * Auto account module
 *
 * Automatically handles logins or registrations on servers when requested.
 */
object ModuleAutoAccount : Module("AutoAccount", Category.MISC, aliases = arrayOf("AutoLogin", "AutoRegister")) {

    private val password by text("Password", "a1b2c3d4")
        .doNotInclude()
    private val delay by intRange("Delay", 3..5, 0..50, "ticks")

    private val registerCommand by text("RegisterCommand", "register")
    private val loginCommand by text("LoginCommand", "login")
    private val resetCommand by text("ResetCommand", "resetpassword")

    private val registerRegexString by text("RegisterRegex", "/register")
    private val loginRegexString by text("LoginRegex", "/login")
    private val resetRegexString by text("ResetRegex", "Wrong Password.")
    private val successRegexString by text("SuccessRegex", "Your request has been accepted")


    var sequence: Sequence<DummyEvent>? = null

    // We can receive chat messages before the world is initialized,
    // so we have to handel events even before the that
    override fun handleEvents() = enabled

    fun login() {
        chat("login")
        network.sendCommand("$loginCommand $password")
    }

    fun register() {
        chat("register")
        network.sendCommand("$registerCommand $password $password")
    }

    fun resetPassword() {
        chat("reset rq")
        network.sendCommand(resetCommand)
    }

    @Suppress("unused")
    val onChat = handler<ChatReceiveEvent> { event ->
        val msg = event.message

        val registerRegex = Regex(registerRegexString)
        if (registerRegex.containsMatchIn(msg)) {
            startDelayedAction { register() }

            network.sendPacket(PlayerMoveC2SPacket.PositionAndOnGround(player.x+0.1,
                player.y+0.1, player.z, true))
            return@handler
        }

        val loginRegex = Regex(loginRegexString)
        if (loginRegex.containsMatchIn(msg)) {
            startDelayedAction { login() }

            network.sendPacket(PlayerMoveC2SPacket.PositionAndOnGround(player.x+0.1,
                player.y+0.1, player.z, true))
            return@handler
        }

        val resetRegex = Regex(resetRegexString)
        if (resetRegex.containsMatchIn(msg)) {
            startDelayedAction { resetPassword() }
            return@handler
        }

        val successRegex = Regex(successRegexString)
        if (successRegex.containsMatchIn(msg)) { // Only kick after reset if it worked
            startDelayedAction { world.disconnect() }
            return@handler
        }
    }

    private fun startDelayedAction(action: SuspendableHandler<DummyEvent>) {
        // cancel the previous sequence
        sequence?.cancel()

        //start the new sequence
        sequence = Sequence(this, {
            waitUntil { mc.networkHandler != null }
            sync()
            waitTicks(delay.random())

            action(it)
        }, DummyEvent())
    }

}
