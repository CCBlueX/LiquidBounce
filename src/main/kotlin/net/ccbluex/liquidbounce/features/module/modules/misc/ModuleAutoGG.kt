package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.session.GameWins

object ModuleAutoGG : ClientModule("AutoGG", Category.MISC) {
    private val message by text("Message", "Good Game")
    private var lastVictory = GameWins.victoryCount

    @Suppress("unused")
    private val tickHandler = tickHandler {
        if (GameWins.victoryCount > lastVictory) {
            lastVictory = GameWins.victoryCount
            network.sendChatMessage(message)
        }
    }
}
