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
            target = "Lnet/minecraft/util/Language;get(Ljava/lang/String;)Ljava/lang/String;"))
    private String hookClientTranslations(Language instance, String key) {
        if ((Object) this instanceof LanguageText) {
            return LanguageManager.INSTANCE.getTranslation(key);
        } else {
            return instance.get(key);
        }
    }

    @Redirect(method = "updateTranslations", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/Language;get(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"))
    private String hookClientTranslationsWithFallback(Language instance, String key, String fallback) {
        if ((Object) this instanceof LanguageText) {
            return LanguageManager.INSTANCE.getTranslation(key);
        } else {
            return instance.get(key, fallback);
        }
    }

}
