package net.ccbluex.liquidbounce.features.module.modules.`fun`

import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.event.player.AttackEntityCallback
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.ActionResult

/**
 * Kill Assault module
 *
 * Sends messages upon a kill.
 *
 * USAGE ->
 *  * USING {TARGET} will replace with the enemy that you killed's name.
 */
object ModuleKillAssault : Module("KillAssault", Category.FUN) {

    private val toxicWordsArray by textArray("Toxic words", mutableListOf("Liquidbounce dogged {TARGET}", "Liquidbounce > any cheat", "Tired of getting crapped on? Get Liquidbounce"))

    private var lastAttackedEntity: PlayerEntity? = null

    init {
        repeatable {
            AttackEntityCallback.EVENT.register { player, world, hand, entity, hitResult ->
                if (entity is PlayerEntity && entity !== MinecraftClient.getInstance().player) {
                    lastAttackedEntity = entity
                }
                ActionResult.SUCCESS
            }
        }

        ClientTickEvents.END_CLIENT_TICK.register {
            lastAttackedEntity?.let { entity ->
                if (entity.isDead) {
                    // Selecting a random toxic word and replace {TARGET} with the attacked player's name
                    val randomToxicWord = toxicWordsArray.random().replace("{TARGET}", entity.name.toString())

                    network.sendChatMessage(randomToxicWord.toString())

                    lastAttackedEntity = null
                }
            }
        }
    }
}
