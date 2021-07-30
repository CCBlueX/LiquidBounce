/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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

package net.ccbluex.liquidbounce.features.cosmetic

import com.google.common.hash.Hashing
import com.mojang.authlib.minecraft.MinecraftProfileTexture
import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.MinecraftClient
import net.minecraft.client.texture.AbstractTexture
import net.minecraft.client.texture.MissingSprite
import net.minecraft.client.texture.PlayerSkinProvider
import net.minecraft.client.texture.PlayerSkinTexture
import net.minecraft.client.util.DefaultSkinHelper
import net.minecraft.util.Identifier
import net.minecraft.util.Util
import java.io.File
import java.util.*

object Cosmetics {

    private val cacheFolder = File("cache").apply {
        if (!exists()) {
            mkdir()
        }
    }

    private val capesFolder = File(cacheFolder, "capes").apply {
        if (!exists()) {
            mkdir()
        }
    }

    const val CAPES_API = "http://capes.liquidbounce.net/api/v1/cape/uuid/%s"

    fun loadCosmeticTexture(uuid: UUID, callback: PlayerSkinProvider.SkinTextureAvailableCallback?) {
        val runnable = Runnable {
            MinecraftClient.getInstance().execute {
                RenderSystem.recordRenderCall {
                    val string = Hashing.sha1().hashUnencodedChars(uuid.toString()).toString()
                    val identifier = Identifier("cosmetic_capes/$string")
                    val abstractTexture: AbstractTexture = mc.textureManager.getOrDefault(identifier, MissingSprite.getMissingSpriteTexture())

                    if (abstractTexture === MissingSprite.getMissingSpriteTexture()) {
                        val file = File(capesFolder, if (string.length > 2) string.substring(0, 2) else "xx")
                        val file2 = File(file, string)

                        val playerSkinTexture = PlayerSkinTexture(file2, String.format(CAPES_API, uuid.toString().replace("-", "")), DefaultSkinHelper.getTexture(), false) {
                            callback?.onSkinTextureAvailable(MinecraftProfileTexture.Type.CAPE, identifier, null)
                        }

                        mc.textureManager.registerTexture(identifier, playerSkinTexture)
                    } else {
                        callback?.onSkinTextureAvailable(MinecraftProfileTexture.Type.CAPE, identifier, null)
                    }
                }
            }
        }

        Util.getMainWorkerExecutor().execute(runnable)
    }

}
