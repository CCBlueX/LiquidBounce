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

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Map;

@NullMarked
@Mixin(RenderSetup.class)
public interface MixinRenderSetupAccessor {

    @Accessor
    RenderPipeline getPipeline();

    @Accessor
    Map<String, Object> getTextures();

    @Accessor
    TextureTransform getTextureTransform();

    @Accessor
    RenderSetup.OutlineProperty getOutlineProperty();

    @Accessor
    boolean getUseLightmap();

    @Accessor
    boolean getUseOverlay();

    @Accessor
    boolean getAffectsCrumbling();

    @Accessor
    boolean getSortOnUpload();

    @Accessor
    LayeringTransform getLayeringTransform();

    @Invoker("<init>")
    static RenderSetup liquid_bounce$invokeInit(
        RenderPipeline pipeline,
        Map<String, Object> textures,
        boolean useLightmap,
        boolean useOverlay,
        LayeringTransform layeringTransform,
        OutputTarget outputTarget,
        TextureTransform textureTransform,
        RenderSetup.OutlineProperty outlineProperty,
        boolean affectsCrumbling,
        boolean sortOnUpload
    ) {
        throw new AssertionError();
    }

}
