/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.script.api.global.Chat
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.utils.RotationUtils.getAngleDifference
import net.ccbluex.liquidbounce.utils.RotationUtils.lastServerRotation
import net.minecraft.client.Minecraft
import net.minecraft.network.play.client.C03PacketPlayer
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object RotationRecorder : Module("RotationRecorder", Category.MISC) {

    private val rotationList: MutableList<Rotation> = mutableListOf()

    override fun onEnable() {
        rotationList.clear()

        Chat.print("Started recording rotations.")
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (event.packet is C03PacketPlayer) {
            if (event.packet.rotating) {
                rotationList.add(Rotation(getAngleDifference(event.packet.yaw, lastServerRotation.yaw),
                    getAngleDifference(event.packet.pitch, lastServerRotation.pitch)
                )
                )
            } else {
                rotationList.add(Rotation(0f, 0f))
            }
        }
    }

    override fun onDisable() {
        writeToFile("rotations ${LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"))}.txt",
            rotationList
        )
    }

    private fun writeToFile(fileName: String, content: List<Rotation>) {
        // Get the Minecraft directory
        val mcDir = Minecraft.getMinecraft().mcDataDir
        // Create the file object in the Minecraft directory
        val file = File(mcDir, fileName)
        try {
            BufferedWriter(FileWriter(file)).use { writer ->
                content.forEach {
                    writer.write("YAW: ${it.yaw}, PITCH: ${it.pitch}")
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