/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity;
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityLivingBase;
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.IEntityPlayer;
import net.ccbluex.liquidbounce.api.minecraft.client.network.INetworkPlayerInfo;
import net.ccbluex.liquidbounce.api.minecraft.network.IPacket;
import net.ccbluex.liquidbounce.api.minecraft.network.play.server.ISPacketAnimation;
import net.ccbluex.liquidbounce.api.minecraft.network.play.server.ISPacketEntity;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ModuleInfo(name = "AntiBot", description = "Prevents KillAura from attacking AntiCheat bots.", category = ModuleCategory.MISC)
public class AntiBot extends Module {
    private final BoolValue tabValue = new BoolValue("Tab", true);
    private final ListValue tabModeValue = new ListValue("TabMode", new String[]{"Equals", "Contains"}, "Contains");
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

    public static boolean isBot(final IEntityLivingBase entity) {
        if (!classProvider.isEntityPlayer(entity))
            return false;

        final AntiBot antiBot = (AntiBot) LiquidBounce.moduleManager.getModule(AntiBot.class);

        if (antiBot == null || !antiBot.getState())
            return false;

        if (antiBot.colorValue.get() && !entity.getDisplayName().getFormattedText()
                .replace("§r", "").contains("§"))
            return true;

        if (antiBot.livingTimeValue.get() && entity.getTicksExisted() < antiBot.livingTimeTicksValue.get())
            return true;

        if (antiBot.groundValue.get() && !antiBot.ground.contains(entity.getEntityId()))
            return true;

        if (antiBot.airValue.get() && !antiBot.air.contains(entity.getEntityId()))
            return true;

        if (antiBot.swingValue.get() && !antiBot.swing.contains(entity.getEntityId()))
            return true;

        if (antiBot.healthValue.get() && entity.getHealth() > 20F)
            return true;

        if (antiBot.entityIDValue.get() && (entity.getEntityId() >= 1000000000 || entity.getEntityId() <= -1))
            return true;

        if (antiBot.derpValue.get() && (entity.getRotationPitch() > 90F || entity.getRotationPitch() < -90F))
            return true;

        if (antiBot.wasInvisibleValue.get() && antiBot.invisible.contains(entity.getEntityId()))
            return true;

        if (antiBot.armorValue.get()) {
            final IEntityPlayer player = entity.asEntityPlayer();

            if (player.getInventory().armorInventory[0] == null && player.getInventory().armorInventory[1] == null &&
                    player.getInventory().armorInventory[2] == null && player.getInventory().armorInventory[3] == null)
                return true;
        }

        if (antiBot.pingValue.get()) {
            IEntityPlayer player = entity.asEntityPlayer();

            if (mc.getNetHandler().getPlayerInfo(player.getUniqueID()).getResponseTime() == 0)
                return true;
        }

        if (antiBot.needHitValue.get() && !antiBot.hitted.contains(entity.getEntityId()))
            return true;

        if (antiBot.invalidGroundValue.get() && antiBot.invalidGround.getOrDefault(entity.getEntityId(), 0) >= 10)
            return true;

        if (antiBot.tabValue.get()) {
            final boolean equals = antiBot.tabModeValue.get().equalsIgnoreCase("Equals");
            final String targetName = ColorUtils.stripColor(entity.getDisplayName().getFormattedText());

            if (targetName != null) {
                for (INetworkPlayerInfo networkPlayerInfo : mc.getNetHandler().getPlayerInfoMap()) {
                    final String networkName = ColorUtils.stripColor(EntityUtils.getName(networkPlayerInfo));

                    if (networkName == null)
                        continue;

                    if (equals ? targetName.equals(networkName) : targetName.contains(networkName))
                        return false;
                }

                return true;
            }
        }

        if (antiBot.duplicateInWorldValue.get()) {
            if (mc.getTheWorld().getLoadedEntityList().stream()
                    .filter(currEntity -> classProvider.isEntityPlayer(currEntity) && currEntity.asEntityPlayer()
                            .getDisplayNameString().equals((currEntity.asEntityPlayer()).getDisplayNameString()))
                    .count() > 1)
                return true;
        }

        if (antiBot.duplicateInTabValue.get()) {
            if (mc.getNetHandler().getPlayerInfoMap().stream()
                    .filter(networkPlayer -> entity.getName().equals(ColorUtils.stripColor(EntityUtils.getName(networkPlayer))))
                    .count() > 1)
                return true;
        }

        return entity.getName().isEmpty() || entity.getName().equals(mc.getThePlayer().getName());
    }

    @Override
    public void onDisable() {
        clearAll();
        super.onDisable();
    }

    @EventTarget
    public void onPacket(final PacketEvent event) {
        if (mc.getThePlayer() == null || mc.getTheWorld() == null)
            return;

        final IPacket packet = event.getPacket();

        if (classProvider.isSPacketEntity(packet)) {
            final ISPacketEntity packetEntity = packet.asSPacketEntity();
            final IEntity entity = packetEntity.getEntity(mc.getTheWorld());

            if (classProvider.isEntityPlayer(entity)) {
                if (packetEntity.getOnGround() && !ground.contains(entity.getEntityId()))
                    ground.add(entity.getEntityId());

                if (!packetEntity.getOnGround() && !air.contains(entity.getEntityId()))
                    air.add(entity.getEntityId());

                if (packetEntity.getOnGround()) {
                    if (entity.getPrevPosY() != entity.getPosY())
                        invalidGround.put(entity.getEntityId(), invalidGround.getOrDefault(entity.getEntityId(), 0) + 1);
                } else {
                    final int currentVL = invalidGround.getOrDefault(entity.getEntityId(), 0) / 2;

                    if (currentVL <= 0)
                        invalidGround.remove(entity.getEntityId());
                    else
                        invalidGround.put(entity.getEntityId(), currentVL);
                }

                if (entity.isInvisible() && !invisible.contains(entity.getEntityId()))
                    invisible.add(entity.getEntityId());
            }
        }

        if (classProvider.isSPacketAnimation(packet)) {
            final ISPacketAnimation packetAnimation = packet.asSPacketAnimation();
            final IEntity entity = mc.getTheWorld().getEntityByID(packetAnimation.getEntityID());

            if (entity != null && classProvider.isEntityLivingBase(entity) && packetAnimation.getAnimationType() == 0 && !swing.contains(entity.getEntityId()))
                swing.add(entity.getEntityId());
        }
    }

    @EventTarget
    public void onAttack(final AttackEvent e) {
        final IEntity entity = e.getTargetEntity();

        if (entity != null && classProvider.isEntityLivingBase(entity) && !hitted.contains(entity.getEntityId()))
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

}
