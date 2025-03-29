package cn.xor7.iseeyou;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabric Mod主类
 */
public class ISeeYouClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("iseeyou");
    private static KeyBinding keyBinding;

    /**
     * 初始化模组
     */
    @Override
    public void onInitializeClient() {
        LOGGER.info("ISeeYou Mod初始化中...");
        
        // 注册按键绑定
        keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.iseeyou.instant_replay", // 翻译键
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R, // 默认为R键
                "category.iseeyou.general" // 翻译键
        ));

        // 注册按键事件
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (keyBinding.wasPressed()) {
                LOGGER.info("按键被按下，触发即时回放功能");
                // 触发即时回放功能
                // InstantReplayManager.replay(client.player);
            }
        });

        LOGGER.info("ISeeYou Mod初始化完成!");
    }
} 