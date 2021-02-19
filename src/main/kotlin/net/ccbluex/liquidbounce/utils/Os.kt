package net.ccbluex.liquidbounce.utils

private val OS = System.getProperty("os.name").toLowerCase()
var IS_WINDOWS = OS.indexOf("win") >= 0
var IS_MAC = OS.indexOf("mac") >= 0
var IS_UNIX = OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0
