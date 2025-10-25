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
