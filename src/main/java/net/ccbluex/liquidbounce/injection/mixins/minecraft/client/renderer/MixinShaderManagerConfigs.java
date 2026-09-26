/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.client.renderer;

import com.mojang.renderpearl.api.pipeline.ShaderType;
import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.render.ClientShaders;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Locale;

@Mixin(targets = "net.minecraft.client.renderer.ShaderManager$Configs")
public abstract class MixinShaderManagerConfigs {

    @Inject(method = "getShader", at = @At("HEAD"), cancellable = true)
    private void getLiquidBounceShader(
        Identifier id,
        ShaderType type,
        CallbackInfoReturnable<String> cir
    ) {
        if (id.getNamespace().equals(LiquidBounce.CLIENT_NAME.toLowerCase(Locale.ROOT))) {
            cir.setReturnValue(ClientShaders.Source.getShader(id, type));
        }
    }
}
