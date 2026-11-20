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
package net.ccbluex.liquidbounce.injection.mixins.minecraft.render.entity.feature;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.ccbluex.liquidbounce.features.cosmetic.EarsCosmeticsManager;
import net.ccbluex.liquidbounce.features.misc.SelfDestruct;
import net.ccbluex.liquidbounce.interfaces.EntityRenderStateAddition;
import net.minecraft.client.renderer.entity.layers.Deadmau5EarsLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Deadmau5EarsLayer.class)
public abstract class MixinDeadmau5EarsLayer {

    @ModifyExpressionValue(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;showExtraEars:Z", remap = false, opcode = Opcodes.GETFIELD))
    private boolean onRender(boolean original, @Local(argsOnly = true, name = "state") AvatarRenderState state) {
        return original || liquid_bounce$earsTexture(state) != null;
    }

    @ModifyArg(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;entitySolid(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;"))
    private Identifier useEarsTexture(Identifier skin, @Local(argsOnly = true, name = "state") AvatarRenderState state) {
        var texture = liquid_bounce$earsTexture(state);
        return texture != null ? texture : skin;
    }

    @Unique
    private static @Nullable Identifier liquid_bounce$earsTexture(AvatarRenderState state) {
        if (SelfDestruct.INSTANCE.isDestructed()) return null;

        var entity = ((EntityRenderStateAddition) state).liquid_bounce$getEntity();
        return entity != null ? EarsCosmeticsManager.INSTANCE.earsTexture(entity.getUUID()) : null;
    }

}
