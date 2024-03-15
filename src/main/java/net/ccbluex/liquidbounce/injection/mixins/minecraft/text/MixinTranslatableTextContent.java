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
