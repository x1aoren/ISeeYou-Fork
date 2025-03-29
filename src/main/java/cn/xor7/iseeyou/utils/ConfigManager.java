package cn.xor7.iseeyou.utils;

import cn.xor7.iseeyou.ISeeYouClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 配置管理器类
 * 负责加载、保存和管理模组配置
 */
public class ConfigManager {
    private static final String CONFIG_FILE = "config/iseeyou.json";
    private static ModConfig config;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    /**
     * 加载配置文件
     * 如果配置文件不存在，则创建默认配置
     */
    public static ModConfig loadConfig() {
        if (config != null) {
            return config;
        }
        
        // 确保配置目录存在
        File configDir = new File("config");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        
        File configFile = new File(CONFIG_FILE);
        if (configFile.exists()) {
            // 加载现有配置
            try (FileReader reader = new FileReader(configFile)) {
                config = GSON.fromJson(reader, ModConfig.class);
                ISeeYouClient.LOGGER.info("已加载配置文件");
            } catch (IOException e) {
                ISeeYouClient.LOGGER.error("加载配置文件时出错", e);
                config = new ModConfig(); // 使用默认配置
            }
        } else {
            // 创建默认配置
            config = new ModConfig();
            saveConfig();
            ISeeYouClient.LOGGER.info("已创建默认配置文件");
        }
        
        // 确保录制目录存在
        ensureDirectoryExists(config.getRecordingPath());
        ensureDirectoryExists(config.getInstantReplayPath());
        
        return config;
    }
    
    /**
     * 保存配置到文件
     */
    public static void saveConfig() {
        if (config == null) {
            ISeeYouClient.LOGGER.warn("尝试保存空配置");
            return;
        }
        
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(config, writer);
            ISeeYouClient.LOGGER.info("配置已保存");
        } catch (IOException e) {
            ISeeYouClient.LOGGER.error("保存配置文件时出错", e);
        }
    }
    
    /**
     * 获取当前配置
     * 如果配置未加载，则加载配置
     */
    public static ModConfig getConfig() {
        if (config == null) {
            loadConfig();
        }
        return config;
    }
    
    /**
     * 确保指定的目录存在
     */
    private static void ensureDirectoryExists(String dirPath) {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            if (dir.mkdirs()) {
                ISeeYouClient.LOGGER.info("已创建目录: " + dirPath);
            } else {
                ISeeYouClient.LOGGER.warn("无法创建目录: " + dirPath);
            }
        }
    }
} 