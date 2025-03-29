package cn.xor7.iseeyou.utils;

/**
 * 模组配置类
 */
public class ModConfig {
    // 录制相关配置
    private String recordingPath = "replay/iseeyou";
    private boolean autoStart = true;
    private int replayDuration = 30; // 单位：秒
    
    // 界面配置
    private boolean showRecordingIndicator = true;
    private boolean showNotifications = true;
    
    // 性能配置
    private boolean highQualityRecording = true;
    
    /**
     * 获取录制文件保存路径
     */
    public String getRecordingPath() {
        return recordingPath;
    }
    
    /**
     * 设置录制文件保存路径
     */
    public void setRecordingPath(String recordingPath) {
        this.recordingPath = recordingPath;
    }
    
    /**
     * 是否自动开始录制
     */
    public boolean isAutoStart() {
        return autoStart;
    }
    
    /**
     * 设置是否自动开始录制
     */
    public void setAutoStart(boolean autoStart) {
        this.autoStart = autoStart;
    }
    
    /**
     * 获取回放持续时间（秒）
     */
    public int getReplayDuration() {
        return replayDuration;
    }
    
    /**
     * 设置回放持续时间（秒）
     */
    public void setReplayDuration(int replayDuration) {
        this.replayDuration = replayDuration;
    }
    
    /**
     * 是否显示录制指示器
     */
    public boolean isShowRecordingIndicator() {
        return showRecordingIndicator;
    }
    
    /**
     * 设置是否显示录制指示器
     */
    public void setShowRecordingIndicator(boolean showRecordingIndicator) {
        this.showRecordingIndicator = showRecordingIndicator;
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
     * 是否使用高质量录制
     */
    public boolean isHighQualityRecording() {
        return highQualityRecording;
    }
    
    /**
     * 设置是否使用高质量录制
     */
    public void setHighQualityRecording(boolean highQualityRecording) {
        this.highQualityRecording = highQualityRecording;
    }
} 