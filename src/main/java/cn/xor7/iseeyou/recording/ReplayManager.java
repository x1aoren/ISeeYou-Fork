package cn.xor7.iseeyou.recording;

import cn.xor7.iseeyou.ISeeYouClient;
import cn.xor7.iseeyou.utils.ConfigManager;
import com.replaymod.recording.ReplayModRecording;
import com.replaymod.replay.ReplayMod;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.packet.State;
import com.replaymod.core.ReplayMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 回放管理器类
 * 负责处理游戏回放的录制、保存和管理
 * 集成ReplayMod API实现录制功能
 */
public class ReplayManager {
    private static boolean isRecording = false;
    private static ReplayModRecording replayModRecording;
    private static String currentReplayFile;

    /**
     * 初始化回放管理器
     */
    public static void initialize() {
        // ReplayMod会在游戏启动时自动初始化
        ISeeYouClient.LOGGER.info("回放管理器初始化");
    }
    
    /**
     * 开始录制新的回放
     * @param client Minecraft客户端实例
     * @return 是否成功开始录制
     */
    public static boolean startRecording(MinecraftClient client) {
        if (isRecording) {
            ISeeYouClient.LOGGER.info("已经在录制中，忽略此次请求");
            return false;
        }
        
        if (client.player == null) {
            ISeeYouClient.LOGGER.warn("无法开始录制：玩家实体为空");
            return false;
        }
        
        try {
            String playerName = client.player.getName().getString();
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            String path = ConfigManager.getConfig().getRecordingPath();
            
            // 确保目录存在
            File directory = new File(path);
            if (!directory.exists()) {
                directory.mkdirs();
            }
            
            // 创建录制文件路径
            currentReplayFile = directory.getPath() + File.separator + playerName + "_" + timestamp + ".mcpr";
            
            // 使用ReplayMod API开始录制
            ReplayMod.instance.startRecording(currentReplayFile);
            
            // 更新状态
            isRecording = true;
            
            // 通知玩家
            if (ConfigManager.getConfig().isShowNotifications()) {
                client.player.sendMessage(Text.translatable("message.iseeyou.recording_started"), true);
            }
            
            ISeeYouClient.LOGGER.info("开始录制回放: " + currentReplayFile);
            return true;
        } catch (Exception e) {
            ISeeYouClient.LOGGER.error("开始录制时发生错误", e);
            return false;
        }
    }
    
    /**
     * 停止当前录制并保存
     * @param client Minecraft客户端实例
     * @return 保存的文件名
     */
    public static String stopRecording(MinecraftClient client) {
        if (!isRecording) {
            ISeeYouClient.LOGGER.warn("没有正在进行的录制");
            return null;
        }
        
        try {
            // 更新状态
            isRecording = false;
            
            // 使用ReplayMod API停止录制
            ReplayMod.instance.stopRecording();
            
            // 获取文件名
            String fileName = new File(currentReplayFile).getName();
            
            // 通知玩家
            if (client.player != null && ConfigManager.getConfig().isShowNotifications()) {
                client.player.sendMessage(
                    Text.translatable("message.iseeyou.replay_saved", fileName), 
                    false
                );
            }
            
            ISeeYouClient.LOGGER.info("回放已保存: " + fileName);
            return fileName;
        } catch (Exception e) {
            ISeeYouClient.LOGGER.error("停止录制时出错", e);
            return null;
        }
    }
    
    /**
     * 检查是否正在录制
     */
    public static boolean isRecording() {
        return isRecording;
    }
    
    /**
     * 获取已完成的录制会话列表
     */
    public static List<File> getCompletedRecordings() {
        String path = ConfigManager.getConfig().getRecordingPath();
        File directory = new File(path);
        List<File> recordings = new ArrayList<>();
        
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles((dir, name) -> name.endsWith(".mcpr"));
            if (files != null) {
                for (File file : files) {
                    recordings.add(file);
                }
            }
        }
        
        return recordings;
    }
    
    /**
     * 删除指定的录制文件
     */
    public static boolean deleteRecording(File recordingFile) {
        if (recordingFile.exists()) {
            return recordingFile.delete();
        }
        return false;
    }
    
    /**
     * 清理所有录制文件
     */
    public static int cleanupRecordings() {
        List<File> recordings = getCompletedRecordings();
        int count = 0;
        
        for (File file : recordings) {
            if (file.delete()) {
                count++;
            }
        }
        
        return count;
    }
} 