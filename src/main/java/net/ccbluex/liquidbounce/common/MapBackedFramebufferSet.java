package net.ccbluex.liquidbounce.common;

import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.util.Handle;
import net.minecraft.util.Identifier;

import javax.annotation.Nullable;
import java.util.HashMap;

/**
 * Stupid class, but Minecraft needed one more abstraction...
 */
public record MapBackedFramebufferSet(
        HashMap<Identifier, Handle<Framebuffer>> backingMap
) implements PostEffectProcessor.FramebufferSet {

    @Override
    public void set(Identifier id, Handle<Framebuffer> framebuffer) {
        this.backingMap.put(id, framebuffer);
    }

    @Override
    public @Nullable Handle<Framebuffer> get(Identifier id) {
        return this.backingMap.get(id);
    }
}
