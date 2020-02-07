/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement;

import net.ccbluex.liquidbounce.event.*;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.utils.ClientUtils;
import net.ccbluex.liquidbounce.utils.MovementUtils;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.ccbluex.liquidbounce.utils.timer.MSTimer;
import net.ccbluex.liquidbounce.utils.timer.TickTimer;
import net.ccbluex.liquidbounce.value.BoolValue;
import net.ccbluex.liquidbounce.value.FloatValue;
import net.ccbluex.liquidbounce.value.IntegerValue;
import net.ccbluex.liquidbounce.value.ListValue;
import net.minecraft.block.BlockAir;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C00PacketKeepAlive;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.potion.Potion;
import net.minecraft.util.*;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

@ModuleInfo(name = "Fly", description = "Allows you to fly in survival mode.", category = ModuleCategory.MOVEMENT, keyBind = Keyboard.KEY_F)
public class Fly extends Module {

    public final ListValue modeValue = new ListValue("Mode", new String[]{
            "Vanilla",
            "SmoothVanilla",

            // NCP
            "NCP",
            "OldNCP",

            // AAC
            "AAC1.9.10",
            "AAC3.0.5",
            "AAC3.1.6-Gomme",
            "AAC3.3.12",
            "AAC3.3.12-Glide",
            "AAC3.3.13",

            // CubeCraft
            "CubeCraft",

            // Hypixel
            "Hypixel",
            "BoostHypixel",
            "FreeHypixel",

            // Rewinside
            "Rewinside",
            "TeleportRewinside",

            // Other server specific flys
            "Mineplex",
            "NeruxVace",
            "Minesucht",

            // Spartan
            "Spartan",
            "Spartan2",
            "BugSpartan",

            // Other anticheats
            "MineSecure",
            "HawkEye",
            "HAC",
            "WatchCat",

            // Other
            "Jetpack",
            "KeepAlive",
            "Flag"
    }, "Vanilla");

    private final FloatValue vanillaSpeedValue = new FloatValue("VanillaSpeed", 2F, 0F, 5F);
    private final BoolValue vanillaKickBypassValue = new BoolValue("VanillaKickBypass", false);

    private final FloatValue ncpMotionValue = new FloatValue("NCPMotion", 0F, 0F, 1F);

    // AAC
    private final FloatValue aacSpeedValue = new FloatValue("AAC1.9.10-Speed", 0.3F, 0F, 1F);
    private final BoolValue aacFast = new BoolValue("AAC3.0.5-Fast", true);
    private final FloatValue aacMotion = new FloatValue("AAC3.3.12-Motion", 10F, 0.1F, 10F);
    private final FloatValue aacMotion2 = new FloatValue("AAC3.3.13-Motion", 10F, 0.1F, 10F);

    // Hypixel
    private final BoolValue hypixelBoost = new BoolValue("Hypixel-Boost", true);
    private final IntegerValue hypixelBoostDelay = new IntegerValue("Hypixel-BoostDelay", 1200, 0, 2000);
    private final FloatValue hypixelBoostTimer = new FloatValue("Hypixel-BoostTimer", 1F, 0F, 5F);

    private final FloatValue mineplexSpeedValue = new FloatValue("MineplexSpeed", 1F, 0.5F, 10F);
    private final IntegerValue neruxVaceTicks = new IntegerValue("NeruxVace-Ticks", 6, 0, 20);

    // Visuals
    private final BoolValue markValue = new BoolValue("Mark", true);

    private double startY;
    private final MSTimer flyTimer = new MSTimer();

    private final MSTimer groundTimer = new MSTimer();

    private boolean noPacketModify;

    private double aacJump;

    private int aac3delay;
    private int aac3glideDelay;

    private boolean noFlag;

    private final MSTimer mineSecureVClipTimer = new MSTimer();

    private final TickTimer spartanTimer = new TickTimer();

    private long minesuchtTP;

    private final MSTimer mineplexTimer = new MSTimer();

