/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.AttackEvent;
import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.PacketEvent;
import net.ccbluex.liquidbounce.event.WorldEvent;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.utils.EntityUtils;
import net.ccbluex.liquidbounce.utils.render.ColorUtils;
import net.ccbluex.liquidbounce.value.BoolValue;
import net.ccbluex.liquidbounce.value.IntegerValue;
import net.ccbluex.liquidbounce.value.ListValue;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S0BPacketAnimation;
import net.minecraft.network.play.server.S14PacketEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ModuleInfo(name = "AntiBot", description = "Prevents KillAura from attacking AntiCheat bots.", category = ModuleCategory.MISC)
public class AntiBot extends Module {

    private final BoolValue tabValue = new BoolValue("Tab", true);
    private final ListValue tabModeValue = new ListValue("TabMode", new String[] {"Equals", "Contains"}, "Contains");
    private final BoolValue entityIDValue = new BoolValue("EntityID", true);
    private final BoolValue colorValue = new BoolValue("Color", false);
    private final BoolValue livingTimeValue = new BoolValue("LivingTime", false);
    private final IntegerValue livingTimeTicksValue = new IntegerValue("LivingTimeTicks", 40, 1, 200);
    private final BoolValue groundValue = new BoolValue("Ground", true);
    private final BoolValue airValue = new BoolValue("Air", false);
    private final BoolValue invalidGroundValue = new BoolValue("InvalidGround", true);
    private final BoolValue swingValue = new BoolValue("Swing", false);
    private final BoolValue healthValue = new BoolValue("Health", false);
    private final BoolValue derpValue = new BoolValue("Derp", true);
    private final BoolValue wasInvisibleValue = new BoolValue("WasInvisible", false);
    private final BoolValue armorValue = new BoolValue("Armor", false);
    private final BoolValue pingValue = new BoolValue("Ping", false);
    private final BoolValue needHitValue = new BoolValue("NeedHit", false);
    private final BoolValue duplicateInWorldValue = new BoolValue("DuplicateInWorld", false);
    private final BoolValue duplicateInTabValue = new BoolValue("DuplicateInTab", false);

    private final List<Integer> ground = new ArrayList<>();
    private final List<Integer> air = new ArrayList<>();
    private final Map<Integer, Integer> invalidGround = new HashMap<>();
    private final List<Integer> swing = new ArrayList<>();
    private final List<Integer> invisible = new ArrayList<>();
    private final List<Integer> hitted = new ArrayList<>();

    @Override
    public void onDisable() {
        clearAll();
        super.onDisable();
    }

    @EventTarget
    public void onPacket(final PacketEvent event) {
        if(mc.thePlayer == null || mc.theWorld == null)
            return;

        final Packet<?> packet = event.getPacket();

        if(packet instanceof S14PacketEntity) {
            final S14PacketEntity packetEntity = (S14PacketEntity) event.getPacket();
            final Entity entity = packetEntity.getEntity(mc.theWorld);

            if(entity instanceof EntityPlayer) {
                if(packetEntity.getOnGround() && !ground.contains(entity.getEntityId()))
                    ground.add(entity.getEntityId());

                if(!packetEntity.getOnGround() && !air.contains(entity.getEntityId()))
                    air.add(entity.getEntityId());

                if(packetEntity.getOnGround()) {
                    if(entity.prevPosY != entity.posY)
                        invalidGround.put(entity.getEntityId(), invalidGround.getOrDefault(entity.getEntityId(), 0) + 1);
                }else{
                    final int currentVL = invalidGround.getOrDefault(entity.getEntityId(), 0) / 2;

                    if(currentVL <= 0)
                        invalidGround.remove(entity.getEntityId());
                    else
                        invalidGround.put(entity.getEntityId(), currentVL);
                }

                if(entity.isInvisible() && !invisible.contains(entity.getEntityId()))
                    invisible.add(entity.getEntityId());
            }
        }

        if(packet instanceof S0BPacketAnimation) {
            final S0BPacketAnimation packetAnimation = (S0BPacketAnimation) event.getPacket();
            final Entity entity = mc.theWorld.getEntityByID(packetAnimation.getEntityID());

            if(entity instanceof EntityLivingBase && packetAnimation.getAnimationType() == 0 && !swing.contains(entity.getEntityId()))
                swing.add(entity.getEntityId());
        }
    }

