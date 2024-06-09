package net.ccbluex.liquidbounce.injection.mixins.minecraft.text;

import net.ccbluex.liquidbounce.interfaces.ChatMessageAddition;
import net.minecraft.client.gui.hud.ChatHudLine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ChatHudLine.Visible.class)
public abstract class MixinChatHudLineVisible implements ChatMessageAddition {

    @Unique
    private String liquid_bounce$id = null;

    @Unique
    @Override
    public void liquid_bounce$setId(String id) {
        this.liquid_bounce$id = id;
    }

    @Unique
    @Override
    public String liquid_bounce$getId() {
        return liquid_bounce$id;
    }

}
