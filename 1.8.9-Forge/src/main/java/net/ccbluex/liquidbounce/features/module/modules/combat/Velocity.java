/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat;

import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.JumpEvent;
import net.ccbluex.liquidbounce.event.PacketEvent;
import net.ccbluex.liquidbounce.event.UpdateEvent;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.utils.MovementUtils;
import net.ccbluex.liquidbounce.value.BoolValue;
import net.ccbluex.liquidbounce.value.FloatValue;
import net.ccbluex.liquidbounce.value.ListValue;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.util.MathHelper;

@ModuleInfo(name = "Velocity", description = "Allows you to modify the amount of knockback you take.", category = ModuleCategory.COMBAT)
public class Velocity extends Module {
    private final FloatValue horizontalValue = new FloatValue("Horizontal", 0F, 0F, 1F);
    private final FloatValue verticalValue = new FloatValue("Vertical", 0F, 0F, 1F);
    private final ListValue modeValue = new ListValue("Mode", new String[] {"Simple", "AAC", "AACPush", "AACZero", "Jump", "Reverse", "Reverse2", "Glitch"}, "Simple");
    private final FloatValue reverseStrengthValue = new FloatValue("ReverseStrength", 1.0F, 0.1F, 1F);
    private final FloatValue reverse2StrengthValue = new FloatValue("Reverse2Strength", 0.05F, 0.02F, 0.1F);
    private final FloatValue aacPushXZReducerValue = new FloatValue("AACPushXZReducer", 2F, 1F, 3F);
    private final BoolValue aacPushYReducerValue = new BoolValue("AACPushYReducer", true);

    private long velocityTime;
    private boolean gotVelocity;
    private boolean gotHurt;

    @Override
    public void onDisable() {
        if(mc.thePlayer == null)
            return;

        mc.thePlayer.speedInAir = 0.02F;
        super.onDisable();
    }

    @EventTarget
    public void onUpdate(final UpdateEvent event) {
        if(mc.thePlayer.isInWater())
            return;

        switch(modeValue.get().toLowerCase()) {
            case "reverse":
                if(!gotVelocity)
                    break;

                if(!mc.thePlayer.onGround && !mc.thePlayer.isInWater() && !mc.thePlayer.isInLava() && !mc.thePlayer.isInWeb) {
                    MovementUtils.strafe(MovementUtils.getSpeed() * reverseStrengthValue.get());
                }else if(System.currentTimeMillis() - velocityTime > 80L)
                    gotVelocity = false;
                break;
            case "aac":
                if(velocityTime != 0L && System.currentTimeMillis() - velocityTime > 80L) {
                    mc.thePlayer.motionX *= horizontalValue.get();
                    mc.thePlayer.motionZ *= verticalValue.get();
                    velocityTime = 0L;
                }
                break;
            case "jump":
                if(mc.thePlayer.hurtTime > 0 && mc.thePlayer.onGround) {
                    mc.thePlayer.motionY = 0.42;

                    final float f = mc.thePlayer.rotationYaw * 0.017453292F;
                    mc.thePlayer.motionX -= MathHelper.sin(f) * 0.2F;
                    mc.thePlayer.motionZ += MathHelper.cos(f) * 0.2F;
                }
                break;
            case "aacpush":
                if(mc.thePlayer.movementInput.jump) break;

                if(velocityTime != 0L && System.currentTimeMillis() - velocityTime > 80L) velocityTime = 0L;

                if(mc.thePlayer.hurtTime > 0 && mc.thePlayer.motionX != 0 && mc.thePlayer.motionZ != 0)
                    mc.thePlayer.onGround = true;

                if(mc.thePlayer.hurtResistantTime > 0 && aacPushYReducerValue.get())
                    mc.thePlayer.motionY -= 0.0144;

                if(mc.thePlayer.hurtResistantTime >= 19) {
                    final double reduce = aacPushXZReducerValue.get();

                    mc.thePlayer.motionX /= reduce;
                    mc.thePlayer.motionZ /= reduce;
                }
                break;
            case "glitch":
                mc.thePlayer.noClip = gotVelocity;
                if (mc.thePlayer.hurtTime == 7)
                    mc.thePlayer.motionY = 0.4;
                gotVelocity = false;
                break;
            case "reverse2":
                if(!gotVelocity) {
                    mc.thePlayer.speedInAir = 0.02F;
                    break;
                }

                if(mc.thePlayer.hurtTime > 0)
                    gotHurt = true;

                if(!mc.thePlayer.onGround && !mc.thePlayer.isInWater() && !mc.thePlayer.isInLava() && !mc.thePlayer.isInWeb) {
                    if(gotHurt)
                        mc.thePlayer.speedInAir = reverse2StrengthValue.get();
                }else if(System.currentTimeMillis() - velocityTime > 80L) {
                    gotVelocity = false;
                    gotHurt = false;
                }
                break;
            case "aaczero":
                if(mc.thePlayer.hurtTime > 0) {
                    if(!gotVelocity || mc.thePlayer.onGround || mc.thePlayer.fallDistance > 2F)
                        break;

                    mc.thePlayer.addVelocity(0, -1, 0);
                    mc.thePlayer.onGround = true;
                }else
                    gotVelocity = false;
                break;
        }
    }

    @EventTarget
    public void onPacket(final PacketEvent event) {
        final Packet<?> packet = event.getPacket();

        if(packet instanceof S12PacketEntityVelocity && mc.thePlayer != null && mc.theWorld != null) {
            final S12PacketEntityVelocity packetEntityVelocity = (S12PacketEntityVelocity) packet;

            if(mc.theWorld.getEntityByID(packetEntityVelocity.getEntityID()) == mc.thePlayer) {
                velocityTime = System.currentTimeMillis();

                final String mode = modeValue.get();

                switch(mode.toLowerCase()) {
                    case "simple":
                        final double horizontal = horizontalValue.get();
                        final double vertical = verticalValue.get();

                        if(horizontal == 0D && vertical == 0D)
                            event.cancelEvent();

                        packetEntityVelocity.motionX = (int) (packetEntityVelocity.getMotionX() * horizontal);
                        packetEntityVelocity.motionY = (int) (packetEntityVelocity.getMotionY() * vertical);
                        packetEntityVelocity.motionZ = (int) (packetEntityVelocity.getMotionZ() * horizontal);
                        break;
                    case "reverse":
                    case "reverse2":
                    case "aaczero":
                        gotVelocity = true;
                        break;
                    case "glitch":
                        if(!mc.thePlayer.onGround)
                            break;

                        gotVelocity = true;
                        event.cancelEvent();
                        break;
                }
            }
        }

        if(packet instanceof S27PacketExplosion)
            event.cancelEvent();
    }

    @EventTarget
    public void onJump(final JumpEvent event) {
        if(mc.thePlayer == null || mc.thePlayer.isInWater())
            return;

        switch(modeValue.get().toLowerCase()) {
            case "aacpush":
            case "aaczero":
                if(mc.thePlayer.hurtTime > 0)
                    event.cancelEvent();
                break;
        }
    }

    @Override
    public String getTag() {
        return modeValue.get();
    }
}
