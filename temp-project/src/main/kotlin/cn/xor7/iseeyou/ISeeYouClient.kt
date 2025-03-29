package cn.xor7.iseeyou

import cn.xor7.iseeyou.utils.ConfigData
import cn.xor7.iseeyou.utils.TomlEx
import com.replaymod.core.ReplayMod
import com.replaymod.core.versions.MCVer
import com.replaymod.recording.packet.PacketListener
import com.replaymod.replay.ReplayHandler
import com.replaymod.replay.ReplayModReplay
import com.replaymod.replaystudio.replay.ReplayFile
import com.replaymod.replaystudio.replay.ZipReplayFile
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.KeyMapping
import net.minecraft.client.player.LocalPlayer
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW
import java.io.File
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.io.path.isDirectory

class ISeeYouClient : ClientModInitializer {
    companion object {
        lateinit var INSTANCE: ISeeYouClient
        val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
        var toml: TomlEx<ConfigData>? = null
        var highSpeedPausedPlayers = mutableSetOf<UUID>()
        var outdatedRecordRetentionDays: Int = 0
        val minecraft: Minecraft = MCVer.getMinecraft()
    }

    private val recorders = mutableMapOf<UUID, PlayerRecorder>()
    private lateinit var instantReplayKey: KeyMapping

    override fun onInitialize() {
        INSTANCE = this
        setupConfig()
        registerKeyBindings()
        registerEventHandlers()
        
        // 清理临时文件
        if (toml!!.data.deleteTmpFileOnLoad) {
            try {
                Files.walk(Paths.get(toml!!.data.recordPath), Int.MAX_VALUE, FileVisitOption.FOLLOW_LINKS).use { paths ->
                    paths.filter { it.isDirectory() && it.fileName.toString().endsWith(".tmp") }
                        .forEach { deleteTmpFolder(it) }
                }
            } catch (_: IOException) {
                // 忽略异常
            }
        }

        // 清理过期记录文件
        if (toml!!.data.clearOutdatedRecordFile.enabled) {
            cleanOutdatedRecordings()
            // 定期清理
            Thread {
                while (true) {
                    try {
                        Thread.sleep(toml!!.data.clearOutdatedRecordFile.interval * 60 * 60 * 1000L)
                        cleanOutdatedRecordings()
                    } catch (e: InterruptedException) {
                        break
                    }
                }
            }.start()
        }
        
        logInfo("ISeeYou Mod 已加载!")
    }
    
