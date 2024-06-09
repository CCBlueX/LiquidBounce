package net.ccbluex.liquidbounce.injection.mixins.minecraft.text;

import net.ccbluex.liquidbounce.interfaces.ChatHudLineAddition;
import net.ccbluex.liquidbounce.interfaces.ChatMessageAddition;
import net.minecraft.client.gui.hud.ChatHudLine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ChatHudLine.class)
public abstract class MixinChatHudLine implements ChatMessageAddition, ChatHudLineAddition {

    @Unique
    private String liquid_bounce$id = null;

    @Unique
    int liquid_bounce$count = 1;

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

    @Unique
    @Override
    public void liquid_bounce$setCount(int count) {
        this.liquid_bounce$count = count;
    }

    @Unique
    @Override
    public int liquid_bounce$getCount() {
        return liquid_bounce$count;
    }

}
