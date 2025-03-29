import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("java")
    id("io.github.goooler.shadow") version "8.1.7"
    id("fabric-loom") version "1.2-SNAPSHOT"
    kotlin("jvm") version "1.9.20"
}

group = "cn.xor7"
version = "1.3.5"

val minecraftVersion = "1.21.1"
val yarnMappings = "1.21.1+build.2"
val loaderVersion = "0.15.5"
val fabricVersion = "0.115.2+1.21.1"

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
    implementation("com.moandjiezana.toml:toml4j:0.7.2")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.0")
    implementation("net.jodah:expiringmap:0.5.11")
    
    // Shadow配置
    shadow("org.jetbrains.kotlin:kotlin-stdlib:1.9.0")
    shadow("com.moandjiezana.toml:toml4j:0.7.2")
    shadow("net.jodah:expiringmap:0.5.11")
}

val targetJavaVersion = 21
java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
    }
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
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
    minimize()
    exclude("META-INF/*.SF")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.RSA")
    mergeServiceFiles()
}

// 创建一个常规的JAR，作为Fabric模组发布
tasks.jar {
    dependsOn("shadowJar")
    archiveClassifier.set("") 
}

kotlin {
    jvmToolchain(21)
}

// 使用正确的语法配置Loom
loom {
    runs {
        // 为所有运行配置添加调试属性
        configureEach {
            property("fabric.log.level", "debug")
        }
    }
}

// 启用详细的日志输出
tasks.withType<org.gradle.api.tasks.JavaExec> {
    jvmArgs("-Dfabric.loom.log.level=debug")
}