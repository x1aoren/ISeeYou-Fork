package cn.xor7.iseeyou.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * ISeeYou模组的配置类
 * 存储与回放录制相关的各种设置
 */
public class ModConfig {
    @Expose private String recordingPath = "replay/recordings";
    @Expose private String instantReplayPath = "replay/instant";
    @Expose private boolean autoStart = true;
    @Expose private int replayMinutes = 5;
    @Expose private boolean enableInstantReplay = true;
    @Expose private String[] blacklist = new String[0];
    @Expose private String[] whitelist = new String[0];
    @Expose private String recordMode = "blacklist"; // 可选值: blacklist, whitelist
    @Expose private boolean notifyAdmins = true; // 是否通知管理员录制状态
    @Expose private boolean recordChat = true; // 是否记录聊天信息
    @Expose private boolean saveOnPlayerQuit = true; // 玩家退出时是否保存回放
    @Expose private boolean autoCleanup = false; // 是否自动清理旧回放
    @Expose private int cleanupDays = 7; // 多少天前的回放会被清理
    
    private static final Gson GSON = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .setPrettyPrinting()
            .create();
    
    public ModConfig() {
        // 默认配置
    }
    
    /**
     * 从文件加载配置
     */
    public static ModConfig load(File file) {
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                return GSON.fromJson(reader, ModConfig.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new ModConfig();
    }
    
    /**
     * 保存配置到文件
     */
    public void save(File file) {
        try {
            File parent = file.getParentFile();
            if (!parent.exists()) {
                parent.mkdirs();
            }
            
            try (FileWriter writer = new FileWriter(file)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 获取录制保存路径
     */
    public String getRecordingPath() {
        return recordingPath;
    }
    
    /**
     * 设置录制保存路径
     */
    public void setRecordingPath(String recordingPath) {
        this.recordingPath = recordingPath;
    }
    
    /**
     * 获取即时回放保存路径
     */
    public String getInstantReplayPath() {
        return instantReplayPath;
    }
    
    /**
     * 设置即时回放保存路径
     */
    public void setInstantReplayPath(String instantReplayPath) {
        this.instantReplayPath = instantReplayPath;
    }
    
    /**
     * 是否在服务器启动时自动开始录制
     */
    public boolean isAutoStart() {
        return autoStart;
    }
    
    /**
     * 设置是否在服务器启动时自动开始录制
     */
    public void setAutoStart(boolean autoStart) {
        this.autoStart = autoStart;
    }
    
    /**
     * 获取即时回放的时长（分钟）
     */
    public int getReplayMinutes() {
        return replayMinutes;
    }
    
    /**
     * 设置即时回放的时长（分钟）
     */
    public void setReplayMinutes(int replayMinutes) {
        this.replayMinutes = replayMinutes;
    }
    
    /**
     * 是否启用即时回放功能
     */
    public boolean isEnableInstantReplay() {
        return enableInstantReplay;
    }
    
    /**
     * 设置是否启用即时回放功能
     */
    public void setEnableInstantReplay(boolean enableInstantReplay) {
        this.enableInstantReplay = enableInstantReplay;
    }
    
    /**
     * 获取黑名单
     */
    public String[] getBlacklist() {
        return blacklist;
    }
    
    /**
     * 设置黑名单
     */
    public void setBlacklist(String[] blacklist) {
        this.blacklist = blacklist;
    }
    
    /**
     * 获取白名单
     */
    public String[] getWhitelist() {
        return whitelist;
    }
    
    /**
     * 设置白名单
     */
    public void setWhitelist(String[] whitelist) {
        this.whitelist = whitelist;
    }
    
    /**
     * 获取录制模式（黑名单或白名单）
     */
    public String getRecordMode() {
        return recordMode;
    }
    
    /**
     * 设置录制模式（黑名单或白名单）
     */
    public void setRecordMode(String recordMode) {
        this.recordMode = recordMode;
    }
    
    /**
     * 是否通知管理员录制状态
     */
    public boolean isNotifyAdmins() {
        return notifyAdmins;
    }
    
    /**
     * 设置是否通知管理员录制状态
     */
    public void setNotifyAdmins(boolean notifyAdmins) {
        this.notifyAdmins = notifyAdmins;
    }
    
    /**
     * 是否记录聊天信息
     */
    public boolean isRecordChat() {
        return recordChat;
    }
    
    /**
     * 设置是否记录聊天信息
     */
    public void setRecordChat(boolean recordChat) {
        this.recordChat = recordChat;
    }
    
    /**
     * 玩家退出时是否保存回放
     */
    public boolean isSaveOnPlayerQuit() {
        return saveOnPlayerQuit;
    }
    
    /**
     * 设置玩家退出时是否保存回放
     */
    public void setSaveOnPlayerQuit(boolean saveOnPlayerQuit) {
        this.saveOnPlayerQuit = saveOnPlayerQuit;
    }
    
    /**
     * 是否自动清理旧回放
     */
    public boolean isAutoCleanup() {
        return autoCleanup;
    }
    
    /**
     * 设置是否自动清理旧回放
     */
    public void setAutoCleanup(boolean autoCleanup) {
        this.autoCleanup = autoCleanup;
    }
    
    /**
     * 获取清理天数
     */
    public int getCleanupDays() {
        return cleanupDays;
    }
    
    /**
     * 设置清理天数
     */
    public void setCleanupDays(int cleanupDays) {
        this.cleanupDays = cleanupDays;
    }
} 