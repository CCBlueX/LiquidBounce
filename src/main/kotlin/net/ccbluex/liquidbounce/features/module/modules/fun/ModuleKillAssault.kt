package net.ccbluex.liquidbounce.features.module.modules.`fun`

import net.ccbluex.liquidbounce.event.events.AttackEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.entity.Entity
import net.minecraft.entity.player.PlayerEntity

/**
 * Kill Assault module
 *
 * Sends toxic messages upon a kill.
 */
object ModuleKillAssault : Module("KillAssault", Category.FUN) {

    private val toxicWordsArray by textArray("Toxic words", mutableListOf("Liquidbounce on top", "Liquidbounce > {TARGET}", "Tired of getting crapped on? Get Liquidbounce"))

    private var lastTarget: Entity? = null

    @Suppress("unused")
    val attackHandler = handler<AttackEvent> { event ->
        val enemy = event.enemy

        if (enemy !is PlayerEntity) {
            return@handler
        }

        lastTarget = enemy
    }

    val repeatable = repeatable {
        val target = lastTarget ?: return@repeatable
        if (target.isAlive) {
            return@repeatable
        }
        
        // Getting a random message from the toxic words list.
        // If we find a {TARGET}, that will get replaced by the enemy's name.
        val randomToxicWord = toxicWordsArray.random().replace("{TARGET}", target.name.string)

        network.sendChatMessage(randomToxicWord)

        lastTarget = null
    }
}
