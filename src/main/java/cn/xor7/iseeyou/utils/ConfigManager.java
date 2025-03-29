package cn.xor7.iseeyou.utils;

import cn.xor7.iseeyou.ISeeYouClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * 配置管理器类，用于处理Fabric模组的配置文件
 */
public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE = "config/iseeyou/config.json";
    private static ModConfig config;
    
    /**
     * 加载配置文件
     */
    public static ModConfig loadConfig() {
        File configFile = new File(CONFIG_FILE);
        
        // 如果配置文件存在，尝试从文件中加载
        if (configFile.exists()) {
            try (Reader reader = new FileReader(configFile, StandardCharsets.UTF_8)) {
                config = GSON.fromJson(reader, ModConfig.class);
                ISeeYouClient.LOGGER.info("配置文件已加载：" + CONFIG_FILE);
            } catch (Exception e) {
                ISeeYouClient.LOGGER.error("加载配置文件时出错：", e);
                config = new ModConfig(); // 加载默认配置
            }
        } else {
            // 如果配置文件不存在，创建默认配置
            config = new ModConfig();
            saveConfig();
            ISeeYouClient.LOGGER.info("已创建默认配置文件：" + CONFIG_FILE);
        }
        
        return config;
    }
    
    /**
     * 保存配置到文件
     */
    public static void saveConfig() {
        File configFile = new File(CONFIG_FILE);
        File parentDir = configFile.getParentFile();
        
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }
        
        try (Writer writer = new FileWriter(configFile, StandardCharsets.UTF_8)) {
            GSON.toJson(config, writer);
            ISeeYouClient.LOGGER.info("配置已保存到：" + CONFIG_FILE);
        } catch (IOException e) {
            ISeeYouClient.LOGGER.error("保存配置文件时出错：", e);
        }
    }
    
    /**
     * 获取当前配置
     */
    public static ModConfig getConfig() {
        if (config == null) {
            loadConfig();
        }
        return config;
    }
    
    /**
     * 重置配置为默认值
     */
    public static void resetConfig() {
        config = new ModConfig();
        saveConfig();
        ISeeYouClient.LOGGER.info("配置已重置为默认值");
    }
} 