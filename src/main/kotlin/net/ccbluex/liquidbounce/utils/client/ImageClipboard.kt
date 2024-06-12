/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.utils.client

import org.apache.commons.exec.CommandLine
import org.apache.commons.exec.DefaultExecutor
import org.apache.commons.exec.OS
import org.apache.commons.exec.PumpStreamHandler
import java.io.File

import java.io.IOException
import java.nio.file.Files

/**
 * Copies the image stored in the [file] to
 * the clipboard using OS specific commands.
 *
 * This is a temporary solution and should
 * be replaced when GLFW adds its own solution
 * ([github.com/glfw/glfw/issues/260](https://github.com/glfw/glfw/issues/260)).
 */
@Throws(IOException::class, IllegalArgumentException::class)
fun copyImageToClipboard(file: File): Boolean {
    require(isFileValid(file)) { "Invalid file" }

    if (OS.isFamilyWindows()) {
        copyImageToClipboardWindows(file)
    } else if (OS.isFamilyMac()) {
        copyImageToClipboardMac(file)
    } else if (OS.isFamilyUnix()) {
        copyImageToClipboardLinux(file)
    } else {
        return false
    }

    return true
}

@Throws(IOException::class)
private fun copyImageToClipboardWindows(file: File) {
    val command =
        "powershell -command \"Add-Type -AssemblyName System.Windows.Forms; " +
            "[System.Windows.Forms.Clipboard]::SetImage([System.Drawing.Image]::FromFile('${
                file.absolutePath.replace("\\", "\\\\")
            }'))\""
    executeCommand(command)
}

@Throws(IOException::class)
private fun copyImageToClipboardMac(file: File) {
    val command = "osascript -e 'set the clipboard to (read (POSIX file \"${
        file.absolutePath
    }\") as JPEG picture)' < ${file.absolutePath}"
    executeCommand(command)
}

@Throws(IOException::class)
private fun copyImageToClipboardLinux(file: File) {
    val command = "xclip -selection clipboard -t image/png -i ${file.absolutePath}"
    executeCommand(command)
}

@Throws(IOException::class)
private fun executeCommand(command: String) {
    val cmdLine = CommandLine.parse(command)
    val executor = DefaultExecutor()
    executor.streamHandler = PumpStreamHandler(System.out, System.err)
    executor.execute(cmdLine)
}

/**
 * Verifies, that the [file] is a valid file
 * that can be safely copied.
 */
private fun isFileValid(file: File): Boolean {
    if (!file.exists() || !file.isFile()) {
        return false
    }

    val canonicalPath = file.canonicalFile.toPath()

    val mimeType = Files.probeContentType(canonicalPath)
    return mimeType != null && mimeType.startsWith("image/")
}
