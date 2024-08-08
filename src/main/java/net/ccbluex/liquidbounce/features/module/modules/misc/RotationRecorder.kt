/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_NAME
import net.ccbluex.liquidbounce.LiquidBounce.MINECRAFT_VERSION
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.script.api.global.Chat
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.utils.RotationUtils.getAngleDifference
import net.ccbluex.liquidbounce.utils.RotationUtils.lastServerRotation
import net.ccbluex.liquidbounce.utils.RotationUtils.serverRotation
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object RotationRecorder : Module("RotationRecorder", Category.MISC) {

    private val rotationList: MutableList<Pair<Rotation, Int>> = mutableListOf()

    override fun onEnable() {
        rotationList.clear()

        Chat.print("Started recording rotations.")
    }

    @EventTarget
    fun onMotion(event: MotionEvent) {
        if (event.eventState != EventState.POST)
            return

        rotationList.add(Rotation(getAngleDifference(serverRotation.yaw, lastServerRotation.yaw),
            getAngleDifference(serverRotation.pitch, lastServerRotation.pitch)
        ) to ClientUtils.runTimeTicks
        )
    }

    override fun onDisable() {
        val currentDateTime = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
        val formattedDateTime = currentDateTime.format(formatter)

        writeToFile("rotations_$formattedDateTime.txt", rotationList)
    }

    private fun writeToFile(fileName: String, content: List<Pair<Rotation, Int>>) {
        // Get the Minecraft directory
        val mcDir = File(mc.mcDataDir, "$CLIENT_NAME-$MINECRAFT_VERSION")
        // Create the file object in the Minecraft directory
        val file = File(mcDir, fileName)
        try {
            BufferedWriter(FileWriter(file)).use { writer ->
                content.forEach {
                    writer.write("YAW: ${it.first.yaw}, PITCH: ${it.first.pitch} in tick ${it.second}")
                    writer.newLine()
                }
                writer.flush()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            Chat.print("Saved as $fileName in $mcDir")
        }
    }

}