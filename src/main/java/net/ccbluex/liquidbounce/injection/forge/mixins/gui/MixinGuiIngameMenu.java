/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import net.ccbluex.liquidbounce.utils.ServerUtils;
import net.minecraft.client.gui.ButtonWidget;
import net.minecraft.client.gui.GameMenuScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameMenuScreen.class)
public abstract class MixinGameMenuScreen extends MixinGuiScreen {

    @Inject(method = "initGui", at = @At("RETURN"))
    private void initGui(CallbackInfo callbackInfo) {
        if (!mc.isIntegratedServerRunning()) {
            final ButtonWidget disconnectButton = buttonList.get(0);
            disconnectButton.xPosition = width / 2 + 2;
            disconnectButton.width = 98;
            disconnectButton.height = 20;
            buttonList.add(new ButtonWidget(1337, width / 2 - 100, height / 4 + 120 - 16, 98, 20, "Reconnect"));
        }
    }

    @Inject(method = "actionPerformed", at = @At("HEAD"))
    private void actionPerformed(ButtonWidget button, CallbackInfo callbackInfo) {
        if (button.id == 1337) {
            mc.world.sendQuittingDisconnectingPacket();
            ServerUtils.INSTANCE.connectToLastServer();
        }
    }
}