package net.ccbluex.liquidbounce.ui.font;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface FontDetails {

    String fontName();

    int fontSize() default -1;
}