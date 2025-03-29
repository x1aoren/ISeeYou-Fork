package cn.xor7.iseeyou.utils;

/**
 * ISeeYou模组的配置类
 * 存储与回放录制相关的各种设置
 */
public class ModConfig {
    private String recordingPath = "replay/recordings";
    private String instantReplayPath = "replay/instant";
    private boolean showNotifications = true;
    private boolean autoStart = true;
    private int replayMinutes = 5;
    private boolean enableInstantReplay = true;
    private String[] blacklist = new String[0];
    private String[] whitelist = new String[0];
    private String recordMode = "blacklist"; // 可选值: blacklist, whitelist
    
    public ModConfig() {
        // 默认配置
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
     * 是否显示通知
     */
    public boolean isShowNotifications() {
        return showNotifications;
    }
    
    /**
     * 设置是否显示通知
     */
    public void setShowNotifications(boolean showNotifications) {
        this.showNotifications = showNotifications;
    }
    
    /**
     * 是否在游戏启动时自动开始录制
     */
    public boolean isAutoStart() {
        return autoStart;
    }
    
    /**
     * 设置是否在游戏启动时自动开始录制
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
} 