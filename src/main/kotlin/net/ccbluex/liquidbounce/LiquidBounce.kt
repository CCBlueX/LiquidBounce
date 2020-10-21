/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce

import org.apache.logging.log4j.LogManager

object LiquidBounce {

    const val CLIENT_NAME = "LiquidBounce"
    const val CLIENT_VERSION = "1.0.0" // TODO: Might use a semver library (yes/no?)

    val logger = LogManager.getLogger(CLIENT_NAME)!!

    /**
     * Should be executed to start the client.
     */
    fun start() {
        // TODO: start client huh

    }

    /**
     * Should be executed to stop the client.
     */
    fun stop() {
        // TODO: stop client

    }

}