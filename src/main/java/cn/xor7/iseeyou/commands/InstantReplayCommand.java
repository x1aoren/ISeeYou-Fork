package cn.xor7.iseeyou.commands;

import cn.xor7.iseeyou.ISeeYouClient;
import cn.xor7.iseeyou.recording.ReplayManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * 即时回放命令处理类
 * 实现/instantreplay命令，允许玩家保存自己的即时回放记录
 */
public class InstantReplayCommand {
    
    /**
     * 注册命令
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal("instantreplay")
                .requires(ServerCommandSource::isExecutedByPlayer) // 只能由玩家执行
                .executes(InstantReplayCommand::createInstantReplay)
        );
    }
    
    /**
     * 为当前玩家创建即时回放
     */
    private static int createInstantReplay(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();
        
        // 检查即时回放功能是否已启用
        if (!ReplayManager.isInstantReplayEnabled()) {
            source.sendError(Text.literal("即时回放功能未启用，请在配置文件中启用"));
            return 0;
        }
        
        // 创建即时回放
        String filePath = ReplayManager.saveInstantReplay(player);
        
        if (filePath != null) {
            source.sendFeedback(() -> Text.literal("即时回放已保存: " + filePath), false);
            ISeeYouClient.LOGGER.info("玩家 {} 保存了即时回放: {}", player.getName().getString(), filePath);
            return 1;
        } else {
            source.sendError(Text.literal("保存即时回放失败"));
            return 0;
        }
    }
} 