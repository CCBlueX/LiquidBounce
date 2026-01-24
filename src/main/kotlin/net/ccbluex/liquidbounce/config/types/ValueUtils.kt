/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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

package net.ccbluex.liquidbounce.config.types

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.api.core.ioScope
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.utils.client.asPlainText
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.kotlin.Minecraft
import net.ccbluex.liquidbounce.utils.render.asTexture
import net.ccbluex.liquidbounce.utils.render.toNativeImage
import net.minecraft.ChatFormatting
import net.minecraft.client.renderer.texture.DynamicTexture
import kotlin.properties.ReadOnlyProperty
import kotlin.time.Duration.Companion.seconds


/**
 * Convert the [FileValue] to a [ReadOnlyProperty] of [DynamicTexture].
 */
fun <V> FileValue.toTextureProperty(
    owner: V,
    printErrorToChat: Boolean = true,
): ReadOnlyProperty<Any?, DynamicTexture?> where V : EventListener, V : Value<*> {
    var texture: DynamicTexture? = null
    ioScope.launch {
        asStateFlow().filter { it.isFile }.collectLatest { file ->
            while (!inGame || !owner.running) {
                delay(1.seconds)
            }

            try {
                val nativeImage = file.inputStream().toNativeImage()
                withContext(Dispatchers.Minecraft) {
                    texture = nativeImage.asTexture("(${owner.name}) File texture: ${file.name}")
                }
            } catch (e: Exception) {
                val message = "Failed to load texture from '${file.name}' for ${owner.name}"
                if (owner.running && printErrorToChat) {
                    chat("$message (${e.javaClass.simpleName})".asPlainText(ChatFormatting.RED))
                }
                logger.error(message, e)
            }
        }
    }

    return ReadOnlyProperty { _, _ -> texture }
}
