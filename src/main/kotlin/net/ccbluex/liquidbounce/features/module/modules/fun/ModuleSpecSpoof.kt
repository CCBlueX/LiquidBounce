package net.ccbluex.liquidbounce.features.module.modules.`fun`

import com.mojang.blaze3d.platform.GlDebugInfo
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule

object ModuleSpecSpoof : ClientModule("SpecSpoof", Category.FUN, aliases = arrayOf("HardwareProtect")) {

    private val spoofCPU by text("CPU", "Intel(R) Core(TM) i9-13900K")
    private val spoofGPU by text("GPU", "NVIDIA GeForce RTX 4090")
    private val spoofDriver by text("Driver", "536.23")
    private val spoofVendor by text("Vendor", "NVIDIA Corporation")

    fun getSpoofedCPU(): String = if (running){
        spoofCPU
    } else {
        GlDebugInfo.getCpuInfo()
    }

    fun getSpoofedGPU(): String = if (running) {
        spoofGPU
    } else {
        GlDebugInfo.getRenderer()
    }

    fun getSpoofedDriver(): String = if (running) {
        spoofDriver
    } else {
        GlDebugInfo.getVersion()
    }

    fun getSpoofedVendor(): String = if (running) {
        spoofVendor
    } else{
        GlDebugInfo.getVendor()
    }
}
