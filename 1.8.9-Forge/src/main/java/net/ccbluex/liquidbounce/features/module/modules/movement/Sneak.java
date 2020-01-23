/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement;

import net.ccbluex.liquidbounce.event.EventState;
import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.MotionEvent;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.utils.MovementUtils;
import net.ccbluex.liquidbounce.value.BoolValue;
import net.ccbluex.liquidbounce.value.ListValue;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.network.play.client.C0BPacketEntityAction;

@ModuleInfo(name = "Sneak", description = "Automatically sneaks all the time.", category = ModuleCategory.MOVEMENT)
public class Sneak extends Module {

    public final ListValue modeValue = new ListValue("Mode", new String[] {"Legit", "Vanilla", "Switch", "MineSecure"}, "MineSecure");
    public final BoolValue stopMoveValue = new BoolValue("StopMove", false);

    private boolean sneaked;

    @Override
    public void onEnable() {
        if(mc.thePlayer == null)
            return;

        if("vanilla".equalsIgnoreCase(modeValue.get())) {
            mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SNEAKING));
        }
    }

    @EventTarget
    public void onMotion(final MotionEvent event) {
        if(stopMoveValue.get() && MovementUtils.isMoving()) {
            if(sneaked) {
                onDisable();
                sneaked = false;
            }
            return;
        }

        sneaked = true;

        switch(modeValue.get().toLowerCase()) {
            case "legit":
                mc.gameSettings.keyBindSneak.pressed = true;
                break;
            case "switch":
                switch(event.getEventState()) {
                    case PRE:
                        if (!MovementUtils.isMoving())
                            return;

                        mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SNEAKING));
                        mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SNEAKING));
                        break;
                    case POST:
                        mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SNEAKING));
                        mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SNEAKING));
                        break;
                }
                break;
            case "minesecure":
                if(event.getEventState() == EventState.PRE)
                    break;

                mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SNEAKING));
                break;
        }
    }

    @Override
    public void onDisable() {
        if(mc.thePlayer == null)
            return;

        switch(modeValue.get().toLowerCase()) {
            case "legit":
                if(!GameSettings.isKeyDown(mc.gameSettings.keyBindSneak))
                    mc.gameSettings.keyBindSneak.pressed = false;
                break;
            case "vanilla":
            case "switch":
            case "minesecure":
                mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SNEAKING));
                break;
        }
        super.onDisable();
    }
}
