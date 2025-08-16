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
package net.ccbluex.liquidbounce.features.module.modules.render.nametags.enchantments

import net.ccbluex.liquidbounce.utils.kotlin.LruCache
import net.minecraft.client.resource.language.I18n
import net.minecraft.enchantment.Enchantment
import net.minecraft.enchantment.Enchantments
import net.minecraft.registry.RegistryKey

/**
 * Data class containing display information for an enchantment
 */
data class EnchantmentInfo(
    val displayName: String,
    val isCurse: Boolean = false
)

/**
 * Helper object for processing enchantment names and generating display information
 */
object EnchantmentDisplayHelper {
    private val enchantmentAbbreviationCache = LruCache<RegistryKey<Enchantment>, String>(100)
    
    private val knownCurses = setOf(
        Enchantments.BINDING_CURSE,
        Enchantments.VANISHING_CURSE
    )
    
    /**
     * Gets display information for an enchantment including abbreviated name and curse status
     */
    fun getEnchantmentInfo(enchantment: RegistryKey<Enchantment>): EnchantmentInfo {
        return EnchantmentInfo(
            displayName = getAbbreviation(enchantment),
            isCurse = isCurse(enchantment)
        )
    }
    
    /**
     * Gets the translated name of an enchantment
     */
    private fun getEnchantmentName(enchantment: RegistryKey<Enchantment>): String {
        val idPath = enchantment.value.toString().substringAfter(':')
        val translationKey = "enchantment.minecraft.$idPath"
        return I18n.translate(translationKey)
    }
    
     // Creates abbreviation for a single word by taking first 3 characters
    private fun getSingleWordAbbreviation(word: String): String = word.take(3)
    
     // Creates abbreviation from multiple words by taking first character of each word
    private fun getInitialsAbbreviation(words: List<String>): String = 
        words.joinToString("") { it.first().toString() }
    
    /**
     * Creates compound abbreviation from multiple words
     */
    private fun getCompoundAbbreviation(words: List<String>): String {
        val firstWord = words[0]
        
        if (firstWord.length >= 3) {
            return firstWord.take(3)
        }
        
        val remainingChars = 3 - firstWord.length
        return firstWord + words.getOrNull(1)?.take(remainingChars).orEmpty()
    }
    
    /**
     * Processes multiple words to create optimal abbreviation
     */
    private fun processMultiWordName(words: List<String>): String {
        val initials = getInitialsAbbreviation(words)
        
        return if (initials.length >= 3) {
            initials
        } else {
            getCompoundAbbreviation(words)
        }
    }

    private fun processName(name: String): String {
        if (name.length <= 3) {
            return name
        }

        val words = name.split(" ").filter { it.isNotEmpty() }

        return if (words.size >= 2) {
            processMultiWordName(words)
        } else {
            getSingleWordAbbreviation(words.getOrNull(0) ?: "")
        }
    }

    private fun getAbbreviation(enchantment: RegistryKey<Enchantment>): String {
        return enchantmentAbbreviationCache.getOrPut(enchantment) {
            val name = getEnchantmentName(enchantment)
            processName(name)
        }
    }
    
     // Determines if an enchantment is a curse
    private fun isCurse(enchantment: RegistryKey<Enchantment>): Boolean = enchantment in knownCurses
}
