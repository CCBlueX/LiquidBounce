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
package net.ccbluex.liquidbounce.injection.mixins.minecraft.render;

import net.ccbluex.liquidbounce.features.session.SessionContext;
import net.minecraft.client.renderer.extract.LevelExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Every level reports its changes to the one renderer, which only draws the level of the active session.
 */
@Mixin(LevelExtractor.class)
public abstract class MixinLevelExtractor {

    @Inject(method = {
        "blockChanged",
        "setBlocksDirty",
        "setBlockDirty",
        "setSectionDirtyWithNeighbors",
        "setSectionRangeDirty",
        "setSectionDirty",
        "queueTransientBlock"
    }, at = @At("HEAD"), cancellable = true)
    private void skipBackgroundSession(CallbackInfo ci) {
        if (SessionContext.isBackground()) {
            ci.cancel();
        }
    }

}
