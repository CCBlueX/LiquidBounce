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

package net.ccbluex.liquidbounce.interfaces;

import com.mojang.blaze3d.systems.RenderPass;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Consumer;

public interface PostEffectProcessorAdditions {

    /**
     * Used for rendering the ui blur as it requires a 3-way merge.
     */
    void liquid_bounce$renderWithAdditionalExternalTargets(
            Framebuffer framebuffer,
            ObjectAllocator objectAllocator,
            @Nullable Consumer<RenderPass> additionalUniformsSetter,
            Map<Identifier, Framebuffer> additionalExternalFramebuffers
    );
}
