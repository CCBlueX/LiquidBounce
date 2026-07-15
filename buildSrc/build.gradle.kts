plugins {
    `kotlin-dsl`
}

group = "net.ccbluex"

repositories {
    mavenCentral()
}

dependencies {
    // Class-hierarchy analysis for ConvertAccessWidenerTask
    implementation("org.ow2.asm:asm:9.9.1")
    // Method-body / annotation-tree reading for MixinDivergenceCheckTask
    implementation("org.ow2.asm:asm-tree:9.9.1")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(25)
}
