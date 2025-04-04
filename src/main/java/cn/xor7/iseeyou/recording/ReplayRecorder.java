package cn.xor7.iseeyou.recording;

import cn.xor7.iseeyou.ISeeYouClient;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.SharedConstants;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 回放录制器
 * 为单个玩家提供录制功能，创建.mcpr格式文件
 */
public class ReplayRecorder {
    private final ServerPlayerEntity player;
    private final File recordFile;
    private final UUID recordId;
    private boolean isRecording = false;
    private long startTime;
    
    /**
     * 创建回放录制器
     * @param player 要录制的玩家
     * @param recordFile 录制文件
     */
    public ReplayRecorder(ServerPlayerEntity player, File recordFile) {
        this.player = player;
        this.recordFile = recordFile;
        this.recordId = UUID.randomUUID();
    }
    
    /**
     * 开始录制
     */
    public void startRecording() {
        if (isRecording) {
            return;
        }
        
        try {
            // 确保目录存在
            if (!recordFile.getParentFile().exists()) {
                recordFile.getParentFile().mkdirs();
            }
            
            // 记录开始时间
            startTime = System.currentTimeMillis();
            
            // 初始化MCPR文件结构
            initMcprFile();
            
            // 开始录制玩家行为
            startRecordingPlayerBehavior();
            
            isRecording = true;
            ISeeYouClient.LOGGER.info("开始录制玩家: " + player.getName().getString());
        } catch (Exception e) {
            ISeeYouClient.LOGGER.error("开始录制玩家时出错: " + player.getName().getString(), e);
        }
    }
    
    /**
     * 停止录制
     * @return 录制文件名
     */
    public String stopRecording() {
        if (!isRecording) {
            return null;
        }
        
        try {
            // 停止录制玩家行为
            stopRecordingPlayerBehavior();
            
            // 完成MCPR文件
            finalizeMcprFile();
            
            isRecording = false;
            ISeeYouClient.LOGGER.info("已停止录制玩家: " + player.getName().getString());
            return recordFile.getName();
        } catch (Exception e) {
            ISeeYouClient.LOGGER.error("停止录制玩家时出错: " + player.getName().getString(), e);
            return null;
        }
    }
    
    /**
     * 初始化MCPR文件结构
     */
    private void initMcprFile() throws Exception {
        // MCPR文件实际上是一个ZIP文件，包含元数据和录制数据
        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(recordFile))) {
            // 添加元数据文件
            ZipEntry metadataEntry = new ZipEntry("metaData.json");
            zipOut.putNextEntry(metadataEntry);
            
            // 创建基本元数据
            String serverName = ISeeYouClient.getServer().getName();
            String serverIP = "localhost"; // 对于服务器端录制这个不重要
            String playerName = player.getName().getString();
            int protocolVersion = SharedConstants.getProtocolVersion();
            String mcVersion = SharedConstants.getGameVersion().getName();
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            String recordingDate = sdf.format(new Date());
            
            String metadata = String.format(
                "{\"serverName\":\"%s\",\"serverIP\":\"%s\",\"generator\":\"ISeeYou Mod\",\"date\":%d," +
                "\"mcversion\":\"%s\",\"fileFormat\":\"MCPR\",\"fileFormatVersion\":14,\"protocol\":%d," +
                "\"duration\":0,\"singleplayer\":false,\"recordingDate\":\"%s\",\"recordingPlayer\":\"%s\"," +
                "\"replayVersion\":\"2.6.10\"}",
                serverName, serverIP, startTime, mcVersion, protocolVersion, recordingDate, playerName
            );
            
            zipOut.write(metadata.getBytes());
            zipOut.closeEntry();
            
            // 添加录制信息文件
            ZipEntry recordingEntry = new ZipEntry("recording.tmcpr");
            zipOut.putNextEntry(recordingEntry);
            zipOut.closeEntry();
            
            // 添加必要的播放标记文件
            ZipEntry markerEntry = new ZipEntry("markers.json");
            zipOut.putNextEntry(markerEntry);
            String markers = "[{\"name\":\"start\",\"time\":0,\"color\":16777215}]";
            zipOut.write(markers.getBytes());
            zipOut.closeEntry();
        }
    }
    
    /**
     * 开始录制玩家行为
     */
    private void startRecordingPlayerBehavior() {
        try {
            // 初始化Fabric事件监听器来记录玩家行为
            ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
                if (handler.getPlayer().getUuid().equals(player.getUuid())) {
                    ISeeYouClient.LOGGER.info("开始录制玩家加入事件: " + player.getName().getString());
                }
            });
            
            ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
                if (handler.getPlayer().getUuid().equals(player.getUuid())) {
                    ISeeYouClient.LOGGER.info("录制玩家断开连接: " + player.getName().getString());
                }
            });
            
            ServerTickEvents.END_SERVER_TICK.register(server -> {
                // 记录玩家位置和动作
                ISeeYouClient.LOGGER.debug("记录玩家位置: " + player.getPos() + " 朝向: " + player.getYaw() + "/" + player.getPitch());
            });
            
            ISeeYouClient.LOGGER.info("已注册Fabric事件监听器录制玩家行为");
        } catch (Exception e) {
            ISeeYouClient.LOGGER.error("开始录制玩家行为时出错", e);
        }
    }
    
    /**
     * 停止录制玩家行为
     */
    private void stopRecordingPlayerBehavior() {
        try {
            // 移除Fabric事件监听器
            ServerPlayConnectionEvents.JOIN.unregister(this.joinListener);
            ServerPlayConnectionEvents.DISCONNECT.unregister(this.disconnectListener);
            ServerTickEvents.END_SERVER_TICK.unregister(this.tickListener);
            
            ISeeYouClient.LOGGER.info("已移除所有Fabric事件监听器");
        } catch (Exception e) {
            ISeeYouClient.LOGGER.error("停止录制玩家行为时出错", e);
        }
    }
    
    /**
     * 完成MCPR文件
     */
    private void finalizeMcprFile() throws Exception {
        long endTime = System.currentTimeMillis();
        long duration = (endTime - startTime) / 1000; // 秒为单位的持续时间
        
        // 更新元数据中的录制时长
        // 这里需要重新打开ZIP文件，修改metaData.json
        try {
            // 实际录制可能需要更复杂的文件操作
            // 这里简化处理，只是确保文件已经写入磁盘
            Files.setLastModifiedTime(recordFile.toPath(), java.nio.file.attribute.FileTime.fromMillis(endTime));
            
            // TODO: 更新元数据中的duration值
            // 理想情况下，我们应该读取zip，修改metaData.json，然后重新写入
            // 但这需要完整的zip文件操作，这里省略
        } catch (IOException e) {
            ISeeYouClient.LOGGER.error("完成录制文件时出错", e);
        }
    }
    
    /**
     * 获取玩家
     */
    public ServerPlayerEntity getPlayer() {
        return player;
    }
    
    /**
     * 获取录制文件
     */
    public File getRecordFile() {
        return recordFile;
    }
    
    /**
     * 获取录制ID
     */
    public UUID getRecordId() {
        return recordId;
    }
    
    /**
     * 是否正在录制
     */
    public boolean isRecording() {
        return isRecording;
    }
}