package cn.xor7.iseeyou.utils

import cn.xor7.iseeyou.ISeeYouClient
import com.replaymod.core.ReplayMod
import com.replaymod.replaystudio.replay.ReplayFile
import net.jodah.expiringmap.ExpirationPolicy
import net.jodah.expiringmap.ExpiringMap
import net.minecraft.client.player.LocalPlayer
import java.io.File
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object InstantReplayManager {
    val replayFileMap: ExpiringMap<String, File> = ExpiringMap.builder()
        .expiration(ISeeYouClient.toml!!.data.instantReplay.replayMinutes.toLong(), TimeUnit.MINUTES)
        .expirationPolicy(ExpirationPolicy.CREATED)
        .expirationListener { uuid: String, file: File ->
            // 自动删除过期的回放文件
            if (file.exists()) {
                file.delete()
            }
        }
        .build()
    
    /**
     * 为玩家创建即时回放
     */
    fun createReplay(player: LocalPlayer): Boolean {
        val currentTime = LocalDateTime.now()
        val recordPath: String = ISeeYouClient.toml!!.data.instantReplay.recordPath
            .replace("\${name}", player.name.string)
            .replace("\${uuid}", player.uuid.toString())
        File(recordPath).mkdirs()
        
        val recordFile = File(recordPath + "/" + currentTime.format(ISeeYouClient.DATE_FORMATTER) + ".mcpr")
        if (recordFile.exists()) recordFile.delete()
        recordFile.createNewFile()
        
        // 获取ReplayMod当前的录制文件
        val activeRecording = ReplayMod.instance.connectionEventHandler.currentPacketListener?.replayFile ?: return false
        
        // 复制最近的数据到新文件
        if (copyRecentRecordingData(activeRecording, recordFile, ISeeYouClient.toml!!.data.instantReplay.replayMinutes)) {
            // 将文件加入到过期管理器
            replayFileMap[player.uuid.toString()] = recordFile
            return true
        }
        
        return false
    }
    
    /**
     * 从当前录制中复制最近的数据到新文件
     */
    private fun copyRecentRecordingData(source: ReplayFile, targetFile: File, minutes: Int): Boolean {
        try {
            // 使用ReplayMod的API处理录像数据
            // 这里是一个简化的逻辑，实际实现可能更复杂
            // 根据最新的ReplayMod API可能需要调整
            
            // TODO: 实现录像数据复制
            // 由于ReplayMod的API可能会变化，这里需要根据实际情况调整
            
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}