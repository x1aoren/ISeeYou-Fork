package cn.xor7.iseeyou.recording;

import cn.xor7.iseeyou.ISeeYouClient;
import cn.xor7.iseeyou.utils.ModConfig;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 回放管理器
 * 实现服务器端录制功能，管理玩家录制状态
 */
public class ReplayManager {
    private static final Map<UUID, ReplayRecorder> activeRecorders = new ConcurrentHashMap<>();
    private static final Map<String, ReplayRecorder> customRecorders = new ConcurrentHashMap<>();
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH-mm-ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy_MM_dd");
    private static ModConfig config;
    private static String currentDateFolder = "";
    private static ScheduledExecutorService scheduler;
    private static LocalDate lastRecordDate = LocalDate.now();
    
    /**
     * 初始化回放管理器
     */
    public static void initialize() {
        ISeeYouClient.LOGGER.info("初始化回放管理器...");
        currentDateFolder = LocalDate.now().format(DATE_FORMATTER);
        scheduler = Executors.newScheduledThreadPool(1);
        
        // 安排每日零点的任务
        scheduleNextDayTask();
    }
    
    /**
     * 安排下一个零点任务
     */
    private static void scheduleNextDayTask() {
        // 计算到下一个零点的毫秒数
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextDay = now.toLocalDate().plusDays(1).atStartOfDay();
        long millisecondsUntilMidnight = java.time.Duration.between(now, nextDay).toMillis();
        
        // 安排任务
        scheduler.schedule(() -> {
            try {
                // 更新当前日期文件夹
                lastRecordDate = LocalDate.now();
                currentDateFolder = lastRecordDate.format(DATE_FORMATTER);
                
                // 重新启动所有录制
                restartAllRecordings();
                
                // 安排下一个零点任务
                scheduleNextDayTask();
            } catch (Exception e) {
                ISeeYouClient.LOGGER.error("执行零点任务时出错", e);
            }
        }, millisecondsUntilMidnight, TimeUnit.MILLISECONDS);
        
        ISeeYouClient.LOGGER.info("已安排下一个零点任务，将在 " + nextDay + " 执行");
    }
    
