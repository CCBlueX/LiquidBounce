package net.ccbluex.liquidinstruction

import net.ccbluex.liquidbounce.LiquidBounce
import java.awt.BorderLayout
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.WindowConstants

fun main() {
    // Setup instruction frame
    val frame = JFrame("LiquidBounce | Installation")
    frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
    frame.layout = BorderLayout()
    frame.isResizable = false
    frame.isAlwaysOnTop = true

    // Add instruction as label
    @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    val label = JLabel(LiquidBounce::class.java.getResourceAsStream("/instructions.html").reader().readText()
            .replace("{assets}", LiquidBounce.javaClass.classLoader.getResource("assets").toString()))
    frame.add(label, BorderLayout.CENTER)

    // Pack frame
    frame.pack()

    // Set location to center of screen
    frame.setLocationRelativeTo(null)

    // Display instruction frame
    frame.isVisible = true
}