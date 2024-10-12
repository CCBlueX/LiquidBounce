/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.extensions;

import net.ccbluex.liquidbounce.utils.MinecraftInstance;
import net.ccbluex.liquidbounce.utils.SimulatedPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.world.World;
import org.apache.commons.lang3.tuple.Pair;

public class SimulatedPlayerJavaExtensions extends MinecraftInstance {

    /**
     * This game movement code had to be kept in its original language as it gives proper results.
     */
    public Pair<Double, Double> checkForCollision(SimulatedPlayer simPlayer, double velocityX, double velocityZ) {
        EntityPlayerSP player = mc.thePlayer;
        World worldObj = player.worldObj;

        double d6;

        double d3 = velocityX;
        double d5 = velocityZ;

        for (d6 = 0.05; velocityX != 0 && worldObj.getCollidingBoundingBoxes(player, simPlayer.getBox().offset(velocityX, -1, 0)).isEmpty(); d3 = velocityX) {
            if (velocityX < d6 && velocityX >= -d6) {
                velocityX = 0;
            } else if (velocityX > 0) {
                velocityX -= d6;
            } else {
                velocityX += d6;
            }
        }

        //noinspection ConstantConditions
        for (; velocityZ != 0 && worldObj.getCollidingBoundingBoxes(player, simPlayer.getBox().offset(0, -1, velocityZ)).isEmpty(); d5 = velocityZ) {
            if (velocityZ < d6 && velocityZ >= -d6) {
                velocityZ = 0;
            } else if (velocityZ > 0) {
                velocityZ -= d6;
            } else {
                velocityZ += d6;
            }
        }

        //noinspection ConstantConditions
        for (; velocityX != 0 && velocityZ != 0 && worldObj.getCollidingBoundingBoxes(player, simPlayer.getBox().offset(velocityX, -1, velocityZ)).isEmpty(); d5 = velocityZ) {
            if (velocityX < d6 && velocityX >= -d6) {
                velocityX = 0;
            } else if (velocityX > 0) {
                velocityX -= d6;
            } else {
                velocityX += d6;
            }

            d3 = velocityX;

            if (velocityZ < d6 && velocityZ >= -d6) {
                velocityZ = 0;
            } else if (velocityZ > 0) {
                velocityZ -= d6;
            } else {
                velocityZ += d6;
            }
        }

        return Pair.of(d3, d5);
    }
}
