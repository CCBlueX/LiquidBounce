/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.script.api.global.Chat
import net.ccbluex.liquidbounce.utils.ClientUtils.runTimeTicks
import net.ccbluex.liquidbounce.utils.RotationUtils.angleDifference
import net.ccbluex.liquidbounce.utils.RotationUtils.lastRotations
import net.ccbluex.liquidbounce.utils.RotationUtils.serverRotation
import net.ccbluex.liquidbounce.value.BoolValue
import org.knowm.xchart.BitmapEncoder
import org.knowm.xchart.XYChart
import org.knowm.xchart.XYSeries
import org.lwjgl.opengl.Display
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue

object RotationRecorder : Module("RotationRecorder", Category.MISC) {

    private val captureNegativeNumbers by BoolValue("CaptureNegativeNumbers", false)

    private val ticks = mutableListOf<Double>()
    private val yawDiffs = mutableListOf<Double>()
    private val pitchDiffs = mutableListOf<Double>()

    private var chart: XYChart? = null
    private var failed = false

    override fun onEnable() {
        updateRecordInfo(true)

        try {
            chart = XYChart(Display.getWidth(), Display.getHeight()).apply {
                title = "Yaw and Pitch Differences Over Time"
                xAxisTitle = "Time (ticks)"
                yAxisTitle = "Differences (degrees)"

                // Add series to the chart
                addSeries("Yaw Differences", ticks.toDoubleArray(), yawDiffs.toDoubleArray()).apply {
                    xySeriesRenderStyle = XYSeries.XYSeriesRenderStyle.Line
                    lineColor = java.awt.Color.BLUE // Set yaw line color to blue
                }

                addSeries("Pitch Differences", ticks.toDoubleArray(), pitchDiffs.toDoubleArray()).apply {
                    xySeriesRenderStyle = XYSeries.XYSeriesRenderStyle.Line
                    lineColor = java.awt.Color.RED // Set pitch line color to red
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Chat.print("Failed to start recording rotations, disabling module")
            TickScheduler += {
                failed = true
                state = false
            }

            return
        }

        Chat.print("Started recording rotations.")
    }

    @EventTarget
    fun onMotion(event: MotionEvent) {
        if (event.eventState != EventState.POST || failed)
            return

        updateRecordInfo()

        // Update the series with new data
        chart?.updateXYSeries("Yaw Differences", ticks.toDoubleArray(), yawDiffs.toDoubleArray(), null)
        chart?.updateXYSeries("Pitch Differences", ticks.toDoubleArray(), pitchDiffs.toDoubleArray(), null)
    }

    override fun onDisable() {
        if (!failed) {
            val currentDateTime = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
            val formattedDateTime = currentDateTime.format(formatter)

            saveChart("rotations_$formattedDateTime.png", mc.mcDataDir)
        }

        failed = false
        ticks.clear()
        yawDiffs.clear()
        pitchDiffs.clear()
    }

    private fun saveChart(fileName: String, mcDir: File) {
        val file = File(mcDir, fileName)

        // Save the chart as an image
        try {
            BitmapEncoder.saveBitmap(chart, file.absolutePath, BitmapEncoder.BitmapFormat.PNG)
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            Chat.print("Saved as $fileName in $mcDir")
        }
    }

    private fun updateRecordInfo(wasPreviousTick: Boolean = false) {
        var yawDiff = angleDifference(serverRotation.yaw, lastRotations[1].yaw)
        var pitchDiff = angleDifference(serverRotation.pitch, lastRotations[1].pitch)

        if (!captureNegativeNumbers) {
            yawDiff = yawDiff.absoluteValue
            pitchDiff = pitchDiff.absoluteValue
        }

        ticks.add(runTimeTicks.toDouble() - if (wasPreviousTick) 1 else 0)
        yawDiffs.add(yawDiff.toDouble())
        pitchDiffs.add(pitchDiff.toDouble())
    }

}