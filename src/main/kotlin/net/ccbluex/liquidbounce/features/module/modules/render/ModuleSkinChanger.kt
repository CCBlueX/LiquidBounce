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
@file:OptIn(FlowPreview::class)

package net.ccbluex.liquidbounce.features.module.modules.render

import com.mojang.authlib.GameProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.core.renderScope
import net.ccbluex.liquidbounce.authlib.utils.generateOfflinePlayerUuid
import net.ccbluex.liquidbounce.authlib.yggdrasil.GameProfileRepository
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.registerTexture
import net.minecraft.client.network.PlayerListEntry
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.util.SkinTextures
import net.minecraft.util.Identifier
import java.util.function.Supplier
import kotlin.time.Duration.Companion.seconds

object ModuleSkinChanger : ClientModule("SkinChanger", Category.RENDER) {

    private val mode = choices("Mode", 0) {
        arrayOf(Mode.Online, Mode.File)
    }

    private suspend fun waitUntilInGame() {
        while (!inGame) {
            delay(1.seconds)
        }
    }

    private inline fun <T> Flow<T>.debounceUntilInGame(crossinline action: suspend (T) -> Unit) {
        renderScope.launch {
            this@debounceUntilInGame.debounce { 2.seconds }.collectLatest {
                waitUntilInGame()
                try {
                    action(it)
                } catch (e: Exception) {
                    if (this@ModuleSkinChanger.running) {
                        chat("Unable to load custom skin because: ${e.message} (${e.javaClass.simpleName})")
                    }
                    logger.error("Unable to load custom skin", e)
                }
            }
        }
    }

    private sealed class Mode(name: String) : Choice(name) {
        final override val parent: ChoiceConfigurable<*>
            get() = mode

        abstract val skinTextures: Supplier<SkinTextures>?

        object Online : Mode("Online") {
            private val username = text("Username", "LiquidBounce")

            init {
                username.asStateFlow().debounceUntilInGame { username ->
                    skinTextures = textureSupplier(username)
                }
            }

            override var skinTextures: Supplier<SkinTextures>? = null

            private suspend fun textureSupplier(username: String): Supplier<SkinTextures> {
                val profile = withContext(Dispatchers.IO) {
                    val uuid = GameProfileRepository().fetchUuidByUsername(username)
                        ?: generateOfflinePlayerUuid(username)
                    mc.sessionService.fetchProfile(uuid, false)?.profile
                        ?: GameProfile(uuid, username)
                }

                return PlayerListEntry.texturesSupplier(profile)
            }
        }

        object File : Mode("File") {
            private val image = file("Image")

            private val model by enumChoice("Model", ModelChoice.WIDE)

            private enum class ModelChoice(
                override val choiceName: String,
                val model: SkinTextures.Model,
            ) : NamedChoice {
                SLIM("Slim", SkinTextures.Model.SLIM),
                WIDE("Default", SkinTextures.Model.WIDE),
            }

            override var skinTextures: Supplier<SkinTextures>? = null

            init {
                image.asStateFlow().filter { it.isFile }.debounceUntilInGame { file ->
                    val id = Identifier.of(
                        LiquidBounce.CLIENT_NAME.lowercase(),
                        "skin-changer-from-file"
                    )

                    // New texture will replace the old one
                    val nativeImage = withContext(Dispatchers.IO) {
                        NativeImage.read(file.inputStream())
                    }

                    nativeImage.registerTexture(id)

                    skinTextures = Supplier {
                        SkinTextures(id, null, null, null, model.model, false)
                    }
                }
            }
        }
    }

    val skinTextures: Supplier<SkinTextures>? get() = mode.activeChoice.skinTextures

}