    /**
     * 重启所有录制
     */
    private static void restartAllRecordings() {
        ISeeYouClient.LOGGER.info("日期变更，重启所有录制...");
        
        // 保存当前所有录制器的引用
        List<UUID> playersToRestart = new ArrayList<>(activeRecorders.keySet());
        List<String> camerasToRestart = new ArrayList<>(customRecorders.keySet());
        Map<String, ServerPlayerEntity> cameraPlayers = new HashMap<>();
        Map<String, Vec3d> cameraLocations = new HashMap<>();
        
        // 保存摄像机信息
        for (String name : camerasToRestart) {
            ReplayRecorder recorder = customRecorders.get(name);
            if (recorder != null) {
                cameraPlayers.put(name, recorder.getPlayer());
            }
        }
        
        // 停止所有录制
        stopAllRecordings();
        
        // 确保新日期文件夹存在
        String basePath = "recording/" + currentDateFolder + "/";
        File dir = new File(basePath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        // 重新开始玩家录制
        for (UUID uuid : playersToRestart) {
            ServerPlayerEntity player = ISeeYouClient.getServer().getPlayerManager().getPlayer(uuid);
            if (player != null && !player.isDisconnected()) {
                if (shouldRecordPlayer(player.getName().getString())) {
                    startRecording(player);
                }
            }
        }
        
        // 重新开始摄像机录制
        for (String name : camerasToRestart) {
            ServerPlayerEntity player = cameraPlayers.get(name);
            if (player != null) {
                Vec3d location = cameraLocations.get(name);
                if (location != null) {
                    startCustomRecordingAtLocation(player, name, location);
                } else {
                    startCustomRecording(player, name);
                }
            }
        }
        
        ISeeYouClient.LOGGER.info("重启录制完成");
    }
    
    /**
     * 设置配置
     */
    public static void setConfig(ModConfig config) {
        ReplayManager.config = config;
    }
    
    /**
     * 获取当前日期文件夹路径
     * 确保目录存在
     */
    private static String getCurrentDateFolderPath() {
        String basePath = "recording/" + currentDateFolder + "/";
        File dir = new File(basePath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return basePath;
    }
    
    /**
     * 根据玩家生成文件名
     */
    private static String generateFileName(ServerPlayerEntity player) {
        String playerName = player.getName().getString();
        String time = LocalDateTime.now().format(TIME_FORMATTER);
        
        // 计算该玩家今天的录制次数
        int count = 1;
        String basePath = getCurrentDateFolderPath();
        File dir = new File(basePath);
        if (dir.exists()) {
            String prefix = playerName + "_";
            File[] files = dir.listFiles((d, name) -> name.startsWith(prefix) && name.endsWith(".mcpr"));
            if (files != null) {
                count = files.length + 1;
            }
        }
        
        return playerName + "_" + time + "_" + count + ".mcpr";
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
            String recordPath = getCurrentDateFolderPath() + generateFileName(player);
            
            // 创建录制文件
            File recordFile = new File(recordPath);
            if (!recordFile.getParentFile().exists()) {
                recordFile.getParentFile().mkdirs();
            }
            
            // 创建录制器
            ReplayRecorder recorder = new ReplayRecorder(player, recordFile);
            activeRecorders.put(playerUUID, recorder);
            
            // 开始录制
            recorder.startRecording();
            
            ISeeYouClient.LOGGER.info("已开始录制玩家: " + player.getName().getString());
            return true;
        } catch (Exception e) {
            ISeeYouClient.LOGGER.error("开始录制玩家时出错: " + player.getName().getString(), e);
            return false;
        }
    }
    
    /**
     * 开始自定义录制
     */
    public static boolean startCustomRecording(ServerPlayerEntity player, String name) {
        if (player == null) {
            return false;
        }
        
        // 检查是否已经存在该名称的录制
        if (customRecorders.containsKey(name)) {
            ISeeYouClient.LOGGER.info("已存在同名录制: " + name);
            return false;
        }
        
        try {
            // 准备录制文件路径
            String time = LocalDateTime.now().format(TIME_FORMATTER);
            String recordPath = getCurrentDateFolderPath() + name + "_" + time + ".mcpr";
            
            // 创建录制文件
            File recordFile = new File(recordPath);
            if (!recordFile.getParentFile().exists()) {
                recordFile.getParentFile().mkdirs();
            }
            
            // 创建录制器
            ReplayRecorder recorder = new ReplayRecorder(player, recordFile);
            customRecorders.put(name, recorder);
            
            // 开始录制
            recorder.startRecording();
            
            ISeeYouClient.LOGGER.info("已开始自定义录制: " + name);
            return true;
        } catch (Exception e) {
            ISeeYouClient.LOGGER.error("开始自定义录制时出错: " + name, e);
            return false;
        }
    }
    
    /**
     * 在指定位置开始自定义录制
     */
    public static boolean startCustomRecordingAtLocation(ServerPlayerEntity player, String name, Vec3d location) {
        if (player == null) {
            return false;
        }
        
        // 检查是否已经存在该名称的录制
        if (customRecorders.containsKey(name)) {
            ISeeYouClient.LOGGER.info("已存在同名录制: " + name);
            return false;
        }
        
        try {
            // 准备录制文件路径
            String time = LocalDateTime.now().format(TIME_FORMATTER);
            String recordPath = getCurrentDateFolderPath() + name + "_" + time + ".mcpr";
            
            // 创建录制文件
            File recordFile = new File(recordPath);
            if (!recordFile.getParentFile().exists()) {
                recordFile.getParentFile().mkdirs();
            }
            
            // 创建录制器 - 未来可以修改为支持特定位置的录制
            ReplayRecorder recorder = new ReplayRecorder(player, recordFile);
            customRecorders.put(name, recorder);
            
            // 开始录制
            recorder.startRecording();
            
            ISeeYouClient.LOGGER.info("已在位置 (" + location.x + "," + location.y + "," + location.z + ") 开始自定义录制: " + name);
            return true;
        } catch (Exception e) {
            ISeeYouClient.LOGGER.error("开始自定义录制时出错: " + name, e);
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
     * 停止自定义录制
     */
    public static boolean stopCustomRecording(ServerPlayerEntity player, String name) {
        ReplayRecorder recorder = customRecorders.get(name);
        
        if (recorder == null) {
            ISeeYouClient.LOGGER.info("找不到自定义录制: " + name);
            return false;
        }
        
        try {
            // 停止录制
            recorder.stopRecording();
            customRecorders.remove(name);
            
            ISeeYouClient.LOGGER.info("已停止自定义录制: " + name);
            return true;
        } catch (Exception e) {
            ISeeYouClient.LOGGER.error("停止自定义录制时出错: " + name, e);
            return false;
        }
    }
    
    /**
     * 停止所有录制
     * 用于服务器关闭时
     */
    public static void stopAllRecordings() {
        ISeeYouClient.LOGGER.info("停止所有录制...");
        
        // 停止玩家录制
        for (Map.Entry<UUID, ReplayRecorder> entry : activeRecorders.entrySet()) {
            try {
                ReplayRecorder recorder = entry.getValue();
                recorder.stopRecording();
                ISeeYouClient.LOGGER.info("已停止录制玩家: " + entry.getKey());
            } catch (Exception e) {
                ISeeYouClient.LOGGER.error("停止录制玩家时出错: " + entry.getKey(), e);
            }
        }
        
        // 停止自定义录制
        for (Map.Entry<String, ReplayRecorder> entry : customRecorders.entrySet()) {
            try {
                ReplayRecorder recorder = entry.getValue();
                recorder.stopRecording();
                ISeeYouClient.LOGGER.info("已停止自定义录制: " + entry.getKey());
            } catch (Exception e) {
                ISeeYouClient.LOGGER.error("停止自定义录制时出错: " + entry.getKey(), e);
            }
        }
        
        activeRecorders.clear();
        customRecorders.clear();
    }
    
    /**
     * 检查即时回放功能是否启用
     */
    public static boolean isInstantReplayEnabled() {
        return config != null && config.isEnableInstantReplay();
    }
    
    /**
     * 保存玩家的即时回放
     */
    public static String saveInstantReplay(ServerPlayerEntity player) {
        if (player == null || !isInstantReplayEnabled()) {
            return null;
        }
        
        try {
            // 准备即时回放文件路径
            String playerName = player.getName().getString();
            String time = LocalDateTime.now().format(TIME_FORMATTER);
            String fileName = playerName + "_instant_" + time + ".mcpr";
            String filePath = getCurrentDateFolderPath() + fileName;
            
            // 检查即时回放缓存
            // 实现即时回放逻辑 - 这里需要根据实际情况完善
            
            ISeeYouClient.LOGGER.info("玩家 " + playerName + " 保存了即时回放");
            return filePath;
        } catch (Exception e) {
            ISeeYouClient.LOGGER.error("保存即时回放时出错", e);
            return null;
        }
    }
    
    /**
     * 获取正在录制的玩家数量
     */
    public static int getActiveRecordingsCount() {
        return activeRecorders.size() + customRecorders.size();
    }
    
    /**
     * 检查玩家是否正在被录制
     */
    public static boolean isRecording(PlayerEntity player) {
        return player != null && activeRecorders.containsKey(player.getUuid());
    }
    
    /**
     * 检查玩家是否应该被录制
     * 根据配置的黑白名单决定
     */
    private static boolean shouldRecordPlayer(String playerName) {
        if (config == null) {
            // 默认录制所有玩家
            return true;
        }
        
        if ("blacklist".equals(config.getRecordMode())) {
            // 黑名单模式：如果玩家在黑名单中，不录制
            for (String name : config.getBlacklist()) {
                if (name.equalsIgnoreCase(playerName)) {
                    return false;
                }
            }
            return true; // 不在黑名单中，可以录制
        } else {
            // 白名单模式：只有在白名单中的玩家才录制
            for (String name : config.getWhitelist()) {
                if (name.equalsIgnoreCase(playerName)) {
                    return true;
                }
            }
            return config.getWhitelist().length == 0; // 如果白名单为空，录制所有玩家
        }
    }
} 