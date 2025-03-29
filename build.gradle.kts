import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("java")
    id("io.github.goooler.shadow") version "8.1.7"
    kotlin("jvm") version "1.9.20"
    id("fabric-loom") version "1.0.17"
}

group = "cn.xor7"
version = "1.3.5"

val commandAPIVer = "9.7.0"
val minecraftVersion = "1.21.1"
val yarnMappings = "1.21.1+build.2"
val loaderVersion = "0.15.5"
val fabricVersion = "0.95.4+1.21.1"

repositories {
    mavenLocal()
    maven("https://jitpack.io/")
    mavenCentral()
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://maven.aliyun.com/repository/public")
    maven("https://maven.fabricmc.net/")
    maven("https://repo.leavesmc.org/releases")
    maven("https://repo.leavesmc.org/snapshots")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.org/repository/maven-public/")
    flatDir {
        dirs("libs")
    }
}

dependencies {
    // Minecraft
    minecraft("com.mojang:minecraft:${minecraftVersion}")
    mappings("net.fabricmc:yarn:${yarnMappings}:v2")
    modImplementation("net.fabricmc:fabric-loader:${loaderVersion}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${fabricVersion}")
    
    // Fabric 版 ReplayMod 依赖，如果有的话
    // modImplementation("com.replaymod:replaymod:1.0.0") // 替换为正确的版本
    
    // 其他依赖
    implementation("com.moandjiezana.toml:toml4j:0.7.2")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.0")
    implementation("net.jodah:expiringmap:0.5.11")

    // 如果需要，可以保留这些作为开发依赖，但不要用于生产
    // compileOnly(files("libs/ThemisAPI_0.15.3.jar"))
    // compileOnly(files("libs/Matrix_7.12.4.jar"))
}

val targetJavaVersion = 17
java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
    }
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

// 处理资源文件
tasks.processResources {
    inputs.property("version", version)
    
    filesMatching("fabric.mod.json") {
        expand(
            mapOf(
                "version" to version,
                "minecraft_version" to minecraftVersion,
                "loader_version" to loaderVersion
            )
        )
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible) {
        options.release.set(targetJavaVersion)
    }
}

tasks.withType<ShadowJar> {
    archiveClassifier.set("dev")
    configurations = listOf(project.configurations.shadow.get())
    minimize()
    exclude("META-INF/*.SF")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.RSA")
    mergeServiceFiles()
}

// 创建一个常规的JAR，作为Fabric模组发布
tasks.jar {
    archiveClassifier.set("")
    from("LICENSE") {
        rename { "${it}_${project.base.archivesName.get()}" }
    }
}

kotlin {
    jvmToolchain(17)
}