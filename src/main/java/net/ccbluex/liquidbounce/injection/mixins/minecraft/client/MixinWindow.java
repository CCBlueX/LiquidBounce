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
 */

package net.ccbluex.liquidbounce.injection.mixins.minecraft.client;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.ScaleFactorChangeEvent;
import net.ccbluex.liquidbounce.event.events.WindowResizeEvent;
import net.ccbluex.liquidbounce.features.misc.HideAppearance;
import net.minecraft.client.util.Icons;
import net.minecraft.client.util.Window;
import net.minecraft.resource.InputSupplier;
import net.minecraft.resource.ResourcePack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Mixin(Window.class)
public class MixinWindow {

    @Shadow
    @Final
    private long handle;

    /**
     * Set the window icon to our client icon.
     *
     * @return modified game icon
     */
    @Redirect(method = "setIcon", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/Icons;getIcons(Lnet/minecraft/resource/ResourcePack;)Ljava/util/List;"))
    private List<InputSupplier<InputStream>> setupIcon(Icons instance, ResourcePack resourcePack) throws IOException {
        if (HideAppearance.INSTANCE.isHidingNow()) {
            return instance.getIcons(resourcePack);
        }

        LiquidBounce.INSTANCE.getLogger().debug("Loading client icons");

        // Find client icons
        final InputStream stream16 = LiquidBounce.class.getResourceAsStream("/assets/liquidbounce/icon_16x16.png");
        final InputStream stream32 = LiquidBounce.class.getResourceAsStream("/assets/liquidbounce/icon_32x32.png");

        // In case one of the icons was not found
        if (stream16 == null || stream32 == null) {
            LiquidBounce.INSTANCE.getLogger().error("Unable to find client icons.");

            // Load default icons
            return instance.getIcons(resourcePack);
        }

        return List.of(() -> stream16, () -> stream32);
    }

    /**
     * Hook window resize
     */
    @Inject(method = "onWindowSizeChanged", at = @At("RETURN"))
    public void hookResize(long window, int width, int height, CallbackInfo callbackInfo) {
        if (window == handle) {
            EventManager.INSTANCE.callEvent(new WindowResizeEvent(width, height));
        }
    }

    @Inject(method = "setScaleFactor", at = @At("RETURN"))
    public void hookScaleFactor(double scaleFactor, CallbackInfo ci) {
        EventManager.INSTANCE.callEvent(new ScaleFactorChangeEvent(scaleFactor));
    }

}
