/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2022 CCBlueX
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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.gui;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.suggestion.Suggestions;
import net.ccbluex.liquidbounce.features.command.CommandManager;
import net.minecraft.client.gui.screen.CommandSuggestor;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.command.CommandSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.CompletableFuture;

@Mixin(CommandSuggestor.class)
public class MixinCommandSuggestor {

    @Shadow @Final private boolean slashOptional;

    @Shadow @Final private TextFieldWidget textField;

    @Shadow private CompletableFuture<Suggestions> pendingSuggestions;

    @Shadow private ParseResults<CommandSource> parse;

    @Redirect(method = "refresh", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screen/CommandSuggestor;slashOptional:Z"))
    private boolean injectAutoCompletionA(CommandSuggestor suggestor) {
        return this.slashOptional || this.textField.getText().startsWith(CommandManager.Options.INSTANCE.getPrefix());
    }

    @Inject(method = "refresh", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;getCommandDispatcher()Lcom/mojang/brigadier/CommandDispatcher;"), cancellable = true)
    private void injectAutoCompletionB(CallbackInfo ci) {
        if (this.textField.getText().startsWith(CommandManager.Options.INSTANCE.getPrefix())) {
            this.pendingSuggestions = CommandManager.INSTANCE.autoComplete(this.textField.getText(), this.textField.getCursor());
            this.parse = null;

            ci.cancel();
        }
    }
}
