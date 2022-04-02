/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.api.IClassProvider;
import net.ccbluex.liquidbounce.api.IExtractedFunctions;
import net.ccbluex.liquidbounce.api.minecraft.client.IMinecraft;

public class MinecraftInstance {
    public static final IMinecraft mc = LiquidBounce.wrapper.getMinecraft();
    public static final IClassProvider classProvider = LiquidBounce.INSTANCE.getWrapper().getClassProvider();
    public static final IExtractedFunctions functions = LiquidBounce.INSTANCE.getWrapper().getFunctions();
}
