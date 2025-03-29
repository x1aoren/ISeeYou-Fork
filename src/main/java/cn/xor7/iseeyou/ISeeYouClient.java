package cn.xor7.iseeyou;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ISeeYouClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("iseeyou");
    private static KeyBinding playRecordKeyBinding;

    @Override
    public void onInitializeClient() {
        LOGGER.info("ISeeYou 客户端初始化...");
        
        // 注册按键绑定
        playRecordKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.iseeyou.playrecord",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "category.iseeyou.general"
        ));
        
        // 注册按键事件监听器
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (playRecordKeyBinding.wasPressed()) {
                LOGGER.info("播放录像按键被按下");
                // 在这里实现录像播放逻辑
            }
        });
        
        LOGGER.info("ISeeYou 客户端初始化完成");
    }
} 