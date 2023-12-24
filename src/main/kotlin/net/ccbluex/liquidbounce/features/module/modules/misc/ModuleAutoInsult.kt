/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
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

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.DummyEvent
import net.ccbluex.liquidbounce.event.Sequence
import net.ccbluex.liquidbounce.event.SuspendableHandler
import net.ccbluex.liquidbounce.event.events.ChatReceiveEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.chat
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket
import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleHighJump


/**
 * Auto insult module
 *
 * Sends a funny message in chat when someone is killed by you.
 */
object ModuleAutoInsult : Module("AutoInsult", Category.MISC) {


    private val modes = choices(
        "Mode", toxic, arrayOf(
            toxic, liquidbounce
        )
    )
    // A list of funny messages to choose from
    val toxic = listOf(
        "lmao",
        "you sit on your ass all day and still die? sad",
        "JEW SPOTTED",
        "You should uninstall the game.",
        "You are a disgrace to your team.",
        "You call that a fight?",
        "You are wasting your time and mine.",
        "You should practice more.",
        "You are not even a challenge.",
        "my disabled brother could beat you in a 1v1"
    )


    val liquidbounce = listOf(
        "Install LiquidBounce Today",
        "You sit on your ass all day and some german developers still kill you, sad",
        "LiquidBounce is The Next Lunar",
        "bro, i'm legit i use LiquidBounce",
        "ur not a sigma if u dont install the liquidbounce",
        "LiquidBounce Gives you Wings!"


    )

    // A random number generator
    val random = java.util.Random()

    // A handler for chat receive events
    val onChat = handler<ChatReceiveEvent> { event ->
        val msg = event.message

        // A regular expression for matching kill messages
        val killRegex = Regex("${mc.player.name} killed (.+)")

        // If the message matches the kill message, send a funny message in chat
        val matchResult = killRegex.matchEntire(msg)
        if (matchResult != null) {
            // Get the name of the player who was killed
            val killedPlayer = matchResult.groupValues[1]

            // Choose a random message from the list
            val toxic = messages.random(random)

            // Send the message using the network handler
            network.sendChatMessage(message)
        }
    }
}
