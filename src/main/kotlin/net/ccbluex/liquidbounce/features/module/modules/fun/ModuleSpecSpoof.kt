package net.ccbluex.liquidbounce.features.module.modules.`fun`

import com.mojang.blaze3d.platform.GlDebugInfo
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule

object ModuleSpecSpoof : ClientModule("SpecSpoof", Category.FUN, aliases = arrayOf("HardwareProtect")) {

    private val spoofCPU by text("CPU", "32x Intel(R) Core(TM) i9-14900KS CPU @6.2Ghz")
    private val spoofGPU by text("GPU", "NVIDIA GeForce RTX 5090/PCIe/SSE2")
    private val spoofDriver by text("Driver", "581.29")
    private val spoofVendor by text("Vendor", "CCBlueX Development")

    private val realSpecs = mapOf(
        "CPU" to { GlDebugInfo.getCpuInfo() },
        "GPU" to { GlDebugInfo.getRenderer() },
        "Driver" to { GlDebugInfo.getVersion() },
        "Vendor" to { GlDebugInfo.getVendor() }
    )

    private val spoofedSpecs = mapOf(
        "CPU" to { spoofCPU },
        "GPU" to { spoofGPU },
        "Driver" to { spoofDriver },
        "Vendor" to { spoofVendor }
    )

    fun getSpec(type: String): String =
        (if (running) {
            spoofedSpecs
        } else {
            realSpecs
        })[type]?.invoke() ?: "Unknown"
}
