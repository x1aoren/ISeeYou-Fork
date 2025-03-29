package cn.xor7.iseeyou.recording;

import cn.xor7.iseeyou.ISeeYouClient;
import cn.xor7.iseeyou.utils.ConfigManager;
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
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 回放管理器类
 * 负责处理游戏回放的录制、保存和管理
 * 实现服务器端的回放录制功能
 */
public class ReplayManager {
    private static final Map<String, PlayerRecording> activeRecordings = new HashMap<>();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    
    /**
     * 初始化回放管理器
     */
    public static void initialize() {
        // 创建必要的目录
        ensureDirectoryExists(ConfigManager.getConfig().getRecordingPath());
        ensureDirectoryExists(ConfigManager.getConfig().getInstantReplayPath());
        
        ISeeYouClient.LOGGER.info("回放管理器初始化完成");
        
        // 如果启用了自动清理，启动清理任务
        if (ConfigManager.getConfig().isAutoCleanup()) {
            scheduleCleanupTask();
        }
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
        if (!ConfigManager.getConfig().isAutoCleanup()) {
            return;
        }
        
        int days = ConfigManager.getConfig().getCleanupDays();
        if (days <= 0) {
            return;
        }
        
        ISeeYouClient.LOGGER.info("开始清理" + days + "天前的回放文件...");
        
        // 获取录制目录下的所有.mcpr文件
        File recordingDir = new File(ConfigManager.getConfig().getRecordingPath());
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
    
    /**
     * 开始录制玩家
     * @param player 要录制的玩家
     * @return 是否成功开始录制
     */
    public static boolean startRecording(ServerPlayerEntity player) {
        if (player == null) {
            ISeeYouClient.LOGGER.warn("无法开始录制：玩家为空");
            return false;
        }
        
        String playerId = player.getUuidAsString();
        if (activeRecordings.containsKey(playerId)) {
            ISeeYouClient.LOGGER.info("玩家" + player.getName().getString() + "已经在录制中");
            return false;
        }
        
        try {
            String playerName = player.getName().getString();
            String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
            String path = ConfigManager.getConfig().getRecordingPath();
            
            // 创建回放文件路径
            String filePath = path + File.separator + playerName + "_" + timestamp + ".mcpr";
            
            // 创建新的录制会话
            PlayerRecording recording = new PlayerRecording(player, filePath);
            activeRecordings.put(playerId, recording);
            
            // 开始记录玩家动作
            recording.start();
            
            ISeeYouClient.LOGGER.info("开始录制玩家" + playerName + "，保存到" + filePath);
            return true;
        } catch (Exception e) {
            ISeeYouClient.LOGGER.error("开始录制时出错", e);
            return false;
        }
    }
    
    /**
     * 停止录制玩家
     * @param player 要停止录制的玩家
     * @return 保存的文件名，如果没有录制则返回null
     */
    public static String stopRecording(ServerPlayerEntity player) {
        if (player == null) {
            return null;
        }
        
        String playerId = player.getUuidAsString();
        PlayerRecording recording = activeRecordings.get(playerId);
        
        if (recording == null) {
            ISeeYouClient.LOGGER.warn("玩家" + player.getName().getString() + "没有进行中的录制");
            return null;
        }
        
        try {
            // 停止录制
            recording.stop();
            
            // 获取文件名并从活动录制中移除
            String fileName = new File(recording.getFilePath()).getName();
            activeRecordings.remove(playerId);
            
            ISeeYouClient.LOGGER.info("已停止录制玩家" + player.getName().getString());
            return fileName;
        } catch (Exception e) {
            ISeeYouClient.LOGGER.error("停止录制时出错", e);
            return null;
        }
    }
    
    /**
     * 停止所有录制
     * 通常在服务器关闭时调用
     */
    public static void stopAllRecordings() {
        ISeeYouClient.LOGGER.info("停止所有录制...");
        
        // 创建一个新的列表以避免并发修改异常
        List<String> playerIds = new ArrayList<>(activeRecordings.keySet());
        
        for (String playerId : playerIds) {
            PlayerRecording recording = activeRecordings.get(playerId);
            if (recording != null) {
                try {
                    recording.stop();
                    ISeeYouClient.LOGGER.info("已停止录制玩家" + recording.getPlayerName());
                } catch (Exception e) {
                    ISeeYouClient.LOGGER.error("停止录制时出错: " + recording.getPlayerName(), e);
                }
            }
        }
        
        // 清空活动录制列表
        activeRecordings.clear();
        ISeeYouClient.LOGGER.info("所有录制已停止");
    }
    
    /**
     * 获取活动录制列表
     */
    public static Map<String, PlayerRecording> getActiveRecordings() {
        return new HashMap<>(activeRecordings);
    }
    
    /**
     * 玩家录制类
     * 表示一个玩家的录制会话
     */
    public static class PlayerRecording {
        private final ServerPlayerEntity player;
        private final String playerName;
        private final String filePath;
        private final LocalDateTime startTime;
        private LocalDateTime endTime;
        private boolean active;
        private final List<PlayerAction> actions = new ArrayList<>();
        
        public PlayerRecording(ServerPlayerEntity player, String filePath) {
            this.player = player;
            this.playerName = player.getName().getString();
            this.filePath = filePath;
            this.startTime = LocalDateTime.now();
            this.active = false;
        }
        
        /**
         * 开始录制
         */
        public void start() {
            active = true;
            
            // 这里应该实现实际的录制逻辑
            // 例如，监听玩家事件，记录玩家动作等
            
            ISeeYouClient.LOGGER.info("开始录制玩家" + playerName);
        }
        
        /**
         * 停止录制并保存
         */
        public void stop() {
            if (!active) {
                return;
            }
            
            active = false;
            endTime = LocalDateTime.now();
            
            // 保存录制数据到文件
            CompletableFuture.runAsync(this::saveToFile);
            
            ISeeYouClient.LOGGER.info("停止录制玩家" + playerName);
        }
        
        /**
         * 保存录制数据到文件
         */
        private void saveToFile() {
            try {
                File file = new File(filePath);
                ensureDirectoryExists(file.getParent());
                
                // 创建一个简单的.mcpr文件（实际上是一个ZIP文件）
                try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(file))) {
                    // 添加元数据
                    ZipEntry metadataEntry = new ZipEntry("metadata.json");
                    zipOut.putNextEntry(metadataEntry);
                    
                    // 创建简单的元数据JSON
                    String metadata = String.format(
                        "{\"serverName\":\"ISeeYou\",\"playerName\":\"%s\",\"startTime\":\"%s\",\"endTime\":\"%s\"}",
                        playerName,
                        startTime.format(DateTimeFormatter.ISO_DATE_TIME),
                        endTime.format(DateTimeFormatter.ISO_DATE_TIME)
                    );
                    
                    zipOut.write(metadata.getBytes());
                    zipOut.closeEntry();
                    
                    // 添加录制数据
                    ZipEntry recordingEntry = new ZipEntry("recording.dat");
                    zipOut.putNextEntry(recordingEntry);
                    
                    // 序列化动作列表（实际实现中应使用更高效的序列化方式）
                    for (PlayerAction action : actions) {
                        String actionStr = action.toString() + "\n";
                        zipOut.write(actionStr.getBytes());
                    }
                    
                    zipOut.closeEntry();
                }
                
                ISeeYouClient.LOGGER.info("已保存回放: " + filePath);
            } catch (Exception e) {
                ISeeYouClient.LOGGER.error("保存回放文件时出错: " + filePath, e);
            }
        }
        
        /**
         * 记录玩家动作
         */
        public void recordAction(PlayerAction action) {
            if (active) {
                actions.add(action);
            }
        }
        
        /**
         * 获取玩家名称
         */
        public String getPlayerName() {
            return playerName;
        }
        
        /**
         * 获取文件路径
         */
        public String getFilePath() {
            return filePath;
        }
        
        /**
         * 是否处于活动状态
         */
        public boolean isActive() {
            return active;
        }
        
        /**
         * 获取录制开始时间
         */
        public LocalDateTime getStartTime() {
            return startTime;
        }
        
        /**
         * 获取录制结束时间
         */
        public LocalDateTime getEndTime() {
            return endTime;
        }
    }
    
    /**
     * 玩家动作类
     * 表示玩家的一个动作
     */
    public static class PlayerAction {
        private final LocalDateTime timestamp;
        private final String type;
        private final String data;
        
        public PlayerAction(String type, String data) {
            this.timestamp = LocalDateTime.now();
            this.type = type;
            this.data = data;
        }
        
        @Override
        public String toString() {
            return String.format(
                "%s,%s,%s",
                timestamp.format(DateTimeFormatter.ISO_DATE_TIME),
                type,
                data
            );
        }
    }
} 