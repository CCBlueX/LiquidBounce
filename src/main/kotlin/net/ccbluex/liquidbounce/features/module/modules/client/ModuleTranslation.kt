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
 *
 */

package net.ccbluex.liquidbounce.features.module.modules.client

import net.ccbluex.liquidbounce.api.thirdparty.translator.TranslateLanguage
import net.ccbluex.liquidbounce.api.thirdparty.translator.TranslationResult
import net.ccbluex.liquidbounce.api.thirdparty.translator.TranslatorApi
import net.ccbluex.liquidbounce.api.thirdparty.translator.providers.GoogleTranslateApi
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule

object ModuleTranslation : ClientModule(
    name = "Translation",
    category = Category.CLIENT,
    notActivatable = true,
    hide = true
), TranslatorApi {
    private val providers = choices("Provider", 0) {
        arrayOf(
            GoogleTranslateApi(it)
        )
    }

    override suspend fun translateInternal(
        sourceLanguage: TranslateLanguage,
        targetLanguage: TranslateLanguage,
        text: String
    ): TranslationResult {
        return providers.activeChoice.translateInternal(
            sourceLanguage,
            targetLanguage,
            text
        )
    }
}
