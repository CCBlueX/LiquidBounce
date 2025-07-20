package net.ccbluex.liquidbounce.injection.mixins.minecraft.text;

import com.google.common.base.Objects;
import net.ccbluex.liquidbounce.interfaces.ClientTextColorAdditions;
import net.minecraft.text.TextColor;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Why not Style? Because it is immutable and we would have to edit each and every new instance creation.
 */
@Mixin(TextColor.class)
public class MixinTextColor implements ClientTextColorAdditions {

    @Shadow
    @Final
    private @Nullable String name;
    @Shadow
    @Final
    private int rgb;
    @Unique
    private boolean bypassesNameProtect = false;

    @Override
    public boolean liquid_bounce$doesBypassingNameProtect() {
        return bypassesNameProtect;
    }

    @Override
    public TextColor liquid_bounce$withNameProtectionBypass() {
        var textColor = new TextColor(this.rgb, this.name);

        ((ClientTextColorAdditions) ((Object) textColor)).liquid_bounce$setBypassingNameProtection(true);

        return textColor;
    }

    @Override
    public void liquid_bounce$setBypassingNameProtection(boolean bypassesNameProtect) {
        this.bypassesNameProtect = bypassesNameProtect;
    }

    @Inject(method = "equals", at = @At("RETURN"), cancellable = true)
    private void equals(Object o, CallbackInfoReturnable<Boolean> cir) {
        if (o instanceof TextColor) {
            if (this.bypassesNameProtect != ((ClientTextColorAdditions) o).liquid_bounce$doesBypassingNameProtect()) {
                cir.setReturnValue(false);
            }
        }
    }
    /**
     * @author superblaubeere27
     * @reason Nobody will ever overwrite this method too fr.
     */
    @Overwrite
    public int hashCode() {
        return Objects.hashCode(this.name, this.rgb, this.bypassesNameProtect);
    }

}
