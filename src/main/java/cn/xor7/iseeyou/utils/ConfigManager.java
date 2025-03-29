package cn.xor7.iseeyou.utils;

import cn.xor7.iseeyou.ISeeYouClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;

/**
 * 配置管理器
 * 负责加载和保存模组配置
 */
public class ConfigManager {
    private static final File CONFIG_FILE = new File(
            FabricLoader.getInstance().getConfigDir().toFile(),
            "iseeyou/config.json"
    );
    
    /**
     * 加载配置
     * 如果配置文件不存在，则创建默认配置
     */
    public static ModConfig loadConfig() {
        if (!CONFIG_FILE.exists()) {
            ISeeYouClient.LOGGER.info("配置文件不存在，创建默认配置");
            ModConfig defaultConfig = new ModConfig();
            saveConfig(defaultConfig);
            return defaultConfig;
        }
        
        try {
            ISeeYouClient.LOGGER.info("从" + CONFIG_FILE.getAbsolutePath() + "加载配置");
            return ModConfig.load(CONFIG_FILE);
        } catch (Exception e) {
            ISeeYouClient.LOGGER.error("加载配置失败，使用默认配置", e);
            return new ModConfig();
        }
    }
    
    /**
     * 保存配置
     */
    public static void saveConfig(ModConfig config) {
        try {
            CONFIG_FILE.getParentFile().mkdirs();
            config.save(CONFIG_FILE);
            ISeeYouClient.LOGGER.info("配置已保存到" + CONFIG_FILE.getAbsolutePath());
        } catch (Exception e) {
            ISeeYouClient.LOGGER.error("保存配置失败", e);
        }
    }
} 