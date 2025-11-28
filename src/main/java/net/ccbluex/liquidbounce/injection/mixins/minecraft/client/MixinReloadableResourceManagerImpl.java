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

import net.ccbluex.liquidbounce.common.LazyLanguageLoader_StateManager;
import net.minecraft.client.resource.language.LanguageManager;
import net.minecraft.client.search.SearchManager;
import net.minecraft.resource.ReloadableResourceManagerImpl;
import net.minecraft.resource.ResourceReloader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/*
 * Code From https://github.com/ChachyDev/lazy-language-loader/
 */
@Mixin(ReloadableResourceManagerImpl.class)
public class MixinReloadableResourceManagerImpl {
    @ModifyArg(
        method = "reload",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/resource/SimpleResourceReload;start(Lnet/minecraft/resource/ResourceManager;Ljava/util/List;Ljava/util/concurrent/Executor;Ljava/util/concurrent/Executor;Ljava/util/concurrent/CompletableFuture;Z)Lnet/minecraft/resource/ResourceReload;"
        )
    )
    private List<ResourceReloader> lazyLanguageLoader$$onReload(List<ResourceReloader> reloaders) {
        return LazyLanguageLoader_StateManager.isResourceLoadViaLanguage() ? LazyLanguageLoader_StateManager.getResourceReloaders() : reloaders;
    }

    @Inject(method = "registerReloader", at = @At("HEAD"))
    private void lazyLanguageLoader$$onRegisterReloader(ResourceReloader reloader, CallbackInfo ci) {
        if (reloader instanceof LanguageManager || reloader instanceof SearchManager) {
            LazyLanguageLoader_StateManager.addResourceReloader(reloader);
        }
    }
}
