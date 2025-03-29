package cn.xor7.iseeyou.recording;

import cn.xor7.iseeyou.ISeeYouClient;
import cn.xor7.iseeyou.utils.ModConfig;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 回放管理器
 * 实现服务器端录制功能，管理玩家录制状态
 */
public class ReplayManager {
    private static final Map<UUID, ReplayRecorder> activeRecorders = new HashMap<>();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static ModConfig config;
    
    /**
     * 初始化回放管理器
     */
    public static void initialize() {
        ISeeYouClient.LOGGER.info("初始化回放管理器...");
    }
    
    /**
     * 设置配置
     */
    public static void setConfig(ModConfig config) {
        ReplayManager.config = config;
    }
    
    /**
     * 开始录制玩家
     * @param player 要录制的玩家
     * @return 是否成功开始录制
     */
    public static boolean startRecording(ServerPlayerEntity player) {
        if (player == null) {
            return false;
        }
        
        UUID playerUUID = player.getUuid();
        
        // 检查是否已经在录制
        if (activeRecorders.containsKey(playerUUID)) {
            ISeeYouClient.LOGGER.info("玩家已在录制中: " + player.getName().getString());
            return false;
        }
        
        try {
            // 准备录制文件路径
            String playerName = player.getName().getString();
            String recordPath = config.getRecordingPath()
                .replace("${name}", playerName)
                .replace("${uuid}", playerUUID.toString());
            
            // 确保目录存在
            File recordDir = new File(recordPath);
            if (!recordDir.exists()) {
                recordDir.mkdirs();
            }
            
            // 创建录制文件
            String fileName = LocalDateTime.now().format(DATE_FORMATTER) + ".mcpr";
            File recordFile = new File(recordDir, fileName);
            
            // 创建录制器
            ReplayRecorder recorder = new ReplayRecorder(player, recordFile);
            activeRecorders.put(playerUUID, recorder);
            
            // 开始录制
            recorder.startRecording();
            
            // 通知玩家
            player.sendMessage(Text.translatable("message.iseeyou.recording_started"));
            
            ISeeYouClient.LOGGER.info("已开始录制玩家: " + playerName);
            return true;
        } catch (Exception e) {
            ISeeYouClient.LOGGER.error("开始录制玩家时出错: " + player.getName().getString(), e);
            return false;
        }
    }
    
    /**
     * 停止录制玩家
     * @param player 要停止录制的玩家
     * @return 录制文件名，如果没有录制则返回null
     */
    public static String stopRecording(ServerPlayerEntity player) {
        if (player == null) {
            return null;
        }
        
        UUID playerUUID = player.getUuid();
        ReplayRecorder recorder = activeRecorders.get(playerUUID);
        
        if (recorder == null) {
            ISeeYouClient.LOGGER.info("玩家未在录制中: " + player.getName().getString());
            return null;
        }
        
        try {
            // 停止录制
            String fileName = recorder.stopRecording();
            activeRecorders.remove(playerUUID);
            
            ISeeYouClient.LOGGER.info("已停止录制玩家: " + player.getName().getString());
            return fileName;
        } catch (Exception e) {
            ISeeYouClient.LOGGER.error("停止录制玩家时出错: " + player.getName().getString(), e);
            return null;
        }
    }
    
    /**
     * 停止所有录制
     * 用于服务器关闭时
     */
    public static void stopAllRecordings() {
        ISeeYouClient.LOGGER.info("停止所有录制...");
        
        for (Map.Entry<UUID, ReplayRecorder> entry : activeRecorders.entrySet()) {
            try {
                ReplayRecorder recorder = entry.getValue();
                recorder.stopRecording();
                ISeeYouClient.LOGGER.info("已停止录制玩家: " + entry.getKey());
            } catch (Exception e) {
                ISeeYouClient.LOGGER.error("停止录制玩家时出错: " + entry.getKey(), e);
            }
        }
        
        activeRecorders.clear();
    }
    
    /**
     * 获取播放器正在录制的玩家数量
     */
    public static int getActiveRecordingsCount() {
        return activeRecorders.size();
    }
    
    /**
     * 检查玩家是否正在被录制
     */
    public static boolean isRecording(PlayerEntity player) {
        return player != null && activeRecorders.containsKey(player.getUuid());
    }
    
    /**
     * 确保目录存在
     */
    private static void ensureDirectoryExists(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            if (dir.mkdirs()) {
                ISeeYouClient.LOGGER.info("创建目录：" + path);
            } else {
                ISeeYouClient.LOGGER.warn("无法创建目录：" + path);
            }
        }
    }
    
    /**
     * 安排清理任务
     */
    private static void scheduleCleanupTask() {
        // 创建一个线程，定期清理旧文件
        Thread cleanupThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // 每天运行一次
                    Thread.sleep(24 * 60 * 60 * 1000);
                    cleanupOldRecordings();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        cleanupThread.setDaemon(true);
        cleanupThread.start();
        
        ISeeYouClient.LOGGER.info("已安排自动清理任务");
    }
    
    /**
     * 清理旧回放文件
     */
    public static void cleanupOldRecordings() {
        if (!config.isAutoCleanup()) {
            return;
        }
        
        int days = config.getCleanupDays();
        if (days <= 0) {
            return;
        }
        
        ISeeYouClient.LOGGER.info("开始清理" + days + "天前的回放文件...");
        
        // 获取录制目录下的所有.mcpr文件
        File recordingDir = new File(config.getRecordingPath());
        File[] mcprFiles = recordingDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".mcpr"));
        
        if (mcprFiles == null || mcprFiles.length == 0) {
            ISeeYouClient.LOGGER.info("没有找到需要清理的文件");
            return;
        }
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
        int deletedCount = 0;
        
        for (File file : mcprFiles) {
            try {
                // 获取文件的最后修改时间
                LocalDateTime fileDate = LocalDateTime.ofInstant(
                    Files.getLastModifiedTime(file.toPath()).toInstant(),
                    java.time.ZoneId.systemDefault()
                );
                
                // 如果文件早于截止日期，删除它
                if (fileDate.isBefore(cutoffDate)) {
                    if (file.delete()) {
                        deletedCount++;
                    } else {
                        ISeeYouClient.LOGGER.warn("无法删除文件: " + file.getAbsolutePath());
                    }
                }
            } catch (IOException e) {
                ISeeYouClient.LOGGER.error("处理文件时出错: " + file.getAbsolutePath(), e);
            }
        }
        
        ISeeYouClient.LOGGER.info("清理完成，删除了" + deletedCount + "个过期回放文件");
    }
} 