    @EventTarget
    public void onAttack(final AttackEvent e) {
        final Entity entity = e.getTargetEntity();

        if(entity instanceof EntityLivingBase && !hitted.contains(entity.getEntityId()))
            hitted.add(entity.getEntityId());
    }

    @EventTarget
    public void onWorld(final WorldEvent event) {
        clearAll();
    }

    private void clearAll() {
        hitted.clear();
        swing.clear();
        ground.clear();
        invalidGround.clear();
        invisible.clear();
    }

    public static boolean isBot(final EntityLivingBase entity) {
        if (!(entity instanceof EntityPlayer))
            return false;

        final AntiBot antiBot = (AntiBot) LiquidBounce.moduleManager.getModule(AntiBot.class);

        if (antiBot == null || !antiBot.getState())
            return false;

        if (antiBot.colorValue.get() && !entity.getDisplayName().getFormattedText()
                .replace("§r", "").contains("§"))
            return true;

        if (antiBot.livingTimeValue.get() && entity.ticksExisted < antiBot.livingTimeTicksValue.get())
            return true;

        if (antiBot.groundValue.get() && !antiBot.ground.contains(entity.getEntityId()))
            return true;

        if (antiBot.airValue.get() && !antiBot.air.contains(entity.getEntityId()))
            return true;

        if(antiBot.swingValue.get() && !antiBot.swing.contains(entity.getEntityId()))
            return true;

        if(antiBot.healthValue.get() && entity.getHealth() > 20F)
            return true;

        if(antiBot.entityIDValue.get() && (entity.getEntityId() >= 1000000000 || entity.getEntityId() <= -1))
            return true;

        if(antiBot.derpValue.get() && (entity.rotationPitch > 90F || entity.rotationPitch < -90F))
            return true;

        if(antiBot.wasInvisibleValue.get() && antiBot.invisible.contains(entity.getEntityId()))
            return true;

        if(antiBot.armorValue.get()) {
            final EntityPlayer player = (EntityPlayer) entity;

            if (player.inventory.armorInventory[0] == null && player.inventory.armorInventory[1] == null &&
                    player.inventory.armorInventory[2] == null && player.inventory.armorInventory[3] == null)
                return true;
        }

        if(antiBot.pingValue.get()) {
            EntityPlayer player = (EntityPlayer) entity;

            if(mc.getNetHandler().getPlayerInfo(player.getUniqueID()).getResponseTime() == 0)
                return true;
        }

        if(antiBot.needHitValue.get() && !antiBot.hitted.contains(entity.getEntityId()))
            return true;

        if(antiBot.invalidGroundValue.get() && antiBot.invalidGround.getOrDefault(entity.getEntityId(), 0) >= 10)
            return true;

        if(antiBot.tabValue.get()) {
            final boolean equals = antiBot.tabModeValue.get().equalsIgnoreCase("Equals");
            final String targetName = ColorUtils.stripColor(entity.getDisplayName().getFormattedText());

            if (targetName != null) {
                for (final NetworkPlayerInfo networkPlayerInfo : mc.getNetHandler().getPlayerInfoMap()) {
                    final String networkName = ColorUtils.stripColor(EntityUtils.getName(networkPlayerInfo));

                    if (networkName == null)
                        continue;

                    if (equals ? targetName.equals(networkName) : targetName.contains(networkName))
                        return false;
                }

                return true;
            }
        }

        if(antiBot.duplicateInWorldValue.get()) {
            if (mc.theWorld.loadedEntityList.stream()
                    .filter(currEntity -> currEntity instanceof EntityPlayer && ((EntityPlayer) currEntity)
                            .getDisplayNameString().equals(((EntityPlayer) currEntity).getDisplayNameString()))
                    .count() > 1)
                return true;
        }

        if(antiBot.duplicateInTabValue.get()) {
            if (mc.getNetHandler().getPlayerInfoMap().stream()
                    .filter(networkPlayer -> entity.getName().equals(ColorUtils.stripColor(EntityUtils.getName(networkPlayer))))
                    .count() > 1)
                return true;
        }

        return entity.getName().isEmpty() || entity.getName().equals(mc.thePlayer.getName());
    }

}
