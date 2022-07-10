/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.client;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.Render2DEvent;
import net.ccbluex.liquidbounce.utils.ClassUtilsKt;
import net.minecraft.client.Minecraft;
import net.minecraft.profiler.Profiler;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Profiler.class)
public class MixinProfiler
{
    @Inject(method = "startSection", at = @At("HEAD"))
    private void labyModCompatibility(final String name, final CallbackInfo callbackInfo)
    {
        if ("bossHealth".equals(name) && ClassUtilsKt.hasLabyMod())
        {
            Minecraft.getMinecraft().mcProfiler.startSection("LiquidBounce-Render2DEvent");
            LiquidBounce.eventManager.callEvent(new Render2DEvent(0.0F), true);
            Minecraft.getMinecraft().mcProfiler.endStartSection(name);
        }
    }
}
