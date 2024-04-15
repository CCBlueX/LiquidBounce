package net.ccbluex.liquidbounce.features.module.modules.`fun`

import net.ccbluex.liquidbounce.event.events.AttackEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.Entity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.text.Text

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

        if (enemy !is PlayerEntity) return@handler

        lastTarget = enemy
    }

    val tickRepeatable = repeatable {
        if (lastTarget!!.isAlive) return@repeatable

        val randomToxicWord = toxicWordsArray.random().replace("{TARGET}", lastTarget!!.name.string)
        val toxicMessage = Text.of(randomToxicWord)

        network.sendChatMessage(randomToxicWord)

        lastTarget = null
    }
}
