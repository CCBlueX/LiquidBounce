package net.ccbluex.liquidbounce.injection.mixins.minecraft.gui;

import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

/**
 * @author 00101110001100010111000101111
 * @since 12/18/2024
 **/
@Mixin(ChatHud.class)
public interface MixinChatHudAccessor {
    @Invoker("toChatLineY")
    double invokeToChatLineY(double y);

    @Invoker("getMessageIndex")
    int invokeGetMessageIndex(double chatLineX, double chatLineY);

    @Invoker("getLineHeight")
    int invokeGetLineHeight();

    @Accessor
    List<ChatHudLine.Visible> getVisibleMessages();
}