    private fun registerKeyBindings() {
        instantReplayKey = KeyBindingHelper.registerKeyBinding(KeyMapping(
            "key.iseeyou.instantreplay",
            GLFW.GLFW_KEY_I,
            "key.categories.iseeyou"
        ))
        
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            while (instantReplayKey.consumeClick()) {
                if (toml!!.data.instantReplay.enabled && client.player != null) {
                    createInstantReplay(client.player!!)
                    client.player!!.sendSystemMessage(Component.literal("成功创建即时回放"))
                }
            }
        }
    }
    
    private fun registerEventHandlers() {
        // 玩家连接事件
        ClientPlayConnectionEvents.JOIN.register { _, _, _ ->
            val player = minecraft.player ?: return@register
            if (!toml!!.data.shouldRecordPlayer(player)) {
                return@register
            }
            
            startRecording(player)
        }
        
        // 玩家断开连接事件
        ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
            val player = minecraft.player ?: return@register
            if (toml!!.data.pauseInsteadOfStopRecordingOnPlayerQuit) {
                recorders[player.uuid]?.pause()
            } else {
                recorders[player.uuid]?.stop()
                recorders.remove(player.uuid)
            }
        }
    }
    
    fun startRecording(player: LocalPlayer) {
        val uuid = player.uuid
        val recorder = PlayerRecorder(player)
        recorders[uuid] = recorder
        recorder.start()
    }
    
    fun createInstantReplay(player: LocalPlayer) {
        val currentTime = LocalDateTime.now()
        val recordPath: String = toml!!.data.instantReplay.recordPath
            .replace("\${name}", player.name.string)
            .replace("\${uuid}", player.uuid.toString())
        File(recordPath).mkdirs()
        
        val recordFile = File(recordPath + "/" + currentTime.format(DATE_FORMATTER) + ".mcpr")
        if (recordFile.exists()) recordFile.delete()
        recordFile.createNewFile()
        
        // 使用ReplayMod的API创建录像
        val replayFile = ZipReplayFile(recordFile, false)
        val metadata = replayFile.metaData
        metadata.singleplayer = minecraft.isLocalServer
        metadata.serverName = if (minecraft.currentServer != null) minecraft.currentServer!!.name else "单人世界"
        metadata.duration = (toml!!.data.instantReplay.replayMinutes * 60 * 1000).toLong()
        metadata.date = System.currentTimeMillis()
        metadata.mcversion = minecraft.launchedVersion
        metadata.generator = "ISeeYou Mod v${INSTANCE.javaClass.`package`.implementationVersion}"
        replayFile.writeMetaData(metadata)
        
        // 从当前ReplayMod录制的文件中复制最近的数据
        val activeRecording = ReplayMod.instance.connectionEventHandler.currentPacketListener?.replayFile
        if (activeRecording != null) {
            copyRecordingData(activeRecording, replayFile, toml!!.data.instantReplay.replayMinutes)
        }
        
        replayFile.close()
    }
    
    private fun copyRecordingData(source: ReplayFile, target: ReplayFile, minutes: Int) {
        // 从源文件复制最近n分钟的数据到目标文件
        val currentTime = System.currentTimeMillis()
        val startTime = currentTime - (minutes * 60 * 1000)
        
        // 处理录像数据（这里需要根据ReplayMod的API实现细节来完善）
        // 由于ReplayMod的API可能会变化，以下代码是示例
        // 实际实现可能需要根据ReplayMod的最新API进行调整
        try {
            source.readPacketData { timestamp, _, data ->
                if (timestamp >= startTime) {
                    val adjustedTimestamp = timestamp - startTime
                    target.writePacketData(adjustedTimestamp, 0, data)
                }
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun setupConfig() {
        toml = TomlEx("config/iseeyou/config.toml", ConfigData::class.java)
        val errMsg = toml!!.data.isConfigValid()
        if (errMsg != null) {
            throw IllegalStateException(errMsg)
        }
        toml!!.data.setConfig()
        outdatedRecordRetentionDays = toml!!.data.clearOutdatedRecordFile.days
        toml!!.save()
    }
    
    private fun deleteTmpFolder(folderPath: Path) {
        try {
            Files.walkFileTree(folderPath, EnumSet.noneOf(FileVisitOption::class.java), Int.MAX_VALUE, object : SimpleFileVisitor<Path>() {
                @Throws(IOException::class)
                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    Files.delete(file)
                    return FileVisitResult.CONTINUE
                }

                @Throws(IOException::class)
                override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
                    Files.delete(dir)
                    return FileVisitResult.CONTINUE
                }
            })
        } catch (e: IOException) {
            logSevere("删除临时文件夹时出错: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun cleanOutdatedRecordings() {
        try {
            val recordPathA: String = toml!!.data.recordPath
            val recordingsDirA = Paths.get(recordPathA).parent

            logInfo("开始删除过期的记录文件在 $recordingsDirA")
            val deletedCount = deleteFilesInDirectory(recordingsDirA)

            logInfo("已删除过期的记录文件，删除了 $deletedCount 个文件")
        } catch (e: IOException) {
            logSevere("清理过期记录时出错: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun deleteFilesInDirectory(directory: Path): Int {
        var count = 0
        Files.walk(directory).use { paths ->
            paths.filter { Files.isDirectory(it) && it.parent == directory }
                .forEach { folder ->
                    count += deleteRecordingFiles(folder)
                }
        }
        return count
    }

    private fun deleteRecordingFiles(folderPath: Path): Int {
        var deletedCount = 0
        var fileCount = 0
        try {
            val currentDate = LocalDate.now()
            Files.walk(folderPath).use { paths ->
                paths.filter { Files.isRegularFile(it) && it.toString().endsWith(".mcpr") }
                    .forEach { file ->
                        fileCount++
                        val fileName = file.fileName.toString()
                        val creationDateStr = fileName.substringBefore('@')
                        try {
                            val creationDate = LocalDate.parse(creationDateStr, DATE_FORMATTER)
                            val daysSinceCreation = Duration.between(creationDate.atStartOfDay(), currentDate.atStartOfDay()).toDays()
                            if (daysSinceCreation > outdatedRecordRetentionDays) {
                                val executor = Executors.newSingleThreadExecutor()
                                val future = executor.submit(Callable {
                                    try {
                                        Files.delete(file)
                                        logInfo("删除了记录文件: $fileName")
                                        true
                                    } catch (e: IOException) {
                                        logSevere("删除记录文件时出错: $fileName, 错误: ${e.message}")
                                        e.printStackTrace()
                                        false
                                    }
                                })

                                try {
                                    if (future.get(2, TimeUnit.SECONDS)) {
                                        deletedCount++
                                    }
                                } catch (e: TimeoutException) {
                                    logWarning("删除文件超时: $fileName. 跳过此文件...")
                                    future.cancel(true)
                                } finally {
                                    executor.shutdown()
                                }
                            }
                        } catch (e: Exception) {
                            logWarning("解析文件日期失败: $fileName")
                        }
                    }
            }
            if (fileCount == 0 || deletedCount == 0) {
                logInfo("未找到需要删除的过期记录文件。")
            }
        } catch (e: IOException) {
            logSevere("处理文件夹时出错: $folderPath, 错误: ${e.message}")
            e.printStackTrace()
        }
        return deletedCount
    }

    fun logInfo(message: String) {
        println("[ISeeYou] INFO: $message")
    }

    fun logWarning(message: String) {
        println("[ISeeYou] WARNING: $message")
    }

    fun logSevere(message: String) {
        println("[ISeeYou] SEVERE: $message")
    }

    inner class PlayerRecorder(private val player: LocalPlayer) {
        private var recording = false
        private var paused = false
        private var replayFile: ZipReplayFile? = null
        private var packetListener: PacketListener? = null
        
        fun start() {
            if (recording) return
            
            val currentTime = LocalDateTime.now()
            val recordPath: String = toml!!.data.recordPath
                .replace("\${name}", player.name.string)
                .replace("\${uuid}", player.uuid.toString())
            File(recordPath).mkdirs()
            
            val recordFile = File(recordPath + "/" + currentTime.format(DATE_FORMATTER) + ".mcpr")
            if (recordFile.exists()) recordFile.delete()
            recordFile.createNewFile()
            
            // 创建ReplayMod录像文件
            replayFile = ZipReplayFile(recordFile, false)
            val metadata = replayFile!!.metaData
            metadata.singleplayer = minecraft.isLocalServer
            metadata.serverName = if (minecraft.currentServer != null) minecraft.currentServer!!.name else "单人世界"
            metadata.date = System.currentTimeMillis()
            metadata.mcversion = minecraft.launchedVersion
            metadata.generator = "ISeeYou Mod v${INSTANCE.javaClass.`package`.implementationVersion}"
            replayFile!!.writeMetaData(metadata)
            
            // 使用ReplayMod的录制API
            packetListener = PacketListener(minecraft.player!!.connection, replayFile!!, null, null)
            packetListener!!.start()
            
            recording = true
            paused = false
            logInfo("开始为玩家 ${player.name.string} 录制")
        }
        
        fun pause() {
            if (!recording || paused) return
            packetListener?.stop()
            paused = true
            logInfo("暂停为玩家 ${player.name.string} 录制")
        }
        
        fun resume() {
            if (!recording || !paused) return
            packetListener?.start()
            paused = false
            logInfo("恢复为玩家 ${player.name.string} 录制")
        }
        
        fun stop() {
            if (!recording) return
            packetListener?.stop()
            replayFile?.close()
            recording = false
            paused = false
            logInfo("停止为玩家 ${player.name.string} 录制")
        }
    }
} 