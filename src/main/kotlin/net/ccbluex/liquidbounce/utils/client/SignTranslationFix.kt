package net.ccbluex.liquidbounce.utils.client

import net.fabricmc.fabric.api.resource.ModResourcePack
import net.minecraft.resource.AbstractFileResourcePack
import net.minecraft.resource.DefaultResourcePack
import net.minecraft.resource.ResourcePack
import net.minecraft.text.KeybindTextContent
import net.minecraft.text.MutableText
import net.minecraft.text.StringVisitable
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.text.TranslatableTextContent
import java.util.*

object VanillaTranslationRecognizer {
    val vanillaKeybinds = mutableSetOf<String>()
    val vanillaTranslations = HashSet<String>()

    fun registerKey(translationKey: String) {
        if (isBuildingVanillaKeybinds) {
            this.vanillaKeybinds.add(translationKey)
        }
    }

    fun isPackLegit(pack: ResourcePack): Boolean {
        return pack is DefaultResourcePack || pack is AbstractFileResourcePack
    }

    fun shouldPreventLoad(pack: ResourcePack): Boolean {
        return pack is ModResourcePack && pack.fabricModMetadata.id == "liquidbounce"
    }

    var isBuildingVanillaKeybinds = false
}

const val ENABLE_TRANSLATION_FIX = true

fun filterNonVanillaText(text: Text): Text {
    if (!ENABLE_TRANSLATION_FIX) {
        return text
    }

    val result: MutableText = when (val content = text.content) {
        is KeybindTextContent -> {
            val keybind: String = content.key

            if (VanillaTranslationRecognizer.vanillaKeybinds.contains(keybind)) {
                MutableText.of(content)
            } else {
                MutableText.of(SuppressedKeybindTextContent(keybind))
            }
        }

        is TranslatableTextContent -> {
            val translationKey: String = content.key

            if (VanillaTranslationRecognizer.vanillaTranslations.contains(translationKey)) {
                MutableText.of(content)
            } else {
                MutableText.of(SuppressedTranslatableTextContent(translationKey, content.fallback, content.args))
            }
        }

        else -> MutableText.of(text.content)
    }

    result.setStyle(text.style)

    for (sibling in text.siblings) {
        result.append(filterNonVanillaText(sibling))
    }

    return result
}

class SuppressedKeybindTextContent(key: String) : KeybindTextContent(key) {
    private val translated: Text = Text.of(key)

    override fun <T : Any?> visit(visitor: StringVisitable.Visitor<T>?): Optional<T> {
        return translated.visit(visitor)
    }

    override fun <T : Any?> visit(visitor: StringVisitable.StyledVisitor<T>?, style: Style?): Optional<T> {
        return translated.visit(visitor, style)
    }
}

class SuppressedTranslatableTextContent(key: String, fallback: String?, args: Array<Any>) :
    TranslatableTextContent(key, fallback, args) {

    private val translated: Text = Text.of(fallback ?: key)

    override fun <T : Any?> visit(visitor: StringVisitable.Visitor<T>?): Optional<T> {
        return translated.visit(visitor)
    }

    override fun <T : Any?> visit(visitor: StringVisitable.StyledVisitor<T>?, style: Style?): Optional<T> {
        return translated.visit(visitor, style)
    }
}
