/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.injection.mixins.minecraft.text;

import net.ccbluex.liquidbounce.lang.LanguageManager;
import net.ccbluex.liquidbounce.lang.LanguageText;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Language;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TranslatableTextContent.class)
public class MixinTranslatableTextContent {

    @Redirect(method = "updateTranslations", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/Language;getInstance()Lnet/minecraft/util/Language;"))
    private Language hookClientTranslations() {
        if ((Object) this instanceof LanguageText) {
            return LanguageManager.INSTANCE.getLanguage();
        } else {
            return Language.getInstance();
        }
    }

}
