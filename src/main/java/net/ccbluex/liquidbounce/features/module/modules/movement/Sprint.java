/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement;

import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.TickEvent;
import net.ccbluex.liquidbounce.event.UpdateEvent;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.utils.MovementUtils;
import net.ccbluex.liquidbounce.utils.Rotation;
import net.ccbluex.liquidbounce.utils.RotationUtils;
import net.ccbluex.liquidbounce.value.BoolValue;
import net.ccbluex.liquidbounce.value.ListValue;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.potion.Potion;

@ModuleInfo(name = "Sprint", description = "Automatically sprints all the time.", category = ModuleCategory.MOVEMENT)
public class Sprint extends Module {
    public final ListValue modeValue = new ListValue("Mode", new String[] {"Legit", "Vanilla"}, "Vanilla") {
        @Override
        protected void onUpdate(final String value) {
            if (modeValue.get().equalsIgnoreCase("legit")) {
                allDirectionsValue.setIsSupported(false);
                blindnessValue.setIsSupported(false);
                foodValue.setIsSupported(false);
                checkServerSide.setIsSupported(false);
                checkServerSideGround.setIsSupported(false);
            } else {
                allDirectionsValue.setIsSupported(true);
                blindnessValue.setIsSupported(true);
                foodValue.setIsSupported(true);
                checkServerSide.setIsSupported(true);
                checkServerSideGround.setIsSupported(true);
            }
        }
    };

    public final BoolValue allDirectionsValue = new BoolValue("AllDirections", true);
    public final BoolValue blindnessValue = new BoolValue("Blindness", true);
    public final BoolValue foodValue = new BoolValue("Food", true);

    public final BoolValue checkServerSide = new BoolValue("CheckServerSide", false);
    public final BoolValue checkServerSideGround = new BoolValue("CheckServerSideOnlyGround", false);

    @Override
    public final String getTag() {
        return modeValue.get();
    }

    @EventTarget
    public void onTick(final TickEvent event) {
        if (modeValue.get().equalsIgnoreCase("legit")) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), true);
        }
    }

    @Override
    public void onDisable() {
        if (modeValue.get().equalsIgnoreCase("legit")) {
            final int keyCode = mc.gameSettings.keyBindSprint.getKeyCode();
            KeyBinding.setKeyBindState(keyCode, keyCode > 0 && mc.gameSettings.keyBindSprint.isKeyDown());
        }
    }

    @EventTarget
    public void onUpdate(final UpdateEvent event) {
        if (modeValue.get().equalsIgnoreCase("vanilla")) {
            if (!MovementUtils.isMoving() || mc.thePlayer.isSneaking() ||
                    (blindnessValue.get() && mc.thePlayer.isPotionActive(Potion.blindness)) ||
                    (foodValue.get() && !(mc.thePlayer.getFoodStats().getFoodLevel() > 6.0F || mc.thePlayer.capabilities.allowFlying))
                    || (checkServerSide.get() && (mc.thePlayer.onGround || !checkServerSideGround.get())
                    && !allDirectionsValue.get() && RotationUtils.targetRotation != null &&
                    RotationUtils.getRotationDifference(new Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch)) > 30)) {
                mc.thePlayer.setSprinting(false);
                return;
            }

            if (allDirectionsValue.get() || mc.thePlayer.movementInput.moveForward >= 0.8F)
                mc.thePlayer.setSprinting(true);
        }
    }
}
