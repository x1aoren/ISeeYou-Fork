package cn.xor7.iseeyou;

import cn.xor7.iseeyou.commands.InstantReplayCommand;
import cn.xor7.iseeyou.commands.PhotographerCommand;
import cn.xor7.iseeyou.recording.ReplayManager;
import cn.xor7.iseeyou.utils.ConfigManager;
import cn.xor7.iseeyou.utils.ModConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ISeeYou Fabric Mod主类
 * 实现服务器端玩家录制功能，记录玩家行为并生成回放文件
 */
public class ISeeYouClient implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("iseeyou");
    public static final String MOD_ID = "iseeyou";
    public static final String VERSION = "1.3.5"; // 模组版本
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static ModConfig config;
    private static MinecraftServer server;
    
    /**
     * 初始化模组
     */
    @Override
    public void onInitialize() {
        LOGGER.info("ISeeYou Mod初始化中...");
        
        // 加载配置
        config = ConfigManager.loadConfig();
        
        // 确保录制目录存在
        File recordingDir = new File("recording");
        if (!recordingDir.exists()) {
            recordingDir.mkdirs();
            LOGGER.info("创建录制目录: " + recordingDir.getAbsolutePath());
        }
        
        // 初始化回放管理器
        ReplayManager.initialize();
        ReplayManager.setConfig(config);
        
        // 注册命令
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            PhotographerCommand.register(dispatcher);
            InstantReplayCommand.register(dispatcher);
        });
        
        // 注册服务器生命周期事件
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            ISeeYouClient.server = server;
            LOGGER.info("服务器启动中，初始化录制系统...");
        });
        
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            LOGGER.info("服务器关闭中，停止所有录制...");
            ReplayManager.stopAllRecordings();
        });
        
        // 注册玩家连接事件
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            String playerName = handler.player.getName().getString();
            LOGGER.info("玩家加入: " + playerName);
            
            // 检查玩家是否在白名单/黑名单中
            if (shouldRecordPlayer(playerName)) {
                // 开始录制玩家
                boolean success = ReplayManager.startRecording(handler.player);
                if (success) {
                    LOGGER.info("开始录制玩家: " + playerName);
                    
                    // 注释掉用户消息通知，确保安全性
                    // 不向玩家发送录制开始消息
                    // 只有管理员需要收到通知
                    if (config.isNotifyAdmins()) {
                        sendMessageToAdmins(Text.translatable("message.iseeyou.recording_started_for", playerName));
                    }
                }
            } else {
                LOGGER.info("玩家" + playerName + "不在录制列表中，跳过录制");
            }
        });
        
        // 注册玩家断开连接事件
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            String playerName = handler.player.getName().getString();
            LOGGER.info("玩家断开连接: " + playerName);
            
            // 停止录制
            String fileName = ReplayManager.stopRecording(handler.player);
            if (fileName != null) {
                LOGGER.info("已停止录制玩家" + playerName + "，保存到: " + fileName);
                
                // 发送通知给管理员
                if (config.isNotifyAdmins()) {
                    sendMessageToAdmins(Text.translatable("message.iseeyou.recording_stopped_for", playerName, fileName));
                }
            }
        });

        LOGGER.info("ISeeYou Mod初始化完成!");
    }
    
    /**
     * 检查玩家是否应该被录制
     * 根据配置的黑白名单决定
     */
    private boolean shouldRecordPlayer(String playerName) {
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
    
    /**
     * 向所有管理员发送消息
     */
    private void sendMessageToAdmins(Text message) {
        if (server != null) {
            server.getPlayerManager().getPlayerList().forEach(player -> {
                if (player.hasPermissionLevel(2)) { // 检查是否是OP
                    player.sendMessage(message);
                }
            });
        }
    }
    
    /**
     * 获取服务器实例
     */
    public static MinecraftServer getServer() {
        return server;
    }
    
    /**
     * 获取模组版本
     */
    public static String getVersion() {
        return VERSION;
    }
} 