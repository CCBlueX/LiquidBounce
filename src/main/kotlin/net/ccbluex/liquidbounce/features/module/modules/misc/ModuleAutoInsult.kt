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

import net.ccbluex.liquidbounce.event.events.ChatReceiveEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable


/**
 * Auto insult on kill module
 *
 * Sends an insult in chat when someone is killed by you.
 */
object ModuleAutoInsult : Module("AutoInsult", Category.MISC) {


    private val modes = choices(
        "Mode", Toxic, arrayOf(
            Toxic, LiquidBounce
        )
    )


    private object Toxic : Choice("Toxic") {
        override val parent : ChoiceConfigurable
            get() = modes
    }

    val ToxicInsults = listOf(
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

    private object LiquidBounce : Choice("LiquidBounce") {
        override val parent: ChoiceConfigurable
            get() = modes
    }
    val LiquidBounceInsults = listOf(
        "Install LiquidBounce Today",
        "You sit on your ass all day and some german developers still kill you, sad",
        "LiquidBounce is The Next Lunar",
        "bro, i'm legit i use LiquidBounce",
        "ur not a sigma if u dont install the liquidbounce",
        "LiquidBounce Gives you Wings!"
    )

    // A regular expression for matching kill messages
    private val killRegex = Regex("(\\w+) was slain by ${player.nameForScoreboard}")

    // A handler for chat receive events
    val onChat = handler<ChatReceiveEvent> { event ->
        val msg = event.message

        // If the message matches the kill message, send a funny message in chat
        val matchResult = killRegex.matchEntire(msg)
        if (matchResult != null) {
            // Get the name of the player who was killed
            val killedPlayer = matchResult.groupValues[1]

            // Choose a random message from the list based on the active choice
            val insult = when (modes.activeChoice) {
                Toxic -> ToxicInsults.random()
                LiquidBounce -> LiquidBounceInsults.random()
                else -> "oops, something went wrong"
            }

            // Send the message using the network handler
            network.sendChatMessage(insult)
        }
    }
}
