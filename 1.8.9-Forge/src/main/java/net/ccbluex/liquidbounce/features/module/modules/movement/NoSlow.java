package net.ccbluex.liquidbounce.features.module.modules.movement;

import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.MotionEvent;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.features.module.ModuleManager;
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura;
import net.ccbluex.liquidbounce.utils.MovementUtils;
import net.ccbluex.liquidbounce.value.BoolValue;
import net.ccbluex.liquidbounce.value.FloatValue;
import net.ccbluex.liquidbounce.value.ListValue;
import net.minecraft.item.*;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
@ModuleInfo(name = "NoSlow", description = "Cancels slowness effects caused by soulsand and using items.", category = ModuleCategory.MOVEMENT)
public class NoSlow extends Module {

    private final String[] modes = new String[] {"Off", "Vanilla", "NCP", "AAC", "Spartan", "KillSwitch", "AntiAura", "NNC", "Custom"};
    private final String defaultMode = "NCP";

    private final ListValue swordValue = new ListValue("Sword", modes, defaultMode);
    private final ListValue consumeValue = new ListValue("Consume", modes, defaultMode);
    private final ListValue bowValue = new ListValue("Bow", modes, defaultMode);
    public final BoolValue soulsandValue = new BoolValue("Soulsand", true);
    public final FloatValue customReducementValue = new FloatValue("CustomReducement", 0.6F, 0F, 1F);

    @EventTarget
    public void onMotion(MotionEvent event) {
        if(mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword) {
            final KillAura killAura = (KillAura) ModuleManager.getModule(KillAura.class);

            if (mc.thePlayer.isBlocking() || killAura.getBlockingStatus()) {
                final String mode = getMode(mc.thePlayer.getHeldItem().getItem());

                if (!MovementUtils.isMoving())
                    return;

                if(mode.equalsIgnoreCase("NCP") || mode.equalsIgnoreCase("NNC")) {
                    switch(event.getEventState()) {
                        case PRE:
                            mc.getNetHandler().addToSendQueue(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
                            break;
                        case POST:
                            mc.getNetHandler().addToSendQueue(new C08PacketPlayerBlockPlacement(mc.thePlayer.inventory.getCurrentItem()));
                            break;
                    }
                }
            }
        }
    }

    public String getMode(final Item item) {
        if(item instanceof ItemSword)
            return swordValue.get();

        if(item instanceof ItemFood || item instanceof ItemPotion || item instanceof ItemBucketMilk)
            return consumeValue.get();

        if(item instanceof ItemBow)
            return bowValue.get();

        return "OFF";
    }
}
