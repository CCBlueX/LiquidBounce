package net.ccbluex.liquidbounce.common;

import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.util.Handle;
import net.minecraft.util.Identifier;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Stupid class, but Minecraft needed one more abstraction...
 */
public class MapBackedFramebufferSet implements PostEffectProcessor.FramebufferSet {
    private final HashMap<Identifier, Handle<Framebuffer>> backingMap;

    public MapBackedFramebufferSet(HashMap<Identifier, Handle<Framebuffer>> backingMap) {
        this.backingMap = backingMap;
    }

    @Override
    public void set(Identifier id, Handle<Framebuffer> framebuffer) {
        this.backingMap.put(id, framebuffer);
    }

    @Override
    public @Nullable Handle<Framebuffer> get(Identifier id) {
        return this.backingMap.get(id);
    }
}
