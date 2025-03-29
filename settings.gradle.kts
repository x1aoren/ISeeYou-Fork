pluginManagement {
    repositories {
        gradlePluginPortal()
        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }
        maven {
            name = "Fabric Snapshots"
            url = uri("https://maven.fabricmc.net/snapshots")
        }
        maven {
            name = "Quilt"
            url = uri("https://maven.quiltmc.org/repository/release")
        }
        maven {
            name = "Forge"
            url = uri("https://maven.minecraftforge.net/")
        }
        mavenCentral()
    }
    plugins {
        id("fabric-loom") version "1.4.+"
    }
}

rootProject.name = "iseeyou"