    private boolean wasDead;

    private final TickTimer hypixelTimer = new TickTimer();

    private int boostHypixelState = 1;
    private double moveSpeed, lastDistance;
    private boolean failedStart = false;

    private final TickTimer cubecraft2TickTimer = new TickTimer();
    private final TickTimer cubecraftTeleportTickTimer = new TickTimer();

    private final TickTimer freeHypixelTimer = new TickTimer();
    private float freeHypixelYaw;
    private float freeHypixelPitch;

    @Override
    public void onEnable() {
        if(mc.thePlayer == null)
            return;

        flyTimer.reset();

        noPacketModify = true;

        double x = mc.thePlayer.posX;
        double y = mc.thePlayer.posY;
        double z = mc.thePlayer.posZ;

        final String mode = modeValue.get();

        switch(mode.toLowerCase()) {
            case "ncp":
                if(!mc.thePlayer.onGround)
                    break;

                for(int i = 0; i < 65; ++i) {
                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(x, y + 0.049D, z, false));
                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(x, y, z, false));
                }
                mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(x, y + 0.1D, z, true));

                mc.thePlayer.motionX *= 0.1D;
                mc.thePlayer.motionZ *= 0.1D;
                mc.thePlayer.swingItem();
                break;
            case "oldncp":
                if(!mc.thePlayer.onGround)
                    break;

                for(int i = 0; i < 4; i++) {
                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(x, y + 1.01, z, false));
                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(x, y, z, false));
                }
                mc.thePlayer.jump();
                mc.thePlayer.swingItem();
                break;
            case "bugspartan":
                for(int i = 0; i < 65; ++i) {
                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(x, y + 0.049D, z, false));
                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(x, y, z, false));
                }
                mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(x, y + 0.1D, z, true));

                mc.thePlayer.motionX *= 0.1D;
                mc.thePlayer.motionZ *= 0.1D;
                mc.thePlayer.swingItem();
                break;
            case "infinitycubecraft":
                ClientUtils.displayChatMessage("§8[§c§lCubeCraft-§a§lFly§8] §aPlace a block before landing.");
                break;
            case "infinityvcubecraft":
                ClientUtils.displayChatMessage("§8[§c§lCubeCraft-§a§lFly§8] §aPlace a block before landing.");

                mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY + 2, mc.thePlayer.posZ);
                break;
            case "boosthypixel":
                if(!mc.thePlayer.onGround) break;

                for (int i = 0; i < 10; i++) //Imagine flagging to NCP.
                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, true));
                
                double fallDistance = 3.0125; //add 0.0125 to ensure we get the fall dmg
                while (fallDistance > 0) {
                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY + 0.0624986421, mc.thePlayer.posZ, false));
                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY + 0.0625      , mc.thePlayer.posZ, false));
                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY + 0.0624986421, mc.thePlayer.posZ, false));
                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY + 0.0000013579, mc.thePlayer.posZ, false));
                    fallDistance -= 0.0624986421;
                }
                mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, true));
                
                mc.thePlayer.jump();
                mc.thePlayer.posY += 0.42F; // Visual
                boostHypixelState = 1;
                moveSpeed = 0.1D;
                lastDistance = 0D;
                failedStart = false;
                break;
        }

        startY = mc.thePlayer.posY;
        aacJump = -3.8D;
        noPacketModify = false;

        if(mode.equalsIgnoreCase("freehypixel")) {
            freeHypixelTimer.reset();
            mc.thePlayer.setPositionAndUpdate(mc.thePlayer.posX, mc.thePlayer.posY + 0.42D, mc.thePlayer.posZ);
            freeHypixelYaw = mc.thePlayer.rotationYaw;
            freeHypixelPitch = mc.thePlayer.rotationPitch;
        }
        super.onEnable();
    }

    @Override
    public void onDisable() {
        wasDead = false;

        if (mc.thePlayer == null)
            return;

        noFlag = false;

        final String mode = modeValue.get();

        if (!mode.toUpperCase().startsWith("AAC") && !mode.equalsIgnoreCase("Hypixel") &&
                !mode.equalsIgnoreCase("CubeCraft")) {
            mc.thePlayer.motionX = 0;
            mc.thePlayer.motionY = 0;
            mc.thePlayer.motionZ = 0;
        }
        mc.thePlayer.capabilities.isFlying = false;

        mc.timer.timerSpeed = 1F;
        mc.thePlayer.speedInAir = 0.02F;
    }

    @EventTarget
    public void onUpdate(final UpdateEvent event) {
        final float vanillaSpeed = vanillaSpeedValue.get();

        switch (modeValue.get().toLowerCase()) {
            case "vanilla":
                mc.thePlayer.capabilities.isFlying = false;
                mc.thePlayer.motionY = 0;
                mc.thePlayer.motionX = 0;
                mc.thePlayer.motionZ = 0;
                if (mc.gameSettings.keyBindJump.isKeyDown())
                    mc.thePlayer.motionY += vanillaSpeed;
                if (mc.gameSettings.keyBindSneak.isKeyDown())
                    mc.thePlayer.motionY -= vanillaSpeed;
                MovementUtils.strafe(vanillaSpeed);

                handleVanillaKickBypass();
                break;
            case "smoothvanilla":
                mc.thePlayer.capabilities.isFlying = true;

                handleVanillaKickBypass();
                break;
            case "cubecraft":
                mc.timer.timerSpeed = 0.6F;

                cubecraftTeleportTickTimer.update();
                break;
            case "ncp":
                mc.thePlayer.motionY = -ncpMotionValue.get();

                if(mc.gameSettings.keyBindSneak.isKeyDown())
                    mc.thePlayer.motionY = -0.5D;
                MovementUtils.strafe();
                break;
            case "oldncp":
                if(startY > mc.thePlayer.posY)
                    mc.thePlayer.motionY = -0.000000000000000000000000000000001D;

                if(mc.gameSettings.keyBindSneak.isKeyDown())
                    mc.thePlayer.motionY = -0.2D;

                if(mc.gameSettings.keyBindJump.isKeyDown() && mc.thePlayer.posY < (startY - 0.1D))
                    mc.thePlayer.motionY = 0.2D;
                MovementUtils.strafe();
                break;
            case "aac1.9.10":
                if(mc.gameSettings.keyBindJump.isKeyDown())
                    aacJump += 0.2D;

                if(mc.gameSettings.keyBindSneak.isKeyDown())
                    aacJump -= 0.2D;

                if((startY + aacJump) > mc.thePlayer.posY) {
                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer(true));
                    mc.thePlayer.motionY = 0.8D;
                    MovementUtils.strafe(aacSpeedValue.get());
                }

                MovementUtils.strafe();
                break;
            case "aac3.0.5":
                if (aac3delay == 2)
                    mc.thePlayer.motionY = 0.1D;
                else if (aac3delay > 2)
                    aac3delay = 0;

                if (aacFast.get()) {
                    if (mc.thePlayer.movementInput.moveStrafe == 0D)
                        mc.thePlayer.jumpMovementFactor = 0.08F;
                    else
                        mc.thePlayer.jumpMovementFactor = 0F;
                }

                aac3delay++;
                break;
            case "aac3.1.6-gomme":
                mc.thePlayer.capabilities.isFlying = true;

                if (aac3delay == 2) {
                    mc.thePlayer.motionY += 0.05D;
                } else if (aac3delay > 2) {
                    mc.thePlayer.motionY -= 0.05D;
                    aac3delay = 0;
                }

                aac3delay++;

                if(!noFlag)
                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, mc.thePlayer.onGround));
                if(mc.thePlayer.posY <= 0D)
                    noFlag = true;
                break;
            case "flag":
                mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C06PacketPlayerPosLook(mc.thePlayer.posX + mc.thePlayer.motionX * 999, mc.thePlayer.posY + (mc.gameSettings.keyBindJump.isKeyDown() ? 1.5624 : 0.00000001) - (mc.gameSettings.keyBindSneak.isKeyDown() ? 0.0624 : 0.00000002), mc.thePlayer.posZ + mc.thePlayer.motionZ * 999, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, true));
                mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C06PacketPlayerPosLook(mc.thePlayer.posX + mc.thePlayer.motionX * 999, mc.thePlayer.posY - 6969, mc.thePlayer.posZ + mc.thePlayer.motionZ * 999, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, true));
                mc.thePlayer.setPosition(mc.thePlayer.posX + mc.thePlayer.motionX * 11, mc.thePlayer.posY, mc.thePlayer.posZ + mc.thePlayer.motionZ * 11);
                mc.thePlayer.motionY = 0F;
                break;
            case "keepalive":
                mc.getNetHandler().addToSendQueue(new C00PacketKeepAlive());

                mc.thePlayer.capabilities.isFlying = false;
                mc.thePlayer.motionY = 0;
                mc.thePlayer.motionX = 0;
                mc.thePlayer.motionZ = 0;
                if(mc.gameSettings.keyBindJump.isKeyDown())
                    mc.thePlayer.motionY += vanillaSpeed;
                if(mc.gameSettings.keyBindSneak.isKeyDown())
                    mc.thePlayer.motionY -= vanillaSpeed;
                MovementUtils.strafe(vanillaSpeed);
                break;
            case "minesecure":
                mc.thePlayer.capabilities.isFlying = false;

                if(!mc.gameSettings.keyBindSneak.isKeyDown())
                    mc.thePlayer.motionY = -0.01F;

                mc.thePlayer.motionX = 0;
                mc.thePlayer.motionZ = 0;
                MovementUtils.strafe(vanillaSpeed);

                if(mineSecureVClipTimer.hasTimePassed(150) && mc.gameSettings.keyBindJump.isKeyDown()) {
                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY + 5, mc.thePlayer.posZ, false));
                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(0.5D, -1000, 0.5D, false));
                    final double yaw = Math.toRadians(mc.thePlayer.rotationYaw);
                    final double x = -Math.sin(yaw) * 0.4D;
                    final double z = Math.cos(yaw) * 0.4D;
                    mc.thePlayer.setPosition(mc.thePlayer.posX + x, mc.thePlayer.posY, mc.thePlayer.posZ + z);

                    mineSecureVClipTimer.reset();
                }
                break;
            case "hac":
                mc.thePlayer.motionX *= 0.8;
                mc.thePlayer.motionZ *= 0.8;
            case "hawkeye":
                mc.thePlayer.motionY = mc.thePlayer.motionY <= -0.42 ? 0.42 : -0.42;
                break;
            case "teleportrewinside":
                final Vec3 vectorStart = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
                final float yaw = -mc.thePlayer.rotationYaw;
                final float pitch = -mc.thePlayer.rotationPitch;
                final double length = 9.9;
                final Vec3 vectorEnd = new Vec3(
                        Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)) * length + vectorStart.xCoord,
                        Math.sin(Math.toRadians(pitch)) * length + vectorStart.yCoord,
                        Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)) * length + vectorStart.zCoord
                );
                mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(vectorEnd.xCoord, mc.thePlayer.posY + 2, vectorEnd.zCoord, true));
                mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(vectorStart.xCoord, mc.thePlayer.posY + 2, vectorStart.zCoord, true));
                mc.thePlayer.motionY = 0;
                break;
            case "minesucht":
                final double posX = mc.thePlayer.posX;
                final double posY = mc.thePlayer.posY;
                final double posZ = mc.thePlayer.posZ;

                if(!mc.gameSettings.keyBindForward.isKeyDown())
                    break;

                if(System.currentTimeMillis() - minesuchtTP > 99) {
                    final Vec3 vec3 = mc.thePlayer.getPositionEyes(0);
                    final Vec3 vec31 = mc.thePlayer.getLook(0);
                    final Vec3 vec32 = vec3.addVector(vec31.xCoord * 7, vec31.yCoord * 7, vec31.zCoord * 7);

                    if(mc.thePlayer.fallDistance > 0.8) {
                        mc.thePlayer.sendQueue.addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(posX, posY + 50, posZ, false));
                        mc.thePlayer.fall(100, 100);
                        mc.thePlayer.fallDistance = 0;
                        mc.thePlayer.sendQueue.addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(posX, posY + 20, posZ, true));
                    }

                    mc.thePlayer.sendQueue.addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(vec32.xCoord, mc.thePlayer.posY + 50, vec32.zCoord, true));
                    mc.thePlayer.sendQueue.addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(posX, posY, posZ, false));
                    mc.thePlayer.sendQueue.addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(vec32.xCoord, posY, vec32.zCoord, true));
                    mc.thePlayer.sendQueue.addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(posX, posY, posZ, false));
                    minesuchtTP = System.currentTimeMillis();
                }else{
                    mc.thePlayer.sendQueue.addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, false));
                    mc.thePlayer.sendQueue.addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(posX, posY, posZ, true));
                }
                break;
            case "jetpack":
                if(mc.gameSettings.keyBindJump.isKeyDown()) {
                    mc.effectRenderer.spawnEffectParticle(EnumParticleTypes.FLAME.getParticleID(), mc.thePlayer.posX, mc.thePlayer.posY + 0.2D, mc.thePlayer.posZ, -mc.thePlayer.motionX, -0.5D, -mc.thePlayer.motionZ);
                    mc.thePlayer.motionY += 0.15D;
                    mc.thePlayer.motionX *= 1.1D;
                    mc.thePlayer.motionZ *= 1.1D;
                }
                break;
            case "mineplex":
                if(mc.thePlayer.inventory.getCurrentItem() == null) {
                    if(mc.gameSettings.keyBindJump.isKeyDown() && mineplexTimer.hasTimePassed(100)) {
                        mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY + 0.6, mc.thePlayer.posZ);
                        mineplexTimer.reset();
                    }

                    if(mc.thePlayer.isSneaking() && mineplexTimer.hasTimePassed(100)) {
                        mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY - 0.6, mc.thePlayer.posZ);
                        mineplexTimer.reset();
                    }

                    final BlockPos blockPos = new BlockPos(mc.thePlayer.posX, mc.thePlayer.getEntityBoundingBox().minY - 1, mc.thePlayer.posZ);
                    final Vec3 vec = new Vec3(blockPos).addVector(0.4F, 0.4F, 0.4F).add(new Vec3(EnumFacing.UP.getDirectionVec()));
                    mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.inventory.getCurrentItem(), blockPos, EnumFacing.UP, new Vec3(vec.xCoord * 0.4F, vec.yCoord * 0.4F, vec.zCoord * 0.4F));
                    MovementUtils.strafe(0.27F);

                    mc.timer.timerSpeed = (1 + mineplexSpeedValue.get());
                }else{
                    mc.timer.timerSpeed = 1;
                    setState(false);
                    ClientUtils.displayChatMessage("§8[§c§lMineplex-§a§lFly§8] §aSelect an empty slot to fly.");
                }
                break;
            case "aac3.3.12":
                if(mc.thePlayer.posY < -70)
                    mc.thePlayer.motionY = aacMotion.get();

                mc.timer.timerSpeed = 1F;

                if(Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)) {
                    mc.timer.timerSpeed = 0.2F;
                    mc.rightClickDelayTimer = 0;
                }
                break;
            case "aac3.3.12-glide":
                if(!mc.thePlayer.onGround)
                    aac3glideDelay++;

                if(aac3glideDelay == 2)
                    mc.timer.timerSpeed = 1F;

                if(aac3glideDelay == 12)
                    mc.timer.timerSpeed = 0.1F;

                if(aac3glideDelay >= 12 && !mc.thePlayer.onGround) {
                    aac3glideDelay = 0;
                    mc.thePlayer.motionY = .015;
                }
                break;
            case "aac3.3.13":
                if(mc.thePlayer.isDead)
                    wasDead = true;

                if(wasDead || mc.thePlayer.onGround) {
                    wasDead = false;

                    mc.thePlayer.motionY = aacMotion2.get();
                    mc.thePlayer.onGround = false;
                }

                mc.timer.timerSpeed = 1F;

                if(Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)) {
                    mc.timer.timerSpeed = 0.2F;
                    mc.rightClickDelayTimer = 0;
                }
                break;
            case "watchcat":
                MovementUtils.strafe(0.15F);
                mc.thePlayer.setSprinting(true);

                if(mc.thePlayer.posY < startY + 2) {
                    mc.thePlayer.motionY = Math.random() * 0.5;
                    break;
                }

                if(startY > mc.thePlayer.posY)
                    MovementUtils.strafe(0F);
                break;
            case "spartan":
                mc.thePlayer.motionY = 0;
                spartanTimer.update();
                if(spartanTimer.hasTimePassed(12)) {
                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY + 8, mc.thePlayer.posZ, true));
                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY - 8, mc.thePlayer.posZ, true));
                    spartanTimer.reset();
                }
                break;
            case "spartan2":
                MovementUtils.strafe(0.264F);

                if(mc.thePlayer.ticksExisted % 8 == 0)
                    mc.thePlayer.sendQueue.addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY + 10, mc.thePlayer.posZ, true));
                break;
            case "neruxvace":
                if (!mc.thePlayer.onGround)
                    aac3glideDelay++;

                if (aac3glideDelay >= neruxVaceTicks.get() && !mc.thePlayer.onGround) {
                    aac3glideDelay = 0;
                    mc.thePlayer.motionY = .015;
                }
                break;
            case "hypixel":
                final int boostDelay = hypixelBoostDelay.get();
                if (hypixelBoost.get() && !flyTimer.hasTimePassed(boostDelay)) {
                    mc.timer.timerSpeed = 1F + (hypixelBoostTimer.get() * ((float) flyTimer.hasTimeLeft(boostDelay) / (float) boostDelay));
                }

                hypixelTimer.update();

                if (hypixelTimer.hasTimePassed(2)) {
                    mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY + 1.0E-5, mc.thePlayer.posZ);
                    hypixelTimer.reset();
                }
                break;
            case "freehypixel":
                if(freeHypixelTimer.hasTimePassed(10)) {
                    mc.thePlayer.capabilities.isFlying = true;
                    break;
                }else{
                    mc.thePlayer.rotationYaw = freeHypixelYaw;
                    mc.thePlayer.rotationPitch = freeHypixelPitch;
                    mc.thePlayer.motionX = mc.thePlayer.motionZ = mc.thePlayer.motionY = 0;
                }

                if(startY == new BigDecimal(mc.thePlayer.posY).setScale(3, RoundingMode.HALF_DOWN).doubleValue())
                    freeHypixelTimer.update();
                break;
            case "bugspartan":
                mc.thePlayer.capabilities.isFlying = false;
                mc.thePlayer.motionY = 0;
                mc.thePlayer.motionX = 0;
                mc.thePlayer.motionZ = 0;
                if (mc.gameSettings.keyBindJump.isKeyDown())
                    mc.thePlayer.motionY += vanillaSpeed;
                if (mc.gameSettings.keyBindSneak.isKeyDown())
                    mc.thePlayer.motionY -= vanillaSpeed;
                MovementUtils.strafe(vanillaSpeed);
                break;
        }
    }

    @EventTarget
    public void onMotion(final MotionEvent event) {
        if(modeValue.get().equalsIgnoreCase("boosthypixel")) {
            switch(event.getEventState()) {
                case PRE:
                    hypixelTimer.update();

                    if (hypixelTimer.hasTimePassed(2)) {
                        mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY + 1.0E-5, mc.thePlayer.posZ);
                        hypixelTimer.reset();
                    }

                    if(!failedStart) mc.thePlayer.motionY = 0D;
                    break;
                case POST:
                    double xDist = mc.thePlayer.posX - mc.thePlayer.prevPosX;
                    double zDist = mc.thePlayer.posZ - mc.thePlayer.prevPosZ;
                    lastDistance = Math.sqrt(xDist * xDist + zDist * zDist);
                    break;
            }
        }
    }

    @EventTarget
    public void onRender3D(final Render3DEvent event) {
        final String mode = modeValue.get();

        if (!markValue.get() || mode.equalsIgnoreCase("Vanilla") || mode.equalsIgnoreCase("SmoothVanilla"))
            return;

        double y = startY + 2D;

        RenderUtils.drawPlatform(y, mc.thePlayer.getEntityBoundingBox().maxY < y ? new Color(0, 255, 0, 90) : new Color(255, 0, 0, 90), 1);

        switch (mode.toLowerCase()) {
            case "aac1.9.10":
                RenderUtils.drawPlatform(startY + aacJump, new Color(0, 0, 255, 90), 1);
                break;
            case "aac3.3.12":
                RenderUtils.drawPlatform(-70, new Color(0, 0, 255, 90), 1);
                break;
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if(noPacketModify)
            return;

        final Packet<?> packet = event.getPacket();

        if(packet instanceof C03PacketPlayer) {
            final C03PacketPlayer packetPlayer = (C03PacketPlayer) packet;

            final String mode = modeValue.get();

            if (mode.equalsIgnoreCase("NCP") || mode.equalsIgnoreCase("Rewinside") ||
                    (mode.equalsIgnoreCase("Mineplex") && mc.thePlayer.inventory.getCurrentItem() == null))
                packetPlayer.onGround = true;

            if (mode.equalsIgnoreCase("Hypixel") || mode.equalsIgnoreCase("BoostHypixel"))
                packetPlayer.onGround = false;
        }

        if(packet instanceof S08PacketPlayerPosLook) {
            final String mode = modeValue.get();

            if(mode.equalsIgnoreCase("BoostHypixel")) {
                failedStart = true;
                ClientUtils.displayChatMessage("§8[§c§lBoostHypixel-§a§lFly§8] §cSetback detected.");
            }
        }
    }

    @EventTarget
    public void onMove(final MoveEvent event) {
        switch(modeValue.get().toLowerCase()) {
            case "cubecraft": {
                final double yaw = Math.toRadians(mc.thePlayer.rotationYaw);

                if (cubecraftTeleportTickTimer.hasTimePassed(2)) {
                    event.setX(-Math.sin(yaw) * 2.4D);
                    event.setZ(Math.cos(yaw) * 2.4D);

                    cubecraftTeleportTickTimer.reset();
                } else {
                    event.setX(-Math.sin(yaw) * 0.2D);
                    event.setZ(Math.cos(yaw) * 0.2D);
                }
                break;
            }
            case "boosthypixel":
                if (!MovementUtils.isMoving()) {
                    event.setX(0D);
                    event.setZ(0D);
                    break;
                }

                if (failedStart)
                    break;

                final double amplifier = 1 + (mc.thePlayer.isPotionActive(Potion.moveSpeed) ? 0.2 *
                        (mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).getAmplifier() + 1) : 0);
                final double baseSpeed = 0.29D * amplifier;

                switch (boostHypixelState) {
                    case 1:
                        moveSpeed = (mc.thePlayer.isPotionActive(Potion.moveSpeed) ? 1.56 : 2.034) * baseSpeed;
                        boostHypixelState = 2;
                        break;
                    case 2:
                        moveSpeed *= 2.16D;
                        boostHypixelState = 3;
                        break;
                    case 3:
                        moveSpeed = lastDistance - (mc.thePlayer.ticksExisted % 2 == 0 ? 0.0103D : 0.0123D) * (lastDistance - baseSpeed);

                        boostHypixelState = 4;
                        break;
                    default:
                        moveSpeed = lastDistance - lastDistance / 159.8D;
                        break;
                }

                moveSpeed = Math.max(moveSpeed, 0.3D);

                final double yaw = MovementUtils.getDirection();
                event.setX(-Math.sin(yaw) * moveSpeed);
                event.setZ(Math.cos(yaw) * moveSpeed);
                mc.thePlayer.motionX = event.getX();
                mc.thePlayer.motionZ = event.getZ();
                break;
            case "freehypixel":
                if (!freeHypixelTimer.hasTimePassed(10))
                    event.zero();
                break;
        }
    }

    @EventTarget
    public void onBB(final BlockBBEvent event) {
        if (mc.thePlayer == null) return;

        final String mode = modeValue.get();

        if (event.getBlock() instanceof BlockAir && (mode.equalsIgnoreCase("Hypixel") ||
                mode.equalsIgnoreCase("BoostHypixel") || mode.equalsIgnoreCase("Rewinside") ||
                (mode.equalsIgnoreCase("Mineplex") && mc.thePlayer.inventory.getCurrentItem() == null)) && event.getY() < mc.thePlayer.posY)
            event.setBoundingBox(AxisAlignedBB.fromBounds(event.getX(), event.getY(), event.getZ(), event.getX() + 1, mc.thePlayer.posY, event.getZ() + 1));
    }

    @EventTarget
    public void onJump(final JumpEvent e) {
        final String mode = modeValue.get();

        if (mode.equalsIgnoreCase("Hypixel") || mode.equalsIgnoreCase("BoostHypixel") ||
                mode.equalsIgnoreCase("Rewinside") || (mode.equalsIgnoreCase("Mineplex") && mc.thePlayer.inventory.getCurrentItem() == null))
            e.cancelEvent();
    }

    @EventTarget
    public void onStep(final StepEvent e) {
        final String mode = modeValue.get();

        if (mode.equalsIgnoreCase("Hypixel") || mode.equalsIgnoreCase("BoostHypixel") ||
                mode.equalsIgnoreCase("Rewinside") || (mode.equalsIgnoreCase("Mineplex") && mc.thePlayer.inventory.getCurrentItem() == null))
            e.setStepHeight(0F);
    }

    private void handleVanillaKickBypass() {
        if(!vanillaKickBypassValue.get() || !groundTimer.hasTimePassed(1000)) return;

        final double ground = calculateGround();

        for(double posY = mc.thePlayer.posY; posY > ground; posY -= 8D) {
            mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, posY, mc.thePlayer.posZ, true));

            if(posY - 8D < ground) break; // Prevent next step
        }

        mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, ground, mc.thePlayer.posZ, true));


        for(double posY = ground; posY < mc.thePlayer.posY; posY += 8D) {
            mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, posY, mc.thePlayer.posZ, true));

            if(posY + 8D > mc.thePlayer.posY) break; // Prevent next step
        }

        mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, true));

        groundTimer.reset();
    }

    // TODO: Make better and faster calculation lol
    private double calculateGround() {
        final AxisAlignedBB playerBoundingBox = mc.thePlayer.getEntityBoundingBox();
        double blockHeight = 1D;

        for(double ground = mc.thePlayer.posY; ground > 0D; ground -= blockHeight) {
            final AxisAlignedBB customBox = new AxisAlignedBB(playerBoundingBox.maxX, ground + blockHeight, playerBoundingBox.maxZ, playerBoundingBox.minX, ground, playerBoundingBox.minZ);

            if(mc.theWorld.checkBlockCollision(customBox)) {
                if(blockHeight <= 0.05D)
                    return ground + blockHeight;

                ground += blockHeight;
                blockHeight = 0.05D;
            }
        }

        return 0F;
    }

    @Override
    public String getTag() {
        return modeValue.get();
    }
}
