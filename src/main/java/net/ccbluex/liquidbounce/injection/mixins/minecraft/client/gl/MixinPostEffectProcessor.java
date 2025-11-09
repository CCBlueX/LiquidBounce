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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.client.gl;

import com.mojang.blaze3d.systems.RenderPass;
import net.ccbluex.liquidbounce.common.MapBackedFramebufferSet;
import net.ccbluex.liquidbounce.interfaces.PostEffectProcessorAdditions;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.render.FrameGraphBuilder;
import net.minecraft.client.util.Handle;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Mixin(PostEffectProcessor.class)
public abstract class MixinPostEffectProcessor implements PostEffectProcessorAdditions {

    @Shadow
    public abstract void render(FrameGraphBuilder builder, int textureWidth, int textureHeight, PostEffectProcessor.FramebufferSet framebufferSet, @Nullable Consumer<RenderPass> additionalUniformsSetter);

    @Override
    public void liquid_bounce$renderWithAdditionalExternalTargets(
            Framebuffer framebuffer,
            ObjectAllocator objectAllocator,
            @Nullable Consumer<RenderPass> additionalUniformsSetter,
            Map<Identifier, Framebuffer> additionalExternalFramebuffers
    ) {
        // Copied from PostEffectProcessor.render
        // WARNING: original method is deprecated.
        FrameGraphBuilder frameGraphBuilder = new FrameGraphBuilder();

        var externalFramebufferMap = new HashMap<Identifier, Handle<Framebuffer>>();

        externalFramebufferMap.put(PostEffectProcessor.MAIN, frameGraphBuilder.createObjectNode("main", framebuffer));

        for (var identifierFramebufferEntry : additionalExternalFramebuffers.entrySet()) {
            externalFramebufferMap.put(identifierFramebufferEntry.getKey(), frameGraphBuilder.createObjectNode(identifierFramebufferEntry.getKey().toString(), identifierFramebufferEntry.getValue()));
        }

        PostEffectProcessor.FramebufferSet framebufferSet = new MapBackedFramebufferSet(externalFramebufferMap);

        this.render(frameGraphBuilder, framebuffer.textureWidth, framebuffer.textureHeight, framebufferSet, additionalUniformsSetter);

        frameGraphBuilder.run(objectAllocator);
    }

}
