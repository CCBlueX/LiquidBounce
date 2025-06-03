package net.ccbluex.liquidbounce.utils.client.error

import net.ccbluex.liquidbounce.utils.client.error.errors.JcefIsntCompatible

class Instructions(
    val showStepIndex: Boolean,
    val steps: (error: Throwable) -> Array<String>?
)

enum class QuickFix (
    val description: String,
    val testError: (error: Throwable) -> Boolean = { false },
    val whatYouNeed: Instructions? = null,
    val whatToDo: Instructions? = null
) {
    JCEF_ISNT_COMPATIBLE_WITH_THAT_SYSTEM(
        description = "Your system isn't compatible with JCEF",
        testError = { it is JcefIsntCompatible },
        whatYouNeed = Instructions(false) { _ ->
            arrayOf(
                "A 64-bit computer",
                "Windows 10 or newer, macOS 10.15 or newer, or a Linux system"
            )
        },
        whatToDo = Instructions(false) { _ ->
            arrayOf(
                "Please update your operating system to a never version"
            )
        }
    ),
    DOWNLOAD_JCEF_FAILED(
        description = "A fatal error occurred while loading libraries required for JCEF to work",
        whatYouNeed = Instructions(true) { _ ->
            arrayOf(
                "Stable internet connection",
                "Free space on the disk"
            )
        },
        whatToDo = Instructions(true) { _ ->
            arrayOf(
                "Check your internet connection",
                "Use a VPN such as Cloudflare Warp or another one",
                "Check if there is free space on the disk",
                "Make sure that the client folder is not blocked by the file system"
            )
        }
    ),
    CLASS_NOT_FOUND(
        description = "Some class not found",
        testError = { it is ClassNotFoundException },
        whatYouNeed = Instructions(false) { _ ->
            arrayOf(
                "Make sure you have all the libraries required by minecraft installed"
            )
        },
        whatToDo = Instructions(false) {
            val message = it.message
            if (message == null) {
                null
            } else {
                when {
                    message.contains("viaversion") -> arrayOf("Try to install ViaFabric")
                    message.contains("modmenu") -> arrayOf("Try to install ModMenu")
                    else -> null
                }
            }
        }
    );

    val messages = mapOf(
        "What you need" to whatYouNeed,
        "What to do" to whatToDo
    ).filter { it.value != null }
}
