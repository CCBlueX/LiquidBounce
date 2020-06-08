/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.Render2DEvent;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.file.configs.FriendsConfig;
import net.ccbluex.liquidbounce.utils.ClientUtils;
import net.ccbluex.liquidbounce.utils.render.ColorUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.input.Mouse;

@ModuleInfo(name = "MidClick", description = "Allows you to add a player as a friend by right clicking him.", category = ModuleCategory.MISC)
public class MidClick extends Module {
    private boolean wasDown;

    @EventTarget
    public void onRender(Render2DEvent event) {
        if(mc.currentScreen != null)
            return;

        if(!wasDown && Mouse.isButtonDown(2)) {
            final Entity entity = mc.objectMouseOver.entityHit;

            if(entity instanceof EntityPlayer) {
                final String playerName = ColorUtils.stripColor(entity.getName());
                final FriendsConfig friendsConfig = LiquidBounce.fileManager.friendsConfig;

                if(!friendsConfig.isFriend(playerName)) {
                    friendsConfig.addFriend(playerName);
                    LiquidBounce.fileManager.saveConfig(friendsConfig);
                    ClientUtils.displayChatMessage("§a§l" + playerName + "§c was added to your friends.");
                }else{
                    friendsConfig.removeFriend(playerName);
                    LiquidBounce.fileManager.saveConfig(friendsConfig);
                    ClientUtils.displayChatMessage("§a§l" + playerName + "§c was removed from your friends.");
                }
            }else
                ClientUtils.displayChatMessage("§c§lError: §aYou need to select a player.");
        }

        wasDown = Mouse.isButtonDown(2);
    }
}
