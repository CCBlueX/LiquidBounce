// CapeCosmeticsManager.kt
package net.ccbluex.liquidbounce.features.cosmetic

import com.mojang.authlib.GameProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.ccbluex.liquidbounce.api.core.withScope
import net.ccbluex.liquidbounce.api.models.cosmetics.Cosmetic
import net.ccbluex.liquidbounce.api.models.cosmetics.CosmeticCategory
import net.ccbluex.liquidbounce.api.services.cosmetics.CapeApi
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.DisconnectEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.features.module.modules.client.ModuleCapes
import net.minecraft.util.Identifier

object CapeCosmeticsManager : EventListener {

    private val cachedCapes = mutableMapOf<String, Identifier>()

    interface ReturnCapeTexture {
        fun response(id: Identifier)
    }

    private fun isLocalPlayer(profile: GameProfile): Boolean =
        mc.player != null && mc.player!!.gameProfile.id == profile.id

    fun loadPlayerCape(player: GameProfile, response: ReturnCapeTexture) {
        withScope {
            if (isLocalPlayer(player)) {

                val id = ModuleCapes.getCapeTextureId()
                response.response(id)
            }

            runCatching {
                val uuid = player.id
                CosmeticService.fetchCosmetic(uuid, CosmeticCategory.CAPE) { cosmetic ->
                    val name = getCapeName(cosmetic) ?: return@fetchCosmetic

                    cachedCapes[name]?.let {
                        response.response(it)
                        return@fetchCosmetic
                    }

                    val nativeImageBackedTexture = runCatching {
                        runBlocking(Dispatchers.IO) {
                            CapeApi.getCape(name)
                        }
                    }.getOrNull() ?: return@fetchCosmetic

                    val id = Identifier.of("liquidbounce", "cape-$name")
                    mc.textureManager.registerTexture(id, nativeImageBackedTexture)
                    cachedCapes[name] = id
                    response.response(id)
                }
            }
        }
    }

    private fun getCapeName(cosmetic: Cosmetic): String? =
        if (cosmetic.category == CosmeticCategory.CAPE) cosmetic.extra else null

    @Suppress("unused")
    private val disconnectHandler = handler<DisconnectEvent> {
        cachedCapes.values.forEach { mc.textureManager.destroyTexture(it) }
        cachedCapes.clear()
    }

    fun clearAllCachedCapes() {
        cachedCapes.values.forEach { mc.textureManager.destroyTexture(it) }
        cachedCapes.clear()
    }
}
