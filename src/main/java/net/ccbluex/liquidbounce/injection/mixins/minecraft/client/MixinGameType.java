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
package net.ccbluex.liquidbounce.injection.mixins.minecraft.client;

import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(GameType.class)
public abstract class MixinGameType {

    /**
     * In 1.8.X, there was a gamemode type called "NOT_SET" which existed in case the game could not spawn a player/bot with an appropriate gamemode.
     * <p>
     * In newer versions, that has been removed but replaced with a constant variable called DEFAULT which is equals to SURVIVAL.
     * However, this puts AntiBot to a disadvantage because it can't know what gamemode type the player/bot actually has.
     * <p>
     * With this injection though, this is no longer a problem.
     */
    @ModifyVariable(method = "byName(Ljava/lang/String;Lnet/minecraft/world/level/GameType;)Lnet/minecraft/world/level/GameType;", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private static GameType setDefaultAsNull(GameType gameMode) {
        return null;
    }

}
