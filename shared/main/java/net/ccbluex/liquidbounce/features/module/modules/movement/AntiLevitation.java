package net.ccbluex.liquidbounce.features.module.modules.movement;

import net.ccbluex.liquidbounce.api.minecraft.potion.PotionType;
import net.ccbluex.liquidbounce.event.EventState;
import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.MotionEvent;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.minecraft.potion.Potion;

@ModuleInfo(name = "AntiLevitation", description = "", category = ModuleCategory.MOVEMENT)
public class AntiLevitation extends Module {
    @EventTarget
    public void onMotion(MotionEvent event) {
        if(event.getEventState() == EventState.PRE) {
            if (mc.getThePlayer().isPotionActive(classProvider.getPotionEnum(PotionType.BLINDNESS))) {
                mc.getThePlayer().removeActivePotionEffect(classProvider.getPotionEnum(PotionType.BLINDNESS)));
            }
        }
    }
}
