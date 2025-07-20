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
package net.ccbluex.liquidbounce.injection.mixins.minecraft.gui;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleAntiExploit;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fixes a ViaFabricPlus bug where it would not correctly handle the scoreboard data, resulting in a disconnect.
 * <p>
 * ViaFabricPlus versions: 3.4.8-?
 */
@Mixin(Scoreboard.class)
public abstract class MixinScoreboard {

    @Shadow
    @Nullable
    public abstract Team getScoreHolderTeam(String scoreHolderName);


    @ModifyExpressionValue(method = "addObjective", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/objects/Object2ObjectMap;containsKey(Ljava/lang/Object;)Z", remap = false))
    private boolean noCrash(boolean original) {
        var antiExploit = ModuleAntiExploit.INSTANCE;
        if (antiExploit.getRunning() && antiExploit.getVfpScoreboardFix()) {
            return false;
        } else {
            return original;
        }
    }

    @Inject(method = "removeScoreHolderFromTeam", at = @At("HEAD"), cancellable = true)
    private void noCrash2(String scoreHolderName, Team team, CallbackInfo ci) {
        var antiExploit = ModuleAntiExploit.INSTANCE;
        if (antiExploit.getRunning() && antiExploit.getVfpScoreboardFix() && getScoreHolderTeam(scoreHolderName) != team) {
            ci.cancel();
        }
    }

}
