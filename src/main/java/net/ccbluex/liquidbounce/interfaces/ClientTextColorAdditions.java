package net.ccbluex.liquidbounce.interfaces;

import net.minecraft.text.TextColor;

public interface ClientTextColorAdditions {
    boolean liquid_bounce$doesBypassingNameProtect();
    TextColor liquid_bounce$withNameProtectionBypass();

    /**
     * Please don't use this method, it is only for internal use.
     */
    @Deprecated
    void liquid_bounce$setBypassingNameProtection(boolean bypassesNameProtect);
}
