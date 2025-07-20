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
package net.ccbluex.liquidbounce.injection.mixins.minecraft.client;

import net.ccbluex.liquidbounce.utils.client.VanillaTranslationRecognizer;
import net.minecraft.client.resource.language.TranslationStorage;
import net.minecraft.resource.Resource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(TranslationStorage.class)
public abstract class MixinTranslationStorage {

    @Shadow
    private static void load(String langCode, List<Resource> resourceRefs, Map<String, String> translations) {
    }

    @Redirect(method = "load(Lnet/minecraft/resource/ResourceManager;Ljava/util/List;Z)Lnet/minecraft/client/resource/language/TranslationStorage;", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resource/language/TranslationStorage;load(Ljava/lang/String;Ljava/util/List;Ljava/util/Map;)V"))
    private static void injectShit(String langCode, List<Resource> resourceRefs, Map<String, String> translations) {
        List<Resource> vanillaResources = new ArrayList<>();
        List<Resource> loadedResources = new ArrayList<>();

        for (Resource res : resourceRefs) {
            if (VanillaTranslationRecognizer.INSTANCE.isPackLegit(res.getPack())) {
                vanillaResources.add(res);
            } else {
                loadedResources.add(res);
            }
        }

        load(langCode, loadedResources, translations);

        var map = new HashMap<String, String>();
        load(langCode, vanillaResources, map);
        VanillaTranslationRecognizer.INSTANCE.getVanillaTranslations().addAll(map.keySet());

        translations.putAll(map);
    }

}
