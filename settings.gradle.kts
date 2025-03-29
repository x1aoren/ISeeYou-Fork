plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.quiltmc.org/repository/release")
        maven("https://maven.minecraftforge.net/")
    }
    plugins {
        id("fabric-loom") version "1.0.17"
    }
}

rootProject.name = "iseeyou"