package cn.xor7.iseeyou;

import cn.xor7.iseeyou.recording.ReplayManager;
import cn.xor7.iseeyou.utils.ConfigManager;
import cn.xor7.iseeyou.utils.ModConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ISeeYou Fabric Mod主类
 * 实现即时回放功能的客户端模组，集成ReplayMod功能
 */
public class ISeeYouClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("iseeyou");
    private static KeyBinding recordKeyBinding;
    private static KeyBinding instantReplayKeyBinding;
    public static final String MOD_ID = "iseeyou";
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static ModConfig config;
    
    /**
     * 初始化模组
     */
    @Override
    public void onInitializeClient() {
        LOGGER.info("ISeeYou Mod初始化中...");
        
        // 加载配置
        config = ConfigManager.loadConfig();
        
        // 确保录制目录存在
        File recordingDir = new File(config.getRecordingPath());
        if (!recordingDir.exists()) {
            recordingDir.mkdirs();
            LOGGER.info("创建录制目录: " + recordingDir.getAbsolutePath());
        }
        
        File instantDir = new File(config.getInstantReplayPath());
        if (!instantDir.exists()) {
            instantDir.mkdirs();
            LOGGER.info("创建即时回放目录: " + instantDir.getAbsolutePath());
        }
        
        // 初始化回放管理器
        ReplayManager.initialize();
        
        // 注册按键绑定 - 开始/停止录制
        recordKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.iseeyou.record", // 翻译键
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F8, // 默认为F8键
                "category.iseeyou.general" // 翻译键
        ));
        
        // 注册按键绑定 - 即时回放
        instantReplayKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.iseeyou.instant_replay", // 翻译键
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F9, // 默认为F9键
                "category.iseeyou.general" // 翻译键
        ));

        // 注册按键事件
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // 录制按键
            while (recordKeyBinding.wasPressed()) {
                handleRecordKeyPressed(client);
            }
            
            // 即时回放按键
            while (instantReplayKeyBinding.wasPressed()) {
                handleInstantReplayKeyPressed(client);
            }
        });
        
        // 如果配置了自动开始录制，则在游戏启动后自动开始录制
        if (config.isAutoStart() && ReplayManager.isAutoRecordEnabled()) {
            ClientTickEvents.END_CLIENT_TICK.register(new ClientTickEvents.EndTick() {
                private boolean initialized = false;
                
                @Override
                public void onEndTick(MinecraftClient client) {
                    // 确保玩家已加载并且只执行一次
                    if (!initialized && client.player != null) {
                        initialized = true;
                        // 启动录制
                        boolean success = ReplayManager.startRecording(client);
                        if (success) {
                            LOGGER.info("已自动开始录制");
                            if (config.isShowNotifications()) {
                                client.player.sendMessage(
                                    Text.translatable("message.iseeyou.recording_started"), 
                                    true
                                );
                            }
                        }
                    }
                }
            });
        }

        LOGGER.info("ISeeYou Mod初始化完成!");
    }
    
    /**
     * 处理录制按键事件
     */
    private void handleRecordKeyPressed(MinecraftClient client) {
        LOGGER.info("录制按键被按下");
        
        // 如果有玩家实体，才执行录制功能
        if (client.player != null) {
            if (ReplayManager.isRecording()) {
                // 如果正在录制，则停止并保存
                String fileName = ReplayManager.stopRecording(client);
                LOGGER.info("已停止录制并保存: " + fileName);
            } else {
                // 如果没在录制，则开始新录制
                boolean success = ReplayManager.startRecording(client);
                if (success) {
                    LOGGER.info("已开始新录制");
                    if (config.isShowNotifications()) {
                        client.player.sendMessage(
                            Text.translatable("message.iseeyou.recording_started"), 
                            true
                        );
                    }
                } else {
                    LOGGER.warn("无法开始录制");
                }
            }
        }
    }
    
    /**
     * 处理即时回放按键事件
     */
    private void handleInstantReplayKeyPressed(MinecraftClient client) {
        LOGGER.info("即时回放按键被按下");
        
        // 如果有玩家实体，才执行即时回放功能
        if (client.player != null && config.isEnableInstantReplay()) {
            boolean success = ReplayManager.createInstantReplay(client);
            if (success) {
                LOGGER.info("已创建即时回放");
                if (config.isShowNotifications()) {
                    client.player.sendMessage(
                        Text.translatable("message.iseeyou.instant_replay_saved"), 
                        true
                    );
                }
            } else {
                LOGGER.warn("无法创建即时回放");
            }
        }
    }
} 