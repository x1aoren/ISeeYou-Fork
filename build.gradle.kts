plugins {
    id("java")
    id("fabric-loom") version "1.4.1"
    kotlin("jvm") version "1.9.22"
}

group = "cn.xor7"
version = "1.3.5"

val minecraftVersion = "1.21"
val yarnMappings = "1.21+build.5"
val loaderVersion = "0.15.5"
val fabricVersion = "0.102.0+1.21"

base {
    archivesName.set("iseeyou")
}

repositories {
    mavenLocal()
    maven {
        name = "Fabric"
        url = uri("https://maven.fabricmc.net/")
    }
    maven {
        name = "Fabric Snapshots"
        url = uri("https://maven.fabricmc.net/snapshots")
    }
    maven {
        name = "JitPack"
        url = uri("https://jitpack.io/")
    }
    mavenCentral()
    maven {
        name = "Sonatype OSS"
        url = uri("https://oss.sonatype.org/content/groups/public/")
    }
    maven {
        name = "Aliyun Maven"
        url = uri("https://maven.aliyun.com/repository/public")
    }
}

dependencies {
    // Minecraft
    minecraft("com.mojang:minecraft:${minecraftVersion}")
    mappings("net.fabricmc:yarn:${yarnMappings}:v2")
    modImplementation("net.fabricmc:fabric-loader:${loaderVersion}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${fabricVersion}")
    
    // 其他依赖
    include("com.moandjiezana.toml:toml4j:0.7.2")
    implementation("com.moandjiezana.toml:toml4j:0.7.2")
    
    include("org.jetbrains.kotlin:kotlin-stdlib:1.9.22")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.22")
    
    include("net.jodah:expiringmap:0.5.11")
    implementation("net.jodah:expiringmap:0.5.11")
    
    // 添加Gson依赖
    include("com.google.code.gson:gson:2.10.1")
    implementation("com.google.code.gson:gson:2.10.1")
    annotationProcessor("com.google.code.gson:gson:2.10.1")
}

val targetJavaVersion = 21 // 保持Java 21版本
java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
    }
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
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

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = targetJavaVersion.toString()
    }
}

kotlin {
    jvmToolchain(21) // 保持Kotlin JVM目标为21
}

// 使用正确的语法配置Loom
loom {
    runs {
        // 为所有运行配置添加调试属性
        configureEach {
            property("fabric.log.level", "debug")
        }
    }
    
    mixin {
        defaultRefmapName.set("iseeyou-refmap.json")
        useLegacyMixinAp.set(false)
    }
}

// 启用详细的日志输出
tasks.withType<org.gradle.api.tasks.JavaExec> {
    jvmArgs("-Dfabric.loom.log.level=debug")
}