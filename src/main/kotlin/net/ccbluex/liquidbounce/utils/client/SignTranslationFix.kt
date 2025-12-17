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
 *
 */

package net.ccbluex.liquidbounce.utils.client

import net.ccbluex.liquidbounce.features.spoofer.SpooferTranslation
import net.minecraft.server.packs.AbstractPackResources
import net.minecraft.server.packs.VanillaPackResources
import net.minecraft.server.packs.PackResources
import net.minecraft.network.chat.contents.KeybindContents
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.FormattedText
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.contents.TranslatableContents
import java.util.*

object VanillaTranslationRecognizer {
    val vanillaKeybinds = mutableSetOf<String>()
    val vanillaTranslations = HashSet<String>()

    fun registerKey(translationKey: String) {
        if (isBuildingVanillaKeybinds) {
            this.vanillaKeybinds.add(translationKey)
        }
    }

    fun isPackLegit(pack: PackResources): Boolean {
        return pack is VanillaPackResources || pack is AbstractPackResources
    }

    var isBuildingVanillaKeybinds = false
}

fun filterNonVanillaText(text: Component): Component {
    if (!SpooferTranslation.running) {
        return text
    }

    val result: MutableComponent = when (val content = text.contents) {
        is KeybindContents -> {
            val keybind: String = content.name

            if (VanillaTranslationRecognizer.vanillaKeybinds.contains(keybind)) {
                MutableComponent.create(content)
            } else {
                MutableComponent.create(SuppressedKeybindTextContent(keybind))
            }
        }

        is TranslatableContents -> {
            val translationKey: String = content.key

            if (VanillaTranslationRecognizer.vanillaTranslations.contains(translationKey)) {
                MutableComponent.create(content)
            } else {
                MutableComponent.create(
                    SuppressedTranslatableTextContent(translationKey, content.fallback, content.args)
                )
            }
        }

        else -> MutableComponent.create(text.contents)
    }

    result.setStyle(text.style)

    for (sibling in text.siblings) {
        result.append(filterNonVanillaText(sibling))
    }

    return result
}

class SuppressedKeybindTextContent(key: String) : KeybindContents(key) {
    private val translated: Component = Component.nullToEmpty(key)

    override fun <T : Any> visit(visitor: FormattedText.ContentConsumer<T>): Optional<T> {
        return translated.visit(visitor)
    }

    override fun <T : Any> visit(visitor: FormattedText.StyledContentConsumer<T>, style: Style): Optional<T> {
        return translated.visit(visitor, style)
    }
}

private class SuppressedTranslatableTextContent(key: String, fallback: String?, args: Array<Any>) :
    TranslatableContents(key, fallback, args) {

    private val translated: Component = Component.nullToEmpty(fallback ?: key)

    override fun <T : Any> visit(visitor: FormattedText.ContentConsumer<T>): Optional<T> {
        return translated.visit(visitor)
    }

    override fun <T : Any> visit(visitor: FormattedText.StyledContentConsumer<T>, style: Style): Optional<T> {
        return translated.visit(visitor, style)
    }
}
