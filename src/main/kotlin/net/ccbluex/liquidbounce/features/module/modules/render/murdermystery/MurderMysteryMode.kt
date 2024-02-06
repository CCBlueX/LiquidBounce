package net.ccbluex.liquidbounce.features.module.modules.render.murdermystery

import net.minecraft.client.network.AbstractClientPlayerEntity
import net.minecraft.util.Identifier

interface MurderMysteryMode {
    fun handleHasBow(
        entity: AbstractClientPlayerEntity,
        locationSkin: Identifier,
    )

    fun handleHasSword(
        entity: AbstractClientPlayerEntity,
        locationSkin: Identifier,
    )

    fun disallowsArrowDodge(): Boolean = false

    fun shouldAttack(entity: AbstractClientPlayerEntity): Boolean

    fun getPlayerType(player: AbstractClientPlayerEntity): PlayerType

    fun reset()

    enum class PlayerType {
        NEUTRAL,
        DETECTIVE_LIKE,
        MURDERER,
    }
}
