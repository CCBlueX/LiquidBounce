package net.ccbluex.liquidinstruction

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.injection.backend.Backend
import java.awt.BorderLayout
import java.io.File
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.UIManager
import javax.swing.WindowConstants

fun main()
{
	UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())

	// Setup instruction frame
	val frame = JFrame("LiquidBounce | Installation")
	frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
	frame.layout = BorderLayout()
	frame.isResizable = false
	frame.isAlwaysOnTop = true

	// Add instruction as label
	val label = JLabel(LiquidBounce::class.java.getResourceAsStream("/instructions.html")?.run { reader().readText().replace("{assets}", "${LiquidBounce.javaClass.classLoader.getResource("assets")}").replace("{filename}", LiquidBounce.javaClass.protectionDomain?.codeSource?.location?.path?.let { File(it).name } ?: "").replace("{mcversion}", Backend.MINECRAFT_VERSION) } ?: "Instruction HTML unavailable")
	frame.add(label, BorderLayout.CENTER)

	// Pack frame
	frame.pack()

	// Set location to center of screen
	frame.setLocationRelativeTo(null)

	// Display instruction frame
	frame.isVisible = true
}
